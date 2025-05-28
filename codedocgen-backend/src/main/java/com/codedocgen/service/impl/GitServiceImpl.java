package com.codedocgen.service.impl;

import com.codedocgen.service.GitService;
import com.codedocgen.util.JavaVersionUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Service
public class GitServiceImpl implements GitService {

    private static final Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

    @Override
    public File cloneRepository(String repoUrl, String localPath) throws GitAPIException, IOException {
        logger.info("Cloning repository from {} to {}", repoUrl, localPath);
        File localDir = new File(localPath);
        if (localDir.exists()) {
            logger.warn("Local path {} already exists. Deleting it to ensure a fresh clone.", localPath);
            deleteRepository(localDir);
        }
        if (!localDir.mkdirs() && !localDir.exists()) {
             logger.error("Could not create directory: {}", localDir.getAbsolutePath());
            throw new IOException("Could not create directory: " + localDir.getAbsolutePath());
        }

        try (Git result = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localDir)
                .setCloneAllBranches(false) // For initial scan, only main branch is usually enough
                .setDepth(1) // Shallow clone for speed, full history might not be needed for doc gen
                .call()) {
            logger.info("Repository cloned successfully to: {}", result.getRepository().getDirectory().getParent());

            // Detect and log Java version from pom.xml
            File pomFile = new File(localDir, "pom.xml");
            if (pomFile.exists()) {
                String detectedJavaVersion = JavaVersionUtil.detectJavaVersionFromPom(pomFile);
                if (detectedJavaVersion != null) {
                    logger.info("Detected Java version {} for project in {}", detectedJavaVersion, localDir.getAbsolutePath());
                } else {
                    logger.warn("Could not detect Java version from pom.xml in {}", localDir.getAbsolutePath());
                }
            } else {
                logger.info("No pom.xml found in the root of the cloned repository at {}. Skipping Java version detection.", localDir.getAbsolutePath());
            }

            return localDir;
        } catch (GitAPIException e) {
            logger.error("Error cloning repository {}: {}", repoUrl, e.getMessage(), e);
            // Attempt to clean up partially cloned directory
            if (localDir.exists()) {
                try {
                    deleteRepository(localDir);
                } catch (IOException ex) {
                    logger.error("Failed to delete partially cloned directory {} after clone error: {}", localPath, ex.getMessage(), ex);
                }
            }
            throw e;
        }
    }

    @Override
    public void deleteRepository(File repoDir) throws IOException {
        if (repoDir != null && repoDir.exists()) {
            logger.info("Deleting directory: {}", repoDir.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(repoDir);
                logger.info("Directory {} deleted successfully.", repoDir.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to delete directory {}: {}", repoDir.getAbsolutePath(), e.getMessage(), e);
                throw e;
            }
        }
    }
} 