package com.autograder.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class JobTest {

    // Test that the paramterized constructor is initialized with correct field values
    @Test
    void constructor_initializesFieldsCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();

        Job job = new Job("submission.py", "Fibonacci", now, JobStatus.QUEUED);

        assertEquals("submission.py", job.getOriginalFilename());
        assertEquals("Fibonacci", job.getGraderType());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertEquals(now, job.getCreatedAt());
        assertEquals(now, job.getUpdatedAt());
    }

    // Test that setter methods correctly update Job fields
    @Test
    void setters_updateFieldsCorrectly() {
        Job job = new Job();

        job.setOriginalFilename("test.py");
        job.setGraderType("Sorting");
        job.setSubmissionPath("/tmp/test.py");
        job.setGraderImage("grader:v1");
        job.setStatus(JobStatus.RUNNING);

        assertEquals("test.py", job.getOriginalFilename());
        assertEquals("Sorting", job.getGraderType());
        assertEquals("/tmp/test.py", job.getSubmissionPath());
        assertEquals("grader:v1", job.getGraderImage());
        assertEquals(JobStatus.RUNNING, job.getStatus());
    }
    
    // Test that lifecycle stamps can be set and retrieved correctly
    @Test
    void timestamps_canBeUpdated() {
        Job job = new Job();

        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime finish = start.plusMinutes(2);
        OffsetDateTime updated = start.plusMinutes(1);

        job.setStartedAt(start);
        job.setFinishedAt(finish);
        job.setUpdatedAt(updated);

        assertEquals(start, job.getStartedAt());
        assertEquals(finish, job.getFinishedAt());
        assertEquals(updated, job.getUpdatedAt());
    }

    // Test the grading metric such as score and test counts are stored correctly
    @Test
    void gradingResults_storeCorrectly() {
        Job job = new Job();

        job.setScore(new BigDecimal("95.5"));
        job.setTestsPassed(9);
        job.setTestsTotal(10);

        assertEquals(0, new BigDecimal("95.5").compareTo(job.getScore()));
        assertEquals(9, job.getTestsPassed());
        assertEquals(10, job.getTestsTotal());
    }

    //Test that Job status field can be updated across multiple enum states
    @Test
    void status_canBeUpdated() {
        Job job = new Job();

        job.setStatus(JobStatus.QUEUED);
        assertEquals(JobStatus.QUEUED, job.getStatus());

        job.setStatus(JobStatus.RUNNING);
        assertEquals(JobStatus.RUNNING, job.getStatus());

        job.setStatus(JobStatus.FAILED);
        assertEquals(JobStatus.FAILED, job.getStatus());
    }

    // Test that executing metadata such as error messages and result JSON are stored correctly
    @Test
    void errorMessage_and_resultJson_storeCorrectly() {
        Job job = new Job();

        job.setErrorMessage("Compilation failed");
        job.setResultJson("{\"tests\": []}");

        assertEquals("Compilation failed", job.getErrorMessage());
        assertEquals("{\"tests\": []}", job.getResultJson());
    }

  
}