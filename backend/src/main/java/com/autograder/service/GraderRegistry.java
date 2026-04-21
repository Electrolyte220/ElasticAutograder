package com.autograder.service;

import com.autograder.config.GraderConfigLoader;
import com.autograder.model.GraderDefinition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
public class GraderRegistry {

    private final Map<String, GraderDefinition> graders;

    @Autowired
    public GraderRegistry(GraderConfigLoader graderConfigLoader) {
        List<GraderDefinition> loadedGraders = graderConfigLoader.loadGraders();

        this.graders = loadedGraders.stream()
                .collect(Collectors.toMap(
                        GraderDefinition::getKey,
                        Function.identity()
                ));
    }

    // manual constructor for graderRegistry object based off a set list 
    public GraderRegistry(List<GraderDefinition> graderDefinitions){
        this.graders = graderDefinitions.stream()
                .collect(Collectors.toMap(
                        GraderDefinition::getKey,
                        Function.identity()
                ));

    }

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