package com.autograder.config;

import com.autograder.model.GraderDefinition;

import java.util.List;

// class exists to read it form the main config graders.json file :P
public class GraderConfig {
    private List<GraderDefinition> graders;

    //default getter and setter for graders
    public List<GraderDefinition> getGraders() {
        return graders;
    }

    public void setGraders(List<GraderDefinition> graders) {
        this.graders = graders;
    }
}