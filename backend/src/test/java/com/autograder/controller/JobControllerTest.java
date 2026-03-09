package com.autograder.controller;

import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class JobControllerTest {

    private JobRepository jobRepository;
    private JobController jobController;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
        jobController = new JobController(jobRepository);
    }

    /**
     * Test 1: Valid file upload returns 200 with success message.
     */
    @Test
    void uploadFile_validFile_returns200WithMessage() {
        Job savedJob = new Job("test_submission.py", "Fibonacci", OffsetDateTime.now(), JobStatus.QUEUED);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_submission.py",
                "text/plain",
                "print('hello')".getBytes()
        );

        ResponseEntity<Map<String, Object>> response = jobController.uploadFile(file);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Successfully uploaded file.", response.getBody().get("message"));
    }

    /**
     * Test 2: File that already exists on disk returns 409 conflict.
     */
    @Test
    void uploadFile_duplicateFile_returns409() {
        // Use a filename that we know won't exist so we can test the happy path first,
        // then test the conflict by uploading the same file twice.
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate_test_" + System.currentTimeMillis() + ".py",
                "text/plain",
                "print('hello')".getBytes()
        );

        when(jobRepository.save(any(Job.class))).thenReturn(
                new Job(file.getOriginalFilename(), "Fibonacci", OffsetDateTime.now(), JobStatus.QUEUED)
        );

        // First upload should succeed
        ResponseEntity<Map<String, Object>> first = jobController.uploadFile(file);
        assertEquals(200, first.getStatusCode().value());

        // Second upload of same filename should return 409
        ResponseEntity<Map<String, Object>> second = jobController.uploadFile(file);
        assertEquals(409, second.getStatusCode().value());
        assertEquals("File with this name already exists.", second.getBody().get("message"));
    }
}