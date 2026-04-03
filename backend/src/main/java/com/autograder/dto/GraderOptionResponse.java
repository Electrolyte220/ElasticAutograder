package com.autograder.dto;

// For some context, DTO is Data Transfer Object
// basically this class tells frontend, hey these are all the current job autograders we have so use em
public class GraderOptionResponse {
    private final String key;
    private final String label;

    public GraderOptionResponse(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}