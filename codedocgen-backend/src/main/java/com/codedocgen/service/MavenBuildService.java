package com.codedocgen.service;

import com.codedocgen.dto.MavenExecutionResult;
import java.io.File;
import java.io.IOException;

public interface MavenBuildService {

    /**
     * Runs a Maven command on the specified project directory, automatically detecting
     * the Java version from the project's pom.xml if possible.
     *
     * @param projectDir The directory of the Maven project.
     * @param goals The Maven goals to execute (e.g., "clean", "install").
     * @return The result of the Maven execution, including exit code and output.
     * @throws IOException If an I/O error occurs during command execution.
     * @throws InterruptedException If the command execution is interrupted.
     */
    MavenExecutionResult runMavenCommandWithAutoDetect(File projectDir, String... goals) throws IOException, InterruptedException;

    /**
     * Runs a Maven command on the specified project directory, attempting to use a specific, explicitly provided Java version.
     *
     * @param projectDir The directory of the Maven project.
     * @param detectedJavaVersion The Java version to attempt to use for the project (e.g., "8", "11", "17").
     *                            If null, Maven's default JDK will be used.
     * @param goalsForExplicit The Maven goals to execute (e.g., "clean", "install").
     * @return The result of the Maven execution, including exit code and output.
     * @throws IOException If an I/O error occurs during command execution.
     * @throws InterruptedException If the command execution is interrupted.
     */
    MavenExecutionResult runMavenCommandWithExplicitVersion(File projectDir, String detectedJavaVersion, String... goalsForExplicit) throws IOException, InterruptedException;
} 