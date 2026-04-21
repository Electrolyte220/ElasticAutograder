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
    private final String description;

    // Constructor for reading a grader with only a key and label available
    // Automatically sets the description to no details provided
    public GraderOptionResponse(String key, String label) {
        this.key = key;
        this.label = label;
        this.description = "No details provided!";
    }

    // Constructor for reading a grader with a key, label, and description 
    
    public GraderOptionResponse(String key, String label, String description){
        this.key = key;
        this.label = label;
        this.description = description;
    }

    // basic setter and getters below
    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription(){
        return description;
    }
}