package com.codedocgen.service.impl;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DaoOperationDetail;
import com.codedocgen.model.DbAnalysisResult;
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
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;

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
    public DbAnalysisResult analyzeDbOperations(List<ClassMetadata> classes, File projectDir) {
        Map<String, List<DaoOperationDetail>> operationsByClassResult = new HashMap<>();
        
        if (classes == null || projectDir == null) {
            logger.warn("Null input: classes={}, projectDir={}", classes == null ? "null" : "not null", projectDir == null ? "null" : "not null");
            return new DbAnalysisResult(operationsByClassResult, new HashMap<>());
        }
        
        List<ClassMetadata> daoCandidates = classes.stream()
                .filter(Objects::nonNull)
                .filter(this::isPotentialDaoClass)
                .collect(Collectors.toList());
        
        logger.info("Found {} potential DAO/Repository classes for analysis", daoCandidates.size());
        int processedCount = 0;
        Map<String, ClassMetadata> fqnToClassMetadata = classes.stream()
            .filter(Objects::nonNull)
            .filter(cm -> cm.getPackageName() != null && cm.getName() != null)
            .collect(Collectors.toMap(cm -> cm.getPackageName() + "." + cm.getName(), cm -> cm, (cm1, cm2) -> cm1));

        for (ClassMetadata classMetadata : daoCandidates) {
            if (classMetadata.getPackageName() == null || classMetadata.getName() == null) {
                logger.warn("Skipping class with null package name or class name");
                continue;
            }
            String fullyQualifiedName = classMetadata.getPackageName() + "." + classMetadata.getName();
            List<DaoOperationDetail> finalOperations = new ArrayList<>();
            
            try {
                if (classMetadata.getFilePath() != null) {
                    File sourceFile = new File(projectDir, classMetadata.getFilePath());
                    if (sourceFile.exists()) {
                        try {
                            List<DaoOperationDetail> sourceOps = analyzeSourceFile(sourceFile, classMetadata.getName());
                            if (sourceOps != null && !sourceOps.isEmpty()) {
                                finalOperations.addAll(sourceOps);
                                logger.debug("Successfully analyzed source file for {}: {} operations found", 
                                            fullyQualifiedName, sourceOps.size());
                            }
                        } catch (Exception e) {
                            logger.debug("Error parsing source file {}: {}", classMetadata.getFilePath(), e.getMessage());
                        }
                    } else {
                        logger.debug("Source file not found: {}", classMetadata.getFilePath());
                    }
                }
                
                if (classMetadata.getMethods() != null) {
                    List<DaoOperationDetail> metadataOps = analyzeFromMethodMetadata(classMetadata);
                    
                    if (!finalOperations.isEmpty() && !metadataOps.isEmpty()) {
                        Set<String> existingMethodNames = finalOperations.stream()
                            .map(DaoOperationDetail::getMethodName)
                            .collect(Collectors.toSet());
                        
                        metadataOps.stream()
                            .filter(op -> !existingMethodNames.contains(op.getMethodName()))
                            .forEach(finalOperations::add);
                    } else {
                        finalOperations.addAll(metadataOps);
                    }
                }
                
                if (isSpringDataRepository(classMetadata) && finalOperations.isEmpty()) {
                    finalOperations.addAll(generateSpringDataRepositoryOperations(classMetadata));
                    logger.debug("Generated synthetic operations for Spring Data repository {}", fullyQualifiedName);
                }
                
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
                    Map<String, ClassMetadata> entityMetadataMap = classes.stream()
                        .filter(Objects::nonNull)
                        .filter(cm -> "entity".equalsIgnoreCase(cm.getType()) || 
                                      (cm.getAnnotations() != null && 
                                       cm.getAnnotations().stream().anyMatch(a -> a.contains("@Entity"))))
                        .collect(Collectors.toMap(ClassMetadata::getName, cm -> cm, (cm1, cm2) -> cm1));
                    
                    logger.debug("Collected {} entity metadata entries for table validation.", entityMetadataMap.size());

                    List<DaoOperationDetail> validFinalOperations = new ArrayList<>();
                    for (DaoOperationDetail op : finalOperations) {
                        if (op.getTables() == null || op.getTables().isEmpty()) {
                            validFinalOperations.add(op);
                            continue;
                        }
                        List<String> validatedTables = new ArrayList<>();
                        for (String tableNameFromOp : op.getTables()) {
                            boolean tableIsValid = false;
                            for (ClassMetadata entityCm : entityMetadataMap.values()) {
                                String effectiveTableName = getTableNameFromEntity(entityCm);
                                if (effectiveTableName == null || effectiveTableName.isEmpty()) continue;
                                if (effectiveTableName.equalsIgnoreCase(tableNameFromOp)) {
                                    tableIsValid = true; break;
                                }
                                String entityClassName = entityCm.getName();
                                if (entityClassName.equalsIgnoreCase(tableNameFromOp) ||
                                    entityClassName.equalsIgnoreCase(snakeToCamel(tableNameFromOp, true)) ||
                                    camelToSnake(entityClassName).equalsIgnoreCase(tableNameFromOp)) {
                                    tableIsValid = true; break;
                                }
                            }
                            if (tableIsValid) validatedTables.add(tableNameFromOp);
                            else logger.warn("Table '{}' from DAO op in {} not mapped. Query: {}", tableNameFromOp, fullyQualifiedName, op.getSqlQuery());
                        }
                        if (!validatedTables.isEmpty()) {
                            validFinalOperations.add(new DaoOperationDetail(op.getMethodName(), op.getSqlQuery(), op.getOperationType(), validatedTables));
                        } else {
                            logger.warn("DAO op in {} has no valid tables after validation. Query: {}. Op discarded.", fullyQualifiedName, op.getSqlQuery());
                        }
                    }

                    if (!validFinalOperations.isEmpty()) {
                        operationsByClassResult.put(fullyQualifiedName, validFinalOperations);
                        processedCount++;
                    }
                }
            } catch (Exception e) {
                logger.warn("Error analyzing DAO class {}: {}", fullyQualifiedName, e.getMessage(), e);
            }
        }
        
        logger.info("Completed DAO analysis with {} classes having operations (out of {} candidates)", processedCount, daoCandidates.size());
        
        Map<String, List<DaoOperationDetail>> finalOperationsByClass = new HashMap<>();
        Set<String> interfacesToRemove = new HashSet<>();
        for (Map.Entry<String, List<DaoOperationDetail>> entry : operationsByClassResult.entrySet()) {
            String fqn = entry.getKey();
            ClassMetadata cm = fqnToClassMetadata.get(fqn);
            if (cm != null && "interface".equalsIgnoreCase(cm.getType())) {
                for (Map.Entry<String, List<DaoOperationDetail>> otherEntry : operationsByClassResult.entrySet()) {
                    String otherFqn = otherEntry.getKey();
                    if (fqn.equals(otherFqn)) continue;
                    ClassMetadata otherCm = fqnToClassMetadata.get(otherFqn);
                    if (otherCm != null && !"interface".equalsIgnoreCase(otherCm.getType())) {
                        if (otherCm.getInterfaces() != null && otherCm.getInterfaces().contains(fqn)) {
                            interfacesToRemove.add(fqn); break;
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, List<DaoOperationDetail>> entry : operationsByClassResult.entrySet()) {
            if (!interfacesToRemove.contains(entry.getKey())) {
                finalOperationsByClass.put(entry.getKey(), entry.getValue());
            }
        }
        logger.info("DAO analysis result after removing redundant interfaces: {} classes", finalOperationsByClass.size());

        Map<String, Set<String>> classesByEntity = new HashMap<>();
        Map<String, ClassMetadata> allEntityMetadataMap = classes.stream()
            .filter(Objects::nonNull)
            .filter(cm -> "entity".equalsIgnoreCase(cm.getType()) || 
                          (cm.getAnnotations() != null && 
                           cm.getAnnotations().stream().anyMatch(a -> a.contains("@Entity"))))
            .collect(Collectors.toMap(ClassMetadata::getName, cm -> cm, (cm1, cm2) -> cm1));

        for (Map.Entry<String, List<DaoOperationDetail>> entry : finalOperationsByClass.entrySet()) {
            String operatingClassFqn = entry.getKey();
            List<DaoOperationDetail> ops = entry.getValue();
            for (DaoOperationDetail op : ops) {
                if (op.getTables() != null) {
                    for (String tableNameInOp : op.getTables()) {
                        String mappedEntityName = null;
                        for (ClassMetadata entityCm : allEntityMetadataMap.values()) {
                            String effectiveTableNameForEntity = getTableNameFromEntity(entityCm);
                            if (effectiveTableNameForEntity != null && effectiveTableNameForEntity.equalsIgnoreCase(tableNameInOp)) {
                                mappedEntityName = entityCm.getName();
                                break;
                            }
                            if (entityCm.getName().equalsIgnoreCase(tableNameInOp) || 
                                camelToSnake(entityCm.getName()).equalsIgnoreCase(tableNameInOp) ||
                                snakeToCamel(tableNameInOp, true).equalsIgnoreCase(entityCm.getName())){
                                mappedEntityName = entityCm.getName();
                                break;
                            }
                        }

                        if (mappedEntityName != null) {
                            classesByEntity.computeIfAbsent(mappedEntityName, k -> new HashSet<>()).add(operatingClassFqn);
                        } else {
                            logger.debug("Table '{}' from op in {} could not be mapped to a known Entity for classesByEntity map.", tableNameInOp, operatingClassFqn);
                        }
                    }
                }
            }
        }
        logger.info("Built classesByEntity map with {} entities/tables.", classesByEntity.size());

        return new DbAnalysisResult(finalOperationsByClass, classesByEntity);
    }
    
    private boolean isPotentialDaoClass(ClassMetadata classMetadata) {
        if (classMetadata == null) {
            return false;
        }
        
        if (classMetadata.getType() != null && 
            (classMetadata.getType().equalsIgnoreCase("repository") || 
             classMetadata.getType().equalsIgnoreCase("dao"))) {
            return true;
        }
        
        if (classMetadata.getName() != null) {
            String name = classMetadata.getName().toLowerCase();
            if (name.contains("dao") || name.contains("repository") || name.contains("mapper")) {
                return true;
            }
        }
        
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
        
        if (classMetadata.getAnnotations() != null) {
            for (String annotation : classMetadata.getAnnotations()) {
                if (annotation != null && (annotation.contains("Repository") || annotation.contains("DAO"))) {
                    return true;
                }
            }
        }
        
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
            
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration md, Void arg) {
                    String methodName = md.getNameAsString();
                    boolean queryFoundInAnnotation = false;

                    for (AnnotationExpr annotation : md.getAnnotations()) {
                        String annotationName = annotation.getNameAsString();
                        if ("Query".equals(annotationName) || annotationName.endsWith(".Query")) {
                            String sqlQuery = null;
                            if (annotation.isNormalAnnotationExpr()) {
                                NormalAnnotationExpr normalAnn = annotation.asNormalAnnotationExpr();
                                for (com.github.javaparser.ast.expr.MemberValuePair pair : normalAnn.getPairs()) {
                                    if ("value".equals(pair.getNameAsString()) || "query".equals(pair.getNameAsString())) {
                                        if (pair.getValue().isStringLiteralExpr()) {
                                            sqlQuery = pair.getValue().asStringLiteralExpr().getValue();
                                        }
                                    }
                                }
                            } else if (annotation.isSingleMemberAnnotationExpr()) {
                                Expression valueExpr = annotation.asSingleMemberAnnotationExpr().getMemberValue();
                                if (valueExpr.isStringLiteralExpr()) {
                                    sqlQuery = valueExpr.asStringLiteralExpr().getValue();
                                }
                            }

                            if (sqlQuery != null && !sqlQuery.trim().isEmpty()) {
                                DaoOperationDetail.SqlOperationType type = daoAnalyzer.extractSqlOperationType(sqlQuery);
                                List<String> tables = daoAnalyzer.extractTableNames(sqlQuery);
                                operations.add(new DaoOperationDetail(methodName, sqlQuery, type, tables));
                                queryFoundInAnnotation = true;
                                logger.debug("Found SQL in @Query for method {}: {}", methodName, sqlQuery);
                                break;
                            }
                        }
                    }

                    if (!queryFoundInAnnotation) {
                        DaoAnalyzer.DaoAnalysisResult result = daoAnalyzer.analyze(md);
                        if (result != null && result.getOperations() != null && !result.getOperations().isEmpty()) {
                           operations.addAll(result.getOperations());
                        }
                    }
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
        
        if (classMetadata.getMethods() != null) {
            for (MethodMetadata method : classMetadata.getMethods()) {
                String methodName = method.getName().toLowerCase();
                
                for (String pattern : DAO_METHOD_PATTERNS) {
                    if (methodName.startsWith(pattern.toLowerCase())) {
                        String entityName = extractEntityNameFromMethod(method.getName(), pattern);
                        
                        if (entityName != null && !entityName.isEmpty()) {
                            DaoOperationDetail.SqlOperationType opType = inferOperationTypeFromMethodName(methodName);
                            
                            String syntheticQuery = createSyntheticQuery(opType, entityName);
                            List<String> tables = Collections.singletonList(entityName);
                            
                            operations.add(new DaoOperationDetail(
                                    syntheticQuery, 
                                    opType,
                                    tables
                            ));
                            
                            break;
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
        
        String remaining = methodName;
        if (methodName.toLowerCase().startsWith(prefix.toLowerCase())) {
            remaining = methodName.substring(prefix.length());
        }
        
        if (remaining.startsWith("By")) {
            remaining = remaining.substring(2);
            int andIndex = remaining.indexOf("And");
            int orIndex = remaining.indexOf("Or");
            int orderByIndex = remaining.indexOf("OrderBy");
            
            int endIndex = -1;
            if (andIndex > 0) endIndex = andIndex;
            if (orIndex > 0 && (endIndex == -1 || orIndex < endIndex)) endIndex = orIndex;
            if (orderByIndex > 0 && (endIndex == -1 || orderByIndex < endIndex)) endIndex = orderByIndex;
            
            if (endIndex > 0) {
                String conditionPart = remaining.substring(0, endIndex);
                
                Map<String, Integer> potentialEntities = new HashMap<>();
                for (String part : conditionPart.split("And|Or")) {
                    if (part.length() > 0) {
                        String potentialEntity = extractPotentialEntityFromField(part);
                        potentialEntities.put(potentialEntity, 
                                             potentialEntities.getOrDefault(potentialEntity, 0) + 1);
                    }
                }
                
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
            
            return singularize(camelToSnake(remaining).replace("_", ""));
        }
        
        if (remaining.startsWith("All")) {
            remaining = remaining.substring(3);
        }
        
        return singularize(remaining);
    }
    
    private String extractPotentialEntityFromField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        
        String lowerField = fieldName.toLowerCase();
        if (lowerField.equals("id") || lowerField.equals("ids") || 
            lowerField.equals("uuid") || lowerField.equals("count") ||
            lowerField.equals("active") || lowerField.equals("enabled") ||
            lowerField.equals("status") || lowerField.equals("type")) {
            return "";
        }
        
        if (fieldName.endsWith("Id")) {
            return fieldName.substring(0, fieldName.length() - 2);
        }
        
        for (int i = 1; i < fieldName.length(); i++) {
            if (Character.isUpperCase(fieldName.charAt(i))) {
                return fieldName.substring(0, i);
            }
        }
        
        return fieldName;
    }
    
    private String singularize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        
        String lowerWord = word.toLowerCase();
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
    public String generateDbDiagram(List<ClassMetadata> allClassMetadata, Map<String, List<DaoOperationDetail>> daoOperationsMap, String outputPath) {
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
                        plantUmlBuilder.append("  +").append(fieldName).append(" : ").append(fieldType).append(pkMarker).append("\n");
                    }
                }
                if (!hasId) {
                    plantUmlBuilder.append("  +id : long <<PK>>\n");
                }
                plantUmlBuilder.append("}\n\n");
            }
        }

        for (ClassMetadata cm : allClassMetadata) {
            if (cm == null || !entityClassMap.containsKey(cm.getName()) || cm.getFields() == null) continue;
            
            String sourceEntityName = cm.getName();
            String safeSourceEntityName = entityToSafeName.get(sourceEntityName);
            if (safeSourceEntityName == null) continue;

            for (com.codedocgen.model.FieldMetadata fm : cm.getFields()) {
                if (fm == null || fm.getAnnotations() == null || fm.getType() == null) continue;

                String fieldSimpleType = fm.getType().substring(fm.getType().lastIndexOf('.') + 1);
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
                        plantUmlBuilder.append(safeSourceEntityName).append(" }o--o{ ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append(" (ManyToMany)\n");
                    } else if (isCollection) {
                        plantUmlBuilder.append(safeSourceEntityName).append(" .. ").append(safeTargetEntityName).append(" : ").append(fm.getName()).append(" (collection)\n");
                    }
                }
            }
            plantUmlBuilder.append("\n");
        }

        for (Map.Entry<String, List<DaoOperationDetail>> entry : daoOperationsMap.entrySet()) {
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
            
            Set<String> operatedEntityNames = new HashSet<>();
            if (entry.getValue() != null) {
                for (DaoOperationDetail op : entry.getValue()) {
                    if (op != null && op.getTables() != null) {
                        for (String tableNameFromDaoOp : op.getTables()) {
                            String entityNameGuess = snakeToCamel(tableNameFromDaoOp, true);
                            if (entityClassMap.containsKey(entityNameGuess)) {
                                operatedEntityNames.add(entityNameGuess);
                            } else {
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
        
        plantUmlBuilder.append("@enduml");
        
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
        String firstTable = op.getTables().get(0);
        String entityName = snakeToCamel(firstTable, true);

        switch (op.getOperationType()) {
            case SELECT: return "find" + entityName + "s";
            case INSERT: return "save" + entityName;
            case UPDATE: return "update" + entityName;
            case DELETE: return "delete" + entityName;
            default: return "access" + entityName;
        }
    }
    
    private String snakeToCamel(String str, boolean capitalizeFirstLetter) {
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
                    String syntheticQuery = createSyntheticQuery(opType, entityName, method.getName());
                    List<String> tablesInvolved = new ArrayList<>();
                    tablesInvolved.add(entityName);
                    Pattern entityPattern = Pattern.compile("([A-Z][a-z]+)+");
                    Matcher matcher = entityPattern.matcher(method.getName());
                    while(matcher.find()) {
                        String potentialEntity = matcher.group();
                        if (!potentialEntity.equals(entityName) && Character.isUpperCase(potentialEntity.charAt(0))) {
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
        logger.debug("Extracting entity name for repository: {}", repoClass.getName());
        List<String> interfaces = repoClass.getInterfaces();
        if (interfaces != null) {
            for (String iface : interfaces) {
                logger.debug("  Analyzing interface: {}", iface);
                if (iface.matches(".*<.+?>.*") &&
                    (iface.contains("CrudRepository") || 
                     iface.contains("JpaRepository") || 
                     iface.contains("PagingAndSortingRepository") ||
                     iface.contains("MongoRepository") || 
                     iface.contains("ReactiveCrudRepository") ||
                     iface.contains("ReactiveMongoRepository"))) {
                    logger.debug("    Interface {} matches Spring Data pattern.", iface);
                    Pattern pattern = Pattern.compile("[<,]\\s*([\\w\\.]+)\\s*(?:,[\\w\\.\\s]*>|>)"); 
                    Matcher matcher = pattern.matcher(iface);
                    if (matcher.find()) {
                        String fullEntityName = matcher.group(1);
                        logger.debug("      Found generic type by regex: {}", fullEntityName);
                        String simpleName = fullEntityName.substring(fullEntityName.lastIndexOf('.') + 1);
                        
                        boolean isLikelyIdType = Arrays.asList("Long", "Integer", "String", "UUID", "Short", "Byte", "Double", "Float", "Boolean", "Character").contains(simpleName);
                        boolean looksLikeSecondGenericId = isLikelyIdType && iface.matches(".*<[\\w\\.]+,\\s*" + Pattern.quote(fullEntityName) + "(?:\\s*,.*?)?>.*");

                        if (looksLikeSecondGenericId) {
                           logger.debug("      Generic type {} looks like an ID type and might be the second generic parameter. This regex might pick ID type over Entity type if ID is listed first or Entity is not first. Check interface: {}", simpleName, iface);
                           if (iface.matches(".*<" + Pattern.quote(fullEntityName) + "\\s*,\\s*([\\w\\.]+).*>.*")) {
                               Matcher secondAttemptMatcher = Pattern.compile(".*<" + Pattern.quote(fullEntityName) + "\\s*,\\s*([\\w\\.]+).*>.*").matcher(iface);
                               if (secondAttemptMatcher.find()) {
                                   String potentiallyRealEntity = secondAttemptMatcher.group(1);
                                   logger.debug("      Second attempt found: {}. Using this instead of {}", potentiallyRealEntity, simpleName);
                                   simpleName = potentiallyRealEntity.substring(potentiallyRealEntity.lastIndexOf('.') + 1);
                               }                               
                           }
                           if (Arrays.asList("Long", "Integer", "String", "UUID").contains(simpleName)) {
                                logger.warn("      Potential issue: Extracted name '{}' for repository '{}' from interface '{}' looks like an ID type. Ensure the Entity is the first generic type or enhance parsing.", simpleName, repoClass.getName(), iface);
                           }
                           logger.info("    Extracted entity name '{}' from Spring Data interface '{}' for repository '{}' (after checking for ID type).", simpleName, iface, repoClass.getName());
                           return simpleName;
                        } else {
                            logger.info("    Extracted entity name '{}' from Spring Data interface '{}' for repository '{}'", simpleName, iface, repoClass.getName());
                            return simpleName; 
                        }
                    } else {
                        logger.debug("    Could not find generic type with regex in: {}", iface);
                    }
                }
            }
        }
        logger.debug("  No entity name found from interfaces for {}. Applying fallback.", repoClass.getName());
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

    private String getTableNameFromEntity(ClassMetadata entityCm) {
        if (entityCm == null) {
            return null;
        }
        if (entityCm.getAnnotations() != null) {
            for (String annotation : entityCm.getAnnotations()) {
                if (annotation.startsWith("@Table")) {
                    Pattern pattern = Pattern.compile("name\s*=\s*\"([^\"]*)\"");
                    Matcher matcher = pattern.matcher(annotation);
                    if (matcher.find()) {
                        String tableName = matcher.group(1);
                        if (tableName != null && !tableName.trim().isEmpty()) {
                            logger.debug("Found @Table(name=\"{}\" for entity class {}", tableName, entityCm.getName());
                            return tableName.trim();
                        }
                    }
                }
            }
        }
        logger.debug("No @Table(name=...) found for entity class {}, falling back to class name for table name derivation.", entityCm.getName());
        return camelToSnake(entityCm.getName());
    }
} 