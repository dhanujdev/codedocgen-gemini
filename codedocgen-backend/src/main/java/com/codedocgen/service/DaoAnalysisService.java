package com.codedocgen.service;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DaoOperationDetail;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing DAO/Repository classes to extract database operations
 */
public interface DaoAnalysisService {
    
    /**
     * Analyzes classes to identify and extract database operations
     * @param classes List of class metadata to analyze
     * @param projectDir The root directory of the project
     * @return Map of class FQN to list of DAO operations
     */
    Map<String, List<DaoOperationDetail>> analyzeDbOperations(List<ClassMetadata> classes, File projectDir);
    
    /**
     * Generate diagram showing database tables and their relationships
     * @param allClassMetadata List of all class metadata
     * @param daoOperations The map of class FQN to DAO operations
     * @param outputPath The path to save the diagram
     * @return The path to the generated diagram
     */
    String generateDbDiagram(List<ClassMetadata> allClassMetadata, Map<String, List<DaoOperationDetail>> daoOperations, String outputPath);
} 