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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                
                // Merge operations: prioritize source-found, then add heuristic/Spring Data if not covered
                List<DaoOperationDetail> finalOperations = new ArrayList<>();
                if (operations != null && !operations.isEmpty()) {
                    finalOperations.addAll(operations);
                }

                // Add operations from Spring Data interface methods if it's a Spring Data repo
                if (isSpringDataRepository(classMetadata)) {
                    List<DaoOperationDetail> springDataOps = generateSpringDataRepositoryOperations(classMetadata);
                    Set<String> existingMethodNamesInFinal = finalOperations.stream()
                                                                    .map(DaoOperationDetail::getMethodName)
                                                                    .filter(Objects::nonNull)
                                                                    .collect(Collectors.toSet());
                    for (DaoOperationDetail springOp : springDataOps) {
                        if (!existingMethodNamesInFinal.contains(springOp.getMethodName())) {
                            finalOperations.add(springOp);
                        }
                    }
                    logger.debug("Processed Spring Data synthetic operations for {}. Total ops now: {}", fullyQualifiedName, finalOperations.size());
                }
                
                // Add operations from other method metadata if not already covered by source or Spring Data
                if (classMetadata.getMethods() != null) {
                    List<DaoOperationDetail> metadataBasedOps = analyzeFromMethodMetadata(classMetadata);
                    Set<String> existingMethodNamesInFinal = finalOperations.stream()
                                                                    .map(DaoOperationDetail::getMethodName)
                                                                    .filter(Objects::nonNull)
                                                                    .collect(Collectors.toSet());
                    for (DaoOperationDetail metaOp : metadataBasedOps) {
                        if (!existingMethodNamesInFinal.contains(metaOp.getMethodName())) {
                            finalOperations.add(metaOp);
                        }
                    }
                }

                if (!finalOperations.isEmpty()) {
                    result.put(fullyQualifiedName, finalOperations);
                    processedCount++;
                    logger.debug("Found {} database operations in {}", finalOperations.size(), fullyQualifiedName);
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
    public String generateDbDiagram(List<ClassMetadata> allClassMetadata, Map<String, List<DaoOperationDetail>> daoOperations, String outputPath) {
        StringBuilder plantUmlBuilder = new StringBuilder();
        plantUmlBuilder.append("@startuml Database Schema\n");
        plantUmlBuilder.append("!theme plain\n");
        plantUmlBuilder.append("hide empty members\n");
        plantUmlBuilder.append("skinparam linetype ortho\n");
        plantUmlBuilder.append("skinparam classAttributeIconSize 0\n");
        plantUmlBuilder.append("skinparam defaultTextAlignment center\n");
        plantUmlBuilder.append("skinparam roundcorner 10\n");
        plantUmlBuilder.append("skinparam shadowing false\n");

        plantUmlBuilder.append("skinparam class {\n");
        plantUmlBuilder.append("  BackgroundColor PaleGreen\n");
        plantUmlBuilder.append("  ArrowColor SeaGreen\n");
        plantUmlBuilder.append("  BorderColor SeaGreen\n");
        plantUmlBuilder.append("}\n");

        plantUmlBuilder.append("skinparam entity {\n");
        plantUmlBuilder.append("  BackgroundColor LightYellow\n");
        plantUmlBuilder.append("  ArrowColor Orange\n");
        plantUmlBuilder.append("  BorderColor Orange\n");
        plantUmlBuilder.append("}\n\n");

        Map<String, String> entityToSafeName = new HashMap<>();
        Map<String, ClassMetadata> entityClassMap = new HashMap<>();

        // Process @Entity classes first
        if (allClassMetadata != null) {
            for (ClassMetadata cm : allClassMetadata) {
                if (cm == null || cm.getName() == null) continue;
                boolean isEntity = "entity".equalsIgnoreCase(cm.getType()) || 
                                 (cm.getAnnotations() != null && cm.getAnnotations().stream().anyMatch(a -> a.contains("@Entity")));

                if (isEntity) {
                    String entityName = cm.getName();
                    String safeEntityName = "entity_" + entityName.replaceAll("[^a-zA-Z0-9_]", "_");
                    entityToSafeName.put(entityName, safeEntityName);
                    entityClassMap.put(entityName, cm);

                    plantUmlBuilder.append("entity \"").append(entityName).append("\" as ").append(safeEntityName).append(" {\n");
                    boolean hasId = false;
                    if (cm.getFields() != null) {
                        for (com.codedocgen.model.FieldMetadata fm : cm.getFields()) {
                            if (fm == null || fm.getName() == null) continue;
                            String fieldName = fm.getName();
                            String fieldType = fm.getType() != null ? fm.getType().substring(fm.getType().lastIndexOf('.') + 1) : "Object";
                            String pkMarker = "";
                            if (fm.getAnnotations() != null && fm.getAnnotations().stream().anyMatch(a -> a.contains("@Id"))) {
                                pkMarker = " <<PK>>";
                                hasId = true;
                            }
                            // Basic field display - enhance with FK detection later
                            plantUmlBuilder.append("  +").append(fieldName).append(" : ").append(fieldType).append(pkMarker).append("\n");
                        }
                    }
                    if (!hasId) { // Add a default ID if no @Id was found, common for simple entities
                        plantUmlBuilder.append("  +id : long <<PK>>\n");
                    }
                    plantUmlBuilder.append("}\n\n");
                }
            }
        }

        // Add relationships based on JPA annotations (OneToMany, ManyToOne, etc.) - Simplified for now
        if (allClassMetadata != null) {
            for (ClassMetadata cm : allClassMetadata) {
                if (cm == null || !entityClassMap.containsKey(cm.getName()) || cm.getFields() == null) continue;
                
                String sourceEntityName = cm.getName();
                String safeSourceEntityName = entityToSafeName.get(sourceEntityName);
                if (safeSourceEntityName == null) continue;

                for (com.codedocgen.model.FieldMetadata fm : cm.getFields()) {
                    if (fm == null || fm.getAnnotations() == null || fm.getType() == null) continue;

                    String fieldSimpleType = fm.getType().substring(fm.getType().lastIndexOf('.') + 1);
                    // Clean generic types like List<TargetEntity> -> TargetEntity
                    String targetEntityName = fieldSimpleType.replaceAll(".*<", "").replace(">", "");

                    if (entityClassMap.containsKey(targetEntityName)) {
                        String safeTargetEntityName = entityToSafeName.get(targetEntityName);
                        if (safeTargetEntityName == null) continue;

                        boolean isCollection = fieldSimpleType.startsWith("List") || fieldSimpleType.startsWith("Set") || fieldSimpleType.startsWith("Collection");

                        if (fm.getAnnotations().stream().anyMatch(a -> a.contains("@OneToMany"))) {
                            plantUmlBuilder.append(safeSourceEntityName).append(" ||--o{ ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append("\n");
                        } else if (fm.getAnnotations().stream().anyMatch(a -> a.contains("@ManyToOne"))) {
                            plantUmlBuilder.append(safeSourceEntityName).append(" }o--|| ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append("\n");
                        } else if (fm.getAnnotations().stream().anyMatch(a -> a.contains("@OneToOne"))) {
                            plantUmlBuilder.append(safeSourceEntityName).append(" ||--|| ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append("\n");
                        } else if (fm.getAnnotations().stream().anyMatch(a -> a.contains("@ManyToMany"))) {
                            // For ManyToMany, a join table is implied but not explicitly drawn here without more info
                            plantUmlBuilder.append(safeSourceEntityName).append(" }o--o{ ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append(" (ManyToMany)\n");
                        } else if (isCollection) {
                            // Default for collections if no specific annotation, assume OneToMany like
                             plantUmlBuilder.append(safeSourceEntityName).append(" .. ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append(" (collection)\n");
                        } else {
                            // Default for single field reference, assume ManyToOne like or simple association
                            // plantUmlBuilder.append(safeSourceEntityName).append(" ..> ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append("\n");
                        }
                    }
                }
                plantUmlBuilder.append("\n");
            }
        }

        // Add DAO classes and link them to ENTITY names (not table names directly from DAO ops anymore)
        if (daoOperations != null) {
            for (Map.Entry<String, List<DaoOperationDetail>> entry : daoOperations.entrySet()) {
                String daoClassFQN = entry.getKey();
                if (daoClassFQN == null) continue;

                String simpleName = daoClassFQN.contains(".") ? daoClassFQN.substring(daoClassFQN.lastIndexOf('.') + 1) : daoClassFQN;
                String safeDaoClassName = "dao_" + simpleName.replaceAll("[^a-zA-Z0-9_]", "_");

                plantUmlBuilder.append("class \"").append(simpleName).append("\" as ").append(safeDaoClassName).append(" <<DAO>> {\n");
                Set<String> methodsInDao = new HashSet<>();
                if (entry.getValue() != null) {
                    for (DaoOperationDetail op : entry.getValue()) {
                        if (op == null) continue;
                        String methodName = op.getMethodName() != null ? op.getMethodName() : inferMethodNameFromOperation(op);
                        if (methodName != null && !methodName.trim().isEmpty() && methodsInDao.add(methodName)) {
                             plantUmlBuilder.append("  +").append(methodName).append("()\n");
                        }
                    }
                }
                plantUmlBuilder.append("}\n\n");
                
                // Connect DAO to entities it operates on
                // This requires guessing the entity from the table name in DaoOperationDetail for now
                Set<String> operatedEntityNames = new HashSet<>();
                if (entry.getValue() != null) {
                    for (DaoOperationDetail op : entry.getValue()) {
                        if (op != null && op.getTables() != null) {
                            for (String tableNameFromDaoOp : op.getTables()) {
                                // Try to map table name back to an entity name
                                String entityNameGuess = snakeToCamel(tableNameFromDaoOp, true);
                                if (entityClassMap.containsKey(entityNameGuess)) {
                                    operatedEntityNames.add(entityNameGuess);
                                } else {
                                    // If direct guess fails, check if any known entity produces this table name
                                    for(String knownEntityName : entityClassMap.keySet()){
                                        if(tableNameFromDaoOp.equalsIgnoreCase(camelToSnake(knownEntityName))){
                                            operatedEntityNames.add(knownEntityName);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (String entityName : operatedEntityNames) {
                    String safeEntityName = entityToSafeName.get(entityName);
                    if (safeEntityName != null) {
                        plantUmlBuilder.append(safeDaoClassName).append(" ..> ").append(safeEntityName).append(" : operates on\n");
                    }
                }
                plantUmlBuilder.append("\n");
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

    private String inferMethodNameFromOperation(DaoOperationDetail op) {
        if (op == null || op.getOperationType() == null || op.getTables() == null || op.getTables().isEmpty()) {
            return "unknownOperation";
        }
        String firstTable = op.getTables().get(0); // Use first table for naming
        String entityName = snakeToCamel(firstTable, true);

        switch (op.getOperationType()) {
            case SELECT: return "find" + entityName + "s"; // Pluralize for find
            case INSERT: return "save" + entityName;
            case UPDATE: return "update" + entityName;
            case DELETE: return "delete" + entityName;
            default: return "access" + entityName;
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
        String entityName = extractEntityNameFromRepo(classMetadata);
        if (entityName == null) {
            logger.warn("Could not determine entity name for Spring Data repository: {}. Skipping synthetic query generation.", classMetadata.getName());
            return operations;
        }

        if (classMetadata.getMethods() != null) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                DaoOperationDetail.SqlOperationType opType = inferOperationTypeFromMethodName(method.getName());
                if (opType != DaoOperationDetail.SqlOperationType.UNKNOWN) {
                    // For Spring Data, the "table" is the entity name.
                    // The actual SQL might be more complex (e.g., involving joins based on method name conventions),
                    // but for a basic representation, the primary entity is key.
                    String syntheticQuery = createSyntheticQuery(opType, entityName, method.getName());
                    List<String> tablesInvolved = new ArrayList<>();
                    tablesInvolved.add(entityName); // Primary table
                    // Attempt to find other mentioned entities in method name (e.g. findBy<Entity>And<OtherEntity>)
                    // This is a heuristic
                    Pattern entityPattern = Pattern.compile("([A-Z][a-z]+)+");
                    Matcher matcher = entityPattern.matcher(method.getName());
                    while(matcher.find()) {
                        String potentialEntity = matcher.group();
                        if (!potentialEntity.equals(entityName) && Character.isUpperCase(potentialEntity.charAt(0))) { // Check if it looks like a class name
                            // We'd ideally check if this is a known entity from allClassMetadata
                            // For now, just add if it's different from the main entity
                            tablesInvolved.add(potentialEntity);
                        }
                    }

                    operations.add(new DaoOperationDetail(method.getName(), syntheticQuery, opType, tablesInvolved.stream().distinct().collect(Collectors.toList())));
                }
            }
        }
        return operations;
    }

    private String extractEntityNameFromRepo(ClassMetadata repoClass) {
        // Try to get from generic type arguments of CrudRepository, JpaRepository, etc.
        List<String> interfaces = repoClass.getInterfaces();
        if (interfaces != null) {
            for (String iface : interfaces) {
                if (iface.contains("Repository") || iface.contains("Dao")) {
                    // Examples: CrudRepository<Customer, Long>, JpaRepository<Account, String>
                    Pattern pattern = Pattern.compile("[<,]\\s*([\\w.]+)\\s*[,>]?"); // Capture first type argument
                    Matcher matcher = pattern.matcher(iface);
                    if (matcher.find() && matcher.groupCount() >= 1) {
                        String fullEntityName = matcher.group(1);
                        return fullEntityName.substring(fullEntityName.lastIndexOf('.') + 1); // Simple name
                    }
                }
            }
        }
        // Fallback: if the repository name follows a convention like XyzRepository, assume Xyz is the entity.
        if (repoClass.getName().endsWith("Repository")) {
            return repoClass.getName().substring(0, repoClass.getName().length() - "Repository".length());
        }
        if (repoClass.getName().endsWith("Dao")) {
            return repoClass.getName().substring(0, repoClass.getName().length() - "Dao".length());
        }
        return null;
    }

    private String createSyntheticQuery(DaoOperationDetail.SqlOperationType opType, String tableName, String methodName) {
        switch (opType) {
            case SELECT:
                return String.format("SELECT * FROM %s (based on method: %s)", tableName, methodName);
            case INSERT:
                return String.format("INSERT INTO %s (...) VALUES (...) (based on method: %s)", tableName, methodName);
            case UPDATE:
                return String.format("UPDATE %s SET ... WHERE ... (based on method: %s)", tableName, methodName);
            case DELETE:
                return String.format("DELETE FROM %s WHERE ... (based on method: %s)", tableName, methodName);
            default:
                return String.format("Custom operation on %s (based on method: %s)", tableName, methodName);
        }
    }
} 