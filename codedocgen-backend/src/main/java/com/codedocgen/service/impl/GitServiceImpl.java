package com.codedocgen.service.impl;

import com.codedocgen.service.GitService;
import com.codedocgen.util.JavaVersionUtil;
import com.codedocgen.util.SystemInfoUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the GitService for Git repository operations
 */
@Service
public class GitServiceImpl implements GitService {

    private static final Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);
    
    @Value("${app.git.username:}")
    private String gitUsername;
    
    @Value("${app.git.password:}")
    private String gitPassword;
    
    @Value("${app.repoStoragePath:/tmp/repos}")
    private String repoStoragePath;
    
    @Value("${app.git.shallowClone:true}")
    private boolean shallowClone;
    
    @Value("${app.git.depth:1}")
    private int cloneDepth;
    
    @Value("${app.git.cloneTimeout:300}")
    private int cloneTimeout;

    @Override
    public File cloneRepository(String repoUrl, String localPath, String branch, String username, String password) throws IOException {
        logger.info("Cloning Git repository from URL: {}", repoUrl);
        
        CredentialsProvider credentialsProvider = null;
        if (username != null && !username.isEmpty() && password != null) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
            logger.info("Using provided credentials for Git authentication");
        }
        
        File localDir = new File(localPath);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        
        try {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localDir)
                .setProgressMonitor(new LoggingProgressMonitor())
                .setCredentialsProvider(credentialsProvider)
                .setBranch(branch != null && !branch.isEmpty() ? branch : null)
                .call();
            
            logger.info("Successfully cloned repository to: {}", localDir.getAbsolutePath());
            return localDir;
        } catch (GitAPIException e) {
            logger.error("Error cloning Git repository", e);
            throw new IOException("Failed to clone Git repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteRepository(File repositoryDir) {
        logger.info("Deleting local Git repository: {}", repositoryDir.getAbsolutePath());
        
        if (!repositoryDir.exists()) {
            logger.warn("Repository directory does not exist: {}", repositoryDir.getAbsolutePath());
            return true; // Already deleted
        }
        
        try {
            Path pathToBeDeleted = Paths.get(repositoryDir.getAbsolutePath());
            Files.walk(pathToBeDeleted)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            
            logger.info("Successfully deleted repository directory: {}", repositoryDir.getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error("Error deleting repository directory", e);
            return false;
        }
    }

    private static class LoggingProgressMonitor implements ProgressMonitor {
        private static final Logger logger = LoggerFactory.getLogger(LoggingProgressMonitor.class);
        private int totalWork;
        private int completed;
        private String task;
        private long lastUpdate;
        
        @Override
        public void start(int totalTasks) {
            logger.info("Starting Git operation with {} total tasks", totalTasks);
        }
        
        @Override
        public void beginTask(String title, int totalWork) {
            this.task = title;
            this.totalWork = totalWork;
            this.completed = 0;
            this.lastUpdate = System.currentTimeMillis();
            logger.info("Beginning task: {} (total work: {})", title, totalWork);
        }
        
        @Override
        public void update(int completed) {
            this.completed += completed;
            
            // Only log every 3 seconds to avoid too many log messages
            long now = System.currentTimeMillis();
            if (now - lastUpdate > TimeUnit.SECONDS.toMillis(3)) {
                int percentage = totalWork > 0 ? (this.completed * 100 / totalWork) : 0;
                logger.info("Progress on {}: {}% ({}/{})", task, percentage, this.completed, totalWork);
                lastUpdate = now;
            }
        }
        
        @Override
        public void endTask() {
            logger.info("Task completed: {}", task);
        }
        
        @Override
        public boolean isCancelled() {
            return false;
        }
        
        @Override
        public void showDuration(boolean enabled) {
            // Method added in newer JGit versions
            // Just a no-op implementation for compatibility
        }
    }
} 