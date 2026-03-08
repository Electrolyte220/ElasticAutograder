package com.autograder.controller;

import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
        //save file, and add to db
        String fileName = file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            Path uploads = Path.of("uploads");
            Path filePath = Path.of(uploads +  "/" + fileName);
            if(!Files.exists(uploads)) {
                Files.createDirectories(uploads);
            }
            if(Files.exists(filePath)) {
                return ResponseEntity.status(409).body("File with this name already exists.");
            }
            Files.write(filePath, bytes);
            Job job = new Job(fileName, OffsetDateTime.now(), JobStatus.QUEUED);
            jobRepository.save(job);
            return ResponseEntity.ok("Successfully uploaded file.");
        } catch(IOException e) {
            return ResponseEntity.status(500).body("Failed to read file: " + fileName);
        }
    }

    // added by app0cal
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<Job>> getRecentJobs() {
        return ResponseEntity.ok(jobRepository.findTop5ByOrderByCreatedAtDesc());
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<Job> getJobObject(@PathVariable Long id) {
        //job details & result
        Optional<Job> jobEntity = jobRepository.findById(id);
        return jobEntity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).build());
    }

    @PostMapping("/job/{id}/callback")
    public ResponseEntity<String> updateJob(@PathVariable String id) {
        //worker status & result update
        return ResponseEntity.ok("test");
    }
}
