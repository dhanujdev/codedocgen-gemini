package com.codedocgen.model;

import lombok.Data;
import java.util.List;
import java.util.Map;
import com.codedocgen.model.DaoOperationDetail;

@Data
public class MethodMetadata {
    private String name;
    private String returnType;
    private List<String> parameters; // Parameter type and name
    private List<String> annotations;
    private List<String> exceptionsThrown;
    private String visibility; // public, private, protected, default
    private boolean isStatic;
    private boolean isAbstract;
    private String packageName;
    private String className;
    // For call flow: 
    private List<String> calledMethods; // List of fully qualified method names or simplified representation
    private List<String> externalCalls; // Map<String, String> in spec, simplified to List<String> for now
    private List<DaoOperationDetail> daoOperations; // Added for DAO analysis, non-final for Lombok setter
    // Local variable declarations (type and name as 'Type name')
    private List<String> localVariables;
    private List<List<String>> parameterAnnotations; // Annotations for each parameter, in order
    private List<String> returnTypeAnnotations; // Annotations for the return type
    
    // For deeper analysis of bean methods, store the original JavaParser node
    @com.fasterxml.jackson.annotation.JsonIgnore // Prevent Jackson from trying to serialize this complex AST node
    private transient com.github.javaparser.ast.body.MethodDeclaration resolvedMethodNode;
    
    // The following fields are likely superseded by daoOperations. Review for removal.
    /** @deprecated Replaced by {@link #daoOperations} */
    @Deprecated
    private List<String> sqlQueries; 
    /** @deprecated Replaced by {@link #daoOperations} */
    @Deprecated
    private List<String> sqlTables; 
    /** @deprecated Replaced by {@link #daoOperations} */
    @Deprecated
    private List<String> sqlOperations; 

    // No explicit constructor or getters/setters for daoOperations needed here.
    // Lombok's @Data will generate them.
    // Existing constructor (if any, or default) will be used by JavaParserServiceImpl,
    // and then setDaoOperations() will be called.
} 