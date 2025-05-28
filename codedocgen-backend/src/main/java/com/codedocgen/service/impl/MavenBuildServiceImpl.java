package com.codedocgen.service.impl;

import com.codedocgen.dto.MavenExecutionResult;
import com.codedocgen.service.MavenBuildService;
import com.codedocgen.util.JavaVersionUtil;
import com.codedocgen.util.SystemInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class MavenBuildServiceImpl implements MavenBuildService {

    private static final Logger logger = LoggerFactory.getLogger(MavenBuildServiceImpl.class);

    // Configuration for JDK paths. Example: app.jdk.paths.8=/usr/lib/jvm/java-8,app.jdk.paths.11=/usr/lib/jvm/java-11
    @Value("#{${app.jdk.paths:{}}}") // Default to empty map if not configured
    private Map<String, String> configuredJdkPaths;

    @Override
    public MavenExecutionResult runMavenCommand(File projectDir, String... goals) throws IOException, InterruptedException {
        String detectedJavaVersion = null;
        File pomFile = new File(projectDir, "pom.xml");
        if (pomFile.exists() && pomFile.isFile()) {
            detectedJavaVersion = JavaVersionUtil.detectJavaVersionFromPom(pomFile);
            if (detectedJavaVersion != null) {
                logger.info("Detected Java version {} for project in {}", detectedJavaVersion, projectDir.getAbsolutePath());
            } else {
                logger.warn("Could not detect Java version from pom.xml in {}. Will use system default Maven JDK.", projectDir.getAbsolutePath());
            }
        } else {
            logger.warn("No pom.xml found in {}. Cannot detect Java version. Will use system default Maven JDK.", projectDir.getAbsolutePath());
        }
        return runMavenCommand(projectDir, detectedJavaVersion, goals);
    }

    @Override
    public MavenExecutionResult runMavenCommand(File projectDir, String detectedJavaVersion, String... goals) throws IOException, InterruptedException {
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            String errorMsg = "Project directory " + projectDir.getAbsolutePath() + " does not exist or is not a directory.";
            logger.error(errorMsg);
            // Still return a MavenExecutionResult to keep method signature consistent
            return new MavenExecutionResult( -1, "Project directory not found: " + projectDir.getAbsolutePath());
        }

        String mvnCommand = SystemInfoUtil.isWindows() ? "mvn.cmd" : "mvn";
        List<String> commandParts = new ArrayList<>();
        commandParts.add(mvnCommand);
        commandParts.addAll(Arrays.asList(goals));

        logger.info("Executing Maven command: {} in directory: {}", String.join(" ", commandParts), projectDir.getAbsolutePath());
        if (detectedJavaVersion != null) {
            logger.info("Attempting to use Java version: {}", detectedJavaVersion);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.directory(projectDir);

        Map<String, String> environment = processBuilder.environment();
        String originalJavaHome = environment.get("JAVA_HOME");

        if (detectedJavaVersion != null) {
            String jdkPath = findJdkPathForVersion(detectedJavaVersion);
            if (jdkPath != null && new File(jdkPath).exists()) {
                logger.info("Setting JAVA_HOME to: {} for Maven execution.", jdkPath);
                environment.put("JAVA_HOME", jdkPath);
            } else {
                logger.warn("JDK path for version {} not found or configured. Maven will use its default JDK.", detectedJavaVersion);
            }
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder mavenOutput = new StringBuilder(); // Capture output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                mavenOutput.append(line).append(System.lineSeparator());
                logger.info("[MAVEN] {}", line); // Continue logging for real-time feedback
            }
        }

        int exitCode = process.waitFor();
        logger.info("Maven command finished with exit code: {}", exitCode);

        if (originalJavaHome != null) {
            environment.put("JAVA_HOME", originalJavaHome);
        } else {
            environment.remove("JAVA_HOME");
        }

        if (exitCode != 0) {
            logger.error("Maven command failed with exit code {}. Goals: {}\nOutput:\n{}", exitCode, String.join(" ", goals), mavenOutput.toString());
        } else {
            // Optionally log output even on success for debugging, can be very verbose
            // logger.debug("Maven command successful. Goals: {}\nOutput:\n{}", String.join(" ", goals), mavenOutput.toString());
        }
        return new MavenExecutionResult(exitCode, mavenOutput.toString());
    }

    /**
     * Finds the path to a JDK installation for the given Java version string (e.g., "8", "11").
     * This method relies on the `app.jdk.paths` configuration in application.properties.
     * Example format in application.properties:
     * app.jdk.paths.8=/path/to/jdk8
     * app.jdk.paths.11=/path/to/jdk11
     * app.jdk.paths.17=/opt/jdks/jdk-17
     *
     * @param javaVersion The major Java version string (e.g., "8", "11").
     * @return The path to the JDK, or null if not found/configured.
     */
    private String findJdkPathForVersion(String javaVersion) {
        if (javaVersion == null || javaVersion.trim().isEmpty()) {
            return null;
        }
        String normalizedVersion = javaVersion.startsWith("1.") ? javaVersion.substring(2) : javaVersion;
        
        String path = configuredJdkPaths.get(normalizedVersion);
        if (path != null) {
             logger.info("Found configured JDK path for Java {}: {}", normalizedVersion, path);
             return path;
        }
        
        // Fallback: check common environment variables like JAVA_HOME_8_X64, etc.
        // This part is highly system-dependent and might need more robust discovery
        String envVarName = "JAVA_HOME_" + normalizedVersion;
        String envPath = System.getenv(envVarName);
        if (envPath != null && !envPath.isEmpty()) {
            logger.info("Found JDK path for Java {} via environment variable {}: {}", normalizedVersion, envVarName, envPath);
            return envPath;
        }
        
        // Check for specific versions if a more general one (e.g. 11 for 11.0.2) is passed
        for(Map.Entry<String, String> entry : configuredJdkPaths.entrySet()){
            if(normalizedVersion.startsWith(entry.getKey())){
                logger.info("Found configured JDK path for Java {} (matched {}): {}", normalizedVersion, entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }

        logger.warn("No JDK path configured or found via environment variable for Java version: {}", normalizedVersion);
        return null;
    }
} 