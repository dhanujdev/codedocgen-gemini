package com.codedocgen.model;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.codedocgen.model.DaoOperationDetail;

/**
 * Metadata for a Java method
 */
@Data
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
    public static class VariableMetadata {
        private String type;
        private String name;
        
        public VariableMetadata(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public String getName() {
            return name;
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
    private List<Object> sqlQueries = new ArrayList<>();
    
    @Deprecated
    private List<Object> sqlTables = new ArrayList<>();
    
    @Deprecated
    private List<Object> sqlOperations = new ArrayList<>();
    
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
    
    // Local variable declarations (type and name as 'Type name')
    private List<String> localVariablesList;
    
    // No explicit constructor or getters/setters for daoOperations needed here.
    // Lombok's @Data will generate them.
    // Existing constructor (if any, or default) will be used by JavaParserServiceImpl,
    // and then setDaoOperations() will be called.

    /**
     * Add a parameter to this method
     * @param parameter The parameter to add
     */
    public void addParameter(ParameterMetadata parameter) {
        parameters.add(parameter);
    }
    
    /**
     * Add an annotation to this method
     * @param annotation The annotation to add
     */
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }
    
    /**
     * Add an exception to this method
     * @param exception The exception to add
     */
    public void addException(String exception) {
        exceptionsThrown.add(exception);
    }
    
    /**
     * Add a called method to this method
     * @param methodName The method name to add
     */
    public void addCalledMethod(String methodName) {
        calledMethods.add(methodName);
    }
    
    /**
     * Add a local variable to this method
     * @param variable The variable to add
     */
    public void addLocalVariable(VariableMetadata variable) {
        localVariables.add(variable);
    }
    
    /**
     * Add a used field to this method
     * @param fieldName The field name to add
     */
    public void addUsedField(String fieldName) {
        // Implementation needed
    }
    
    public void addMethodCall(String methodCall) {
        methodCalls.add(methodCall);
    }
    
    public void addFieldReference(String fieldRef) {
        fieldReferences.add(fieldRef);
    }

    /**
     * Get the name of this method
     * @return The method name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this method
     * @param name The method name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the return type of this method
     * @return The return type
     */
    public String getReturnType() {
        return returnType;
    }
    
    /**
     * Set the return type of this method
     * @param returnType The return type
     */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    
    /**
     * Get the parameters of this method
     * @return The list of parameters
     */
    public List<ParameterMetadata> getParameters() {
        return parameters;
    }
    
    /**
     * Set the parameters of this method
     * @param parameters The list of parameters
     */
    public void setParameters(List<ParameterMetadata> parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Get the class name of this method
     * @return The class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Set the class name of this method
     * @param className The class name
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
    /**
     * Get the package name of this method
     * @return The package name
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name of this method
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Get the called methods of this method
     * @return The list of called methods
     */
    public List<String> getCalledMethods() {
        return calledMethods;
    }
    
    /**
     * Set the called methods of this method
     * @param calledMethods The list of called methods
     */
    public void setCalledMethods(List<String> calledMethods) {
        this.calledMethods = calledMethods;
    }
    
    /**
     * Get the local variables of this method
     * @return The list of local variables
     */
    public List<VariableMetadata> getLocalVariables() {
        return localVariables;
    }
    
    /**
     * Set the local variables of this method
     * @param localVariables The list of local variables
     */
    public void setLocalVariables(List<VariableMetadata> localVariables) {
        this.localVariables = localVariables;
    }
    
    /**
     * Get the visibility of this method
     * @return The visibility
     */
    public String getVisibility() {
        return visibility;
    }
    
    /**
     * Set the visibility of this method
     * @param visibility The visibility
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
    
    /**
     * Check if this method is static
     * @return true if static, false otherwise
     */
    public boolean isStatic() {
        return isStatic;
    }
    
    /**
     * Set whether this method is static
     * @param isStatic true if static, false otherwise
     */
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }
    
    /**
     * Check if this method is abstract
     * @return true if abstract, false otherwise
     */
    public boolean isAbstract() {
        return isAbstract;
    }
    
    /**
     * Set whether this method is abstract
     * @param isAbstract true if abstract, false otherwise
     */
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }
    
    /**
     * Get the annotations of this method
     * @return The list of annotations
     */
    public List<String> getAnnotations() {
        return annotations;
    }
    
    /**
     * Set the annotations of this method
     * @param annotations The list of annotations
     */
    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
    
    /**
     * Get the parameter annotations of this method
     * @return The list of parameter annotations
     */
    public List<List<String>> getParameterAnnotations() {
        return parameterAnnotations;
    }
    
    /**
     * Set the parameter annotations of this method
     * @param parameterAnnotations The list of parameter annotations
     */
    public void setParameterAnnotations(List<List<String>> parameterAnnotations) {
        this.parameterAnnotations = parameterAnnotations;
    }
    
    /**
     * Get the DAO operations of this method
     * @return The list of DAO operations
     */
    public List<DaoOperationDetail> getDaoOperations() {
        return daoOperations;
    }
    
    /**
     * Set the DAO operations of this method
     * @param daoOperations The list of DAO operations
     */
    public void setDaoOperations(List<DaoOperationDetail> daoOperations) {
        this.daoOperations = daoOperations;
    }
    
    /**
     * Get the SQL queries of this method
     * @return The list of SQL queries
     */
    @Deprecated
    public List<Object> getSqlQueries() {
        return sqlQueries;
    }
    
    /**
     * Set the SQL queries of this method
     * @param sqlQueries The list of SQL queries
     */
    @Deprecated
    public void setSqlQueries(List<Object> sqlQueries) {
        this.sqlQueries = sqlQueries;
    }
    
    /**
     * Get the SQL tables of this method
     * @return The list of SQL tables
     */
    @Deprecated
    public List<Object> getSqlTables() {
        return sqlTables;
    }
    
    /**
     * Set the SQL tables of this method
     * @param sqlTables The list of SQL tables
     */
    @Deprecated
    public void setSqlTables(List<Object> sqlTables) {
        this.sqlTables = sqlTables;
    }
    
    /**
     * Get the SQL operations of this method
     * @return The list of SQL operations
     */
    @Deprecated
    public List<Object> getSqlOperations() {
        return sqlOperations;
    }
    
    /**
     * Set the SQL operations of this method
     * @param sqlOperations The list of SQL operations
     */
    @Deprecated
    public void setSqlOperations(List<Object> sqlOperations) {
        this.sqlOperations = sqlOperations;
    }
    
    /**
     * Get the exceptions thrown by this method
     * @return The list of exceptions
     */
    public List<String> getExceptionsThrown() {
        return exceptionsThrown;
    }
    
    /**
     * Set the exceptions thrown by this method
     * @param exceptions The list of exceptions
     */
    public void setExceptionsThrown(List<String> exceptions) {
        this.exceptionsThrown = exceptions;
    }
    
    /**
     * Get the external calls of this method
     * @return The list of external calls
     */
    public List<String> getExternalCalls() {
        return externalCalls;
    }
    
    /**
     * Set the external calls of this method
     * @param externalCalls The list of external calls
     */
    public void setExternalCalls(List<String> externalCalls) {
        this.externalCalls = externalCalls;
    }
} 