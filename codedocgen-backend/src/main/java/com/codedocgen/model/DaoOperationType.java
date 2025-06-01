package com.codedocgen.model;

/**
 * Enum representing different types of DAO operations
 */
public enum DaoOperationType {
    QUERY,
    SAVE,
    UPDATE,
    DELETE,
    BULK_INSERT,
    BULK_UPDATE,
    BULK_DELETE,
    NATIVE_QUERY,
    JPQL_QUERY,
    CRITERIA_QUERY,
    STORED_PROCEDURE,
    FUNCTION_CALL,
    BATCH_OPERATION,
    TRANSACTION,
    FIND_BY_ID,
    FIND_ALL,
    COUNT,
    EXISTS,
    CUSTOM,
    UNKNOWN
} 