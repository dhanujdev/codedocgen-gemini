package com.codedocgen.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for repository request information
 */
@Data
@NoArgsConstructor
public class RepoRequest {
    private String repoUrl;
    private String username;
    private String password;
    private String branch;
    private boolean skipTests;
    private boolean includeWsdl;
    private boolean includeFeatureFiles;
    
    public String getRepoUrl() {
        return repoUrl;
    }
    
    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public boolean isSkipTests() {
        return skipTests;
    }
    
    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }
    
    public boolean isIncludeWsdl() {
        return includeWsdl;
    }
    
    public void setIncludeWsdl(boolean includeWsdl) {
        this.includeWsdl = includeWsdl;
    }
    
    public boolean isIncludeFeatureFiles() {
        return includeFeatureFiles;
    }
    
    public void setIncludeFeatureFiles(boolean includeFeatureFiles) {
        this.includeFeatureFiles = includeFeatureFiles;
    }
} 