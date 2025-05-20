package com.codedocgen.service;

import com.codedocgen.model.ClassMetadata;
import java.io.File;
import java.util.List;

public interface JavaParserService {
    List<ClassMetadata> parseProject(File projectDir);
    List<ClassMetadata> parseProject(File projectDir, List<String> parseWarnings);
    ClassMetadata parseFile(File javaFile);
    // Potentially add methods for specific parsing tasks, e.g., find all annotations of a certain type
} 