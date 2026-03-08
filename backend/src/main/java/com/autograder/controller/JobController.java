package com.autograder.controller;

import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.autograder.model.Job;
import com.autograder.repository.JobRepository;
import org.springframework.web.multipart.MultipartFile;

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
        //create jobs & upload
        String fileName = file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            Path uploads = Path.of("uploads");
            Path filePath = Path.of(uploads +  "/" + fileName);
            if(!Files.exists(uploads)) {
                Files.createDirectories(uploads);
            }
            Files.write(filePath, bytes);
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
    public ResponseEntity<JsonObject> getJobObject(@PathVariable Long id) {
        //job details & result
        JsonObject jobObj = new JsonObject();
        Optional<Job> jobEntity = jobRepository.findById(id);
        if(jobEntity.isEmpty()) {
            jobObj.addProperty("Error", "No job with this id exists.");
            return ResponseEntity.status(404).body(jobObj);
        }
        Job job = jobEntity.get();
        jobObj.addProperty("jobId", job.getId());
        jobObj.addProperty("assignmentId", job.getAssignmentId());
        jobObj.addProperty("originalFileName", job.getOriginalFilename());
        jobObj.addProperty("submissionPath", job.getSubmissionPath());
        jobObj.addProperty("graderImage", job.getGraderImage());
        jobObj.addProperty("status", job.getStatus().toString());
        jobObj.addProperty("createdAt", job.getCreatedAt().toString());
        jobObj.addProperty("updatedAt", job.getUpdatedAt().toString());

        if(job.getStartedAt() != null) {
            jobObj.addProperty("startedAt", job.getStartedAt().toString());
        } else {
            jobObj.addProperty("startedAt", (String) null);
        }
        if(job.getFinishedAt() != null) {
            jobObj.addProperty("finishedAt", job.getFinishedAt().toString());
        } else {
            jobObj.addProperty("finishedAt", (String) null);
        }

        jobObj.addProperty("score", job.getScore());
        jobObj.addProperty("testsPassed", job.getTestsPassed());
        jobObj.addProperty("testsTotal", job.getTestsTotal());
        jobObj.addProperty("errorMessage", job.getErrorMessage());
        jobObj.addProperty("resultJson", job.getResultJson());
        jobObj.addProperty("jobName", job.getK8sJobName());
        return ResponseEntity.ok(jobObj);
    }

    @PostMapping("/job/{id}/callback")
    public ResponseEntity<String> updateJob(@PathVariable String id) {
        //worker status & result update
        return ResponseEntity.ok("test");
    }
}
