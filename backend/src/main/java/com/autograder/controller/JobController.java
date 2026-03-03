package com.autograder.controller;

import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class JobController {

    @PostMapping({"/jobs"})
    public ResponseEntity<String> createJob() {
        //create jobs & upload
        return ResponseEntity.ok("test");
    }

    @GetMapping("/jobs")
    public ResponseEntity<JsonObject> getAllJobs() {
        //job history & filters
        JsonObject test = new JsonObject();
        test.addProperty("test", "If you see this in your browser, it works!");
        return ResponseEntity.ok(test);
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
