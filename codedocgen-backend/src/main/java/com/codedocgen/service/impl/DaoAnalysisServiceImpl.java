package com.codedocgen.service.impl;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DaoOperationDetail;
import com.codedocgen.model.DiagramType;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.parser.DaoAnalyzer;
import com.codedocgen.service.DaoAnalysisService;
import com.codedocgen.util.PlantUMLRenderer;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DaoAnalysisServiceImpl implements DaoAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DaoAnalysisServiceImpl.class);
    private final DaoAnalyzer daoAnalyzer;
    private final PlantUMLRenderer plantUMLRenderer;

    private static final Set<String> DAO_METHOD_PATTERNS = new HashSet<>(Arrays.asList(
            "find", "get", "load", "select", "query", 
            "save", "insert", "persist", "store", 
            "update", "merge", 
            "delete", "remove", 
            "execute"
    ));

    @Autowired
    public DaoAnalysisServiceImpl(DaoAnalyzer daoAnalyzer, PlantUMLRenderer plantUMLRenderer) {
        this.daoAnalyzer = daoAnalyzer;
        this.plantUMLRenderer = plantUMLRenderer;
    }

    @Override
    public Map<String, List<DaoOperationDetail>> analyzeDbOperations(List<ClassMetadata> classes, File projectDir) {
        Map<String, List<DaoOperationDetail>> result = new HashMap<>();
        
        // Check for null inputs
        if (classes == null || projectDir == null) {
            logger.warn("Null input: classes={}, projectDir={}", classes == null ? "null" : classes.size(), projectDir);
            return result;
        }
        
        // Filter for potential DAO/Repository classes
        List<ClassMetadata> daoCandidates = classes.stream()
                .filter(Objects::nonNull)  // Filter out null classes
                .filter(this::isPotentialDaoClass)
                .collect(Collectors.toList());
        
        logger.info("Found {} potential DAO/Repository classes for analysis", daoCandidates.size());
        
        // Count successfully processed classes
        int processedCount = 0;
        
        for (ClassMetadata classMetadata : daoCandidates) {
            // Skip if package name or class name is null
            if (classMetadata.getPackageName() == null || classMetadata.getName() == null) {
                logger.warn("Skipping class with null package name or class name");
                continue;
            }
            
            String fullyQualifiedName = classMetadata.getPackageName() + "." + classMetadata.getName();
            List<DaoOperationDetail> operations = new ArrayList<>();
            
            try {
                // First attempt: analyze from source code if available
                if (classMetadata.getFilePath() != null) {
                    File sourceFile = new File(projectDir, classMetadata.getFilePath());
                    if (sourceFile.exists()) {
                        try {
                            List<DaoOperationDetail> sourceOps = analyzeSourceFile(sourceFile, classMetadata.getName());
                            if (sourceOps != null && !sourceOps.isEmpty()) {
                                operations.addAll(sourceOps);
                                logger.debug("Successfully analyzed source file for {}: {} operations found", 
                                            fullyQualifiedName, sourceOps.size());
                            }
                        } catch (Exception e) {
                            logger.debug("Error parsing source file {}: {}", classMetadata.getFilePath(), e.getMessage());
                            // Continue to metadata analysis as fallback
                        }
                    } else {
                        logger.debug("Source file not found: {}", classMetadata.getFilePath());
                    }
                }
                
                // Second attempt: analyze from method information if no operations found yet
                // or to augment the source-based operations
                if (classMetadata.getMethods() != null) {
                    List<DaoOperationDetail> metadataOps = analyzeFromMethodMetadata(classMetadata);
                    
                    // If we found operations from source code, only add operations for methods not already covered
                    if (!operations.isEmpty() && !metadataOps.isEmpty()) {
                        Set<String> existingMethodNames = operations.stream()
                            .map(DaoOperationDetail::getMethodName)
                            .collect(Collectors.toSet());
                        
                        metadataOps.stream()
                            .filter(op -> !existingMethodNames.contains(op.getMethodName()))
                            .forEach(operations::add);
                    } else {
                        operations.addAll(metadataOps);
                    }
                }
                
                // Special handling for Spring Data repositories
                if (isSpringDataRepository(classMetadata) && operations.isEmpty()) {
                    // For Spring Data repositories with no concrete implementations,
                    // generate synthetic operations for common methods
                    operations.addAll(generateSpringDataRepositoryOperations(classMetadata));
                    logger.debug("Generated synthetic operations for Spring Data repository {}", fullyQualifiedName);
                }
                
                if (!operations.isEmpty()) {
                    result.put(fullyQualifiedName, operations);
                    processedCount++;
                    logger.debug("Found {} database operations in {}", operations.size(), fullyQualifiedName);
                }
            } catch (Exception e) {
                logger.warn("Error analyzing DAO class {}: {}", fullyQualifiedName, e.getMessage());
            }
        }
        
        logger.info("Completed DAO analysis with {} classes containing database operations (out of {} candidates)", 
                  processedCount, daoCandidates.size());
        return result;
    }
    
    private boolean isPotentialDaoClass(ClassMetadata classMetadata) {
        if (classMetadata == null) {
            return false;
        }
        
        // Check type
        if (classMetadata.getType() != null && 
            (classMetadata.getType().equalsIgnoreCase("repository") || 
             classMetadata.getType().equalsIgnoreCase("dao"))) {
            return true;
        }
        
        // Check class name
        if (classMetadata.getName() != null) {
            String name = classMetadata.getName().toLowerCase();
            if (name.contains("dao") || name.contains("repository") || name.contains("mapper")) {
                return true;
            }
        }
        
        // Check implemented interfaces
        if (classMetadata.getInterfaces() != null) {
            for (String iface : classMetadata.getInterfaces()) {
                if (iface != null) {
                    String ifaceLower = iface.toLowerCase();
                    if (ifaceLower.contains("repository") || ifaceLower.contains("dao") || 
                        ifaceLower.contains("crudrepository") || ifaceLower.contains("jparepository")) {
                        return true;
                    }
                }
            }
        }
        
        // Check annotations
        if (classMetadata.getAnnotations() != null) {
            for (String annotation : classMetadata.getAnnotations()) {
                if (annotation != null && (annotation.contains("Repository") || annotation.contains("DAO"))) {
                    return true;
                }
            }
        }
        
        // Check if methods have DAO patterns
        if (classMetadata.getMethods() != null) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                if (method != null && method.getName() != null) {
                    for (String pattern : DAO_METHOD_PATTERNS) {
                        if (method.getName().toLowerCase().startsWith(pattern.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    private List<DaoOperationDetail> analyzeSourceFile(File sourceFile, String className) {
        List<DaoOperationDetail> operations = new ArrayList<>();
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);
            
            // Visitor to find all method declarations and analyze them
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration md, Void arg) {
                    DaoAnalyzer.DaoAnalysisResult result = daoAnalyzer.analyze(md);
                    operations.addAll(result.getOperations());
                    super.visit(md, arg);
                }
            }, null);
            
        } catch (FileNotFoundException e) {
            logger.warn("Source file not found: {}", sourceFile.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Error parsing source file {}: {}", sourceFile.getAbsolutePath(), e.getMessage());
        }
        
        return operations;
    }
    
    private List<DaoOperationDetail> analyzeFromMethodMetadata(ClassMetadata classMetadata) {
        List<DaoOperationDetail> operations = new ArrayList<>();
        
        // Infer operations from method names when SQL can't be directly analyzed
        if (classMetadata.getMethods() != null) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                String methodName = method.getName().toLowerCase();
                
                for (String pattern : DAO_METHOD_PATTERNS) {
                    if (methodName.startsWith(pattern.toLowerCase())) {
                        // Extract entity name from method name
                        // This is a heuristic and can be improved
                        String entityName = extractEntityNameFromMethod(method.getName(), pattern);
                        
                        if (entityName != null && !entityName.isEmpty()) {
                            DaoOperationDetail.SqlOperationType opType = inferOperationTypeFromMethodName(methodName);
                            
                            // Create a synthetic SQL query
                            String syntheticQuery = createSyntheticQuery(opType, entityName);
                            List<String> tables = Collections.singletonList(entityName);
                            
                            operations.add(new DaoOperationDetail(
                                    syntheticQuery, 
                                    opType,
                                    tables
                            ));
                            
                            break; // Found a match, move to next method
                        }
                    }
                }
            }
        }
        
        return operations;
    }
    
    private String extractEntityNameFromMethod(String methodName, String prefix) {
        if (methodName == null) {
            return "";
        }
        
        // Remove the prefix pattern
        String remaining = methodName;
        if (methodName.toLowerCase().startsWith(prefix.toLowerCase())) {
            remaining = methodName.substring(prefix.length());
        }
        
        // Handle common naming patterns
        if (remaining.startsWith("By")) {
            remaining = remaining.substring(2);
            int andIndex = remaining.indexOf("And");
            int orIndex = remaining.indexOf("Or");
            int orderByIndex = remaining.indexOf("OrderBy");
            
            // Extract entity name before conditions
            int endIndex = -1;
            if (andIndex > 0) endIndex = andIndex;
            if (orIndex > 0 && (endIndex == -1 || orIndex < endIndex)) endIndex = orIndex;
            if (orderByIndex > 0 && (endIndex == -1 || orderByIndex < endIndex)) endIndex = orderByIndex;
            
            if (endIndex > 0) {
                // Handle 'findByUserIdAndUserName' -> extract 'User' as the entity
                String conditionPart = remaining.substring(0, endIndex);
                
                // Look for repeating patterns to identify entity name
                Map<String, Integer> potentialEntities = new HashMap<>();
                for (String part : conditionPart.split("And|Or")) {
                    if (part.length() > 0) {
                        // For each field like 'UserId', extract potential entity 'User'
                        String potentialEntity = extractPotentialEntityFromField(part);
                        potentialEntities.put(potentialEntity, 
                                             potentialEntities.getOrDefault(potentialEntity, 0) + 1);
                    }
                }
                
                // Find most common entity pattern
                String mostLikelyEntity = "";
                int maxCount = 0;
                for (Map.Entry<String, Integer> entry : potentialEntities.entrySet()) {
                    if (entry.getValue() > maxCount && entry.getKey().length() > 0) {
                        maxCount = entry.getValue();
                        mostLikelyEntity = entry.getKey();
                    }
                }
                
                if (!mostLikelyEntity.isEmpty()) {
                    return mostLikelyEntity;
                }
            }
            
            // If no entity name found through field pattern analysis, use standard heuristics
            return singularize(camelToSnake(remaining).replace("_", ""));
        }
        
        // For methods like findUsers, findAllUsers, etc.
        if (remaining.startsWith("All")) {
            remaining = remaining.substring(3);
        }
        
        // Handle plural forms for better table name mapping
        return singularize(remaining);
    }
    
    // Helper method to extract potential entity name from a field name
    private String extractPotentialEntityFromField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        
        // Skip known common field names that don't indicate entity
        String lowerField = fieldName.toLowerCase();
        if (lowerField.equals("id") || lowerField.equals("ids") || 
            lowerField.equals("uuid") || lowerField.equals("count") ||
            lowerField.equals("active") || lowerField.equals("enabled") ||
            lowerField.equals("status") || lowerField.equals("type")) {
            return "";
        }
        
        // Handle common field naming patterns like userId
        if (fieldName.endsWith("Id")) {
            return fieldName.substring(0, fieldName.length() - 2);
        }
        
        // For other fields, assume they could be entity properties
        // Look for camel case boundaries to extract potential entity name
        for (int i = 1; i < fieldName.length(); i++) {
            if (Character.isUpperCase(fieldName.charAt(i))) {
                return fieldName.substring(0, i);
            }
        }
        
        return fieldName;
    }
    
    // Helper method to convert plural to singular (basic rules)
    private String singularize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        
        String lowerWord = word.toLowerCase();
        // Handle common plural forms
        if (lowerWord.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        } else if (lowerWord.endsWith("es")) {
            return word.substring(0, word.length() - 2);
        } else if (lowerWord.endsWith("s") && !lowerWord.endsWith("ss") && !lowerWord.endsWith("us")) {
            return word.substring(0, word.length() - 1);
        }
        
        return word;
    }
    
    private String camelToSnake(String str) {
        // Convert from camelCase to snake_case
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return str.replaceAll(regex, replacement).toLowerCase();
    }
    
    private DaoOperationDetail.SqlOperationType inferOperationTypeFromMethodName(String methodName) {
        methodName = methodName.toLowerCase();
        
        if (methodName.startsWith("find") || methodName.startsWith("get") || 
            methodName.startsWith("load") || methodName.startsWith("select") || 
            methodName.startsWith("query")) {
            return DaoOperationDetail.SqlOperationType.SELECT;
        } else if (methodName.startsWith("save") || methodName.startsWith("insert") || 
                   methodName.startsWith("persist") || methodName.startsWith("store")) {
            return DaoOperationDetail.SqlOperationType.INSERT;
        } else if (methodName.startsWith("update") || methodName.startsWith("merge")) {
            return DaoOperationDetail.SqlOperationType.UPDATE;
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return DaoOperationDetail.SqlOperationType.DELETE;
        } else {
            return DaoOperationDetail.SqlOperationType.UNKNOWN;
        }
    }
    
    private String createSyntheticQuery(DaoOperationDetail.SqlOperationType opType, String tableName) {
        switch (opType) {
            case SELECT:
                return "SELECT * FROM " + tableName;
            case INSERT:
                return "INSERT INTO " + tableName + " (...) VALUES (...)";
            case UPDATE:
                return "UPDATE " + tableName + " SET ... WHERE ...";
            case DELETE:
                return "DELETE FROM " + tableName + " WHERE ...";
            default:
                return "/* Operation on " + tableName + " */";
        }
    }

    @Override
    public String generateDbDiagram(Map<String, List<DaoOperationDetail>> daoOperations, String outputPath) {
        StringBuilder plantUmlBuilder = new StringBuilder();
        plantUmlBuilder.append("@startuml Database Schema\n");
        plantUmlBuilder.append("!theme plain\n");
        plantUmlBuilder.append("skinparam linetype ortho\n");
        
        // Extract all tables
        Set<String> allTables = new HashSet<>();
        for (List<DaoOperationDetail> operations : daoOperations.values()) {
            for (DaoOperationDetail op : operations) {
                if (op.getTables() != null) {
                    allTables.addAll(op.getTables());
                }
            }
        }
        
        // Extract relationships (this is a heuristic based on common naming patterns)
        Map<String, Set<String>> relationships = new HashMap<>();
        
        // Generate entities
        for (String table : allTables) {
            plantUmlBuilder.append("entity \"").append(table).append("\" {\n");
            plantUmlBuilder.append("  +id : number <<PK>>\n");
            
            // Add placeholder fields, ideally these would be extracted from actual code
            if (table.contains("_")) {
                String[] parts = table.split("_");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        // Check if it's a potential foreign key to another entity
                        if (allTables.contains(part)) {
                            plantUmlBuilder.append("  +").append(part).append("_id : number <<FK>>\n");
                            
                            // Add to relationships
                            relationships.computeIfAbsent(table, k -> new HashSet<>()).add(part);
                        }
                    }
                }
            }
            
            plantUmlBuilder.append("}\n\n");
        }
        
        // Add relationships
        for (Map.Entry<String, Set<String>> entry : relationships.entrySet()) {
            String table = entry.getKey();
            for (String relatedTable : entry.getValue()) {
                plantUmlBuilder.append(table).append(" }o--|| ").append(relatedTable).append("\n");
            }
        }
        
        // Add DAO classes that interact with tables
        for (Map.Entry<String, List<DaoOperationDetail>> entry : daoOperations.entrySet()) {
            String daoClass = entry.getKey();
            String simpleName = daoClass.contains(".") ? daoClass.substring(daoClass.lastIndexOf('.') + 1) : daoClass;
            
            plantUmlBuilder.append("class \"").append(simpleName).append("\" <<DAO>> {\n");
            
            // Group operations by table
            Map<String, Set<DaoOperationDetail.SqlOperationType>> tableOps = new HashMap<>();
            for (DaoOperationDetail op : entry.getValue()) {
                if (op.getTables() != null) {
                    for (String table : op.getTables()) {
                        tableOps.computeIfAbsent(table, k -> new HashSet<>()).add(op.getOperationType());
                    }
                }
            }
            
            // List operations in the DAO class
            for (Map.Entry<String, Set<DaoOperationDetail.SqlOperationType>> tableEntry : tableOps.entrySet()) {
                String table = tableEntry.getKey();
                Set<DaoOperationDetail.SqlOperationType> ops = tableEntry.getValue();
                
                for (DaoOperationDetail.SqlOperationType opType : ops) {
                    String methodName = "";
                    switch (opType) {
                        case SELECT: methodName = "find" + snakeToCamel(table, true); break;
                        case INSERT: methodName = "save" + snakeToCamel(table, true); break;
                        case UPDATE: methodName = "update" + snakeToCamel(table, true); break;
                        case DELETE: methodName = "delete" + snakeToCamel(table, true); break;
                        default: methodName = "operate" + snakeToCamel(table, true);
                    }
                    plantUmlBuilder.append("  +").append(methodName).append("()\n");
                }
            }
            
            plantUmlBuilder.append("}\n\n");
            
            // Connect DAO to its tables
            for (String table : tableOps.keySet()) {
                plantUmlBuilder.append(simpleName).append(" ..> ").append(table).append(" : operates on\n");
            }
        }
        
        plantUmlBuilder.append("@enduml");
        
        // Render the diagram
        String outputFilePath = new File(outputPath, "database_schema.svg").getAbsolutePath();
        try {
            return plantUMLRenderer.renderDiagram(plantUmlBuilder.toString(), outputFilePath, DiagramType.DATABASE_DIAGRAM);
        } catch (Exception e) {
            logger.error("Error generating database diagram: {}", e.getMessage());
            return null;
        }
    }
    
    private String snakeToCamel(String str, boolean capitalizeFirstLetter) {
        // Convert snake_case to camelCase
        String[] parts = str.split("_");
        StringBuilder camelCase = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            if (i == 0 && !capitalizeFirstLetter) {
                camelCase.append(part.toLowerCase());
            } else {
                camelCase.append(Character.toUpperCase(part.charAt(0)));
                camelCase.append(part.substring(1).toLowerCase());
            }
        }
        
        return camelCase.toString();
    }

    // Check if a class is a Spring Data repository
    private boolean isSpringDataRepository(ClassMetadata classMetadata) {
        if (classMetadata.getInterfaces() != null) {
            for (String iface : classMetadata.getInterfaces()) {
                if (iface != null && 
                    (iface.contains("CrudRepository") || 
                     iface.contains("JpaRepository") || 
                     iface.contains("MongoRepository") ||
                     iface.contains("ReactiveMongoRepository") ||
                     iface.contains("ReactiveCrudRepository") ||
                     iface.contains("PagingAndSortingRepository"))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Generate synthetic operations for Spring Data repositories
    private List<DaoOperationDetail> generateSpringDataRepositoryOperations(ClassMetadata classMetadata) {
        List<DaoOperationDetail> operations = new ArrayList<>();
        
        // Extract entity name from class name
        String repositoryName = classMetadata.getName();
        String entityName = repositoryName;
        if (repositoryName.endsWith("Repository")) {
            entityName = repositoryName.substring(0, repositoryName.length() - "Repository".length());
        } else if (repositoryName.endsWith("DAO") || repositoryName.endsWith("Dao")) {
            entityName = repositoryName.substring(0, repositoryName.length() - "DAO".length());
        }
        
        // Generate table name from entity name
        String tableName = camelToSnake(entityName);
        
        // Add common Spring Data operations
        // Find by ID
        operations.add(new DaoOperationDetail(
            "findById", 
            "SELECT * FROM " + tableName + " WHERE id = ?",
            DaoOperationDetail.SqlOperationType.SELECT,
            tableName
        ));
        
        // Find all
        operations.add(new DaoOperationDetail(
            "findAll", 
            "SELECT * FROM " + tableName,
            DaoOperationDetail.SqlOperationType.SELECT,
            tableName
        ));
        
        // Save
        operations.add(new DaoOperationDetail(
            "save", 
            "INSERT INTO " + tableName + " VALUES (...) ON DUPLICATE KEY UPDATE ...",
            DaoOperationDetail.SqlOperationType.INSERT,
            tableName
        ));
        
        // Delete by ID
        operations.add(new DaoOperationDetail(
            "deleteById", 
            "DELETE FROM " + tableName + " WHERE id = ?",
            DaoOperationDetail.SqlOperationType.DELETE,
            tableName
        ));
        
        return operations;
    }
} 