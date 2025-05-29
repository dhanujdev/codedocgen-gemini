package com.codedocgen.parser;

import com.codedocgen.model.DaoOperationDetail;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DaoAnalyzer {

    private static final Set<String> JDBC_METHODS = Set.of(
            "prepareStatement", "createStatement", "execute", "executeQuery", "executeUpdate", "addBatch"
    );

    // Regex to capture table names. This is a simplified version and might need enhancements for complex scenarios.
    // Handles: FROM table, FROM schema.table, JOIN table, JOIN schema.table
    // Also: UPDATE table, UPDATE schema.table, INTO table, INTO schema.table
    // And DELETE FROM table
    // Improved to better handle aliases and clean names.
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "\\b(?:FROM|JOIN|UPDATE|INTO)\\s+([`\\w$.]+(?:\\s+AS\\s+[`\\w$]+)?)|DELETE\\s+FROM\\s+([`\\w$.]+)",
        Pattern.CASE_INSENSITIVE
    );

    public DaoAnalysisResult analyze(MethodDeclaration methodDeclaration) {
        List<DaoOperationDetail> operations = new ArrayList<>();
        String methodName = methodDeclaration.getNameAsString();

        if (methodDeclaration.getBody().isPresent()) {
            // Look for specific JDBC method calls
            methodDeclaration.getBody().get().findAll(MethodCallExpr.class).forEach(methodCall -> {
                String jdbcMethodName = methodCall.getNameAsString();
                if (JDBC_METHODS.contains(jdbcMethodName) && !methodCall.getArguments().isEmpty()) {
                    Expression firstArg = methodCall.getArgument(0);
                    if (firstArg.isStringLiteralExpr()) {
                        String sqlQuery = firstArg.asStringLiteralExpr().getValue();
                        if (isPotentiallySql(sqlQuery)) {
                            addOperationDetail(operations, methodName, sqlQuery);
                        }
                    }
                    // TODO: Handle cases where SQL is in a variable or constructed dynamically
                }
            });

            // Fallback: also check all string literals if no specific JDBC calls found yielding SQL
            // This is less precise but can catch queries not directly in JDBC method calls shown above.
            if (operations.isEmpty()) {
                 methodDeclaration.getBody().get().findAll(StringLiteralExpr.class).forEach(str -> {
                    String value = str.getValue();
                    if (isPotentiallySql(value)) {
                        addOperationDetail(operations, methodName, value);
                    }
                });
            }
        }
        return new DaoAnalysisResult(operations);
    }

    private void addOperationDetail(List<DaoOperationDetail> operations, String methodName, String sqlQuery) {
        DaoOperationDetail.SqlOperationType operationType = extractSqlOperationType(sqlQuery);
        List<String> tables = extractTableNames(sqlQuery);
        // Avoid duplicate entries if the same query string appears multiple times
        DaoOperationDetail newOp = new DaoOperationDetail(methodName, sqlQuery, operationType, tables);
        if (!operations.contains(newOp)) {
            operations.add(newOp);
        }
    }

    private boolean isPotentiallySql(String s) {
        if (s == null || s.trim().isEmpty()) {
            return false;
        }
        String lower = s.trim().toLowerCase();
        // Basic check for SQL keywords, can be expanded
        // Ensure it's not just a comment or log message containing these words.
        // Check for common query structures.
        return (lower.startsWith("select ") && lower.contains(" from ")) ||
               (lower.startsWith("insert into ") && lower.contains(" values ")) ||
               (lower.startsWith("update ") && lower.contains(" set ")) ||
               (lower.startsWith("delete from ") && lower.contains(" where ")); // `where` is common but not strictly necessary for delete all
    }

    public DaoOperationDetail.SqlOperationType extractSqlOperationType(String sql) {
        String trimmedSql = sql.trim().toLowerCase();
        if (trimmedSql.startsWith("select")) return DaoOperationDetail.SqlOperationType.SELECT;
        if (trimmedSql.startsWith("insert")) return DaoOperationDetail.SqlOperationType.INSERT;
        if (trimmedSql.startsWith("update")) return DaoOperationDetail.SqlOperationType.UPDATE;
        if (trimmedSql.startsWith("delete")) return DaoOperationDetail.SqlOperationType.DELETE;
        return DaoOperationDetail.SqlOperationType.UNKNOWN;
    }

    public List<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>(); // Use Set to avoid duplicate table names from the same query
        Matcher m = TABLE_PATTERN.matcher(sql);
        while (m.find()) {
            String tableName = null;
            if (m.group(1) != null) { // FROM, JOIN, UPDATE, INTO clauses
                tableName = m.group(1).trim();
            } else if (m.group(2) != null) { // DELETE FROM clause
                 tableName = m.group(2).trim();
            }

            if (tableName != null) {
                // Remove potential alias (e.g., "table_name AS t" -> "table_name")
                tableName = tableName.split("\\s+AS\\s+")[0].replace("`", "");
                // Remove schema prefix if present
                if (tableName.contains(".")) {
                    tableName = tableName.substring(tableName.lastIndexOf('.') + 1);
                }
                tables.add(tableName);
            }
        }
        // If no tables found by complex regex, try a simpler one for basic names
        // This is for cases like "SELECT * FROM mytable" where 'mytable' might not be caught if regex is too strict
        // or for Spring Data generated queries that only list the table name.
        if (tables.isEmpty()) {
            // Look for any word that could be a table name if it's preceded by typical keywords.
            // This is a heuristic and might need adjustment.
            Pattern simpleTablePattern = Pattern.compile("\\b(?:FROM|JOIN|UPDATE|INTO|TABLE)\\s+([a-zA-Z_][\\w]*)\\b", Pattern.CASE_INSENSITIVE);
            Matcher simpleMatcher = simpleTablePattern.matcher(sql);
            while (simpleMatcher.find()) {
                tables.add(simpleMatcher.group(1));
            }
        }

        return tables.stream().distinct().collect(Collectors.toList());
    }

    public static class DaoAnalysisResult {
        public final List<DaoOperationDetail> operations;

        public DaoAnalysisResult(List<DaoOperationDetail> operations) {
            this.operations = operations != null ? List.copyOf(operations) : List.of();
        }

        public List<DaoOperationDetail> getOperations() {
            return operations;
        }
    }
} 