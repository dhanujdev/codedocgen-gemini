package com.codedocgen.dto;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DiagramType;
import com.codedocgen.model.EndpointMetadata;
import com.codedocgen.model.DaoOperationDetail;
import com.codedocgen.model.DbAnalysisResult;
import com.codedocgen.model.LogStatement;
import com.codedocgen.model.PiiPciFinding;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for parsed codebase data response
 */
@Data
@NoArgsConstructor
public class ParsedDataResponse {
    // Project information
    private String projectName;
    private String projectType; // e.g., Maven, Gradle
    private boolean isSpringBootProject;
    private String springBootVersion; // if applicable
    private String projectSummary;
    
    // Codebase analysis
    private List<ClassMetadata> classes = new ArrayList<>();
    private List<String> parseWarnings = new ArrayList<>();
    
    // REST API and endpoints
    private List<EndpointMetadata> endpoints = new ArrayList<>();
    private String openApiSpec; // Swagger/OpenAPI JSON or YAML
    
    // SOAP/WSDL
    private Map<String, String> wsdlFilesContent = new HashMap<>();
    private Map<String, String> xsdFilesContent = new HashMap<>();
    
    // Feature files (for BDD)
    private List<String> featureFiles = new ArrayList<>();
    
    // Diagrams
    private Map<DiagramType, String> diagrams = new HashMap<>();
    private Map<String, String> sequenceDiagrams = new HashMap<>();
    private String dbDiagramPath; // Path to database schema diagram
    
    // Call flow analysis
    private Map<String, List<String>> callFlows = new HashMap<>();
    
    // Database analysis
    private DbAnalysisResult dbAnalysis;
    private Map<String, List<DaoOperationDetail>> daoOperations = new HashMap<>();
    
    // Log analysis
    private List<LogStatement> logStatements = new ArrayList<>();
    
    // Security findings
    private List<PiiPciFinding> piiPciFindings = new ArrayList<>();

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

    // Explicit getters and setters
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getProjectType() {
        return projectType;
    }
    
    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }
    
    public boolean isSpringBootProject() {
        return isSpringBootProject;
    }
    
    public void setSpringBootProject(boolean springBootProject) {
        isSpringBootProject = springBootProject;
    }
    
    public String getSpringBootVersion() {
        return springBootVersion;
    }
    
    public void setSpringBootVersion(String springBootVersion) {
        this.springBootVersion = springBootVersion;
    }
    
    public String getProjectSummary() {
        return projectSummary;
    }
    
    public void setProjectSummary(String projectSummary) {
        this.projectSummary = projectSummary;
    }
    
    public List<ClassMetadata> getClasses() {
        return classes;
    }
    
    public void setClasses(List<ClassMetadata> classes) {
        this.classes = classes;
    }
    
    public List<String> getParseWarnings() {
        return parseWarnings;
    }
    
    public void setParseWarnings(List<String> parseWarnings) {
        this.parseWarnings = parseWarnings;
    }
    
    public List<EndpointMetadata> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<EndpointMetadata> endpoints) {
        this.endpoints = endpoints;
    }
    
    public String getOpenApiSpec() {
        return openApiSpec;
    }
    
    public void setOpenApiSpec(String openApiSpec) {
        this.openApiSpec = openApiSpec;
    }
    
    public Map<String, String> getWsdlFilesContent() {
        return wsdlFilesContent;
    }
    
    public void setWsdlFilesContent(Map<String, String> wsdlFilesContent) {
        this.wsdlFilesContent = wsdlFilesContent;
    }
    
    public Map<String, String> getXsdFilesContent() {
        return xsdFilesContent;
    }
    
    public void setXsdFilesContent(Map<String, String> xsdFilesContent) {
        this.xsdFilesContent = xsdFilesContent;
    }
    
    public List<String> getFeatureFiles() {
        return featureFiles;
    }
    
    public void setFeatureFiles(List<String> featureFiles) {
        this.featureFiles = featureFiles;
    }
    
    public Map<DiagramType, String> getDiagrams() {
        return diagrams;
    }
    
    public void setDiagrams(Map<DiagramType, String> diagrams) {
        this.diagrams = diagrams;
    }
    
    public Map<String, String> getSequenceDiagrams() {
        return sequenceDiagrams;
    }
    
    public void setSequenceDiagrams(Map<String, String> sequenceDiagrams) {
        this.sequenceDiagrams = sequenceDiagrams;
    }
    
    public String getDbDiagramPath() {
        return dbDiagramPath;
    }
    
    public void setDbDiagramPath(String dbDiagramPath) {
        this.dbDiagramPath = dbDiagramPath;
    }
    
    public Map<String, List<String>> getCallFlows() {
        return callFlows;
    }
    
    public void setCallFlows(Map<String, List<String>> callFlows) {
        this.callFlows = callFlows;
    }
} 