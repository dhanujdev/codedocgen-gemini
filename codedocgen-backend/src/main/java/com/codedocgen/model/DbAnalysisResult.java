package com.codedocgen.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DbAnalysisResult {
    private final Map<String, List<DaoOperationDetail>> operationsByClass;
    private final Map<String, Set<String>> classesByEntity;

    public DbAnalysisResult(Map<String, List<DaoOperationDetail>> operationsByClass, Map<String, Set<String>> classesByEntity) {
        this.operationsByClass = operationsByClass;
        this.classesByEntity = classesByEntity;
    }

    public Map<String, List<DaoOperationDetail>> getOperationsByClass() {
        return operationsByClass;
    }

    public Map<String, Set<String>> getClassesByEntity() {
        return classesByEntity;
    }

    // Consider adding isEmpty() or other utility methods if needed
} 