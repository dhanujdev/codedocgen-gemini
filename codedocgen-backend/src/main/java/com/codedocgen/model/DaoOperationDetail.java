package com.codedocgen.model;

import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.Arrays;

public class DaoOperationDetail {

    private final String methodName;
    private final String sqlQuery;
    private final SqlOperationType operationType;
    private final List<String> tables;

    public enum SqlOperationType {
        SELECT, INSERT, UPDATE, DELETE, UNKNOWN
    }

    public DaoOperationDetail(String sqlQuery, SqlOperationType operationType, List<String> tables) {
        this(null, sqlQuery, operationType, tables);
    }
    
    public DaoOperationDetail(String methodName, String sqlQuery, SqlOperationType operationType, List<String> tables) {
        this.methodName = methodName;
        this.sqlQuery = sqlQuery;
        this.operationType = operationType;
        this.tables = tables;
    }
    
    // Convenience constructor for single table operations
    public DaoOperationDetail(String methodName, String sqlQuery, SqlOperationType operationType, String table) {
        this(methodName, sqlQuery, operationType, Collections.singletonList(table));
    }

    public String getMethodName() {
        return methodName;
    }
    
    public String getSqlQuery() {
        return sqlQuery;
    }

    public SqlOperationType getOperationType() {
        return operationType;
    }

    public List<String> getTables() {
        return tables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DaoOperationDetail that = (DaoOperationDetail) o;
        return Objects.equals(methodName, that.methodName) &&
               Objects.equals(sqlQuery, that.sqlQuery) &&
               operationType == that.operationType &&
               Objects.equals(tables, that.tables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, sqlQuery, operationType, tables);
    }

    @Override
    public String toString() {
        return "DaoOperationDetail{" +
               "methodName='" + methodName + "'" +
               ", sqlQuery='''" + sqlQuery + "'''" +
               ", operationType=" + operationType +
               ", tables=" + tables +
               '}';
    }
} 