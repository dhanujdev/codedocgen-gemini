package com.codedocgen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for monitoring and managing system resources like disk space
 */
@Component
public class ResourceMonitorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ResourceMonitorUtil.class);
    
    @Value("${app.repoStoragePath:/tmp/repos}")
    private String repoStoragePath;
    
    @Value("${app.resourceManagement.autoCleanup:true}")
    private boolean autoCleanup;
    
    @Value("${app.resourceManagement.maxTempFileAge:1800}")
    private int maxTempFileAgeSeconds;
    
    /**
     * Check available disk space and log a warning if it's below a threshold
     * @param directory Directory to check
     * @param thresholdMB Warning threshold in MB
     * @return true if space is above threshold, false otherwise
     */
    public boolean checkDiskSpace(File directory, long thresholdMB) {
        if (directory == null || !directory.exists()) {
            logger.warn("Cannot check disk space for non-existent directory");
            return false;
        }
        
        long freeSpaceMB = directory.getFreeSpace() / (1024 * 1024);
        logger.info("Available disk space in {}: {} MB", directory.getAbsolutePath(), freeSpaceMB);
        
        if (freeSpaceMB < thresholdMB) {
            logger.warn("Low disk space alert: Only {} MB available in {}, threshold is {} MB", 
                    freeSpaceMB, directory.getAbsolutePath(), thresholdMB);
            return false;
        }
        
        return true;
    }
    
    /**
     * Clean up old temporary files in the repository storage path
     * Runs every hour by default
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupTemporaryFiles() {
        if (!autoCleanup) {
            logger.debug("Auto cleanup disabled, skipping temporary file cleanup");
            return;
        }
        
        File reposDir = new File(repoStoragePath);
        if (!reposDir.exists() || !reposDir.isDirectory()) {
            logger.warn("Repository storage path does not exist or is not a directory: {}", repoStoragePath);
            return;
        }
        
        logger.info("Starting cleanup of temporary files in {}", repoStoragePath);
        
        try {
            // Find all classpath files older than maxTempFileAgeSeconds
            Instant cutoffTime = Instant.now().minus(maxTempFileAgeSeconds, ChronoUnit.SECONDS);
            
            List<Path> oldTempFiles;
            try (Stream<Path> pathStream = Files.walk(reposDir.toPath())) {
                oldTempFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        if (path.getFileName().toString().endsWith("_cp.txt")) {
                            try {
                                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                                return attrs.creationTime().toInstant().isBefore(cutoffTime);
                            } catch (IOException e) {
                                logger.warn("Failed to read file attributes for {}: {}", path, e.getMessage());
                                return false;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            }
            
            // Delete the found temporary files
            int deletedCount = 0;
            for (Path file : oldTempFiles) {
                try {
                    Files.delete(file);
                    deletedCount++;
                    logger.debug("Deleted old temporary file: {}", file);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file {}: {}", file, e.getMessage());
                }
            }
            
            logger.info("Cleanup completed. Deleted {} old temporary files", deletedCount);
            
        } catch (IOException e) {
            logger.error("Error during temporary file cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Cleanup unused repositories that haven't been accessed for a while
     * This could be expanded with more sophisticated logic based on your needs
     * @param daysOld Number of days since last access to consider a repository unused
     * @return Number of repositories cleaned up
     */
    public int cleanupUnusedRepositories(int daysOld) {
        File reposDir = new File(repoStoragePath);
        if (!reposDir.exists() || !reposDir.isDirectory()) {
            logger.warn("Repository storage path does not exist or is not a directory: {}", repoStoragePath);
            return 0;
        }
        
        logger.info("Starting cleanup of unused repositories older than {} days in {}", daysOld, repoStoragePath);
        
        Instant cutoffTime = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        int cleanedCount = 0;
        
        // Get immediate subdirectories (each one is a repository)
        File[] repoDirs = reposDir.listFiles(File::isDirectory);
        if (repoDirs == null || repoDirs.length == 0) {
            logger.info("No repositories found for cleanup");
            return 0;
        }
        
        for (File repoDir : repoDirs) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(repoDir.toPath(), BasicFileAttributes.class);
                Instant lastAccessTime = attrs.lastAccessTime().toInstant();
                
                if (lastAccessTime.isBefore(cutoffTime)) {
                    logger.info("Removing unused repository: {} (Last accessed: {})", 
                              repoDir.getName(), lastAccessTime);
                    
                    try {
                        Files.walk(repoDir.toPath())
                             .sorted(Comparator.reverseOrder())
                             .forEach(path -> {
                                 try {
                                     Files.delete(path);
                                 } catch (IOException e) {
                                     logger.warn("Failed to delete {}: {}", path, e.getMessage());
                                 }
                             });
                        cleanedCount++;
                    } catch (IOException e) {
                        logger.error("Failed to delete repository directory {}: {}", 
                                   repoDir.getAbsolutePath(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to read attributes for {}: {}", repoDir.getAbsolutePath(), e.getMessage());
            }
        }
        
        logger.info("Repository cleanup completed. Removed {} unused repositories", cleanedCount);
        return cleanedCount;
    }
} 