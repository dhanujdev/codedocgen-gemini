package com.codedocgen.model;

import java.util.List;
import java.util.Objects;
import java.util.Collections;
import lombok.NoArgsConstructor;
import lombok.Data;

/**
 * Detailed information about a DAO (Data Access Object) operation
 */
@Data
@NoArgsConstructor
public class DaoOperationDetail {

    private String methodName;
    private String sqlQuery;
    private DaoOperationType operationType;
    private List<String> tables;

    public DaoOperationDetail(String sqlQuery, DaoOperationType operationType, List<String> tables) {
        this(null, sqlQuery, operationType, tables);
    }
    
    public DaoOperationDetail(String methodName, String sqlQuery, DaoOperationType operationType, List<String> tables) {
        this.methodName = methodName;
        this.sqlQuery = sqlQuery;
        this.operationType = operationType;
        this.tables = tables;
    }
    
    // Convenience constructor for single table operations
    public DaoOperationDetail(String methodName, String sqlQuery, DaoOperationType operationType, String table) {
        this(methodName, sqlQuery, operationType, Collections.singletonList(table));
    }

    /**
     * Gets the method name
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the method name
     * @param methodName the method name to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Gets the SQL query
     * @return the SQL query
     */
    public String getSqlQuery() {
        return sqlQuery;
    }

    /**
     * Sets the SQL query
     * @param sqlQuery the SQL query to set
     */
    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    /**
     * Gets the operation type
     * @return the operation type
     */
    public DaoOperationType getOperationType() {
        return operationType;
    }

    /**
     * Sets the operation type
     * @param operationType the operation type to set
     */
    public void setOperationType(DaoOperationType operationType) {
        this.operationType = operationType;
    }

    /**
     * Gets the tables
     * @return the tables
     */
    public List<String> getTables() {
        return tables;
    }

    /**
     * Sets the tables
     * @param tables the tables to set
     */
    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    /**
     * Inner enum for SQL operation types for backward compatibility
     */
    public enum SqlOperationType {
        SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, TRUNCATE, MERGE, UNKNOWN
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