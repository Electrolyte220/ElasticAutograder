package com.autograder.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// temp added for testing kubectl approach, will be removed once we have a working implementation 
// w only Fabric8  

@Service
public class KubectlGradingOrchestrator implements GradingOrchestrator {

    private static final Path UPLOAD_ROOT = Path.of("grading/uploads");
    private static final String GRADER_IMAGE = "ea-grader-fibbonaci:v1";
    private static final String MANIFEST_PATH_IN_IMAGE = "/app/grader/manifest.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode runJobInKubernetes(Long jobId, String fileName) throws Exception {
      String cleanedFileName = sanitizeFileName(fileName);
      Path submissionPath = UPLOAD_ROOT.resolve(cleanedFileName).normalize();

      if (!Files.exists(submissionPath)) {
          throw new IllegalArgumentException("Submission file not found: " + cleanedFileName);
      }

      String configMapName = "submission-job-" + jobId;
      String jobName = "grading-job-" + jobId;

      Path jobYamlPath = createJobYaml(jobId, jobName, configMapName);

      try {
          deleteIfExists(configMapName, jobName);

          runCommand(List.of(
                  "kubectl", "create", "configmap", configMapName,
                  "--from-file=submission.py=" + submissionPath.toAbsolutePath()
          ));

          runCommand(List.of(
                  "kubectl", "apply", "-f", jobYamlPath.toAbsolutePath().toString()
          ));

          runCommand(List.of(
                  "kubectl", "wait",
                  "--for=condition=complete",
                  "job/" + jobName,
                  "--timeout=60s"
          ));

          String logs = runCommand(List.of(
                  "kubectl", "logs", "job/" + jobName
          ));

          return objectMapper.readTree(logs);

      } finally {
          try {
              runCommand(List.of(
                      "kubectl", "delete", "configmap", configMapName, "--ignore-not-found=true"
              ));
          } catch (Exception ignored) {
          }

          Files.deleteIfExists(jobYamlPath);
      }
  }

    private void deleteIfExists(String configMapName, String jobName) {
        try {
            runCommand(List.of(
                    "kubectl", "delete", "job", jobName, "--ignore-not-found=true"
            ));
        } catch (Exception ignored) {
        }

        try {
            runCommand(List.of(
                    "kubectl", "delete", "configmap", configMapName, "--ignore-not-found=true"
            ));
        } catch (Exception ignored) {
        }
    }

    private Path createJobYaml(Long jobId, String jobName, String configMapName) throws IOException {
        String yaml =
            "apiVersion: batch/v1\n" +  
            "kind: Job\n" +
            "metadata:\n" +
            "  name: " + jobName + "\n" +
            "  labels:\n" +
            "    app: elastic-autograder\n" +
            "    job-id: \"" + jobId + "\"\n" +
            "spec:\n" +
            "  ttlSecondsAfterFinished: 300\n" +
            "  backoffLimit: 0\n" +
            "  template:\n" +
            "    metadata:\n" +
            "      labels:\n" +
            "        app: elastic-autograder\n" +
            "        job-id: \"" + jobId + "\"\n" +
            "    spec:\n" +
            "      restartPolicy: Never\n" +
            "      containers:\n" +
            "        - name: grader\n" +
            "          image: " + GRADER_IMAGE + "\n" +
            "          imagePullPolicy: IfNotPresent\n" +
            "          command: [\"python\", \"/app/main.py\"]\n" +
            "          args: [\"/work/submission.py\", \"" + MANIFEST_PATH_IN_IMAGE + "\"]\n" +
            "          volumeMounts:\n" +
            "            - name: submission-volume\n" +
            "              mountPath: /work\n" +
            "      volumes:\n" +
            "        - name: submission-volume\n" +
            "          configMap:\n" +
            "            name: " + configMapName + "\n";

        Path tempFile = Files.createTempFile("grading-job-", ".yaml");
        Files.writeString(tempFile, yaml);
        return tempFile;
    }

    private String runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(
                    "Command failed: " + String.join(" ", command) + "\n" + output
            );
        }

        return output.toString();
    }

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
}