package com.codedocgen.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClassMetadata {
    private String name;
    private String packageName;
    private String type; // E.g., "class", "interface", "enum", "controller", "service", "repository", "entity"
    private List<String> annotations;
    private List<MethodMetadata> methods;
    private List<FieldMetadata> fields;
    private String parentClass; // Fully qualified name of the parent class
    private List<String> interfaces; // List of fully qualified names of implemented interfaces
    private String filePath; // Relative path to the source file
    private boolean isAbstract; // Added to resolve linter error
    private boolean isInterface; // Added to resolve linter error
    // Add more fields as needed, e.g., for imports, static blocks, inner classes
} 