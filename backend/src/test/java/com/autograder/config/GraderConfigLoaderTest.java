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
                      "description": "Classic dynamic programming problem.",
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
                      "description": "Array and hash map problem.",
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
        assertEquals("Classic dynamic programming problem.", fib.getDescription());
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