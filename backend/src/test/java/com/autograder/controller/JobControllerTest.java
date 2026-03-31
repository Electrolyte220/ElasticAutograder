package com.autograder.controller;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.autograder.model.Job;
import com.autograder.repository.JobRepository;
import com.autograder.service.Fabric8GradingOrchestrator;
import com.autograder.service.GradingOrchestrator;
import com.autograder.service.GraderRegistry;

public class JobControllerTest {

    private JobRepository jobRepository;
    private JobController jobController;

    // new part of the mock testing 
    private GradingOrchestrator gradingOrchestrator;
    private Fabric8GradingOrchestrator fabric8GradingOrchestrator;
    private GraderRegistry graderRegistry;
    
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
        assertEquals(409, second.getStatusCode().value());
        assertEquals("File with this name already exists.", second.getBody().get("message"));
    }
}