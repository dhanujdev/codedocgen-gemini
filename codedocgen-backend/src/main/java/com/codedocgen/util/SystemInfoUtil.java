package com.codedocgen.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemInfoUtil {

    private static final Logger LOGGER = Logger.getLogger(SystemInfoUtil.class.getName());
    private static String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    public static boolean isWindows() {
        return osName.contains("windows");
    }

    public static boolean isMac() {
        return osName.contains("mac");
    }

    public static boolean isLinux() {
        return osName.contains("linux") || osName.contains("nix");
    }

    public static String getOperatingSystem() {
        if (isWindows()) {
            return "windows";
        } else if (isMac()) {
            return "macos";
        } else if (isLinux()) {
            return "linux";
        }
        return "unknown";
    }

    /**
     * Determines the executable command to use based on configuration and OS.
     * If the configured path is valid, it is used; otherwise, the default command is used.
     * On Windows, handles .cmd and .exe extensions appropriately.
     *
     * @param configuredPath The path configured in application properties
     * @param defaultCommand The default command to use if configured path is invalid
     * @return The executable command to use
     */
    public static String getExecutableCommand(String configuredPath, String defaultCommand) {
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            File execFile = new File(configuredPath);
            if (execFile.exists() && execFile.canExecute()) {
                LOGGER.info("Using configured executable path: " + configuredPath);
                return configuredPath;
            } else if (execFile.isAbsolute()) {
                LOGGER.warning("Configured executable path does not exist or is not executable: " + configuredPath);
                LOGGER.warning("Falling back to default command: " + defaultCommand);
                return defaultCommand;
            } else {
                // It's a relative path or just a command name - we'll use it and let the OS resolve it
                LOGGER.info("Using configured executable name: " + configuredPath);
                return configuredPath;
            }
        }
        
        // Handle Windows-specific command extensions
        if (isWindows() && "mvn".equals(defaultCommand)) {
            return "mvn.cmd";
        }
        
        return defaultCommand;
    }
    
    /**
     * Checks if the given executable is available on the system PATH.
     *
     * @param executableName The name of the executable to check
     * @return true if the executable is found on PATH, false otherwise
     */
    public static boolean isExecutableOnPath(String executableName) {
        try {
            String command = isWindows() ? "where" : "which";
            ProcessBuilder processBuilder = new ProcessBuilder(command, executableName);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Error checking if " + executableName + " is on PATH", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public static String getMavenVersion() {
        String mvnCommand = isWindows() ? "mvn.cmd" : "mvn";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(mvnCommand, "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                    if (line.startsWith("Apache Maven")) {
                        // Example line: Apache Maven 3.8.1 (05c21c65bdfed0f71a2f2ada8b84da59358ea37)
                        return line.split(" ")[2]; // Extracts "3.8.1"
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.log(Level.WARNING, "Maven version check exited with code: " + exitCode + ". Output:\n" + output.toString());
                return "Error detecting Maven version";
            }
            // Fallback if "Apache Maven" line wasn't found but command succeeded
            LOGGER.log(Level.INFO, "Maven --version output:\n" + output.toString());
            return "Maven found, version not parsed from output.";

        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error executing Maven command or reading output", e);
            Thread.currentThread().interrupt(); // Restore interruption status
            return "Error detecting Maven version: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        // For testing purposes
        System.out.println("Operating System: " + getOperatingSystem());
        System.out.println("Maven Version: " + getMavenVersion());
        System.out.println("Is 'mvn' on PATH: " + isExecutableOnPath("mvn"));
        System.out.println("Is 'dot' on PATH: " + isExecutableOnPath("dot"));
    }
} 