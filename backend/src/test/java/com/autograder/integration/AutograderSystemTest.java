package com.autograder.integration;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class AutograderSystemTest {

@Test
void fullAutograderPipeline_executesSuccessfully() throws Exception {

    System.out.println("SYSTEM TEST IS RUNNING");

    String submissionFile = "fibpass1.py";
    String problem = "fibbonaci";

    File runtimeDir = new File("grading/image-build/runtime");
    File submission = new File("../mocksubmission", submissionFile);
    assertTrue(submission.exists(), "Submission file does not exist: " + submission.getPath());

    File manifest = new File("grading/image-build/" + problem + "/manifest.json");

    ProcessBuilder pb = new ProcessBuilder(
            "python",
            "main.py",
            submission.getAbsolutePath(),
            manifest.getAbsolutePath()
    );

    pb.directory(runtimeDir);
    pb.redirectErrorStream(true);

    Process process = pb.start();

    BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
    );

    StringBuilder output = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
    }

    int exitCode = process.waitFor();

    System.out.println("EXIT CODE = " + exitCode);
    System.out.println("OUTPUT = \n" + output);

    assertEquals(0, exitCode);
    assertTrue(output.length() > 0);
}
}