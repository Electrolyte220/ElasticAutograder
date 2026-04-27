package com.autograder.config;

import com.autograder.model.GraderDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraderConfigLoaderTest {

    @TempDir
    Path tempDir;

    private GraderConfigLoader createLoader() {
        return new GraderConfigLoader(new ObjectMapper());
    }

    @Test
    void loadGraders_validConfig_returnsGraders() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
                {
                  "graders": [
                    {
                      "key": "fib",
                      "label": "Fibonacci",
                      "imageName": "ea-grader-fibbonaci:v1",
                      "manifestPath": "/app/grader/manifest.json",
                      "summary": "Classic dynamic programming problem.",
                      "details": [
                        "Return the nth Fibonacci number.",
                        "Assume the sequence starts at 0 and 1."
                      ],
                      "timeoutSeconds": 10,
                      "cpuRequestMilli": 100,
                      "cpuLimitMilli": 500,
                      "memoryRequestMb": 128,
                      "memoryLimitMb": 512
                    },
                    {
                      "key": "twosum",
                      "label": "Two Sum",
                      "imageName": "ea-grader-twosum:v1",
                      "manifestPath": "/app/grader/manifest.json",
                      "summary": "Array and hash map problem.",
                      "details": [
                        "Return the indices of the two numbers that add up to the target."
                      ],
                      "timeoutSeconds": 10,
                      "cpuRequestMilli": 100,
                      "cpuLimitMilli": 500,
                      "memoryRequestMb": 128,
                      "memoryLimitMb": 512
                    }
                  ]
                }
                """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();
        List<GraderDefinition> graders = loader.loadGraders(configFile);

        assertEquals(2, graders.size());

        GraderDefinition fib = graders.get(0);
        assertEquals("fib", fib.getKey());
        assertEquals("Fibonacci", fib.getLabel());
        assertEquals("ea-grader-fibbonaci:v1", fib.getImageName());
        assertEquals("/app/grader/manifest.json", fib.getManifestPath());
        assertEquals("Classic dynamic programming problem.", fib.getSummary());
        assertEquals(2, fib.getDetails().size());
        assertEquals(10, fib.getTimeoutSeconds());
        assertEquals(100, fib.getCpuRequestMilli());
        assertEquals(500, fib.getCpuLimitMilli());
        assertEquals(128, fib.getMemoryRequestMb());
        assertEquals(512, fib.getMemoryLimitMb());
    }

    @Test
    void loadGraders_missingFile_throwsException() {
        Path missingPath = tempDir.resolve("does-not-exist.json");

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(missingPath)
        );

        assertTrue(ex.getMessage().contains("Grader config file not found"));
    }

    @Test
    void loadGraders_duplicateKeys_throwsException() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
        {
          "graders": [
            {
              "key": "fib",
              "label": "Fibonacci",
              "imageName": "ea-grader-fibbonaci:v1",
              "manifestPath": "/app/grader/manifest.json",
              "summary": "Dynamic programming warm-up.",
              "details": ["Return the nth Fibonacci number."],
              "timeoutSeconds": 10,
              "cpuRequestMilli": 100,
              "cpuLimitMilli": 500,
              "memoryRequestMb": 128,
              "memoryLimitMb": 512
            },
            {
              "key": "fib",
              "label": "Duplicate Fibonacci",
              "imageName": "ea-grader-fibbonaci:v2",
              "manifestPath": "/app/grader/manifest.json",
              "summary": "Duplicate key entry.",
              "details": ["Used to verify duplicate validation."],
              "timeoutSeconds": 10,
              "cpuRequestMilli": 100,
              "cpuLimitMilli": 500,
              "memoryRequestMb": 128,
              "memoryLimitMb": 512
            }
          ]
        }
        """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(configFile)
        );

        assertTrue(ex.getMessage().contains("Duplicate grader key found"));
    }

    @Test
    void loadGraders_requestGreaterThanLimit_throwsException() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
                {
                  "graders": [
                    {
                      "key": "fib",
                      "label": "Fibonacci",
                      "imageName": "ea-grader-fibbonaci:v1",
                      "manifestPath": "/app/grader/manifest.json",
                      "summary": "Dynamic programming warm-up.",
                      "details": ["Return the nth Fibonacci number."],
                      "cpuRequestMilli": 600,
                      "cpuLimitMilli": 500,
                      "memoryRequestMb": 128,
                      "memoryLimitMb": 512
                    }
                  ]
                }
                """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(configFile)
        );

        assertTrue(ex.getMessage().contains("cpuRequestMilli greater than cpuLimitMilli"));
    }

    @Test
    void loadGraders_missingSummary_throwsException() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
                {
                  "graders": [
                    {
                      "key": "fib",
                      "label": "Fibonacci",
                      "imageName": "ea-grader-fibbonaci:v1",
                      "manifestPath": "/app/grader/manifest.json",
                      "details": ["Return the nth Fibonacci number."]
                    }
                  ]
                }
                """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(configFile)
        );

        assertTrue(ex.getMessage().contains("missing a summary"));
    }

    @Test
    void loadGraders_blankDetailsEntry_throwsException() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
                {
                  "graders": [
                    {
                      "key": "fib",
                      "label": "Fibonacci",
                      "imageName": "ea-grader-fibbonaci:v1",
                      "manifestPath": "/app/grader/manifest.json",
                      "summary": "Dynamic programming warm-up.",
                      "details": ["   "]
                    }
                  ]
                }
                """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(configFile)
        );

        assertTrue(ex.getMessage().contains("invalid details entry"));
    }

    @Test
    void loadGraders_emptyGradersList_throwsException() throws Exception {
        Path configFile = tempDir.resolve("graders.json");

        String json = """
                {
                  "graders": []
                }
                """;

        Files.writeString(configFile, json);

        GraderConfigLoader loader = createLoader();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> loader.loadGraders(configFile)
        );

        assertTrue(ex.getMessage().contains("At least one grader must be defined"));
    }
}
