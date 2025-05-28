package com.codedocgen.util;

import java.io.BufferedReader;
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
            return "Windows";
        } else if (isMac()) {
            return "macOS";
        } else if (isLinux()) {
            return "Linux";
        }
        return "Unknown";
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
    }
} 