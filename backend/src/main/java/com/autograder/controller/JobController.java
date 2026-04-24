package com.autograder.controller;

import java.io.File;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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

import com.autograder.model.FailureReason;
import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.dto.GraderOptionResponse;
import com.autograder.repository.JobRepository;
import com.autograder.service.Fabric8GradingOrchestrator;
import com.autograder.service.GraderRegistry;
import com.autograder.service.GradingOrchestrator;
import com.autograder.service.GradingFailureException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.StringNode;

/**
 * Main REST controller for job submission, execution, result retrieval,
 * file cleanup, and grader option lookup.
 *
 * This controller handles the full basic workflow:
 * 1. Upload a submission file
 * 2. Create a Job record
 * 3. Run the job through the grading orchestrator
 * 4. Store results/failure details in the database
 * 5. Return job history and downloadable results to the frontend
 */
@RestController
@RequestMapping("/api")
public class JobController {

    // this is where submissions are stored
    private static final Path UPLOAD_ROOT = Path.of("grading/uploads");

    private final JobRepository jobRepository;
    private final GradingOrchestrator gradingOrchestrator;
    private final ObjectMapper objectMapper;
    private final Fabric8GradingOrchestrator fabric8GradingOrchestrator;
    private final GraderRegistry graderRegistry;

    /**
     * Constructs the controller with the required repository and service dependencies.
     *
     * @param jobRepository database access for Job records
     * @param gradingOrchestrator main grading runner abstraction
     * @param fabric8GradingOrchestrator Fabric8-based Kubernetes orchestrator
     * @param gradingRegistry registry of supported graders loaded from config
     */
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
     * Uploads a submission file to the local staging folder and creates
     * a corresponding Job row in the database.
     *
     * Flow:
     * - sanitize file name
     * - verify grader exists
     * - create upload directory if needed
     * - reject duplicate file names
     * - write file to disk
     * - create queued Job entry
     *
     * @param file uploaded submission file
     * @param graderType selected grader key from the frontend
     * @return message + job id on success, or an error response
     */
    @PostMapping("/jobs/upload")
    public ResponseEntity<Map<Long, String>> uploadFile(
        @RequestParam MultipartFile file,
        @RequestParam String graderType) {
        String cleanedGraderType = graderType.trim();
        graderRegistry.getRequired(cleanedGraderType);

        if(file.getContentType().equals("application/zip")) {
            try(ZipInputStream stream = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while((entry = stream.getNextEntry()) != null) {
                    String fileName = entry.getName().substring(entry.getName().lastIndexOf("/") + 1);
                    File zipEntry = new File(UPLOAD_ROOT + "/" + fileName);
                    if(entry.isDirectory()) {
                        continue;
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
                String sanitizedFileName = sanitizeFileName(uploadedFile.getName());
                Pair<HttpStatus, Pair<Long, String>> response = createJob(uploadedFile, cleanedGraderType, sanitizedFileName);
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
     * @param cleanedGraderType Grader type to run job against.
     * @param sanitizedFileName Actual file name to run the job on.
     * @return pair of {@link HttpStatus} & pair of job id + status message
     */
    private @NonNull Pair<HttpStatus, Pair<Long, String>> createJob(File file, String cleanedGraderType, String sanitizedFileName) {
        try {
            Job job = new Job(sanitizedFileName, cleanedGraderType, OffsetDateTime.now(), JobStatus.QUEUED);
            jobRepository.save(job);

            return Pair.of(HttpStatus.OK, Pair.of(job.getId(), "Successfully uploaded file."));
        } catch (IllegalArgumentException e) {
            return Pair.of(HttpStatus.BAD_REQUEST, Pair.of(-1L, e.getMessage()));
        }
    }

    @PostMapping("jobs/run-all")
    public ResponseEntity<List<JsonNode>> runAllJobs(@RequestParam long minId, @RequestParam long maxId) {
        List<CompletableFuture<Pair<Long, JsonNode>>> responses = new ArrayList<>();
        for(long id = minId; id <= maxId; ++id) {
            long finalId = id;
            String fileName = jobRepository.getReferenceById(finalId).getOriginalFilename();
            CompletableFuture<Pair<Long, JsonNode>> result = CompletableFuture.supplyAsync(() -> {
                try {
                    return Pair.of(finalId, runJob(finalId, fileName).getBody());
                } catch (EntityNotFoundException e) {
                    return Pair.of(finalId, new StringNode("Unable to find job for id: " + finalId));
                }
            });
            responses.add(result);
        }
        var results = responses.stream().map(CompletableFuture::join).toList();
        results.forEach(pair -> {
            try {
                Optional<Job> jobEntity = jobRepository.findById(pair.getFirst());
                if (jobEntity.isPresent()) {
                    Job job = jobEntity.get();
                    applyJobResults(job, pair.getSecond());
                    job.setUpdatedAt(OffsetDateTime.now());
                    jobRepository.saveAndFlush(job);
                }
            } catch(EntityNotFoundException ignored) {}
        });
        return ResponseEntity.ok(results.stream().map(Pair::getSecond).toList());
    }

    /**
     * Runs a staged submission through the grading pipeline.
     *
     * This method:
     * - loads the existing Job row
     * - marks it RUNNING
     * - invokes the grading orchestrator
     * - stores success or failure details back into the database
     *
     * @param id id of the Job row to run
     * @param fileName raw request body containing the uploaded file name
     * @return grader result JSON on success, or an error body on failure
     */
    @PostMapping("/jobs/run/{id}")
    public ResponseEntity<JsonNode> runJob(@PathVariable Long id, @RequestBody String fileName) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        if (jobEntity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StringNode("Unable to find job object for id " + id));
        }

        Job job = jobEntity.get();
        String cleanedFileName = null;

        try {
            cleanedFileName = sanitizeFileName(fileName);

            // marks job as running, removing any other states
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            job.setFailureReason(FailureReason.NONE);
            job.setFailureMessage(null);
            jobRepository.saveAndFlush(job);

            // executes the submission via the kubernetes orchestrator
            JsonNode result = gradingOrchestrator.runJobInKubernetes(id, cleanedFileName, job.getGraderType());

            // persists grading outputs back into the Job row
            applyJobResults(job, result);
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            job.setFailureReason(FailureReason.NONE);
            job.setFailureMessage(null);
            jobRepository.saveAndFlush(job);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // Input/config-level problem such as missing file name or invalid grader.
            job.setStatus(JobStatus.FAILED);
            job.setFailureReason(FailureReason.CONFIG_ERROR);
            job.setFailureMessage(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StringNode(e.getMessage()));

        } catch (GradingFailureException e) {
            // Structured grading failure from the orchestrator, such as timeout/resource failure.
            job.setStatus(JobStatus.FAILED);
            job.setFailureReason(e.getFailureReason());
            job.setFailureMessage(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StringNode(e.getMessage()));

        } catch (Exception e) {
            // Fallback for any unexpected backend/runtime error.
            job.setStatus(JobStatus.FAILED);
            job.setFailureReason(FailureReason.UNKNOWN);
            job.setFailureMessage(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StringNode(e.getMessage()));
        } finally {
            if (cleanedFileName != null) {
                try {
                    Path filePath = UPLOAD_ROOT.resolve(cleanedFileName).normalize();
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Failed to delete staged upload file '" + cleanedFileName + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Returns recent jobs ordered by creation time descending.
     *
     * @return list of recent Job rows for the frontend jobs table
     */
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
     * Deletes a staged uploaded submission file from the local uploads folder.
     *
     * This is currently the controller method responsible for removing the
     * uploaded submission file itself. It is manual, meaning it only runs
     * when the frontend explicitly calls this endpoint.
     *
     * This is kept for manual deletions to be used for admins and other use
     * cases.
     *
     * @param fileName raw request body containing the file name to remove
     * @return OK/Error response depending on whether deletion succeeded
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

    /**
     * Returns the stored result JSON for a given job.
     *
     * This is used by the frontend to download a results file or inspect
     * pretty-printed result JSON directly.
     *
     * @param id job id whose results should be returned
     * @param fromTable whether the response should include an attachment header
     * @return JSON result body or a not-found error
     */
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

    /**
     * Applies grader result JSON to the Job entity.
     *
     * This maps summary data such as status, score, test counts,
     * failure message, and per-test results into the database row.
     *
     * @param job Job entity to update
     * @param jobResults JSON object returned by the grader runtime
     * @throws IOException if result JSON cannot be serialized back into a string
     */
    private void applyJobResults(Job job, JsonNode jobResults) {
        if (jobResults == null || jobResults.get("status") == null) {
            throw new IllegalArgumentException("Grader result is missing required field: status");
        }

        JobStatus parsedStatus = parseJobStatus(jobResults.get("status").asString());
        job.setStatus(parsedStatus);

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
            job.setFailureMessage(jobResults.get("error_message").asString());

            if (parsedStatus == JobStatus.FAILED
                    && (job.getFailureReason() == null || job.getFailureReason() == FailureReason.NONE)) {
                job.setFailureReason(FailureReason.UNKNOWN);
            }
        } else if (parsedStatus == JobStatus.SUCCEEDED) {
            job.setFailureReason(FailureReason.NONE);
            job.setFailureMessage(null);
        }

        if (jobResults.has("results")) {
            job.setResultJson(objectMapper.writeValueAsString(jobResults.get("results")));
        }
    }

    /**
     * Converts a raw grader status string into the JobStatus enum used by the backend.
     *
     * @param rawStatus raw string from grader output
     * @return parsed JobStatus enum value
     * @throws IllegalArgumentException if the status is missing or invalid
     */
    private JobStatus parseJobStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new IllegalArgumentException("Job status is missing.");
        }

        String normalized = rawStatus.trim().toUpperCase();

        // If your grader returns SUCCEEDED/FAILED, this only works
        // if your JobStatus enum contains those exact values.
        return JobStatus.valueOf(normalized);
    }

     /**
     * Sanitizes and validates incoming file names before using them
     * for local file system operations.
     *
     * This prevents blank file names and basic path traversal attempts.
     *
     * @param rawFileName raw incoming file name from the request
     * @return cleaned file name safe to use under the uploads folder
     * @throws IllegalArgumentException if the file name is missing or invalid
     */
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

    /**
     * Returns the list of grader options that should appear in the frontend dropdown.
     *
     * This converts the full grader definitions from the registry into a smaller
     * DTO (Data Transfer Object) containing only the fields needed by the UI.
     *
     * @return list of frontend grader options
     */
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