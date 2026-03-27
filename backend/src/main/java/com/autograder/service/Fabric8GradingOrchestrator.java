package com.autograder.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.autograder.model.GraderDefinition;

@Primary
@Service
public class Fabric8GradingOrchestrator implements GradingOrchestrator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path UPLOAD_ROOT = Path.of("grading/uploads");
    private static final String NAMESPACE = "default";
    private static final String GRADER_IMAGE = "ea-grader-fibbonaci:v1";
    private static final String MANIFEST_PATH_IN_IMAGE = "/app/grader/manifest.json";

    private final KubernetesClient kubernetesClient;
    private final GraderRegistry graderRegistry;

    public Fabric8GradingOrchestrator(KubernetesClient kubernetesClient, GraderRegistry graderRegistry) {
        this.kubernetesClient = kubernetesClient;
        this.graderRegistry = graderRegistry;
    }

    @Override
    public JsonNode runJobInKubernetes(Long jobId, String fileName, String graderType) throws Exception {
        if (graderType == null || graderType.isBlank()) {
            throw new IllegalArgumentException("graderType is required.");
        }
        GraderDefinition grader = graderRegistry.getRequired(graderType);

        String cleanedFileName = sanitizeFileName(fileName);
        String configMapName = "submission-job-" + jobId;

        try{
            createSubmissionConfigMap(jobId, cleanedFileName);
            createGradingJob(jobId,grader);
            waitForJobCompletion(jobId, 60);
            String logs = getJobLogs(jobId);

            return objectMapper.readTree(logs);
        } 
        // add catch error handling pls - we can be more specific with exception types and error messages as needed
        catch (Exception err) {
            throw new RuntimeException(
                "Grading job " + jobId + " failed for grader '" + graderType + "': " + err.getMessage(),
                err
            );
        }
        finally {
            try{
                deleteSubmissionConfigMap(configMapName);
            } catch (Exception e) {
                // Log the error but don't fail the whole process if cleanup fails
                System.err.println("Failed to delete ConfigMap " + configMapName + ": " + e.getMessage());
            }
        }
    }

    public ConfigMap createSubmissionConfigMap(Long jobId, String fileName) throws Exception {
        String cleanedFileName = sanitizeFileName(fileName);
        Path submissionPath = UPLOAD_ROOT.resolve(cleanedFileName).normalize();

        if (!Files.exists(submissionPath)) {
            throw new IllegalArgumentException("Submission file not found: " + cleanedFileName);
        }

        String submissionContents = Files.readString(submissionPath);
        String configMapName = "submission-job-" + jobId;

        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(configMapName)
                    .addToLabels("app", "elastic-autograder")
                    .addToLabels("job-id", String.valueOf(jobId))
                .endMetadata()
                .addToData("submission.py", submissionContents)
                .build();

        return kubernetesClient.configMaps()
                .inNamespace(NAMESPACE)
                .resource(configMap)
                .createOrReplace();
    }

    public Job createGradingJob(Long jobId, GraderDefinition grader) {
        String jobName = "grading-job-" + jobId;
        String configMapName = "submission-job-" + jobId;

        // important note for understanding this, this basically is just a yaml file so it'll look crazy unless you understand k8s yaml structure
        // Template is under backend/grading/graders if you want a reference to how this should look in yaml format but this as pretty as it gets
        // for using k8s :D 
        Job job = new JobBuilder()
                .withNewMetadata()
                    .withName(jobName)
                    .addToLabels("app", "elastic-autograder")
                    .addToLabels("job-id", String.valueOf(jobId))
                .endMetadata()
                .withNewSpec()
                    .withTtlSecondsAfterFinished(300)
                    .withBackoffLimit(0)
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "elastic-autograder")
                            .addToLabels("job-id", String.valueOf(jobId))
                        .endMetadata()
                        .withNewSpec()
                            .withRestartPolicy("Never")
                            .addNewContainer()
                                .withName("grader")
                                .withImage(grader.getImageName())
                                .withImagePullPolicy("IfNotPresent")
                                .withCommand("python", "/app/main.py")
                                .withArgs("/work/submission.py", grader.getManifestPath())
                                .addNewVolumeMount()
                                    .withName("submission-volume")
                                    .withMountPath("/work")
                                .endVolumeMount()
                            .endContainer()
                            .addNewVolume()
                                .withName("submission-volume")
                                .withNewConfigMap()
                                    .withName(configMapName)
                                .endConfigMap()
                            .endVolume()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

        // IMPORTANT NOTE: this assumes default namespace for k8s cluster is left defaulted NOT empty, this can cause issues 
        return kubernetesClient.batch().v1().jobs()
                .inNamespace(NAMESPACE)
                .resource(job)
                .createOrReplace();
    }

    public Job waitForJobCompletion(Long jobId, long timeoutSeconds) throws Exception {
        String jobName = "grading-job-" + jobId;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);

        while (System.currentTimeMillis() < deadline) {
            Job job = kubernetesClient.batch()
                    .v1()
                    .jobs()
                    .inNamespace(NAMESPACE)
                    .withName(jobName)
                    .get();

            if (job == null) {
                throw new IllegalStateException("Job not found while waiting: " + jobName);
            }

            if (job.getStatus() != null) {
                Integer succeeded = job.getStatus().getSucceeded();
                Integer failed = job.getStatus().getFailed();

                if (succeeded != null && succeeded > 0) {
                    return job;
                }

                if (failed != null && failed > 0) {
                    throw new IllegalStateException("Job failed: " + jobName);
                }
            }

            // move around sleep value depending on your preference
            // alternatively punish user/developer using this by changing to 20000000000
            Thread.sleep(1000);
        }

        throw new IllegalStateException("Timed out waiting for job completion: " + jobName);
    }

    public String getJobLogs(Long jobId) throws Exception {
        String jobIdLabel = String.valueOf(jobId);

        PodList podList = kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withLabel("app", "elastic-autograder")
                .withLabel("job-id", jobIdLabel)
                .list();

        if (podList == null || podList.getItems() == null || podList.getItems().isEmpty()) {
            throw new IllegalStateException("No pod found for job-id=" + jobIdLabel);
        }

        Pod pod = podList.getItems().get(0);

        if (pod.getMetadata() == null || pod.getMetadata().getName() == null) {
            throw new IllegalStateException("Pod metadata/name missing for job-id=" + jobIdLabel);
        }

        String podName = pod.getMetadata().getName();

        String logs = kubernetesClient.pods()
                .inNamespace(NAMESPACE)
                .withName(podName)
                .getLog();

        if (logs == null || logs.isBlank()) {
            throw new IllegalStateException("Empty logs returned for pod " + podName);
        }

        return logs;
    }

    // Additional helper methods for cleanup, etc. can be added below

    // basic sanitization to prevent issues can be enhanced as needed
    private String sanitizeFileName(String rawFileName) {
        if (rawFileName == null) {
            throw new IllegalArgumentException("File name is required.");
        }

        String cleaned = rawFileName.trim();

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }

        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        return cleaned;
    }

    // Cleanup method to delete ConfigMap and Job after completion
    // alternatively we COULD overload this to accept jobId, but eh we'll see if we need it first
    public void deleteSubmissionConfigMap(String configMapName) {
        kubernetesClient.configMaps()
                .inNamespace(NAMESPACE)
                .withName(configMapName)
                .delete();
    }
}