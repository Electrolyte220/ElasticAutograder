package com.autograder.config;

import com.autograder.model.GraderDefinition;
import com.autograder.service.GraderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GraderConfigLoader {

    // Assumes backend is started from the project root: gradle should be configured to do this, and the README should specify this as well

    private static final Path DEFAULT_CONFIG_PATH = Path.of("..", "config", "graders.json");

    // platform defaults for graders unless overridden in config
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_CPU_REQUEST_MILLI = 100;
    private static final int DEFAULT_CPU_LIMIT_MILLI = 500;
    private static final int DEFAULT_MEMORY_REQUEST_MB = 128;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 512;

    private final ObjectMapper objectMapper;

    public GraderConfigLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<GraderDefinition> loadGraders() {
        return loadGraders(DEFAULT_CONFIG_PATH);
    }

    public List<GraderDefinition> loadGraders(Path configPath) {
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("Grader config file not found: " + configPath.toAbsolutePath());
        }

        try {
            GraderConfig graderConfig = objectMapper.readValue(configPath.toFile(), GraderConfig.class);

            if (graderConfig == null || graderConfig.getGraders() == null) {
                throw new IllegalStateException("Invalid grader config: missing 'graders' list.");
            }
            applyDefaults(graderConfig.getGraders());
            validateGraders(graderConfig.getGraders());

            return graderConfig.getGraders();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read grader config from: " + configPath.toAbsolutePath(), e);
        }
    }

    private void applyDefaults(List<GraderDefinition> graders) {
        for (GraderDefinition grader : graders) {
            applyDefaults(grader);
        }
    }

    private void applyDefaults(GraderDefinition grader) {
        if (grader.getTimeoutSeconds() == null) {
            grader.setTimeoutSeconds(DEFAULT_TIMEOUT_SECONDS);
        }

        if (grader.getCpuRequestMilli() == null) {
            grader.setCpuRequestMilli(DEFAULT_CPU_REQUEST_MILLI);
        }

        if (grader.getCpuLimitMilli() == null) {
            grader.setCpuLimitMilli(DEFAULT_CPU_LIMIT_MILLI);
        }

        if (grader.getMemoryRequestMb() == null) {
            grader.setMemoryRequestMb(DEFAULT_MEMORY_REQUEST_MB);
        }

        if (grader.getMemoryLimitMb() == null) {
            grader.setMemoryLimitMb(DEFAULT_MEMORY_LIMIT_MB);
        }
    }

    private void validateGraders(List<GraderDefinition> graders) {
        if (graders.isEmpty()) {
            throw new IllegalStateException("Grader config is empty. At least one grader must be defined.");
        }

        HashSet<String> seenKeys = new HashSet<>();

        for (GraderDefinition grader : graders) {
          if (!seenKeys.add(grader.getKey())) {
            throw new IllegalStateException("Duplicate grader key found");
          }
          validateGrader(grader);
        }
    }

    // basic error handling throws exceptions if there's errors 
    // to add: duplicate keys 
    private void validateGrader(GraderDefinition grader){      
      if (grader.getKey() == null || grader.getKey().isBlank()) {
            throw new IllegalStateException("Each grader must have a non-empty key.");
        }

      if (grader.getLabel() == null || grader.getLabel().isBlank()) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' is missing a label.");
      }

      if (grader.getImageName() == null || grader.getImageName().isBlank()) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' is missing an imageName.");
      }

      if (grader.getManifestPath() == null || grader.getManifestPath().isBlank()) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' is missing a manifestPath.");
      }

      if (grader.getTimeoutSeconds() == null || grader.getTimeoutSeconds() <= 0) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' has invalid timeoutSeconds.");
      }

      if (grader.getCpuRequestMilli() != null && grader.getCpuRequestMilli() <= 0) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' has invalid cpuRequestMilli.");
      }

      if (grader.getCpuLimitMilli() != null && grader.getCpuLimitMilli() <= 0) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' has invalid cpuLimitMilli.");
      }

      if (grader.getMemoryRequestMb() != null && grader.getMemoryRequestMb() <= 0) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' has invalid memoryRequestMb.");
      }

      if (grader.getMemoryLimitMb() != null && grader.getMemoryLimitMb() <= 0) {
          throw new IllegalStateException("Grader '" + grader.getKey() + "' has invalid memoryLimitMb.");
      }

      if (grader.getCpuRequestMilli() != null && grader.getCpuLimitMilli() != null
              && grader.getCpuRequestMilli() > grader.getCpuLimitMilli()) {
          throw new IllegalStateException(
                  "Grader '" + grader.getKey() + "' has cpuRequestMilli greater than cpuLimitMilli."
          );
      }

      if (grader.getMemoryRequestMb() != null && grader.getMemoryLimitMb() != null
              && grader.getMemoryRequestMb() > grader.getMemoryLimitMb()) {
          throw new IllegalStateException(
                  "Grader '" + grader.getKey() + "' has memoryRequestMb greater than memoryLimitMb."
          );
      }
    }
}