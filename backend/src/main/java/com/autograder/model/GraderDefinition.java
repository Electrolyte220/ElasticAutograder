package com.autograder.model;

public class GraderDefinition {
    private final String key;
    private final String label;
    private final String imageName;
    private final String manifestPath;

    public GraderDefinition(String key, String label, String imageName, String manifestPath) {
        this.key = key;
        this.label = label;
        this.imageName = imageName;
        this.manifestPath = manifestPath;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getImageName() {
        return imageName;
    }

    public String getManifestPath() {
        return manifestPath;
    }
}