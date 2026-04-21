package com.autograder.dto;

/**
 * DTO used to send grader options from the backend to the frontend.
 *
 * Each object represents one selectable grader in the submission form.
 * Only the fields needed by the UI are exposed here instead of sending
 * the full internal GraderDefinition object.
 */
public class GraderOptionResponse {
    private final String key;
    private final String label;

    public GraderOptionResponse(String key, String label) {
        this.key = key;
        this.label = label;
    }

    // basic setter and getters below
    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}