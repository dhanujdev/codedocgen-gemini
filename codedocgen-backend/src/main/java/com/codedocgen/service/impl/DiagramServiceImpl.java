package com.codedocgen.service.impl;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DiagramType;
import com.codedocgen.model.FieldMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.service.DiagramService;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
public class DiagramServiceImpl implements DiagramService {

    private static final Logger logger = LoggerFactory.getLogger(DiagramServiceImpl.class);

    @Override
    public Map<DiagramType, String> generateDiagrams(List<ClassMetadata> classMetadataList, String baseOutputDir) {
        Map<DiagramType, String> diagramPaths = new HashMap<>();
        File outputDirFile = new File(baseOutputDir);
        if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
            logger.error("Could not create diagram output directory: {}", baseOutputDir);
            return diagramPaths;
        }

        // Class Diagram
        String classDiagramPath = null;
        String classDiagramPlantUmlSource = null; // To hold PlantUML source for logging on error
        try {
            // We need a way to get the PlantUML source from generateClassDiagram if it fails internally
            // For now, we'll assume it returns null on failure or we re-generate it for logging if needed.
            // A better approach would be for generateClassDiagram to return an object with path & source, or throw specific exception.
            classDiagramPath = generateClassDiagram(classMetadataList, baseOutputDir);
            if (classDiagramPath != null) {
                diagramPaths.put(DiagramType.CLASS_DIAGRAM, classDiagramPath);
                logger.info("Class Diagram generation successful. Path: {}", classDiagramPath);
            } else {
                logger.warn("Class Diagram generation returned null.");
                // Attempt to get/log PlantUML source for debugging if possible (conceptual)
                // classDiagramPlantUmlSource = getPlantUmlSourceForClassDiagram(classMetadataList); // Placeholder
                // logger.debug("PlantUML source for failed Class Diagram (if available):\n{}", classDiagramPlantUmlSource);
            }
        } catch (Exception e) {
            logger.error("Error generating class diagram: {}. Type: {}", e.getMessage(), e.getClass().getName(), e);
            // Conceptual: Log PlantUML if possible
            // logger.debug("PlantUML source for failed Class Diagram (if available) during exception:\n{}", classDiagramPlantUmlSource);
        }

        // ER Diagram
        String erDiagramPath = null;
        String erDiagramPlantUmlSource = null; // To hold PlantUML source
        try {
            erDiagramPath = generateEntityRelationshipDiagram(classMetadataList, baseOutputDir);
            if (erDiagramPath != null) {
                diagramPaths.put(DiagramType.ENTITY_RELATIONSHIP_DIAGRAM, erDiagramPath);
                logger.info("ER Diagram generation successful. Path: {}", erDiagramPath);
            } else {
                logger.warn("ER Diagram generation returned null.");
                // erDiagramPlantUmlSource = getPlantUmlSourceForErDiagram(classMetadataList); // Placeholder
                // logger.debug("PlantUML source for failed ER Diagram (if available):\n{}", erDiagramPlantUmlSource);
            }
        } catch (Exception e) {
            logger.error("Error generating ER diagram: {}. Type: {}", e.getMessage(), e.getClass().getName(), e);
            // logger.debug("PlantUML source for failed ER Diagram (if available) during exception:\n{}", erDiagramPlantUmlSource);
        }

        // Component Diagram
        String componentDiagramPath = null;
        try {
            componentDiagramPath = generateComponentDiagram(classMetadataList, baseOutputDir);
            if (componentDiagramPath != null) {
                diagramPaths.put(DiagramType.COMPONENT_DIAGRAM, componentDiagramPath);
                logger.info("Component Diagram generation successful. Path: {}", componentDiagramPath);
            } else {
                logger.warn("Component Diagram generation returned null.");
                // The generateComponentDiagram method already logs its PlantUML source on IOException.
            }
        } catch (Exception e) {
            logger.error("Error generating component diagram: {}. Type: {}", e.getMessage(), e.getClass().getName(), e);
            // The generateComponentDiagram method already logs its PlantUML source on IOException.
        }

        // Usecase Diagram
        String usecaseDiagramPath = null;
        try {
            usecaseDiagramPath = generateUsecaseDiagram(classMetadataList, baseOutputDir);
            if (usecaseDiagramPath != null) {
                diagramPaths.put(DiagramType.USECASE_DIAGRAM, usecaseDiagramPath);
                logger.info("Usecase Diagram generation successful. Path: {}", usecaseDiagramPath);
            } else {
                logger.warn("Usecase Diagram generation returned null.");
                // The generateUsecaseDiagram method already logs its PlantUML source on IOException.
            }
        } catch (Exception e) {
            logger.error("Error generating usecase diagram: {}. Type: {}", e.getMessage(), e.getClass().getName(), e);
            // The generateUsecaseDiagram method already logs its PlantUML source on IOException.
        }
        
        logger.info("Final diagram paths collected: {}", diagramPaths);
        return diagramPaths;
    }

    @Override
    public String generateClassDiagram(List<ClassMetadata> classMetadataList, String outputDir) {
        if (classMetadataList == null || classMetadataList.isEmpty()) {
            logger.info("No class metadata provided for class diagram generation.");
            return null;
        }
        logger.debug("Starting class diagram generation with {} classes", classMetadataList.size());
        StringBuilder plantUmlSource = new StringBuilder();
        plantUmlSource.append("@startuml\n");
        plantUmlSource.append("skinparam classAttributeIconSize 0\n"); // Hide attribute icons for cleaner look
        plantUmlSource.append("skinparam stereotypeCBackgroundColor transparent\n");
        plantUmlSource.append("hide empty members\n"); // Hide empty members

        // Define classes and their members
        int processedClasses = 0;
        for (ClassMetadata cmd : classMetadataList) {
            if (cmd == null || cmd.getName() == null || cmd.getName().isEmpty()) {
                logger.warn("Skipping class with null or empty name in PlantUML generation.");
                continue;
            }
            
            try {
            plantUmlSource.append("class \"").append(cmd.getName()).append("\" as ").append(getClassNameForPuml(cmd.getName()));
            if (cmd.getType() != null && !cmd.getType().equals("class")) {
                 plantUmlSource.append(" <<").append(cmd.getType()).append(">> ");
            }
            plantUmlSource.append(" {\n");
                
                // Process fields
            if (cmd.getFields() != null) {
                cmd.getFields().forEach(field -> {
                        try {
                    // Construct field string for PlantUML from FieldMetadata
                    String visibility = field.getVisibility() != null ? field.getVisibility() : "private";
                    String type = field.getType() != null ? sanitizePuml(field.getType()) : "Object";
                    String name = field.getName() != null ? sanitizePuml(field.getName()) : "unnamedField";
                            
                    String plantUmlFieldString = "";
                    if ("public".equals(visibility)) plantUmlFieldString += "+";
                    else if ("private".equals(visibility)) plantUmlFieldString += "-";
                    else if ("protected".equals(visibility)) plantUmlFieldString += "#";
                    else plantUmlFieldString += "~"; // package private/default

                    plantUmlFieldString += name + ": " + type;
                    if (field.isStatic()) { // Underline static fields
                        plantUmlFieldString = "{static} " + plantUmlFieldString;
                    }

                    plantUmlSource.append("  {field} ").append(plantUmlFieldString).append("\n");
                        } catch (Exception e) {
                            logger.warn("Exception processing field in class {}: {}", cmd.getName(), e.getMessage());
                        }
                });
            }
                
                // Process methods
            if (cmd.getMethods() != null) {
                cmd.getMethods().forEach(method -> {
                        try {
                    plantUmlSource.append("  {method} ").append(sanitizePuml(method.getName()));
                    plantUmlSource.append("(");
                    if (method.getParameters() != null && !method.getParameters().isEmpty()){
                        plantUmlSource.append(String.join(", ", method.getParameters().stream().map(this::sanitizePuml).toArray(String[]::new)));
                    }
                    plantUmlSource.append(")");
                    if(method.getReturnType() != null && !method.getReturnType().equals("void")){
                         plantUmlSource.append(": ").append(sanitizePuml(method.getReturnType()));
                    }
                    plantUmlSource.append("\n");
                        } catch (Exception e) {
                            logger.warn("Exception processing method {} in class {}: {}", method.getName(), cmd.getName(), e.getMessage());
                        }
                });
            }
            plantUmlSource.append("}\n");
                processedClasses++;
            } catch (Exception e) {
                logger.warn("Exception processing class {}: {}", cmd.getName(), e.getMessage());
            }
        }
        logger.debug("Processed {} out of {} classes", processedClasses, classMetadataList.size());

        // Define relationships (inheritance, interface implementation)
        int relationshipsCount = 0;
        for (ClassMetadata cmd : classMetadataList) {
            if (cmd == null || cmd.getName() == null || cmd.getName().isEmpty()) {
                continue; // Already logged warning above
            }
            try {
            String childClassPumlName = getClassNameForPuml(cmd.getName());
            if (cmd.getParentClass() != null && !cmd.getParentClass().isEmpty()) {
                    String parentPumlName = getClassNameForPuml(cmd.getParentClass());
                    if (parentPumlName != null && !parentPumlName.isEmpty()) { // Check if parent could be processed
                        plantUmlSource.append(parentPumlName).append(" <|-- ").append(childClassPumlName).append(" : extends\n");
                        relationshipsCount++;
                    }
            }
            if (cmd.getInterfaces() != null) {
                    // Use an AtomicInteger to track relationships count within the lambda
                    final java.util.concurrent.atomic.AtomicInteger interfaceRelCount = new java.util.concurrent.atomic.AtomicInteger(0);
                    cmd.getInterfaces().forEach(iface -> {
                        try {
                            if (iface != null && !iface.isEmpty()) {
                                String ifacePumlName = getClassNameForPuml(iface);
                                if (ifacePumlName != null && !ifacePumlName.isEmpty()) { // Check if interface could be processed
                                    plantUmlSource.append(ifacePumlName).append(" <|.. ").append(childClassPumlName).append(" : implements\n");
                                    interfaceRelCount.incrementAndGet();
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Exception processing interface relationship for class {}: {}", cmd.getName(), e.getMessage());
                        }
                    });
                    // Add the interface relationship count to the overall count
                    relationshipsCount += interfaceRelCount.get();
                }
            } catch (Exception e) {
                logger.warn("Exception processing class relationships for {}: {}", cmd.getName(), e.getMessage());
            }
        }
        logger.debug("Added {} inheritance/implementation relationships", relationshipsCount);

        // Add association arrows for fields referencing other classes (including collections/arrays)
        int associationCount = 0;
        for (ClassMetadata sourceClass : classMetadataList) {
            if (sourceClass == null || sourceClass.getName() == null || sourceClass.getName().isEmpty()) {
                continue;
            }
            try {
            String sourceClassName = getClassNameForPuml(sourceClass.getName());
                if (sourceClassName == null || sourceClassName.isEmpty()) continue;

            if (sourceClass.getFields() != null) {
                for (com.codedocgen.model.FieldMetadata field : sourceClass.getFields()) {
                        try {
                    String rawType = field.getType();
                    if (rawType == null) continue;

                    String type = rawType.replaceAll("<[^>]+>", ""); // Remove generics for simple matching
                    type = type.replaceAll("\\[\\]", ""); // Remove array indicators for simple matching
                    
                    String fieldName = field.getName();
                    if (fieldName == null) fieldName = "";

                    boolean isCollection = rawType.matches(".*(List|Set|Collection|Iterable|Map|ArrayList|HashSet|HashMap|<.+>|\\[\\]).*");
                    String elementType = type; // Default to the cleaned type

                    // Try to extract generic type for collections more robustly
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:List|Set|Collection|Iterable|Map)<([A-Za-z0-9_.]+)>").matcher(rawType);
                    if (m.find()) {
                        elementType = m.group(1);
                        isCollection = true;
                    } else if (rawType.endsWith("[]")) {
                        elementType = rawType.substring(0, rawType.length() - 2);
                        isCollection = true;
                    }
                    
                    // Check if this type or elementType matches any class in the project
                    for (ClassMetadata targetClass : classMetadataList) {
                                if (targetClass == null || targetClass.getName() == null || targetClass.getName().isEmpty()) {
                                    continue;
                                }
                        if (targetClass.getName().equals(type) || targetClass.getName().equals(elementType) ||
                                    (targetClass.getPackageName() != null && (targetClass.getPackageName() + "." + targetClass.getName()).equals(type)) ||
                                    (targetClass.getPackageName() != null && (targetClass.getPackageName() + "." + targetClass.getName()).equals(elementType)) ) {

                            String targetClassName = getClassNameForPuml(targetClass.getName());
                                    if (targetClassName == null || targetClassName.isEmpty()) continue;

                            String multiplicity = isCollection ? " \"*\"" : ""; // PlantUML uses "1" -- "0..*"
                            plantUmlSource.append(sourceClassName).append(" --> ").append(targetClassName)
                                .append(" : ").append(sanitizePuml(fieldName)).append(multiplicity).append("\n");
                                    associationCount++;
                            break; // Found a target, no need to check other classes for this field
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Exception processing field relationship for {} in class {}: {}", field.getName(), sourceClass.getName(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception processing field relationships for class {}: {}", sourceClass.getName(), e.getMessage());
            }
        }
        logger.debug("Added {} field-based associations", associationCount);

        plantUmlSource.append("@enduml\n");
        logger.debug("Completed PlantUML source generation for class diagram, total size: {} chars", plantUmlSource.length());

        try {
            logger.debug("Creating SourceStringReader for PlantUML processing");
            SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
            String diagramFileName = "class_diagram.svg";
            File outputFile = new File(outputDir, diagramFileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                logger.debug("Generating PlantUML image");
                reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                baos.writeTo(fos);
                logger.info("Class diagram generated: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath(); // Return path to the generated SVG
            } 
        } catch (IOException e) { // Specific catch for IO during image generation/saving
            logger.error("IOException during class diagram image generation/saving: {}", e.getMessage(), e);
            logger.debug("Failed PlantUML source for class diagram:\n{}", plantUmlSource.toString());
            return null; 
        } catch (Throwable t) { // Catch any other throwable during PlantUML processing
            logger.error("Throwable (e.g. PlantUML syntax error) during class diagram processing: {}", t.getMessage(), t);
            logger.debug("Failed PlantUML source for class diagram (Throwable):\n{}", plantUmlSource.toString());
            return null;
        }
    }

    @Override
    public String generateSequenceDiagram(List<String> callFlow, String outputDir, String diagramName) {
        if (callFlow == null || callFlow.isEmpty()) {
            logger.info("No call flow provided for sequence diagram generation.");
            return null;
        }
        StringBuilder plantUmlSource = new StringBuilder();
        plantUmlSource.append("@startuml\n");
        plantUmlSource.append("autonumber\n");
        // Use lifelines for each class in the flow
        List<String> lifelines = new java.util.ArrayList<>();
        for (String fqn : callFlow) {
            String className = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : fqn;
            if (!lifelines.contains(className)) lifelines.add(className);
        }
        // Declare participants
        for (String className : lifelines) {
            // Use quoted FQN for participant name to avoid syntax errors
            plantUmlSource.append("participant \"").append(className).append("\"\n");
        }
        // Draw calls
        for (int i = 0; i < callFlow.size() - 1; i++) {
            String from = callFlow.get(i);
            String to = callFlow.get(i + 1);
            String fromClass = from.contains(".") ? from.substring(0, from.lastIndexOf('.')) : from;
            String toClass = to.contains(".") ? to.substring(0, to.lastIndexOf('.')) : to;
            String fromMethod = from.substring(from.lastIndexOf('.') + 1);
            String toMethod = to.substring(to.lastIndexOf('.') + 1);
            plantUmlSource.append("\"").append(fromClass).append("\" -> \"")
                .append(toClass).append("\": ").append(toMethod).append("()\n");
        }
        plantUmlSource.append("@enduml\n");
        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
            String diagramFileName = diagramName + ".svg";
            File outputFile = new File(outputDir, diagramFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                baos.writeTo(fos);
                logger.info("Sequence diagram generated: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            }
        } catch (IOException e) {
            logger.error("Error generating or saving sequence diagram: {}", e.getMessage(), e);
            return null;
        }
    }

    public String generateEntityRelationshipDiagram(List<ClassMetadata> classMetadataList, String outputDir) {
        if (classMetadataList == null || classMetadataList.isEmpty()) {
            logger.info("No class metadata provided for ER diagram generation.");
            return null;
        }
        logger.debug("Starting ER diagram generation with {} classes", classMetadataList.size());
        StringBuilder plantUmlSource = new StringBuilder();
        plantUmlSource.append("@startuml\n");
        plantUmlSource.append("!define table(x) class x << (T,#FFAAAA) >>\n");
        plantUmlSource.append("hide empty members\n");
        
        // Only include entity classes
        List<ClassMetadata> entities = classMetadataList.stream().filter(c -> "entity".equalsIgnoreCase(c.getType())).toList();
        logger.debug("Found {} entity-typed classes for ER diagram", entities.size());
        
        if (entities.isEmpty()) {
            logger.warn("No entities found for ER diagram. If this is unexpected, check if classes are properly marked with @Entity annotation.");
            // Generate minimal diagram to avoid blank diagram
            plantUmlSource.append("class \"No Entities Found\" as NoEntities << (T,#FFAAAA) >> {\n  This project has no JPA entities\n}\n");
            plantUmlSource.append("@enduml\n");
            
            try {
                SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
                String diagramFileName = "entity_relationship_diagram.svg";
                File outputFile = new File(outputDir, diagramFileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                    baos.writeTo(fos);
                    logger.info("Empty ER diagram generated: {}", outputFile.getAbsolutePath());
                    return outputFile.getAbsolutePath();
                }
            } catch (Exception e) {
                logger.error("Error generating empty ER diagram: {}", e.getMessage(), e);
                return null;
            }
        }
        
        // Process the actual entities
        int processedEntities = 0;
        for (ClassMetadata entity : entities) {
            try {
            plantUmlSource.append("table(").append(entity.getName()).append(") {\n");
            if (entity.getFields() != null) {
                for (com.codedocgen.model.FieldMetadata field : entity.getFields()) {
                        try {
                    // Construct field string for PlantUML, e.g., fieldName : fieldType
                    String fieldName = field.getName() != null ? sanitizePuml(field.getName()) : "unnamedField";
                    String fieldType = field.getType() != null ? sanitizePuml(field.getType()) : "Object";
                    String plantUmlFieldString = fieldName + " : " + fieldType;
                    plantUmlSource.append("  ").append(plantUmlFieldString).append("\n");
                        } catch (Exception e) {
                            logger.warn("Exception processing field {} in entity {}: {}", field.getName(), entity.getName(), e.getMessage());
                        }
                    }
                }
                plantUmlSource.append("}\n");
                processedEntities++;
            } catch (Exception e) {
                logger.warn("Exception processing entity {}: {}", entity.getName(), e.getMessage());
            }
        }
        logger.debug("Processed {} out of {} entities", processedEntities, entities.size());
        
        // Add relationships based on field types and annotations
        int relationshipCount = 0;
        for (ClassMetadata entity : entities) {
            if (entity.getFields() != null) {
                for (com.codedocgen.model.FieldMetadata field : entity.getFields()) {
                    try {
                    String rawFieldType = field.getType();
                    if (rawFieldType == null) continue;

                    // Simplify type for matching (remove generics and array indicators)
                    String simplifiedFieldType = rawFieldType.replaceAll("<[^>]+>", "");
                    simplifiedFieldType = simplifiedFieldType.replaceAll("\\[\\]", "");
                    String fieldNameForLabel = field.getName() != null ? sanitizePuml(field.getName()) : "";

                    // Try to get the element type for collections
                    String potentialElementType = simplifiedFieldType;
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:List|Set|Collection|Iterable|Map)<([A-Za-z0-9_.]+)>").matcher(rawFieldType);
                    if (m.find()) {
                        potentialElementType = m.group(1);
                    } else if (rawFieldType.endsWith("[]")) {
                        potentialElementType = rawFieldType.substring(0, rawFieldType.length() - 2);
                    }

                    for (ClassMetadata target : entities) {
                        // Check against simple name, FQN, and potential element type (simple and FQN)
                        String targetSimpleName = target.getName();
                            String targetFQN = (target.getPackageName() != null ? target.getPackageName() + "." : "") + target.getName();

                        boolean matchesSimpleType = targetSimpleName.equals(simplifiedFieldType) || targetFQN.equals(simplifiedFieldType);
                        boolean matchesElementType = targetSimpleName.equals(potentialElementType) || targetFQN.equals(potentialElementType);

                        if (!target.getName().equals(entity.getName()) && (matchesSimpleType || matchesElementType)) {
                            // Determine relationship type (e.g., one-to-many if collection)
                            boolean isCollectionField = rawFieldType.matches(".*(List|Set|Collection|Iterable|Map|<.+>|\\[\\]).*");
                            String relationshipArrow = isCollectionField ? "}o--|{" : "}--||"; // Example: one-to-many vs one-to-one
                            
                            plantUmlSource.append(getClassNameForPuml(entity.getName()))
                                          .append(relationshipArrow)
                                          .append(getClassNameForPuml(target.getName()))
                                          .append(" : ")
                                          .append(fieldNameForLabel)
                                          .append("\n");
                                relationshipCount++;
                            break; // Found a relationship for this field, move to next field
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Exception processing field relationship for {} in entity {}: {}", field.getName(), entity.getName(), e.getMessage());
                    }
                }
            }
        }
        logger.debug("Added {} entity relationships", relationshipCount);
        
        plantUmlSource.append("@enduml\n");
        logger.debug("Completed PlantUML source generation for ER diagram, total size: {} chars", plantUmlSource.length());
        
        try {
            logger.debug("Creating SourceStringReader for PlantUML processing");
            SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
            String diagramFileName = "entity_relationship_diagram.svg";
            File outputFile = new File(outputDir, diagramFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                logger.debug("Generating PlantUML image");
                reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                baos.writeTo(fos);
                logger.info("ER diagram generated: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            }
        } catch (IOException e) { // Specific catch for IO during image generation/saving
            logger.error("IOException during ER diagram image generation/saving: {}", e.getMessage(), e);
            logger.debug("Failed PlantUML source for ER diagram:\n{}", plantUmlSource.toString());
            return null;
        } catch (Throwable t) { // Catch any other throwable during PlantUML processing
            logger.error("Throwable (e.g. PlantUML syntax error) during ER diagram processing: {}", t.getMessage(), t);
            logger.debug("Failed PlantUML source for ER diagram (Throwable):\n{}", plantUmlSource.toString());
            return null;
        }
    }

    public String generateComponentDiagram(List<ClassMetadata> classMetadataList, String outputDir) {
        if (classMetadataList == null || classMetadataList.isEmpty()) {
            logger.info("No class metadata provided for component diagram generation.");
            return null;
        }
        logger.debug("Starting component diagram generation with {} classes", classMetadataList.size());

        // Expanded stereotypes to include SOAP-specific components
        Set<String> componentStereotypes = Set.of(
            "service", "repository", "controller", "component", "restcontroller", 
            "endpoint", "configuration", "soapendpoint", "webservice", "webserviceendpoint");

        // Enhanced detection for SOAP/legacy components by also checking annotations and class names
        List<ClassMetadata> components = classMetadataList.stream()
                .filter(cmd -> {
                    if (cmd == null) return false;
                    
                    // Check explicit component stereotypes
                    if (cmd.getType() != null && componentStereotypes.contains(cmd.getType().toLowerCase())) {
                        return true;
                    }
                    
                    // Check class name patterns for SOAP components
                    if (cmd.getName() != null && (
                        cmd.getName().endsWith("Endpoint") || 
                        cmd.getName().endsWith("ServiceImpl") ||
                        cmd.getName().endsWith("Service") ||
                        cmd.getName().contains("SOAP") ||
                        cmd.getName().contains("WebService"))) {
                        return true;
                    }
                    
                    // Check annotations for SOAP components
                    if (cmd.getAnnotations() != null) {
                        for (String annotation : cmd.getAnnotations()) {
                            if (annotation.contains("WebService") || 
                                annotation.contains("Endpoint") ||
                                annotation.contains("SOAPBinding")) {
                                return true;
                            }
                        }
                    }
                    
                    return false;
                })
                .collect(Collectors.toList());

        if (components.isEmpty()) {
            logger.info("No classes qualifying as components found for diagram generation.");
            // Generate minimal diagram to avoid blank diagram
            StringBuilder emptyDiagramSource = new StringBuilder();
            emptyDiagramSource.append("@startuml\n");
            emptyDiagramSource.append("skinparam componentStyle uml2\n");
            emptyDiagramSource.append("component [No Components Found] as NoComponents\n");
            emptyDiagramSource.append("note right of NoComponents\n");
            emptyDiagramSource.append("  This project has no Spring components\n");
            emptyDiagramSource.append("  or SOAP services/endpoints identified.\n");
            emptyDiagramSource.append("end note\n");
            emptyDiagramSource.append("@enduml\n");
            
            try {
                SourceStringReader reader = new SourceStringReader(emptyDiagramSource.toString());
                String diagramFileName = "component_diagram.svg";
                File outputFile = new File(outputDir, diagramFileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                    baos.writeTo(fos);
                    logger.info("Empty component diagram generated: {}", outputFile.getAbsolutePath());
                    return outputFile.getAbsolutePath();
                }
            } catch (Exception e) {
                logger.error("Error generating empty component diagram: {}", e.getMessage(), e);
                return null;
            }
        }

        logger.debug("Found {} components for component diagram", components.size());
        StringBuilder plantUmlSource = new StringBuilder();
        plantUmlSource.append("@startuml\n");
        plantUmlSource.append("skinparam componentStyle uml2\n");
        plantUmlSource.append("skinparam packageStyle rect\n");
        plantUmlSource.append("left to right direction\n");
        
        // Check for SOAP Endpoints and create WSDL interface
        boolean hasSoapEndpoints = false;
        for (ClassMetadata cmd : components) {
            if (cmd.getName() != null && 
                (cmd.getName().endsWith("Endpoint") || 
                (cmd.getAnnotations() != null && cmd.getAnnotations().stream().anyMatch(a -> a.contains("Endpoint") || a.contains("WebService"))))) {
                hasSoapEndpoints = true;
                break;
            }
        }
        
        if (hasSoapEndpoints) {
            plantUmlSource.append("interface \"WSDL\" as WSDL0\n");
        }

        try {
            // Group components by package
            Map<String, List<ClassMetadata>> componentsByPackage = components.stream()
                    .collect(Collectors.groupingBy(cmd -> cmd.getPackageName() != null ? cmd.getPackageName() : "default_package"));
            
            logger.debug("Grouped components into {} packages", componentsByPackage.size());
            int processedPackages = 0;
            int totalComponents = 0;
            
            // Map component roles for better styling
            Map<String, String> componentRoles = new HashMap<>();
            for (ClassMetadata cmd : components) {
                if (cmd.getName() == null) continue;
                
                String role = determineComponentRole(cmd);
                componentRoles.put(cmd.getName(), role);
            }

            for (Map.Entry<String, List<ClassMetadata>> packageEntry : componentsByPackage.entrySet()) {
                try {
                    String packageName = packageEntry.getKey();
                    List<ClassMetadata> packageComponents = packageEntry.getValue();

                    if (!"default_package".equals(packageName)) {
                        plantUmlSource.append("package \"").append(packageName).append("\" {\n");
                    }

                    for (ClassMetadata cmp : packageComponents) {
                        try {
                            if (cmp.getName() == null) continue;
                            String componentPumlName = getClassNameForPuml(cmp.getName());
                            if (componentPumlName == null || componentPumlName.isEmpty()) continue;
                            
                            // Add appropriate stereotype based on the component role
                            String role = componentRoles.get(cmp.getName());
                            String displayName = cmp.getName().contains(".") ? 
                                cmp.getName().substring(cmp.getName().lastIndexOf('.') + 1) : 
                                cmp.getName();
                                
                            plantUmlSource.append("  component [").append(sanitizePuml(displayName)).append("] as ").append(componentPumlName);
                            
                            if (role != null) {
                                plantUmlSource.append(" <<").append(role).append(">>");
                            } else if (cmp.getType() != null) {
                                plantUmlSource.append(" <<").append(cmp.getType()).append(">>");
                            }
                            
                            plantUmlSource.append("\n");
                            totalComponents++;
                        } catch (Exception e) {
                            logger.warn("Exception processing component {}: {}", cmp.getName(), e.getMessage());
                        }
                    }

                    if (!"default_package".equals(packageName)) {
                        plantUmlSource.append("}\n");
                    }
                    processedPackages++;
                } catch (Exception e) {
                    logger.warn("Exception processing package {}: {}", packageEntry.getKey(), e.getMessage());
                }
            }
            logger.debug("Added {} components in {} packages", totalComponents, processedPackages);
            
            // Add relationships between components
            Set<String> relationships = new HashSet<>();
            final java.util.concurrent.atomic.AtomicInteger relationshipCount = new java.util.concurrent.atomic.AtomicInteger(0);

            for (ClassMetadata sourceComponent : components) {
                try {
                    if (sourceComponent.getName() == null) continue;
                    String sourcePumlName = getClassNameForPuml(sourceComponent.getName());
                    if (sourcePumlName == null || sourcePumlName.isEmpty()) continue;

                    // Field-based dependencies
                    if (sourceComponent.getFields() != null) {
                        for (FieldMetadata field : sourceComponent.getFields()) {
                            try {
                                if (field.getType() == null || field.getName() == null) continue;
                                String rawFieldType = field.getType();
                                String simpleFieldType = rawFieldType.replaceAll("<[^>]+>", "").replaceAll("\\[\\]", "");
                                
                                final String finalSimpleFieldType = simpleFieldType;
                                components.stream()
                                    .filter(targetCmd -> targetCmd.getName() != null && targetCmd.getPackageName() != null &&
                                            ( (targetCmd.getPackageName() + "." + targetCmd.getName()).equals(finalSimpleFieldType) ||
                                            targetCmd.getName().equals(finalSimpleFieldType) ) )
                                    .findFirst()
                                    .ifPresent(targetComponent -> {
                                        try {
                                            String targetPumlName = getClassNameForPuml(targetComponent.getName());
                                            if (targetPumlName == null || targetPumlName.isEmpty() || sourcePumlName.equals(targetPumlName)) return;
                                            
                                            String relKey = sourcePumlName + "->" + targetPumlName;
                                            if (relationships.add(relKey)) { 
                                                // Add more descriptive relationship for SOAP components
                                                String relation = determineRelationship(sourceComponent, targetComponent);
                                                plantUmlSource.append(sourcePumlName).append(" --> ").append(targetPumlName);
                                                
                                                if (relation != null) {
                                                    plantUmlSource.append(" : ").append(relation);
                                                }
                                                
                                                plantUmlSource.append("\n");
                                                relationshipCount.incrementAndGet();
                                            }
                                        } catch (Exception e) {
                                            logger.warn("Exception creating relationship from {} to {}: {}", 
                                                sourceComponent.getName(), targetComponent.getName(), e.getMessage());
                                        }
                                    });
                            } catch (Exception e) {
                                logger.warn("Exception processing field {} in component {}: {}", 
                                    field.getName(), sourceComponent.getName(), e.getMessage());
                            }
                        }
                    }

                    // Method call-based dependencies
                    if (sourceComponent.getMethods() != null) {
                        for (MethodMetadata method : sourceComponent.getMethods()) {
                            try {
                                if (method.getCalledMethods() != null) {
                                    for (String calledMethodSignature : method.getCalledMethods()) {
                                        try {
                                            if (calledMethodSignature == null || calledMethodSignature.trim().isEmpty()) continue;

                                            String calledClassFqn = null;
                                            final String calledSimpleMethodName;

                                            int paramsOpenParen = calledMethodSignature.indexOf('(');
                                            String beforeParams = paramsOpenParen != -1 ? calledMethodSignature.substring(0, paramsOpenParen) : calledMethodSignature;
                                            
                                            int lastDotBeforeMethodName = beforeParams.lastIndexOf('.');
                                            if (lastDotBeforeMethodName > 0 && lastDotBeforeMethodName < beforeParams.length() - 1) {
                                                calledClassFqn = beforeParams.substring(0, lastDotBeforeMethodName);
                                                calledSimpleMethodName = beforeParams.substring(lastDotBeforeMethodName + 1);
                                            } else {
                                                calledSimpleMethodName = "";
                                            }

                                            if (calledClassFqn != null && !calledClassFqn.trim().isEmpty()) {
                                                final String finalCalledClassFqn = calledClassFqn;
                                                components.stream()
                                                    .filter(targetCmd -> targetCmd.getName() != null && targetCmd.getPackageName() != null &&
                                                            (targetCmd.getPackageName() + "." + targetCmd.getName()).equals(finalCalledClassFqn))
                                                    .findFirst()
                                                    .ifPresent(targetComponent -> {
                                                        try {
                                                            String targetPumlName = getClassNameForPuml(targetComponent.getName());
                                                            if (targetPumlName == null || targetPumlName.isEmpty() || sourcePumlName.equals(targetPumlName)) return;

                                                            String relKey = sourcePumlName + "..>" + targetPumlName + ":" + calledSimpleMethodName;
                                                            if (relationships.add(relKey)) { 
                                                                plantUmlSource.append(sourcePumlName).append(" ..> ").append(targetPumlName);
                                                                plantUmlSource.append(" : calls ").append(sanitizePuml(calledSimpleMethodName)).append("()\n");
                                                                relationshipCount.incrementAndGet();
                                                            }
                                                        } catch (Exception e) {
                                                            logger.warn("Exception creating method call relationship from {} to {}: {}", 
                                                                sourceComponent.getName(), targetComponent.getName(), e.getMessage());
                                                        }
                                                    });
                                            }
                                        } catch (Exception e) {
                                            logger.warn("Exception processing called method {} in component {}: {}", 
                                                calledMethodSignature, sourceComponent.getName(), e.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("Exception processing method {} in component {}: {}", 
                                    method.getName(), sourceComponent.getName(), e.getMessage());
                            }
                        }
                    }
                    
                    // Check for SOAP/WSDL relationships for endpoints
                    if (componentRoles.get(sourceComponent.getName()) != null && 
                        componentRoles.get(sourceComponent.getName()).equals("endpoint") &&
                        hasSoapEndpoints) {
                        // Add relationship between the endpoint and WSDL
                        plantUmlSource.append(sourcePumlName).append(" ..> WSDL0 : implements\n");
                        relationshipCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.warn("Exception processing component relationships for {}: {}", sourceComponent.getName(), e.getMessage());
                }
            }
            logger.debug("Added {} component relationships", relationshipCount.get());
        } catch (Exception e) {
            logger.warn("Exception during component diagram generation: {}", e.getMessage(), e);
        }

        plantUmlSource.append("@enduml\n");
        logger.debug("Completed PlantUML source generation for component diagram, total size: {} chars", plantUmlSource.length());

        try {
            logger.debug("Creating SourceStringReader for PlantUML processing");
            SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
            String diagramFileName = "component_diagram.svg";
            File outputFile = new File(outputDir, diagramFileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                logger.debug("Generating PlantUML image");
                reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                baos.writeTo(fos);
                logger.info("Component diagram generated: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath(); 
            }
        } catch (IOException e) { // Specific catch for IO during image generation/saving
            logger.error("IOException during component diagram image generation/saving: {}", e.getMessage(), e);
            logger.debug("Failed PlantUML source for component diagram:\n{}", plantUmlSource.toString());
            return null;
        } catch (Throwable t) { // Catch any other throwable during PlantUML processing
            logger.error("Throwable (e.g. PlantUML syntax error) during component diagram processing: {}", t.getMessage(), t);
            logger.debug("Failed PlantUML source for component diagram (Throwable):\n{}", plantUmlSource.toString());
            return null;
        }
    }

    public String generateUsecaseDiagram(List<ClassMetadata> classMetadataList, String outputDir) {
        if (classMetadataList == null || classMetadataList.isEmpty()) {
            logger.info("No class metadata provided for usecase diagram generation.");
            return null;
        }
        logger.debug("Starting usecase diagram generation with {} classes", classMetadataList.size());
        
        StringBuilder plantUmlSource = new StringBuilder();
        plantUmlSource.append("@startuml\n");
        plantUmlSource.append("left to right direction\n");
        plantUmlSource.append("actor User\n"); // Default actor
        plantUmlSource.append("actor \"External System\" as External\n"); // Add external system actor for SOAP services

        plantUmlSource.append("rectangle \"System Boundary\" {\n");

        // Expanded controller detection to include SOAP endpoints and any public service methods
        List<ClassMetadata> controllers = classMetadataList.stream()
                .filter(cmd -> cmd != null && 
                    (
                        // Spring controllers
                        (cmd.getType() != null && (cmd.getType().equalsIgnoreCase("controller") || cmd.getType().equalsIgnoreCase("restcontroller"))) ||
                        // SOAP endpoints
                        (cmd.getName() != null && cmd.getName().endsWith("Endpoint")) ||
                        // Check annotations for SOAP or web service-related annotations
                        (cmd.getAnnotations() != null && cmd.getAnnotations().stream().anyMatch(a -> 
                            a.contains("WebService") || a.contains("Endpoint") || a.contains("SOAPBinding")))
                    )
                )
                .collect(Collectors.toList());
                
        // Also include service classes with public methods as use cases
        List<ClassMetadata> services = classMetadataList.stream()
                .filter(cmd -> cmd != null && cmd.getName() != null && 
                    (
                        // Service implementations
                        (cmd.getName().endsWith("ServiceImpl") || 
                         cmd.getName().endsWith("Service")) ||
                        // Service annotation 
                        (cmd.getAnnotations() != null && cmd.getAnnotations().stream().anyMatch(a -> a.contains("Service")))
                    )
                )
                .collect(Collectors.toList());

        if (controllers.isEmpty() && services.isEmpty()) {
            logger.warn("No controller/endpoint/service classes found to derive use cases.");
            // Add a default usecase so diagram isn't empty
            plantUmlSource.append("  usecase \"(No Controllers or Services Found)\" as NoControllers\n");
            plantUmlSource.append("  note right of NoControllers\n");
            plantUmlSource.append("    No controllers, SOAP endpoints or services found in this project.\n");
            plantUmlSource.append("    Check if controllers/endpoints are properly annotated\n");
            plantUmlSource.append("    with @Controller, @RestController, @Endpoint, or @WebService.\n");
            plantUmlSource.append("  end note\n");
        } else {
            logger.debug("Found {} controllers/endpoints and {} services for usecase diagram", 
                controllers.size(), services.size());
            
            final java.util.concurrent.atomic.AtomicInteger usecaseCount = new java.util.concurrent.atomic.AtomicInteger(0);
            
            // Process controllers first
            for (ClassMetadata controller : controllers) {
                try {
                    if (controller.getMethods() != null) {
                        boolean isSoapEndpoint = controller.getName() != null && 
                            (controller.getName().endsWith("Endpoint") || 
                             (controller.getAnnotations() != null && 
                              controller.getAnnotations().stream().anyMatch(a -> 
                                a.contains("WebService") || a.contains("Endpoint") || a.contains("SOAPBinding"))));
                              
                        controller.getMethods().stream()
                            .filter(method -> method.getVisibility() != null && method.getVisibility().equals("public"))
                            .forEach(method -> {
                                try {
                                    String usecaseName = sanitizePuml(controller.getName() + "." + method.getName());
                                    // Replace dots with spaces for better readability in usecase oval
                                    String className = controller.getName().contains(".") ? 
                                        controller.getName().substring(controller.getName().lastIndexOf('.') + 1) : 
                                        controller.getName();
                                    String usecaseLabel = className + "." + method.getName();
                                    
                                    plantUmlSource.append("  usecase \"");
                                    
                                    // Add special icon for SOAP operations
                                    if (isSoapEndpoint) {
                                        plantUmlSource.append("<&cog> ");
                                    }
                                    
                                    plantUmlSource.append(usecaseLabel).append("\" as ").append(usecaseName).append("\n");
                                    
                                    // Connect appropriate actor based on type
                                    if (isSoapEndpoint) {
                                        plantUmlSource.append("  External -- ").append(usecaseName).append("\n");
                                    } else {
                                        plantUmlSource.append("  User -- ").append(usecaseName).append("\n");
                                    }
                                    
                                    // Check for service method calls to add second-level relationships
                                    if (method.getCalledMethods() != null) {
                                        for (String callPath : method.getCalledMethods()) {
                                            if (callPath == null) continue;
                                            
                                            // Extract called class and method
                                            int lastDot = callPath.lastIndexOf('.');
                                            int openParen = callPath.indexOf('(');
                                            
                                            if (lastDot > 0 && (openParen < 0 || lastDot < openParen)) {
                                                String calledClass = callPath.substring(0, lastDot);
                                                String calledMethod = openParen > 0 ? 
                                                    callPath.substring(lastDot + 1, openParen) : 
                                                    callPath.substring(lastDot + 1);
                                                
                                                // Check if the called method is in a service class
                                                for (ClassMetadata service : services) {
                                                    if (service.getName() != null && 
                                                        (service.getName().equals(calledClass) || 
                                                         (service.getPackageName() != null && 
                                                          (service.getPackageName() + "." + service.getName()).equals(calledClass)))) {
                                                        
                                                        // Find the matching method in the service
                                                        if (service.getMethods() != null) {
                                                            for (MethodMetadata serviceMethod : service.getMethods()) {
                                                                if (serviceMethod.getName() != null && 
                                                                    serviceMethod.getName().equals(calledMethod)) {
                                                                    
                                                                    // Add second-level use case and relationship
                                                                    String serviceUsecaseName = sanitizePuml(
                                                                        service.getName() + "." + serviceMethod.getName());
                                                                    
                                                                    String serviceClassName = service.getName().contains(".") ?
                                                                        service.getName().substring(service.getName().lastIndexOf('.') + 1) :
                                                                        service.getName();
                                                                        
                                                                    plantUmlSource.append("  usecase \"<&layers> ")
                                                                        .append(serviceClassName).append(".")
                                                                        .append(serviceMethod.getName())
                                                                        .append("\" as ").append(serviceUsecaseName)
                                                                        .append("\n");
                                                                        
                                                                    plantUmlSource.append("  ")
                                                                        .append(usecaseName).append(" ..> ")
                                                                        .append(serviceUsecaseName)
                                                                        .append(" : <<includes>>\n");
                                                                        
                                                                    usecaseCount.incrementAndGet();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    usecaseCount.incrementAndGet();
                                } catch (Exception e) {
                                    logger.warn("Exception processing usecase for method {} in controller {}: {}", 
                                        method.getName(), controller.getName(), e.getMessage());
                                }
                            });
                    }
                } catch (Exception e) {
                    logger.warn("Exception processing controller {} for usecases: {}", controller.getName(), e.getMessage());
                }
            }
            
            // Add common database interaction use cases if repositories exist
            boolean hasRepositories = classMetadataList.stream()
                .anyMatch(cmd -> cmd != null && 
                    (cmd.getType() != null && cmd.getType().equalsIgnoreCase("repository")) ||
                    (cmd.getName() != null && cmd.getName().contains("Repository")) ||
                    (cmd.getAnnotations() != null && cmd.getAnnotations().stream().anyMatch(a -> a.contains("Repository")))
                );
                
            if (hasRepositories) {
                plantUmlSource.append("  usecase \"<&database> Database Operations\" as DB\n");
                plantUmlSource.append("  actor \"Database\" as DBSystem\n");
                plantUmlSource.append("  DB -- DBSystem\n");
                    
                // Connect service methods to database operations
                for (ClassMetadata service : services) {
                    if (service.getMethods() != null) {
                        for (MethodMetadata method : service.getMethods()) {
                            if (method.getName() != null && 
                                (method.getName().startsWith("get") || 
                                 method.getName().startsWith("find") || 
                                 method.getName().startsWith("save") || 
                                 method.getName().startsWith("delete") || 
                                 method.getName().startsWith("update"))) {
                                 
                                String serviceUsecaseName = sanitizePuml(service.getName() + "." + method.getName());
                                plantUmlSource.append("  ").append(serviceUsecaseName).append(" ..> DB : <<uses>>\n");
                            }
                        }
                    }
                }
            }
            
            logger.debug("Added {} usecases from controller/service methods", usecaseCount.get());
        }
        plantUmlSource.append("}\n"); // End System Boundary
        plantUmlSource.append("@enduml\n");
        logger.debug("Completed PlantUML source generation for usecase diagram, total size: {} chars", plantUmlSource.length());

        try {
            logger.debug("Creating SourceStringReader for PlantUML processing");
            SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
            String diagramFileName = "usecase_diagram.svg";
            File outputFile = new File(outputDir, diagramFileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                logger.debug("Generating PlantUML image");
                reader.outputImage(baos, new FileFormatOption(FileFormat.SVG));
                baos.writeTo(fos);
                logger.info("Usecase diagram generated: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath(); 
            }
        } catch (IOException e) { // Specific catch for IO during image generation/saving
            logger.error("IOException during usecase diagram image generation/saving: {}", e.getMessage(), e);
            logger.debug("Failed PlantUML source for usecase diagram:\n{}", plantUmlSource.toString());
            return null;
        } catch (Throwable t) { // Catch any other throwable during PlantUML processing
            logger.error("Throwable (e.g. PlantUML syntax error) during usecase diagram processing: {}", t.getMessage(), t);
            logger.debug("Failed PlantUML source for usecase diagram (Throwable):\n{}", plantUmlSource.toString());
            return null;
        }
    }

    // Helper method to determine the role of a component based on its class name and annotations
    private String determineComponentRole(ClassMetadata classMetadata) {
        if (classMetadata == null || classMetadata.getName() == null) {
            return null;
        }
        
        String name = classMetadata.getName();
        
        // Check for SOAP/WebService components
        if (name.endsWith("Endpoint")) {
            return "endpoint";
        } else if (name.endsWith("ServiceImpl")) {
            return "service";
        } else if (name.contains("Repository")) {
            return "repository";
        } else if (name.contains("Config") || name.contains("Configuration")) {
            return "configuration";
        } else if (name.contains("Controller")) {
            return "controller";
        }
        
        // Check annotations if available
        if (classMetadata.getAnnotations() != null) {
            for (String annotation : classMetadata.getAnnotations()) {
                if (annotation.contains("WebService")) {
                    return "webservice";
                } else if (annotation.contains("Endpoint")) {
                    return "endpoint";
                } else if (annotation.contains("Repository")) {
                    return "repository";
                } else if (annotation.contains("Service")) {
                    return "service";
                } else if (annotation.contains("Controller")) {
                    return "controller";
                } else if (annotation.contains("Configuration")) {
                    return "configuration";
                }
            }
        }
        
        // Check explicit type if set
        if (classMetadata.getType() != null) {
            return classMetadata.getType().toLowerCase();
        }
        
        return null;
    }
    
    // Helper method to determine the relationship between components
    private String determineRelationship(ClassMetadata source, ClassMetadata target) {
        if (source == null || target == null) {
            return null;
        }
        
        String sourceRole = determineComponentRole(source);
        String targetRole = determineComponentRole(target);
        
        // Special case for SOAP components
        if ("endpoint".equals(sourceRole) && "service".equals(targetRole)) {
            return "delegates to";
        } else if ("service".equals(sourceRole) && "repository".equals(targetRole)) {
            return "uses";
        } else if ("controller".equals(sourceRole) && "service".equals(targetRole)) {
            return "uses";
        } else if ("webservice".equals(sourceRole) && "endpoint".equals(targetRole)) {
            return "exposes";
        }
        
        return null;
    }

    // Utility to create a PlantUML-safe class name (e.g., for aliases)
    // Handles potential null input and sanitizes the name.
    private String getClassNameForPuml(String className) {
        if (className == null || className.isEmpty()) {
            logger.warn("Attempted to get PUML name for a null or empty class name.");
            return "nullOrEmptyClassName_" + java.util.UUID.randomUUID().toString().substring(0, 8); // Return a unique placeholder
        }
        // Replace characters not suitable for PlantUML identifiers (especially in aliases)
        return className.replaceAll("[.:<>\\s()\\\\[\\\\]{}]", "_").replaceAll("[^a-zA-Z0-9_]", ""); // Added more chars to replace for aliases
    }
    
    private String sanitizePuml(String text) {
        if (text == null) {
            return "";
        }
        // Sanitize for PlantUML labels and other text:
        String sanitizedText = text.replace('\"', '\''); // Replace double quote char with single quote char
        // Replace other characters that might break PlantUML syntax or cause issues in labels/notes.
        sanitizedText = sanitizedText.replaceAll("[<>]", "_"); 
        sanitizedText = sanitizedText.replaceAll("\\$", "_dollar_"); // Escapes $ for PlantUML
        sanitizedText = sanitizedText.replaceAll("[\\n\\r]", " "); 
        sanitizedText = sanitizedText.replaceAll("[{}]", "_"); 
        return sanitizedText;
    }

    // Placeholder for Sequence Diagram generation
    // public String generateSequenceDiagram(CallFlowData callFlowData, String outputDir) { ... }
} 
