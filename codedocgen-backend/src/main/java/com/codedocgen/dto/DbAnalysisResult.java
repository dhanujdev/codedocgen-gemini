package com.codedocgen.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for database analysis results
 */
@Data
@NoArgsConstructor
public class DbAnalysisResult {
    private List<TableInfo> tables = new ArrayList<>();
    private Map<String, List<String>> relationships = new HashMap<>();
    private String erDiagram;
    private List<String> entitiesByTable = new ArrayList<>();
    private Map<String, List<String>> tableOperations = new HashMap<>();
    private List<String> warnings = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    public static class TableInfo {
        private String name;
        private List<ColumnInfo> columns = new ArrayList<>();
        private List<String> primaryKeys = new ArrayList<>();
        private List<String> foreignKeys = new ArrayList<>();
        private List<String> indices = new ArrayList<>();
    }
    
    @Data
    @NoArgsConstructor
    public static class ColumnInfo {
        private String name;
        private String type;
        private boolean nullable;
        private boolean primaryKey;
        private boolean foreignKey;
        private String referencedTable;
        private String referencedColumn;
    }
} 