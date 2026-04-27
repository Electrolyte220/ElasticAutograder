package com.autograder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalGraderSetupRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveSetupScript_findsScriptFromRepoRoot() throws Exception {
        Path scriptPath = tempDir.resolve("scripts").resolve("setup-graders.py");
        Files.createDirectories(scriptPath.getParent());
        Files.writeString(scriptPath, "print('hello')");

        LocalGraderSetupRunner runner = new LocalGraderSetupRunner(true, false, "python");

        Path resolved = runner.resolveSetupScript(tempDir);

        assertEquals(scriptPath.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void resolveSetupScript_findsScriptFromBackendDirectory() throws Exception {
        Path scriptPath = tempDir.resolve("scripts").resolve("setup-graders.py");
        Path backendDir = tempDir.resolve("backend");
        Files.createDirectories(scriptPath.getParent());
        Files.createDirectories(backendDir);
        Files.writeString(scriptPath, "print('hello')");

        LocalGraderSetupRunner runner = new LocalGraderSetupRunner(true, false, "python");

        Path resolved = runner.resolveSetupScript(backendDir);

        assertEquals(scriptPath.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void resolveSetupScript_throwsWhenScriptMissing() {
        LocalGraderSetupRunner runner = new LocalGraderSetupRunner(true, false, "python");

        assertThrows(IllegalStateException.class, () -> runner.resolveSetupScript(tempDir));
    }
}
