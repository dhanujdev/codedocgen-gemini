package com.codedocgen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for system information and path operations
 */
public class SystemInfoUtil {
    private static final Logger logger = LoggerFactory.getLogger(SystemInfoUtil.class);
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("nux") || OS_NAME.contains("nix");
    
    /**
     * Checks if the system is running Windows
     * @return true if Windows, false otherwise
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }
    
    /**
     * Checks if the system is running macOS
     * @return true if macOS, false otherwise
     */
    public static boolean isMac() {
        return IS_MAC;
    }
    
    /**
     * Checks if the system is running Linux
     * @return true if Linux, false otherwise
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }
    
    /**
     * Gets the operating system name
     * @return operating system name
     */
    public static String getOsName() {
        return OS_NAME;
    }
    
    /**
     * Gets the operating system name and version
     * @return operating system name and version
     */
    public static String getOperatingSystem() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        return osName + " " + osVersion + " (" + osArch + ")";
    }
    
    /**
     * Gets the Maven version installed on the system
     * @return Maven version, or "Not available" if Maven is not found
     */
    public static String getMavenVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String mavenCommand = isWindows() ? "mvn.cmd" : "mvn";
            processBuilder.command(mavenCommand, "--version");
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.startsWith("Apache Maven")) {
                        boolean completed = process.waitFor(1, TimeUnit.SECONDS);
                        if (!completed) {
                            process.destroyForcibly();
                        }
                        return line.trim();
                    }
                }
            }
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                logger.warn("Maven version check timed out");
                return "Unknown version (check timed out)";
            }
            
            if (process.exitValue() != 0) {
                logger.warn("Maven version check failed with exit code: {}", process.exitValue());
                return "Not available";
            }
            
            return "Maven detected (version details not found)";
        } catch (Exception e) {
            logger.warn("Failed to determine Maven version: {}", e.getMessage());
            return "Not available";
        }
    }
    
    /**
     * Gets the path to the system temp directory
     * @return system temp directory path
     */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * Gets the user home directory
     * @return user home directory path
     */
    public static String getUserHome() {
        return System.getProperty("user.home");
    }
    
    /**
     * Gets the current working directory
     * @return current working directory path
     */
    public static String getCurrentWorkingDir() {
        return System.getProperty("user.dir");
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
                logger.info("Using configured executable path: {}", configuredPath);
                return configuredPath;
            } else if (execFile.isAbsolute()) {
                logger.warn("Configured executable path does not exist or is not executable: {}", configuredPath);
                logger.warn("Falling back to default command: {}", defaultCommand);
                return defaultCommand;
            } else {
                // It's a relative path or just a command name - we'll use it and let the OS resolve it
                logger.info("Using configured executable name: {}", configuredPath);
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
     * Checks if an executable exists in the system PATH
     * @param executableName name of the executable to check
     * @return true if the executable is found, false otherwise
     */
    public static boolean isExecutableInPath(String executableName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return false;
        }
        
        String[] pathDirs = pathEnv.split(File.pathSeparator);
        for (String pathDir : pathDirs) {
            Path execPath = Paths.get(pathDir, executableName);
            if (Files.isExecutable(execPath)) {
                return true;
            }
            
            // For Windows, also check with .exe extension
            if (IS_WINDOWS && !executableName.endsWith(".exe")) {
                execPath = Paths.get(pathDir, executableName + ".exe");
                if (Files.isExecutable(execPath)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Finds the full path to an executable in the system PATH
     * @param executableName name of the executable to find
     * @return the full path if found, null otherwise
     */
    public static String findExecutablePath(String executableName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }
        
        String[] pathDirs = pathEnv.split(File.pathSeparator);
        for (String pathDir : pathDirs) {
            Path execPath = Paths.get(pathDir, executableName);
            if (Files.isExecutable(execPath)) {
                return execPath.toString();
            }
            
            // For Windows, also check with .exe extension
            if (IS_WINDOWS && !executableName.endsWith(".exe")) {
                execPath = Paths.get(pathDir, executableName + ".exe");
                if (Files.isExecutable(execPath)) {
                    return execPath.toString();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Sanitizes file paths for safe logging (removes sensitive information)
     * @param path file path to sanitize
     * @return sanitized path
     */
    public static String sanitizePathForLogging(String path) {
        if (path == null) {
            return null;
        }
        
        // Replace user home directory with ~
        String userHome = System.getProperty("user.home");
        if (path.startsWith(userHome)) {
            path = "~" + path.substring(userHome.length());
        }
        
        return path;
    }
} 