package com.autograder.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.autograder.model.GraderDefinition;

@Service
public class GraderRegistry {

    private final Map<String, GraderDefinition> graders = Map.of(
            "fib", new GraderDefinition(
                    "fib",
                    "Fibonacci",
                    "ea-grader-fibbonaci:v1",
                    "/app/grader/manifest.json"
            ),
            "twosum", new GraderDefinition(
                    "twosum",
                    "Two Sum",
                    "ea-grader-twosum:v1",
                    "/app/grader/manifest.json"
            )
    );

    public GraderDefinition getRequired(String key) {
        GraderDefinition grader = graders.get(key);
        if (grader == null) {
            throw new IllegalArgumentException("Unknown grader key: " + key);
        }
        return grader;
    }

    public List<GraderDefinition> getAll() {
        return List.copyOf(graders.values());
    }
}