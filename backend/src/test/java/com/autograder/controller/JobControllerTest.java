package com.autograder.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.autograder.model.FailureReason;
import com.autograder.model.GraderDefinition;
import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.repository.JobRepository;
import com.autograder.service.Fabric8GradingOrchestrator;
import com.autograder.service.GradingFailureException;
import com.autograder.service.GradingOrchestrator;
import com.autograder.service.GraderRegistry;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

public class JobControllerTest {

    private JobRepository jobRepository;
    private JobController jobController;

    // new part of the mock testing 
    private GradingOrchestrator gradingOrchestrator;
    private Fabric8GradingOrchestrator fabric8GradingOrchestrator;
    private GraderRegistry graderRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        jobRepository = Mockito.mock(JobRepository.class);
        gradingOrchestrator = Mockito.mock(GradingOrchestrator.class);
        fabric8GradingOrchestrator = Mockito.mock(Fabric8GradingOrchestrator.class);
        graderRegistry = Mockito.mock(GraderRegistry.class);

        jobController = new JobController(
                jobRepository,
                gradingOrchestrator,
                fabric8GradingOrchestrator,
                graderRegistry
        );

        when(graderRegistry.getRequired(any(String.class)))
                .thenReturn(new GraderDefinition("fib", "Fibonacci", "python:3.12", "manifest.json"));
        

        // ensure upload directory starts clean
        Path uploadDir = Path.of("grading/uploads");
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                    .sorted((a,b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.delete(p); } catch (Exception ignored) {}
                    });
        }

        // mock repository save to assign an ID
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);

            Field idField = Job.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(job, 1L);

            return job;
        });
        when(jobRepository.saveAndFlush(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadFile_validFile_returns200WithMessage() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_submission.py",
                "text/plain",
                "print('hello')".getBytes()
        );

        ResponseEntity<Map<String,Object>> response = jobController.uploadFile(file,"fib");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully uploaded file.", response.getBody().get("message"));
        List<Map<String, Object>> jobs = castJobs(response.getBody().get("jobs"));
        assertEquals(1, jobs.size());
        assertEquals("test_submission.py", jobs.get(0).get("fileName"));
    }

    @Test
    void uploadFile_duplicateFile_returns409() {

        String name = "duplicate_test.py";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                name,
                "text/plain",
                "print('hello')".getBytes()
        );

        ResponseEntity<Map<String,Object>> first = jobController.uploadFile(file,"fib");
        assertEquals(200, first.getStatusCode().value());

        ResponseEntity<Map<String,Object>> second = jobController.uploadFile(file,"fib");
        assertEquals(400, second.getStatusCode().value());
        assertEquals("File with this name already exists.", second.getBody().get("message"));
    }

    @Test
    void uploadFile_zipBatch_returnsCreatedJobs() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "batch.zip",
                "application/zip",
                zipBytes(
                        Map.of(
                                "alpha.py", "print('alpha')",
                                "nested/beta.py", "print('beta')"
                        )
                )
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(zipFile, "fib");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully uploaded batch.", response.getBody().get("message"));

        List<Map<String, Object>> jobs = castJobs(response.getBody().get("jobs"));
        assertEquals(2, jobs.size());
        List<String> fileNames = jobs.stream()
                .map(job -> job.get("fileName").toString())
                .sorted(Comparator.naturalOrder())
                .toList();
        assertEquals(List.of("alpha.py", "beta.py"), fileNames);
    }

    @Test
    void uploadFile_zipWithDuplicateBasenames_returns400() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "duplicates.zip",
                "application/zip",
                zipBytes(
                        Map.of(
                                "first/duplicate.py", "print('a')",
                                "second/duplicate.py", "print('b')"
                        )
                )
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(zipFile, "fib");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Zip archive contains duplicate file names: duplicate.py", response.getBody().get("message"));
    }

    @Test
    void uploadFile_emptyZip_returns400() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "empty.zip",
                "application/zip",
                zipBytes(Map.of())
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(zipFile, "fib");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Zip archive does not contain any files.", response.getBody().get("message"));
    }

    @Test
    void uploadFile_zipWithOnlyDirectories_returns400() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "dirs.zip",
                "application/zip",
                zipBytesWithDirectoriesOnly("nested/", "nested/deeper/")
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(zipFile, "fib");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Zip archive does not contain any files.", response.getBody().get("message"));
    }

    @Test
    void uploadFile_zipWithPathTraversal_returns400() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "unsafe.zip",
                "application/zip",
                zipBytes(
                        Map.of(
                                "../secret.py", "print('bad')"
                        )
                )
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(zipFile, "fib");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Zip archive contains an invalid file path.", response.getBody().get("message"));
    }

    @Test
    void getJobById_existingId_returns200WithJob() {
        Job job = new Job("submission.py", "fib", OffsetDateTime.now(), JobStatus.QUEUED);
        when(jobRepository.findById(7L)).thenReturn(Optional.of(job));

        ResponseEntity<?> response = jobController.getJobById(7L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(job, response.getBody());
    }

    @Test
    void getJobById_missingId_returns404() {
        when(jobRepository.findById(77L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = jobController.getJobById(77L);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Unable to find job with id: 77", response.getBody());
    }

    @Test
    void runJob_partialResult_persistsPartialStatusAndResults() throws Exception {
        Job job = new Job("submission.py", "fib", OffsetDateTime.now(), JobStatus.QUEUED);
        setJobId(job, 11L);
        job.setSubmissionPath("batch-123/submission.py");
        when(jobRepository.findById(11L)).thenReturn(Optional.of(job));

        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "PARTIAL");
        result.put("tests_passed", 1);
        result.put("tests_total", 2);
        result.put("score", new BigDecimal("50.0"));
        ArrayNode results = result.putArray("results");
        ObjectNode entry = results.addObject();
        entry.put("kind", "test");
        entry.put("name", "case_1");
        entry.put("passed", false);
        entry.put("message", "Expected 55, got 34");

        when(gradingOrchestrator.runJobInKubernetes(11L, "batch-123/submission.py", "fib")).thenReturn(result);

        ResponseEntity<?> response = jobController.runJob(11L, "\"submission.py\"");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.PARTIAL, job.getStatus());
        assertEquals(1, job.getTestsPassed());
        assertEquals(2, job.getTestsTotal());
        assertEquals(0, new BigDecimal("50.0").compareTo(job.getScore()));
        assertEquals(FailureReason.NONE, job.getFailureReason());
        assertNull(job.getFailureMessage());
        assertTrue(job.getResultJson().contains("\"case_1\""));
        assertEquals("batch-123/submission.py", job.getSubmissionPath());
    }

    @Test
    void runJob_zeroPassResult_persistsFailedStatusAndWrongAnswerReason() throws Exception {
        Job job = new Job("submission.py", "fib", OffsetDateTime.now(), JobStatus.QUEUED);
        setJobId(job, 14L);
        job.setSubmissionPath("batch-123/submission.py");
        when(jobRepository.findById(14L)).thenReturn(Optional.of(job));

        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "FAILED");
        result.put("validation_passed", true);
        result.put("tests_passed", 0);
        result.put("tests_total", 2);
        result.put("score", new BigDecimal("0.0"));
        result.put("error_message", "No test cases passed.");
        ArrayNode results = result.putArray("results");
        ObjectNode entry = results.addObject();
        entry.put("kind", "test");
        entry.put("name", "case_1");
        entry.put("passed", false);
        entry.put("message", "Expected 5, got 0");

        when(gradingOrchestrator.runJobInKubernetes(14L, "batch-123/submission.py", "fib")).thenReturn(result);

        ResponseEntity<?> response = jobController.runJob(14L, "\"submission.py\"");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(FailureReason.WRONG_ANSWER, job.getFailureReason());
        assertEquals("No test cases passed.", job.getFailureMessage());
        assertTrue(job.getResultJson().contains("\"case_1\""));
    }

    @Test
    void runJob_validationFailure_persistsInvalidUploadReason() throws Exception {
        Job job = new Job("dog.py", "fib", OffsetDateTime.now(), JobStatus.QUEUED);
        setJobId(job, 15L);
        job.setSubmissionPath("batch-123/dog.py");
        when(jobRepository.findById(15L)).thenReturn(Optional.of(job));

        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "FAILED");
        result.put("validation_passed", false);
        result.put("tests_passed", 0);
        result.put("tests_total", 0);
        result.put("score", new BigDecimal("0.0"));
        result.put("error_message", "submission is missing callable function 'fib'");
        ArrayNode results = result.putArray("results");
        ObjectNode entry = results.addObject();
        entry.put("kind", "validation");
        entry.put("name", "validation_check");
        entry.put("passed", false);
        entry.put("message", "submission is missing callable function 'fib'");

        when(gradingOrchestrator.runJobInKubernetes(15L, "batch-123/dog.py", "fib")).thenReturn(result);

        ResponseEntity<?> response = jobController.runJob(15L, "\"dog.py\"");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(FailureReason.INVALID_UPLOAD, job.getFailureReason());
        assertEquals("submission is missing callable function 'fib'", job.getFailureMessage());
    }

    @Test
    void downloadResults_partialJobWithStoredJson_returnsAttachment() throws Exception {
        Job job = new Job("submission.py", "fib", OffsetDateTime.now(), JobStatus.PARTIAL);
        setJobId(job, 12L);
        job.setResultJson("[{\"kind\":\"test\",\"name\":\"case_1\",\"passed\":false}]");
        when(jobRepository.findById(12L)).thenReturn(Optional.of(job));

        ResponseEntity<String> response = jobController.downloadResults(12L, true);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"case_1\""));
        assertEquals("application/json", response.getHeaders().getContentType().toString());
        assertEquals(
                "form-data; name=\"attachment\"; filename=\"results.json\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
    }

    @Test
    void runJob_gradingFailure_persistsFailedStatusAndFailureReason() throws Exception {
        Job job = new Job("submission.py", "fib", OffsetDateTime.now(), JobStatus.QUEUED);
        setJobId(job, 13L);
        job.setSubmissionPath("batch-123/submission.py");
        when(jobRepository.findById(13L)).thenReturn(Optional.of(job));
        when(gradingOrchestrator.runJobInKubernetes(13L, "batch-123/submission.py", "fib"))
                .thenThrow(new GradingFailureException(FailureReason.TIMEOUT, "Timed out"));

        ResponseEntity<?> response = jobController.runJob(13L, "\"submission.py\"");

        assertEquals(500, response.getStatusCode().value());
        assertInstanceOf(tools.jackson.databind.node.StringNode.class, response.getBody());
        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(FailureReason.TIMEOUT, job.getFailureReason());
        assertEquals("Timed out", job.getFailureMessage());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castJobs(Object jobs) {
        return (List<Map<String, Object>>) jobs;
    }

    private byte[] zipBytes(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue().getBytes());
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    private byte[] zipBytesWithDirectoriesOnly(String... directories) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (String directory : directories) {
                zipOutputStream.putNextEntry(new ZipEntry(directory));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    private void setJobId(Job job, Long id) throws Exception {
        Field idField = Job.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(job, id);
    }
}
