package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

/**
 * Metadata for a Java class
 */
@NoArgsConstructor
public class ClassMetadata {
    private String name;
    private String packageName;
    private String type; // E.g., "class", "interface", "enum", "controller", "service", "repository", "entity"
    private List<String> annotations = new ArrayList<>();
    private List<MethodMetadata> methods = new ArrayList<>();
    private List<FieldMetadata> fields = new ArrayList<>();
    private String parentClass; // Fully qualified name of the parent class
    private List<String> interfaces = new ArrayList<>(); // List of fully qualified names of implemented interfaces
    private String filePath; // Relative path to the source file
    private boolean isAbstract;
    private boolean isInterface;

    // Getter and Setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    public List<MethodMetadata> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodMetadata> methods) {
        this.methods = methods;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }

    public String getParentClass() {
        return parentClass;
    }

    public void setParentClass(String parentClass) {
        this.parentClass = parentClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }
} 