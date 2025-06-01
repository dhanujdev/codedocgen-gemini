package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Metadata for a Java method
 */
@Data
@NoArgsConstructor
public class MethodMetadata {
    private String name;
    private String returnType;
    private String javadoc;
    private List<ParameterMetadata> parameters = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<String> exceptionsThrown = new ArrayList<>();
    private String visibility; // public, private, protected, default
    private boolean isStatic;
    private boolean isAbstract;
    private String packageName;
    private String className;

    // For call flow analysis
    private List<String> calledMethods = new ArrayList<>(); // List of fully qualified method names
    private List<String> externalCalls = new ArrayList<>(); // External method calls
    
    // For DAO analysis
    private List<DaoOperationDetail> daoOperations = new ArrayList<>();

    // Local variable metadata
    @Data
    @NoArgsConstructor
    public static class VariableMetadata {
        private String type;
        private String name;
        private boolean isFinal;
        
        public VariableMetadata(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    private List<VariableMetadata> localVariables = new ArrayList<>();
    private List<List<String>> parameterAnnotations = new ArrayList<>();
    private List<String> returnTypeAnnotations = new ArrayList<>();
    
    // For deeper analysis - not serialized to JSON
    @JsonIgnore
    private transient Object resolvedMethodNode;
    
    // SQL-related fields - being phased out in favor of daoOperations
    @Deprecated
    private List<String> sqlQueries = new ArrayList<>();
    
    @Deprecated
    private List<String> sqlTables = new ArrayList<>();
    
    @Deprecated
    private List<String> sqlOperations = new ArrayList<>();

    // For REST endpoints
    private String httpMethod;
    private String path;
    private String produces;
    private String consumes;
    
    // For metrics
    private int complexity;
    private int linesOfCode;
    
    // For Spring MVC endpoint analysis
    private boolean isRequestMapping;
    
    // For method call analysis
    private List<String> methodCalls = new ArrayList<>();
    private List<String> fieldReferences = new ArrayList<>();
    
    // Local variable declarations (type and name as "Type name")
    private List<String> localVariablesList;

    public void addParameter(ParameterMetadata parameter) {
        parameters.add(parameter);
    }
    
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }
    
    public void addException(String exception) {
        exceptionsThrown.add(exception);
    }
    
    public void addCalledMethod(String methodName) {
        calledMethods.add(methodName);
    }
    
    public void addLocalVariable(VariableMetadata variable) {
        localVariables.add(variable);
    }
    
    public void addMethodCall(String methodCall) {
        methodCalls.add(methodCall);
    }
    
    public void addFieldReference(String fieldRef) {
        fieldReferences.add(fieldRef);
    }
}
