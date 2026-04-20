package com.autograder.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.autograder.model.GraderDefinition;

class GraderRegistryTest {

    private GraderRegistry createRegistry() {
        List<GraderDefinition> graderDefinitions = List.of(
                createGrader("fib", "Fibonacci", "ea-grader-fibbonaci:v1"),
                createGrader("twosum", "Two Sum", "ea-grader-twosum:v1")
        );

        return new GraderRegistry(graderDefinitions);
    }

    private GraderDefinition createGrader(String key, String label, String imageName) {
        GraderDefinition grader = new GraderDefinition();
        grader.setKey(key);
        grader.setLabel(label);
        grader.setImageName(imageName);
        grader.setManifestPath("/app/grader/manifest.json");
        grader.setTimeoutSeconds(10);
        grader.setCpuRequestMilli(100);
        grader.setCpuLimitMilli(500);
        grader.setMemoryRequestMb(128);
        grader.setMemoryLimitMb(512);
        return grader;
    }

    @Test
    void getRequired_validKey_returnsCorrectFibGrader() {
        GraderRegistry registry = createRegistry();

        GraderDefinition grader = registry.getRequired("fib");

        assertNotNull(grader);
        assertEquals("fib", grader.getKey());
        assertEquals("Fibonacci", grader.getLabel());
        assertEquals("ea-grader-fibbonaci:v1", grader.getImageName());
        assertEquals("/app/grader/manifest.json", grader.getManifestPath());
    }

    @Test
    void getRequired_validKey_returnsCorrectTwoSumGrader() {
        GraderRegistry registry = createRegistry();

        GraderDefinition grader = registry.getRequired("twosum");

        assertNotNull(grader);
        assertEquals("twosum", grader.getKey());
        assertEquals("Two Sum", grader.getLabel());
        assertEquals("ea-grader-twosum:v1", grader.getImageName());
        assertEquals("/app/grader/manifest.json", grader.getManifestPath());
    }

    @Test
    void getRequired_unknownKey_throwsIllegalArgumentException() {
        GraderRegistry registry = createRegistry();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getRequired("unknown")
        );

        assertTrue(exception.getMessage().contains("Unknown grader key"));
    }

    @Test
    void getAll_returnsAllRegisteredGraders() {
        GraderRegistry registry = createRegistry();

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
        GraderRegistry registry = createRegistry();

        List<GraderDefinition> graders = registry.getAll();

        assertThrows(UnsupportedOperationException.class, () ->
                graders.add(createGrader("new", "New Grader", "ea-grader-new:v1"))
        );
    }
}