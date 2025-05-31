package com.codedocgen.service.impl;

import com.codedocgen.dto.MavenExecutionResult;
import com.codedocgen.service.MavenBuildService;
import com.codedocgen.util.JavaVersionUtil;
import com.codedocgen.util.SystemInfoUtil;
import com.codedocgen.config.TruststoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class MavenBuildServiceImpl implements MavenBuildService {

    private static final Logger logger = LoggerFactory.getLogger(MavenBuildServiceImpl.class);

    @Value("${app.maven.settings.path:#{null}}")
    private String mavenSettingsPath;
    
    @Value("${app.maven.executable.path:mvn}")
    private String mavenExecutablePath;

    private final TruststoreConfig truststoreConfig;

    // Configuration for JDK paths. Example: app.jdk.paths.8=/usr/lib/jvm/java-8,app.jdk.paths.11=/usr/lib/jvm/java-11
    @Value("#{${app.jdk.paths:{}}}") // Default to empty map if not configured
    private Map<String, String> configuredJdkPaths;

    @Autowired
    public MavenBuildServiceImpl(TruststoreConfig truststoreConfig) {
        this.truststoreConfig = truststoreConfig;
    }

    @Override
    public MavenExecutionResult runMavenCommandWithAutoDetect(File projectDir, String... goals) throws IOException, InterruptedException {
        String detectedJavaVersion = null;
        File pomFile = new File(projectDir, "pom.xml");
        if (pomFile.exists() && pomFile.isFile()) {
            detectedJavaVersion = JavaVersionUtil.detectJavaVersionFromPom(pomFile);
            if (detectedJavaVersion != null) {
                logger.info("Auto-detected Java version {} for project in {}", detectedJavaVersion, projectDir.getAbsolutePath());
            } else {
                logger.warn("Could not auto-detect Java version from pom.xml in {}. Will use system default Maven JDK for auto-detect path.", projectDir.getAbsolutePath());
            }
        } else {
            logger.warn("No pom.xml found in {}. Cannot auto-detect Java version. Will use system default Maven JDK for auto-detect path.", projectDir.getAbsolutePath());
        }
        return runMavenCommandWithExplicitVersion(projectDir, detectedJavaVersion, goals);
    }

    @Override
    public MavenExecutionResult runMavenCommandWithExplicitVersion(File projectDir, String detectedJavaVersion, String... goalsForExplicit) throws IOException, InterruptedException {
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            String errorMsg = "Project directory " + projectDir.getAbsolutePath() + " does not exist or is not a directory.";
            logger.error(errorMsg);
            return new MavenExecutionResult( -1, "Project directory not found: " + projectDir.getAbsolutePath());
        }

        // Use the executable command helper to get OS-aware mvn command
        String mvnCommand = SystemInfoUtil.getExecutableCommand(mavenExecutablePath, SystemInfoUtil.isWindows() ? "mvn.cmd" : "mvn");
        List<String> commandParts = new ArrayList<>();
        commandParts.add(mvnCommand);

        // Process custom Maven settings file if configured
        String resolvedSettingsPath = null;
        File tempSettingsFile = null;
        
        if (mavenSettingsPath != null && !mavenSettingsPath.trim().isEmpty()) {
            String settingsPath = mavenSettingsPath.trim();
            
            // Handle classpath resource
            if (settingsPath.startsWith("classpath:")) {
                String resourcePath = settingsPath.substring("classpath:".length());
                try {
                    Resource resource = new ClassPathResource(resourcePath);
                    if (resource.exists()) {
                        // Create temporary file
                        tempSettingsFile = File.createTempFile("maven-settings", ".xml");
                        tempSettingsFile.deleteOnExit();
                        
                        // Copy classpath resource to temporary file
                        try (FileOutputStream output = new FileOutputStream(tempSettingsFile)) {
                            FileCopyUtils.copy(resource.getInputStream(), output);
                        }
                        
                        resolvedSettingsPath = tempSettingsFile.getAbsolutePath();
                        logger.info("Copied classpath Maven settings from {} to temporary file: {}", resourcePath, resolvedSettingsPath);
                    } else {
                        logger.warn("Maven settings classpath resource not found: {}. Proceeding without custom settings.", resourcePath);
                    }
                } catch (IOException e) {
                    logger.error("Failed to copy Maven settings from classpath resource: {}. Error: {}", resourcePath, e.getMessage(), e);
                }
            } else {
                // Handle file path (absolute or relative)
                File settingsFile = new File(settingsPath);
                if (settingsFile.exists() && settingsFile.isFile()) {
                    resolvedSettingsPath = settingsFile.getAbsolutePath();
                    logger.info("Using Maven settings file: {}", resolvedSettingsPath);
                } else {
                    logger.warn("Maven settings file specified but not found or not a file: {}. Proceeding without custom settings.", settingsPath);
                }
            }
        } else {
            logger.info("No custom Maven settings file specified. Using default Maven settings.");
        }

        // Add settings file argument if we have resolved a path
        if (resolvedSettingsPath != null) {
            commandParts.add("--settings");
            commandParts.add(resolvedSettingsPath);
        }
        
        // Add truststore properties if configured
        String effectiveTrustStorePath = truststoreConfig.getEffectiveTrustStorePath();
        String effectiveTrustStorePassword = truststoreConfig.getEffectiveTrustStorePassword();

        if (effectiveTrustStorePath != null && !effectiveTrustStorePath.trim().isEmpty()) {
            File tsFile = new File(effectiveTrustStorePath);
            if (tsFile.exists() && tsFile.isFile()) {
                commandParts.add("-Djavax.net.ssl.trustStore=" + tsFile.getAbsolutePath());
                logger.info("Setting javax.net.ssl.trustStore to: {}", tsFile.getAbsolutePath());
                if (effectiveTrustStorePassword != null) {
                    commandParts.add("-Djavax.net.ssl.trustStorePassword=" + effectiveTrustStorePassword);
                    logger.info("Setting javax.net.ssl.trustStorePassword.");
                }
            } else {
                 logger.warn("Effective truststore path {} configured but file does not exist. Skipping truststore JVM args for Maven.", effectiveTrustStorePath);
            }
        }

        commandParts.addAll(Arrays.asList(goalsForExplicit));

        // Log the command without sensitive info
        String commandForLogging = String.join(" ", commandParts)
                .replaceAll("-Djavax\\.net\\.ssl\\.trustStorePassword=[^ ]+", "-Djavax.net.ssl.trustStorePassword=******");
        logger.info("Executing Maven command: {} in directory: {} (explicit version path)", commandForLogging, projectDir.getAbsolutePath());
        
        if (detectedJavaVersion != null) {
            logger.info("Attempting to use explicitly provided Java version: {}", detectedJavaVersion);
        } else {
            logger.info("No explicit Java version provided (null). Maven will use its default JDK.");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.directory(projectDir);

        Map<String, String> environment = processBuilder.environment();
        String originalJavaHome = environment.get("JAVA_HOME");

        if (detectedJavaVersion != null) {
            String jdkPath = findJdkPathForVersion(detectedJavaVersion);
            if (jdkPath != null && new File(jdkPath).exists()) {
                logger.info("Setting JAVA_HOME to: {} for Maven execution (explicit version path).", jdkPath);
                environment.put("JAVA_HOME", jdkPath);
            } else {
                logger.warn("JDK path for explicitly provided version {} not found or configured. Maven will use its default JDK (explicit version path).", detectedJavaVersion);
            }
        } // If detectedJavaVersion is null, we intentionally don't modify JAVA_HOME here, letting Maven use its default

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder mavenOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                mavenOutput.append(line).append(System.lineSeparator());
                logger.info("[MAVEN] {}", line);
            }
        }

        int exitCode = process.waitFor();
        logger.info("Maven command (explicit version path) finished with exit code: {}", exitCode);

        // Restore original JAVA_HOME if we changed it
        if (originalJavaHome != null) {
            environment.put("JAVA_HOME", originalJavaHome);
        } else {
            environment.remove("JAVA_HOME");
        }

        // Clean up temporary settings file if we created one
        if (tempSettingsFile != null && tempSettingsFile.exists()) {
            try {
                Files.delete(tempSettingsFile.toPath());
                logger.debug("Deleted temporary Maven settings file: {}", tempSettingsFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to delete temporary Maven settings file: {}", tempSettingsFile.getAbsolutePath(), e);
            }
        }

        if (exitCode != 0) {
            logger.error("Maven command (explicit version path) failed. Exit code {}. Goals: {}\nOutput:\n{}", exitCode, String.join(" ", goalsForExplicit), mavenOutput.toString());
        } else {
            // logger.debug("Maven command (explicit version path) successful. Goals: {}\nOutput:\n{}", String.join(" ", goalsForExplicit), mavenOutput.toString());
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