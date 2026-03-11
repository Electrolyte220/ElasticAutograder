package com.autograder.repository;

import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class JobRepositoryTest {

    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
    }

    /**
     * Test 1: findTop5ByOrderByCreatedAtDesc returns jobs newest-first.
     */
    @Test
    void findTop5ByOrderByCreatedAtDesc_returnsJobsNewestFirst() {
        Job oldest = new Job("oldest.py", "Fibonacci", OffsetDateTime.now().minusDays(3), JobStatus.QUEUED);
        Job middle = new Job("middle.py", "Fibonacci", OffsetDateTime.now().minusDays(1), JobStatus.QUEUED);
        Job newest = new Job("newest.py", "Fibonacci", OffsetDateTime.now(), JobStatus.QUEUED);

        when(jobRepository.findTop5ByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(newest, middle, oldest));

        List<Job> results = jobRepository.findTop5ByOrderByCreatedAtDesc();

        assertEquals(3, results.size());
        assertEquals("newest.py", results.get(0).getOriginalFilename());
        assertEquals("middle.py", results.get(1).getOriginalFilename());
        assertEquals("oldest.py", results.get(2).getOriginalFilename());
    }

    /**
     * Test 2: findTop5ByOrderByCreatedAtDesc returns at most 5 results.
     */
    @Test
    void findTop5ByOrderByCreatedAtDesc_capsAtFiveResults() {
        List<Job> fiveJobs = Arrays.asList(
                new Job("file1.py", "Fibonacci", OffsetDateTime.now(), JobStatus.QUEUED),
                new Job("file2.py", "Fibonacci", OffsetDateTime.now().minusHours(1), JobStatus.QUEUED),
                new Job("file3.py", "Fibonacci", OffsetDateTime.now().minusHours(2), JobStatus.QUEUED),
                new Job("file4.py", "Fibonacci", OffsetDateTime.now().minusHours(3), JobStatus.QUEUED),
                new Job("file5.py", "Fibonacci", OffsetDateTime.now().minusHours(4), JobStatus.QUEUED)
        );

        when(jobRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(fiveJobs);

        List<Job> results = jobRepository.findTop5ByOrderByCreatedAtDesc();

        assertEquals(5, results.size());
    }
}