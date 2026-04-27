package com.autograder.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AutograderSystemTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fullAutograderPipeline_fullPass_returnsSucceeded() throws Exception {
        JsonNode output = runGrader("fibpass1.py", "fib");

        assertEquals("SUCCEEDED", output.get("status").asText());
        assertEquals(2, output.get("tests_passed").asInt());
        assertEquals(2, output.get("tests_total").asInt());
    }

    @Test
    void fullAutograderPipeline_partialCredit_returnsPartial() throws Exception {
        JsonNode output = runGrader("fibfail1.py", "fib");

        assertEquals("FAILED", output.get("status").asText());
        assertEquals(0, output.get("tests_passed").asInt());
        assertEquals(2, output.get("tests_total").asInt());
        assertEquals("No test cases passed.", output.get("error_message").asText());
    }

    @Test
    void fullAutograderPipeline_mixedScore_returnsPartial() throws Exception {
        Path submission = tempDir.resolve("fibpartial.py");
        Files.writeString(submission, """
                def fib(n):
                    if n == 5:
                        return 5
                    return -1
                """);

        JsonNode output = runGrader(submission.toFile(), "fib");

        assertEquals("PARTIAL", output.get("status").asText());
        assertTrue(output.get("tests_passed").asInt() > 0);
        assertTrue(output.get("tests_passed").asInt() < output.get("tests_total").asInt());
    }

    @Test
    void fullAutograderPipeline_missingCallable_returnsFailed() throws Exception {
        Path submission = tempDir.resolve("emptyfile.py");
        Files.writeString(submission, "");
        JsonNode output = runGrader(submission.toFile(), "fib");

        assertEquals("FAILED", output.get("status").asText());
        assertFalse(output.get("validation_passed").asBoolean());
        assertTrue(output.get("error_message").asText().contains("missing callable function"));
    }

    private JsonNode runGrader(String submissionFile, String problem) throws Exception {
        File submission = resolveSubmissionFixture(submissionFile, problem);
        return runGrader(submission, problem);
    }

    private File resolveSubmissionFixture(String submissionFile, String problem) {
        File problemFixture = new File("../mocksubmission/" + problem, submissionFile);
        assertTrue(
                problemFixture.exists(),
                "Submission fixture not found for problem '" + problem + "': " + problemFixture.getPath()
        );

        return problemFixture;
    }

    private JsonNode runGrader(File submission, String problem) throws Exception {
        File runtimeDir = new File("grading/image-build/runtime");
        assertTrue(runtimeDir.exists(), "Runtime directory does not exist: " + runtimeDir.getPath());

        assertTrue(
                submission.exists(),
                "Submission file does not exist for grader '" + problem + "': " + submission.getPath()
        );

        File manifest = new File("grading/image-build/" + problem + "/manifest.json");
        assertTrue(manifest.exists(), "Manifest file does not exist: " + manifest.getPath());

        ProcessBuilder pb = new ProcessBuilder(
                "python",
                "main.py",
                submission.getAbsolutePath(),
                manifest.getAbsolutePath()
        );

        pb.directory(runtimeDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        assertEquals(0, exitCode);
        assertTrue(output.length() > 0);

        return objectMapper.readTree(output.toString());
    }
}
