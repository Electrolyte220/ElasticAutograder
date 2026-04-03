package com.autograder.service;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class Fabric8GradingOrchestratorTest {

    private KubernetesClient kubernetesClient;
    private GraderRegistry graderRegistry;
    private Fabric8GradingOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        kubernetesClient = Mockito.mock(KubernetesClient.class);
        graderRegistry = Mockito.mock(GraderRegistry.class);
        orchestrator = new Fabric8GradingOrchestrator(kubernetesClient, graderRegistry);
    }

    @Test
    void runJobInKubernetes_nullGraderType_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.runJobInKubernetes(1L, "submission.py", null)
        );

        assertEquals("graderType is required.", exception.getMessage());
    }

    @Test
    void runJobInKubernetes_blankGraderType_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.runJobInKubernetes(1L, "submission.py", "   ")
        );

        assertEquals("graderType is required.", exception.getMessage());
    }

    @Test
    void runJobInKubernetes_unknownGraderType_throwsIllegalArgumentException() {
        when(graderRegistry.getRequired("unknown"))
                .thenThrow(new IllegalArgumentException("Unknown grader key: unknown"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.runJobInKubernetes(1L, "submission.py", "unknown")
        );

        assertTrue(exception.getMessage().contains("Unknown grader key"));
    }

    @Test
    void createSubmissionConfigMap_missingSubmissionFile_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.createSubmissionConfigMap(1L, "does_not_exist.py")
        );

        assertTrue(exception.getMessage().contains("Submission file not found"));
    }

    @Test
    void createSubmissionConfigMap_invalidPathTraversalFileName_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.createSubmissionConfigMap(1L, "../secret.py")
        );

        assertEquals("Invalid file name.", exception.getMessage());
    }

    @Test
    void createSubmissionConfigMap_blankFileName_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator.createSubmissionConfigMap(1L, "   ")
        );

        assertEquals("File name is required.", exception.getMessage());
    }
}