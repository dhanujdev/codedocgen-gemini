package com.codedocgen.service;

import java.io.File;

public interface ProjectDetectorService {
    String detectBuildTool(File projectDir);
    boolean isSpringBootProject(File projectDir);
    String detectSpringBootVersion(File projectDir); // Could be complex, might involve parsing pom.xml/build.gradle
    // Add other detection methods as needed (e.g., for legacy project characteristics)
} 