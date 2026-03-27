package com.autograder.service;

import tools.jackson.databind.JsonNode;

public interface GradingOrchestrator {
    JsonNode runJobInKubernetes(Long jobId, String fileName, String graderType) throws Exception;
}