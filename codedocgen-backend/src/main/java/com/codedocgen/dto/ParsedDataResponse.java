package com.codedocgen.dto;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;
import com.codedocgen.model.DiagramType;
import com.codedocgen.model.DaoOperationDetail;
import com.codedocgen.model.DbAnalysisResult;
import com.codedocgen.model.LogStatement;
import com.codedocgen.model.PiiPciFinding;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ParsedDataResponse {
    private String projectName;
    private String projectType; // e.g., Maven, Gradle
    private String springBootVersion; // if applicable
    private boolean isSpringBootProject;
    private List<ClassMetadata> classes;
    private List<EndpointMetadata> endpoints;
    private Map<DiagramType, String> diagrams; // DiagramType -> PlantUML String or image path
    private String projectSummary;
    private String openApiSpec; // Swagger/OpenAPI JSON or YAML
    private List<String> featureFiles; // Gherkin feature file contents or paths
    private Map<String, String> wsdlFilesContent; // WSDL file path/name -> WSDL XML content
    private Map<String, String> xsdFilesContent; // XSD file path/name -> XSD XML content
    private List<String> parseWarnings; // List of files that failed to parse
    private Map<String, List<String>> callFlows; // Entrypoint FQN -> call flow list
    private Map<String, String> sequenceDiagrams; // Entrypoint FQN -> sequence diagram URL
    private Map<String, List<DaoOperationDetail>> daoOperations; // DAO class FQN -> list of database operations
    private String dbDiagramPath; // Path to database schema diagram
    private DbAnalysisResult dbAnalysis; // New field for the comprehensive DB analysis
    private List<LogStatement> logStatements; // Added field for log statements
    private List<PiiPciFinding> piiPciFindings; // New field

    // Consider adding fields for call flows, DAO info, etc., as parsing capabilities are built.

    // Getter and Setter for daoOperations (existing)
    public Map<String, List<DaoOperationDetail>> getDaoOperations() {
        return daoOperations;
    }

    public void setDaoOperations(Map<String, List<DaoOperationDetail>> daoOperations) {
        this.daoOperations = daoOperations;
    }

    // Getter and Setter for dbAnalysis (new)
    public DbAnalysisResult getDbAnalysis() {
        return dbAnalysis;
    }

    public void setDbAnalysis(DbAnalysisResult dbAnalysis) {
        this.dbAnalysis = dbAnalysis;
    }

    // Getter and Setter for logStatements (new)
    public List<LogStatement> getLogStatements() {
        return logStatements;
    }

    public void setLogStatements(List<LogStatement> logStatements) {
        this.logStatements = logStatements;
    }

    public List<PiiPciFinding> getPiiPciFindings() {
        return piiPciFindings;
    }

    public void setPiiPciFindings(List<PiiPciFinding> piiPciFindings) {
        this.piiPciFindings = piiPciFindings;
    }
} 