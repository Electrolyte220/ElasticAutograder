package com.autograder.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.dto.GraderOptionResponse;
import com.autograder.repository.JobRepository;
import com.autograder.service.Fabric8GradingOrchestrator;
import com.autograder.service.GraderRegistry;
import com.autograder.service.GradingOrchestrator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

// this must be changed in prod
@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
public class JobController {

    private static final Path UPLOAD_ROOT = Path.of("grading/uploads");

    private final JobRepository jobRepository;
    private final GradingOrchestrator gradingOrchestrator;
    private final ObjectMapper objectMapper;
    private final Fabric8GradingOrchestrator fabric8GradingOrchestrator;
    private final GraderRegistry graderRegistry;

    public JobController(JobRepository jobRepository,
                     GradingOrchestrator gradingOrchestrator,
                     Fabric8GradingOrchestrator fabric8GradingOrchestrator,
                     GraderRegistry gradingRegistry) {
    this.jobRepository = jobRepository;
    this.gradingOrchestrator = gradingOrchestrator;
    this.fabric8GradingOrchestrator = fabric8GradingOrchestrator;
    this.objectMapper = new ObjectMapper();
    this.graderRegistry = gradingRegistry;
    }

    /**
     * Uploads file to the server, saves it to a local staging folder,
     * creates a Job object, and saves it to the database.
     *
     * @param file submission file to upload
     * @return map of job id + response body, or error
     */
    @PostMapping("/jobs/upload")
    public ResponseEntity<Map<Long, String>> uploadFile(
        @RequestParam MultipartFile file,
        @RequestParam String graderType) {
        if(file.getContentType().equals("application/zip")) {
            try(ZipInputStream stream = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while((entry = stream.getNextEntry()) != null) {
                    File zipEntry = new File(UPLOAD_ROOT + "/" + entry.getName());
                    if(zipEntry.isDirectory()) {
                        zipEntry.mkdirs();
                    } else {
                        Files.copy(stream, zipEntry.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    stream.closeEntry();
                }
            } catch(IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        -1L, "Unable to read zip contents."));
            }
        } else {
            try {
                if(!Files.exists(UPLOAD_ROOT)) {
                    Files.createDirectories(UPLOAD_ROOT);
                }
                String originalFileName = file.getOriginalFilename();
                String fileName = sanitizeFileName(originalFileName);
                Path filePath = UPLOAD_ROOT.resolve(fileName).normalize();
                if(Files.exists(filePath)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(-1L, "File with this name already exists."));
                }
                Files.write(filePath, file.getBytes());
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(-1L, "Failed to save uploaded file."));
            }
        }
        try(Stream<Path> paths = Files.walk(UPLOAD_ROOT)) {
            Map<Long, String> results = new HashMap<>();
            List<Path> pathList = paths.filter(Files::isRegularFile).toList();

            for(Path path : pathList) {
                File uploadedFile = new File(path.toUri());
                String fileName = uploadedFile.getName();
                Pair<HttpStatus, Pair<Long, String>> response = createJob(uploadedFile, graderType, fileName);
                if(response.getFirst().equals(HttpStatus.OK)) {
                    results.put(response.getSecond().getFirst(), response.getSecond().getSecond());
                } else {
                    return ResponseEntity.status(response.getFirst())
                            .body(Map.of(response.getSecond().getFirst(), response.getSecond().getSecond()));
                }
            }
            return ResponseEntity.ok(results);
        } catch(IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    -1L, "Unable to read uploaded files."));
        }
    }

    @PostMapping("jobs/from-id")
    public ResponseEntity<String> getFileNameFromId(@RequestParam long id) {
        try {
            Job job = jobRepository.getReferenceById(id);
            String fileName = job.getOriginalFilename();
            return ResponseEntity.ok(fileName);
        } catch(EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job with id:" + id + " not found.");
        }
    }

    /**
     * Creates a new {@link Job} object for input parameters.
     * @param file File to create a job for.
     * @param graderType Grader type to run job against.
     * @param originalFileName Actual file name to run the job on.
     * @return pair of {@link HttpStatus} & pair of job id + status message
     */
    private @NonNull Pair<HttpStatus, Pair<Long, String>> createJob(File file, String graderType, String originalFileName) {
        String fileName = sanitizeFileName(originalFileName);
        String cleanedGraderType = graderType.trim();

        graderRegistry.getRequired(cleanedGraderType);

        try {
            // TODO: make grader type dynamic later
            Job job = new Job(fileName, cleanedGraderType, OffsetDateTime.now(), JobStatus.QUEUED);
            jobRepository.save(job);

            return Pair.of(HttpStatus.OK, Pair.of(job.getId(), "Successfully uploaded file."));
        } catch (IllegalArgumentException e) {
            return Pair.of(HttpStatus.BAD_REQUEST, Pair.of(-1L, e.getMessage()));
        }
    }

    /**
     * Runs a single uploaded job through Kubernetes using kubectl.
     * Updates job timestamps/status before and after execution.
     *
     * @param id       ID of job to run
     * @param fileName raw request body containing file name
     * @return grader JSON result
     */
    @PostMapping("/jobs/run/{id}")
    public ResponseEntity<JsonNode> runJob(@PathVariable Long id, @RequestBody String fileName) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        if (jobEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StringNode("Unable to find job object for id " + id));
        }

        Job job = jobEntity.get();

        try {
            String cleanedFileName = sanitizeFileName(fileName);

            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            JsonNode result = gradingOrchestrator.runJobInKubernetes(id, cleanedFileName, job.getGraderType());

            applyJobResults(job, result);
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StringNode(e.getMessage()));

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StringNode(e.getMessage()));
        }
    }

    @GetMapping("/jobs/recent")
    public ResponseEntity<List<Job>> getRecentJobs() {
        return ResponseEntity.ok(jobRepository.findAllOrderByCreatedAtDesc());
    }

    /**
     * Gets list of {@link Job} (likely from zip) submissions. Will also sanitize inputs as a sanity check.
     * @param minId Min id to start from
     * @param maxId Max id to stop at
     * @return List of jobs in the provided range
     */
    @GetMapping("jobs/multi-submission")
    public ResponseEntity<List<Job>> getJobsInRange(@RequestParam("from") long minId, @RequestParam("to") long maxId) {
        long min = minId;
        long max = maxId;
        if(min > max) {
            long temp = min;
            min = max;
            max = temp;
        }
        if(min < 0) min = 0;
        long latestId = jobRepository.findAllOrderByIdAsc().get((int) jobRepository.count() -1).getId();
        if(max > latestId) max = latestId;
        return ResponseEntity.ok(jobRepository.selectAllInRange(min, max));
    }

    /**
     * Optional callback endpoint if you later choose a push-based result model.
     * Not required for the current Phase 1 backend-pulls-logs approach.
     */
    @PostMapping("/jobs/{id}/callback")
    public ResponseEntity<String> updateJob(@PathVariable Long id, @RequestBody JsonNode jobResults) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        if (jobEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Unable to find existing job with id " + id);
        }

        try {
            Job job = jobEntity.get();
            applyJobResults(job, jobResults);
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.ok("Successfully updated job.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update job: " + e.getMessage());
        }
    }

    /**
     * Removes a staged uploaded file.
     *
     * @param fileName raw request body containing file name
     * @return OK/Error if file could/could not be deleted
     */
    @DeleteMapping("/files/remove")
    public ResponseEntity<String> removeFile(@RequestBody String fileName) {
        try {
            String cleanedFileName = sanitizeFileName(fileName);
            Path filePath = UPLOAD_ROOT.resolve(cleanedFileName).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
            }

            Files.delete(filePath);
            return ResponseEntity.ok("Successfully deleted file.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to delete file.");
        }
    }

    @GetMapping("/jobs/result/{id}")
    public ResponseEntity<String> downloadResults(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean fromTable) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        if (jobEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Unable to find job with id: " + id);
        }

        Job job = jobEntity.get();
        if (job.getResultJson() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Unable to get results for id: " + id);
        }

        JsonNode resultJson = objectMapper.readTree(job.getResultJson());
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if(fromTable) {
            headers.setContentDispositionFormData("attachment", "results.json");
        }
        return new ResponseEntity<>(prettyJson, headers, HttpStatus.OK);
    }

    private void applyJobResults(Job job, JsonNode jobResults) {
        if (jobResults == null || jobResults.get("status") == null) {
            throw new IllegalArgumentException("Grader result is missing required field: status");
        }

        job.setStatus(parseJobStatus(jobResults.get("status").asString()));

        if (jobResults.has("tests_passed")) {
            job.setTestsPassed(jobResults.get("tests_passed").asInt());
        }

        if (jobResults.has("tests_total")) {
            job.setTestsTotal(jobResults.get("tests_total").asInt());
        }

        if (jobResults.has("score") && !jobResults.get("score").isNull()) {
            job.setScore(jobResults.get("score").decimalValue());
        } else {
            job.setScore(BigDecimal.ZERO);
        }

        if (jobResults.has("error_message") && !jobResults.get("error_message").isNull()) {
            job.setErrorMessage(jobResults.get("error_message").asString());
        } else {
            job.setErrorMessage(null);
        }

        if (jobResults.has("results")) {
            job.setResultJson(objectMapper.writeValueAsString(jobResults.get("results")));
        }
    }

    private JobStatus parseJobStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new IllegalArgumentException("Job status is missing.");
        }

        String normalized = rawStatus.trim().toUpperCase();

        // If your grader returns SUCCEEDED/FAILED, this only works
        // if your JobStatus enum contains those exact values.
        return JobStatus.valueOf(normalized);
    }

    private String sanitizeFileName(String rawFileName) {
        if (rawFileName == null) {
            throw new IllegalArgumentException("File name is required.");
        }

        String cleaned = rawFileName.trim();

        // Common case when frontend sends JSON string body like: "submission.py"
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

    @GetMapping("/graders")
    public ResponseEntity<List<GraderOptionResponse>> getGraders() {
        List<GraderOptionResponse> graders = graderRegistry.getAll().stream()
                .map(grader -> new GraderOptionResponse(
                        grader.getKey(),
                        grader.getLabel()
                ))
                .toList();

        return ResponseEntity.ok(graders);
    }

}