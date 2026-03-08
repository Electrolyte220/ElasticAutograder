package com.autograder.controller;

import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.autograder.model.Job;
import com.autograder.repository.JobRepository;
import org.springframework.web.multipart.MultipartFile;

import javax.print.DocFlavor.INPUT_STREAM;
import java.util.List;

// this must be changed in prod
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }
    @PostMapping({"/jobs/upload"})
    public ResponseEntity<String> createJob(@RequestParam MultipartFile file) {
        //create jobs & upload
        String fileName = file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            Path path = Path.of("uploads/" + fileName);
            if(!Files.exists(path)) {
                Files.createDirectories(Path.of("uploads"));
            }
            Files.write(path, bytes);
            return ResponseEntity.ok("Successfully uploaded file.");
        } catch(IOException e) {
            return ResponseEntity.status(500).body("Failed to read file: " + fileName);
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<JsonObject> getAllJobs() {
        //job history & filters
        JsonObject test = new JsonObject();
        test.addProperty("test", "If you see this in your browser, it works!");
        return ResponseEntity.ok(test);
    }

    // added by app0cal
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<Job>> getRecentJobs() {
        return ResponseEntity.ok(jobRepository.findTop5ByOrderByCreatedAtDesc());
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<JsonObject> getJobObject(@PathVariable String id) {
        //job details & result
        JsonObject exampleJob = new JsonObject();
        exampleJob.addProperty("job_id", id);
        exampleJob.addProperty("assignment_id", "123");
        exampleJob.addProperty("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(exampleJob);
    }

    @PostMapping("/job/{id}/callback")
    public ResponseEntity<String> updateJob(@PathVariable String id) {
        //worker status & result update
        return ResponseEntity.ok("test");
    }
}
