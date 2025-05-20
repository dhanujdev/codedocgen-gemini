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
    // Ignores tables in subqueries for simplicity for now.
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\b(?:FROM|JOIN|UPDATE|INTO)\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)",
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
        return lower.startsWith("select ") || lower.startsWith("insert ") ||
               lower.startsWith("update ") || lower.startsWith("delete ") ||
               lower.startsWith("with ") || lower.startsWith("create ") ||
               lower.startsWith("alter ") || lower.startsWith("drop ");
    }

    private DaoOperationDetail.SqlOperationType extractSqlOperationType(String sql) {
        String trimmedSql = sql.trim().toLowerCase();
        if (trimmedSql.startsWith("select")) return DaoOperationDetail.SqlOperationType.SELECT;
        if (trimmedSql.startsWith("insert")) return DaoOperationDetail.SqlOperationType.INSERT;
        if (trimmedSql.startsWith("update")) return DaoOperationDetail.SqlOperationType.UPDATE;
        if (trimmedSql.startsWith("delete")) return DaoOperationDetail.SqlOperationType.DELETE;
        return DaoOperationDetail.SqlOperationType.UNKNOWN;
    }

    private List<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>(); // Use Set to avoid duplicate table names from the same query
        Matcher m = TABLE_PATTERN.matcher(sql);
        while (m.find()) {
            tables.add(m.group(1));
        }
        return tables.stream().distinct().collect(Collectors.toList()); // Return distinct list
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