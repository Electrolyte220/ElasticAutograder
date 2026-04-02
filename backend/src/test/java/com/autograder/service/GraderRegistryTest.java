package com.autograder.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.autograder.model.GraderDefinition;

class GraderRegistryTest {

    @Test
    void getRequired_validKey_returnsCorrectFibGrader() {
        GraderRegistry registry = new GraderRegistry();

        GraderDefinition grader = registry.getRequired("fib");

        assertNotNull(grader);
        assertEquals("fib", grader.getKey());
        assertEquals("Fibonacci", grader.getLabel());
        assertEquals("ea-grader-fibbonaci:v1", grader.getImageName());
        assertEquals("/app/grader/manifest.json", grader.getManifestPath());
    }

    @Test
    void getRequired_validKey_returnsCorrectTwoSumGrader() {
        GraderRegistry registry = new GraderRegistry();

        GraderDefinition grader = registry.getRequired("twosum");

        assertNotNull(grader);
        assertEquals("twosum", grader.getKey());
        assertEquals("Two Sum", grader.getLabel());
        assertEquals("ea-grader-twosum:v1", grader.getImageName());
        assertEquals("/app/grader/manifest.json", grader.getManifestPath());
    }

    @Test
    void getRequired_unknownKey_throwsIllegalArgumentException() {
        GraderRegistry registry = new GraderRegistry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getRequired("unknown")
        );

        assertTrue(exception.getMessage().contains("Unknown grader key"));
    }

    @Test
    void getAll_returnsAllRegisteredGraders() {
        GraderRegistry registry = new GraderRegistry();

        List<GraderDefinition> graders = registry.getAll();

        assertEquals(2, graders.size());

        boolean hasFib = graders.stream().anyMatch(g -> 
                g.getKey().equals("fib") &&
                g.getLabel().equals("Fibonacci")
        );

        boolean hasTwoSum = graders.stream().anyMatch(g -> 
                g.getKey().equals("twosum") &&
                g.getLabel().equals("Two Sum")
        );

        assertTrue(hasFib);
        assertTrue(hasTwoSum);
    }

    @Test
    void getAll_returnsUnmodifiableList() {
        GraderRegistry registry = new GraderRegistry();

        List<GraderDefinition> graders = registry.getAll();

        assertThrows(UnsupportedOperationException.class, () ->
                graders.add(new GraderDefinition(
                        "new",
                        "New Grader",
                        "ea-grader-new:v1",
                        "/app/grader/manifest.json"
                ))
        );
    }
}