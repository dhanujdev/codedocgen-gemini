package com.codedocgen.service;

import java.io.File;
import java.io.IOException;

/**
 * Service for Git repository operations
 */
public interface GitService {
    /**
     * Clones a Git repository to a local directory
     * @param repoUrl Git repository URL
     * @param localPath Local path to clone to
     * @param branch Branch to clone (optional)
     * @param username Username for authentication (optional)
     * @param password Password for authentication (optional)
     * @return The local directory containing the cloned repository
     * @throws IOException If cloning fails
     */
    File cloneRepository(String repoUrl, String localPath, String branch, String username, String password) throws IOException;
    
    /**
     * Deletes a local Git repository
     * @param repositoryDir Directory containing the repository
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteRepository(File repositoryDir);
} 