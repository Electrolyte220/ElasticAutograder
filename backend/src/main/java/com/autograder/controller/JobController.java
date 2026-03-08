package com.autograder.controller;

import com.autograder.model.Job;
import com.autograder.model.JobStatus;
import com.autograder.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam MultipartFile file) {
        //save file, and add to db
        String fileName = file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            Path uploads = Path.of("grading/graders/assignments/test1");
            Path filePath = Path.of(uploads +  "/" + fileName);
            if(!Files.exists(uploads)) {
                Files.createDirectories(uploads);
            }
            if(Files.exists(filePath)) {
                return ResponseEntity.status(409).body(Map.of("message", "File with this name already exists.", "id", -1L));
            }
            Files.write(filePath, bytes);
            Job job = new Job(fileName, OffsetDateTime.now(), JobStatus.QUEUED);
            jobRepository.save(job);
            return ResponseEntity.ok(Map.of("message", "Successfully uploaded file.", "id", job.getId()));
        } catch(IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to read file: " + fileName, "id",-1L));
        }
    }

    @PostMapping({"/jobs/run/{id}"})
    public ResponseEntity<JsonNode> runJob(@PathVariable Long id, @RequestBody String fileName) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        JsonNode errorObj = new StringNode("Unable to find job object for id " + id);
        if(jobEntity.isEmpty()) return ResponseEntity.status(500).body(errorObj);

        Job job = jobEntity.get();
        job.setStatus(JobStatus.RUNNING);
        job.setUpdatedAt(OffsetDateTime.now());
        job.setStartedAt(OffsetDateTime.now());
        jobRepository.saveAndFlush(job);

        try {
            //only handles uploaded file currently, will change in beta to be a zip of files
            ProcessBuilder builder = new ProcessBuilder("python3", "main.py", String.format("./assignments/test1/%s", fileName), "./assignments/test1/answer_key/key.py");
            builder.directory(new File("grading/graders"));
            builder.redirectErrorStream(true);

            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder outputBuilder = new StringBuilder();
            while((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            job.setUpdatedAt(OffsetDateTime.now());
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.saveAndFlush(job);

            String output = outputBuilder.toString();
            JsonNode jsonObj = new ObjectMapper().readTree(output);

            return ResponseEntity.ok(jsonObj);
        } catch(IOException | InterruptedException e) {
            JsonNode errorObj1 = new StringNode(e.getMessage());
            return ResponseEntity.status(500).body(errorObj1);
        }
    }

    // added by app0cal
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<Job>> getRecentJobs() {
        return ResponseEntity.ok(jobRepository.findTop5ByOrderByCreatedAtDesc());
    }

    @PostMapping("/jobs/{id}/callback")
    public ResponseEntity<String> updateJob(@PathVariable Long id, @RequestBody JsonNode jobResults) {
        Optional<Job> jobEntity = jobRepository.findById(id);
        if(jobEntity.isEmpty()) return ResponseEntity.status(500).body("Unable to find existing job with id " + id);
        //worker status & result update
        Job job = jobEntity.get();
        job.setStatus(JobStatus.valueOf(jobResults.get("status").asString()));
        job.setTestsPassed(jobResults.get("tests_passed").asInt());
        job.setTestsTotal(jobResults.get("tests_total").asInt());
        job.setScore(jobResults.get("score").asDecimal());
        if(jobResults.get("error_message") != null) {
            job.setErrorMessage(jobResults.get("error_message").stringValue());
        }
        job.setResultJson(new ObjectMapper().writeValueAsString(jobResults.get("results")));
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.saveAndFlush(job);
        return ResponseEntity.ok("Successfully updated job.");
    }

    @DeleteMapping({"/files/remove"})
    public ResponseEntity<String> removeFile(@RequestBody String fileName) {
        Path filePath = Path.of("grading/graders/assignments/test1/" + fileName);
        if(Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            }catch(IOException e) {
                return ResponseEntity.status(500).body("Unable to delete file.");
            }
        } else {
            return ResponseEntity.status(404).body("File not found.");
        }
        return ResponseEntity.ok("Successfully deleted file.");
    }

}
