package com.codedocgen.controller;

import com.codedocgen.dto.ParsedDataResponse;
import com.codedocgen.dto.RepoRequest;
import com.codedocgen.service.*;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;

// Import OpenAPI model and ObjectMapper
// import io.swagger.v3.oas.models.OpenAPI; // No longer injecting OpenAPI directly
// import org.springdoc.core.service.OpenAPIService; // No longer using this service directly
import com.fasterxml.jackson.databind.ObjectMapper; // Keep for potential future use, though maybe not strictly needed here
import com.fasterxml.jackson.core.JsonProcessingException;

// Imports for RestTemplate and dynamic URL construction
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import com.codedocgen.parser.CallFlowAnalyzer;
import com.codedocgen.service.DaoAnalysisService;
import com.codedocgen.model.DbAnalysisResult;
import com.codedocgen.model.LogStatement;
import com.codedocgen.service.LoggerInsightsService;
import com.codedocgen.model.PiiPciFinding;
import com.codedocgen.service.PiiPciDetectionService;
import com.codedocgen.service.YamlParserService;

// For FileUtils.deleteDirectory
import org.apache.commons.io.FileUtils;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    private final GitService gitService;
    private final JavaParserService javaParserService;
    private final ProjectDetectorService projectDetectorService;
    private final EndpointExtractorService endpointExtractorService;
    private final DiagramService diagramService;
    private final DocumentationService documentationService;
    // private final OpenAPIService openApiService; // Removed
    private final ObjectMapper objectMapper; 
    private final RestTemplate restTemplate; // Inject RestTemplate
    private final CallFlowAnalyzer callFlowAnalyzer;
    private final DaoAnalysisService daoAnalysisService; // Add DaoAnalysisService
    private final LoggerInsightsService loggerInsightsService; // Added LoggerInsightsService
    private final YamlParserService yamlParserService; // Added YamlParserService
    private final PiiPciDetectionService piiPciDetectionService; // Added PiiPciDetectionService

    @Value("${app.repoStoragePath:/tmp/codedocgen_repos}")
    private String repoStoragePath;

    @Value("${app.outputBasePath:/tmp/codedocgen_output}")
    private String outputBasePath;    

    @Autowired
    public AnalysisController(GitService gitService, 
                              JavaParserService javaParserService,
                              ProjectDetectorService projectDetectorService,
                              EndpointExtractorService endpointExtractorService,
                              DiagramService diagramService,
                              DocumentationService documentationService,
                              // OpenAPIService openApiService, // Removed
                              ObjectMapper objectMapper,
                              RestTemplate restTemplate,
                              CallFlowAnalyzer callFlowAnalyzer,
                              DaoAnalysisService daoAnalysisService, // Add DaoAnalysisService
                              LoggerInsightsService loggerInsightsService, // Added LoggerInsightsService
                              YamlParserService yamlParserService, // Added YamlParserService
                              PiiPciDetectionService piiPciDetectionService) { // Added PiiPciDetectionService
        this.gitService = gitService;
        this.javaParserService = javaParserService;
        this.projectDetectorService = projectDetectorService;
        this.endpointExtractorService = endpointExtractorService;
        this.diagramService = diagramService;
        this.documentationService = documentationService;
        // this.openApiService = openApiService; // Removed
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate; // Initialize RestTemplate
        this.callFlowAnalyzer = callFlowAnalyzer;
        this.daoAnalysisService = daoAnalysisService; // Initialize DaoAnalysisService
        this.loggerInsightsService = loggerInsightsService; // Initialize LoggerInsightsService
        this.yamlParserService = yamlParserService; // Added YamlParserService
        this.piiPciDetectionService = piiPciDetectionService; // Added PiiPciDetectionService
    }

    @PostMapping("/analyze")
    public ResponseEntity<ParsedDataResponse> analyzeRepository(@RequestBody RepoRequest repoRequest) {
        logger.info("Received request to /api/analysis/analyze with repoUrl: {}", repoRequest != null ? repoRequest.getRepoUrl() : "null RepoRequest");

        String repoUrl = null;
        if (repoRequest != null && repoRequest.getRepoUrl() != null) {
            repoUrl = repoRequest.getRepoUrl().trim();
        }

        if (repoUrl == null || repoUrl.isEmpty()) {
            logger.warn("/api/analysis/analyze: repoUrl is null or empty.");
            return ResponseEntity.badRequest().body(null);
        }
        logger.info("/api/analysis/analyze: Processing repoUrl: {}", repoUrl);

        String extractedProjectName = "unknown_project";
        try {
            extractedProjectName = extractProjectNameFromUrl(repoUrl);
        } catch (URISyntaxException e) {
            logger.warn("Invalid repository URL syntax, using default name: {}", repoUrl, e);
            extractedProjectName = "default_project"; // Fallback project name
        }

        String uniqueRepoId = UUID.randomUUID().toString().substring(0, 8);
        File localRepoPath = new File(repoStoragePath, "repo_" + uniqueRepoId);
        File outputDir = new File(outputBasePath, "docs_" + uniqueRepoId);

        // Clean up the general outputBasePath directory BEFORE creating a new unique outputDir
        // This will remove all previously generated docs_* directories
        File baseOutputDir = new File(outputBasePath);
        if (baseOutputDir.exists()) {
            File[] existingOutputDirs = baseOutputDir.listFiles(pathname -> pathname.isDirectory() && pathname.getName().startsWith("docs_"));
            if (existingOutputDirs != null) {
                for (File dir : existingOutputDirs) {
                    logger.info("Deleting old output directory: {}", dir.getAbsolutePath());
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        logger.warn("Could not delete old output directory {}: {}", dir.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        }

        if (!outputDir.mkdirs()) {
            logger.error("Could not create output directory: {}. Check permissions and path.", outputDir.getAbsolutePath());
            ParsedDataResponse errorResponse = new ParsedDataResponse();
            errorResponse.setErrorMessage("Failed to create output directory: " + outputDir.getAbsolutePath());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); 
        }
        logger.info("/api/analysis/analyze: Output directory set to: {}", outputDir.getAbsolutePath());

        try {
            logger.info("/api/analysis/analyze: Starting analysis for {}", repoUrl);
            logger.info("Attempting to clone repository: {}", repoUrl);
            gitService.cloneRepository(repoUrl, localRepoPath.getAbsolutePath());
            logger.info("Repository cloned to: {}", localRepoPath.getAbsolutePath());

            ParsedDataResponse response = new ParsedDataResponse();
            response.setProjectName(extractedProjectName);

            // 1. Detect Project Type & Spring Boot info
            response.setProjectType(projectDetectorService.detectBuildTool(localRepoPath));
            response.setSpringBootProject(projectDetectorService.isSpringBootProject(localRepoPath));
            if (response.isSpringBootProject()) {
                response.setSpringBootVersion(projectDetectorService.detectSpringBootVersion(localRepoPath));
            }

            // 2. Parse Java Code
            List<String> parseWarnings = new java.util.ArrayList<>();
            List<ClassMetadata> classMetadataList = javaParserService.parseProject(localRepoPath, parseWarnings);
            response.setClasses(classMetadataList);
            response.setParseWarnings(parseWarnings);

            // 3. Extract Endpoints
            List<EndpointMetadata> endpointMetadataList = endpointExtractorService.extractEndpoints(classMetadataList, localRepoPath);
            response.setEndpoints(endpointMetadataList);
            
            // 4. Generate Diagrams
            File diagramsSubDir = new File(outputDir, "diagrams");
            if (!diagramsSubDir.exists() && !diagramsSubDir.mkdirs()){
                logger.warn("Could not create specific diagrams output directory: {}", diagramsSubDir.getAbsolutePath());
            } else {
                 Map<com.codedocgen.model.DiagramType, String> absoluteDiagramPaths = diagramService.generateDiagrams(classMetadataList, diagramsSubDir.getAbsolutePath());
                 Map<com.codedocgen.model.DiagramType, String> relativeDiagramPaths = new java.util.HashMap<>();
                 if (absoluteDiagramPaths != null) {
                    String pathPrefixToTrim = new File(outputBasePath).getAbsolutePath();
                    for (Map.Entry<com.codedocgen.model.DiagramType, String> entry : absoluteDiagramPaths.entrySet()) {
                        String absolutePath = entry.getValue();
                        String relativePath = absolutePath.replace(pathPrefixToTrim, "").replace("\\", "/");
                        if (relativePath.startsWith("/")) {
                            relativePath = relativePath.substring(1);
                        }
                        relativeDiagramPaths.put(entry.getKey(), "/generated-output/" + relativePath);
                    }
                 }
                 response.setDiagrams(relativeDiagramPaths);
            }

            // 5. Find Feature Files
            response.setFeatureFiles(documentationService.findAndReadFeatureFiles(localRepoPath));

            // 5.1 Find and Read WSDL Files
            response.setWsdlFilesContent(documentationService.findAndReadWsdlFiles(localRepoPath));

            // 5.2 Find and Read XSD Files
            response.setXsdFilesContent(documentationService.findAndReadXsdFiles(localRepoPath));

            // 6. Generate OpenAPI Spec
            try {
                // Always generate OpenAPI spec from parsed endpoints for the analyzed repo
                String openApiJson = documentationService.generateOpenApiSpecFromEndpoints(endpointMetadataList, extractedProjectName);
                response.setOpenApiSpec(openApiJson);
                logger.debug("Generated OpenAPI spec from analyzed endpoints.");
            } catch (Exception e) {
                logger.error("Error generating OpenAPI spec from endpoints: {}", e.getMessage(), e);
                response.setOpenApiSpec("Error generating OpenAPI spec: " + e.getMessage());
            }

            // 7. Generate Project Summary
            response.setProjectSummary(documentationService.generateProjectSummary(response));
            
            // 8. Generate Call Flows (for controllers and SOAP endpoints)
            logger.info("Calling CallFlowAnalyzer.getEntrypointCallFlows with {} classes", classMetadataList != null ? classMetadataList.size() : 0);
            Map<String, List<String>> callFlows = callFlowAnalyzer.getEntrypointCallFlows(classMetadataList);
            logger.info("CallFlowAnalyzer returned {} call flows", callFlows != null ? callFlows.size() : 0);
            response.setCallFlows(callFlows);

            // Generate a sequence diagram for each entrypoint call flow
            Map<String, String> sequenceDiagrams = new java.util.HashMap<>();
            if (callFlows != null && !callFlows.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : callFlows.entrySet()) {
                    String entrypointFqn = entry.getKey();
                    List<String> flow = entry.getValue();
                if (flow != null && !flow.isEmpty()) {
                        String seqDiagramAbs = diagramService.generateSequenceDiagram(flow, diagramsSubDir.getAbsolutePath(), "sequence_diagram_" + sanitizeFileName(entrypointFqn));
                    if (seqDiagramAbs != null) {
                        String pathPrefixToTrim = new File(outputBasePath).getAbsolutePath();
                        String relativePath = seqDiagramAbs.replace(pathPrefixToTrim, "").replace("\\", "/");
                        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
                            sequenceDiagrams.put(entrypointFqn, "/generated-output/" + relativePath);
                        }
                    }
                }
                response.setSequenceDiagrams(sequenceDiagrams);
            }

            // 9. Analyze DAO operations
            logger.info("Analyzing DAO/Repository classes for database operations");
            try {
                // Call the updated service method which returns DbAnalysisResult
                DbAnalysisResult dbAnalysisResult = daoAnalysisService.analyzeDbOperations(classMetadataList, localRepoPath);
                
                // Set the new composite object in the response
                response.setDbAnalysis(dbAnalysisResult);

                // For backward compatibility or if still needed directly, also set the old daoOperations field
                // and dbDiagramPath (though dbDiagramPath might be better handled via dbAnalysisResult.getOperationsByClass() if it affects diagram generation)
                if (dbAnalysisResult != null) {
                    response.setDaoOperations(dbAnalysisResult.getOperationsByClass()); 
                    logger.info("Found database operations in {} DAO/Repository classes", 
                                dbAnalysisResult.getOperationsByClass() != null ? dbAnalysisResult.getOperationsByClass().size() : 0);

                    // 10. Generate database schema diagram if DAO operations were found
                    if (dbAnalysisResult.getOperationsByClass() != null && !dbAnalysisResult.getOperationsByClass().isEmpty()) {
                        try {
                            logger.info("Generating database schema diagram");
                            // Pass the operationsByClass part to generateDbDiagram
                            String dbDiagramAbsPath = daoAnalysisService.generateDbDiagram(classMetadataList, dbAnalysisResult.getOperationsByClass(), diagramsSubDir.getAbsolutePath());
                            if (dbDiagramAbsPath != null) {
                                String pathPrefixToTrim = new File(outputBasePath).getAbsolutePath();
                                String relativePath = dbDiagramAbsPath.replace(pathPrefixToTrim, "").replace("\\", "/");
                                if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
                                response.setDbDiagramPath("/generated-output/" + relativePath); 
                                // Also update the main diagrams map if DATABASE_DIAGRAM is a key it uses
                                if (response.getDiagrams() != null) { // Ensure diagrams map exists
                                   response.getDiagrams().put(com.codedocgen.model.DiagramType.DATABASE_DIAGRAM, "/generated-output/" + relativePath);
                                } else {
                                   Map<com.codedocgen.model.DiagramType, String> diagramsMap = new java.util.HashMap<>();
                                   diagramsMap.put(com.codedocgen.model.DiagramType.DATABASE_DIAGRAM, "/generated-output/" + relativePath);
                                   response.setDiagrams(diagramsMap);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error generating database diagram: {}", e.getMessage(), e);
                            response.setDbDiagramPath("Error generating diagram: " + e.getMessage());
                        }
                    } else {
                        logger.info("No DAO operations found, skipping database schema diagram generation.");
                    }
                } else {
                    logger.info("No database analysis result returned.");
                }
            } catch (Exception e) {
                logger.error("Error during DAO analysis or DB diagram generation: {}", e.getMessage(), e);
                // Optionally set an error message in the response for these parts
            }

            // 11. Get Logger Insights
            logger.info("Attempting to get logger insights from: {}", localRepoPath.getAbsolutePath());
            try {
                // Determine the path for log analysis (e.g., src/main/java or root)
                 Path potentialSrcPath = localRepoPath.toPath().resolve("src/main/java");
                 String effectivePathForLogAnalysis = localRepoPath.getAbsolutePath();
                 if (Files.exists(potentialSrcPath) && Files.isDirectory(potentialSrcPath)){
                     effectivePathForLogAnalysis = potentialSrcPath.toString();
                     logger.info("Using src/main/java as source for log analysis: {}", effectivePathForLogAnalysis);
                 } else {
                      logger.info("src/main/java not found, analyzing entire repository for logs: {}", effectivePathForLogAnalysis);
                 }
                List<LogStatement> logStatements = loggerInsightsService.getLogInsights(effectivePathForLogAnalysis);
                response.setLogStatements(logStatements);
                logger.info("Successfully retrieved {} log statements.", logStatements.size());
            } catch (Exception e) {
                logger.error("Error fetching logger insights: {}", e.getMessage(), e);
                // Optionally add a warning or error to the response about log insights failing
                 response.setLogStatements(new ArrayList<>()); // Set empty list on error
            }

            // PCI/PII Scan
            // Convert File to Path before calling the service
            List<PiiPciFinding> piiPciFindings = piiPciDetectionService.scanRepository(localRepoPath.toPath(), null); 
            response.setPiiPciFindings(piiPciFindings);

            // 12. Finalize and return response
            logger.info("Completed analysis for repository: {}", repoUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("!!! UNHANDLED EXCEPTION in /api/analysis/analyze for URL {} !!!: Type: {}, Message: {}", repoUrl, e.getClass().getName(), e.getMessage(), e);
            ParsedDataResponse errorResponse = new ParsedDataResponse();
            errorResponse.setErrorMessage("Internal server error during analysis: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            logger.info("/api/analysis/analyze: Starting finally block for repoUrl: {}", repoUrl != null ? repoUrl : "UNKNOWN_URL");
            try {
                if (localRepoPath.exists()) { // Check if directory was created before attempting to delete
                    logger.info("Deleting cloned repository at: {}", localRepoPath.getAbsolutePath());
                    gitService.deleteRepository(localRepoPath);
                    logger.info("Successfully deleted cloned repository: {}", localRepoPath.getAbsolutePath());
                } else {
                    logger.info("Cloned repository path {} did not exist, skipping deletion.", localRepoPath.getAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("Error deleting repository directory {} in finally block: {}", localRepoPath.getAbsolutePath(), e.getMessage(), e);
            }
            // Optionally, also delete the outputDir if it's temporary and not meant to be served directly
            // This was already present and commented out, leaving as is.
            // try {
            //     logger.info("Deleting output directory: {}", outputDir.getAbsolutePath());
            //     FileUtils.deleteDirectory(outputDir);
            // } catch (IOException e) {
            //     logger.error("Error deleting output directory {}: {}", outputDir.getAbsolutePath(), e.getMessage(), e);
            // }
            logger.info("/api/analysis/analyze: Finished finally block for repoUrl: {}", repoUrl != null ? repoUrl : "UNKNOWN_URL");
        }
    }
    
    private String extractProjectNameFromUrl(String repoUrl) throws URISyntaxException {
        URI uri = new URI(repoUrl);
        String path = uri.getPath();
        String projectName = path.substring(path.lastIndexOf('/') + 1);
        if (projectName.endsWith(".git")) {
            projectName = projectName.substring(0, projectName.length() - 4);
        }
        if (projectName.isEmpty()) {
            // Handle cases like https://github.com/ (if that's even valid for cloning)
            // or if somehow the name is empty after stripping .git
            return "unknown_project";
        }
        return projectName;
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    // Placeholder for OpenAPI generation - this is a complex task
    // private String generateOpenApiSpec(File projectDir, List<ClassMetadata> classes, List<EndpointMetadata> endpoints) {
    //     // This would involve using a library like Springdoc or Swagger Core to generate the spec
    //     // based on annotations and existing configurations (if any).
    //     // For Spring Boot projects, if springdoc-openapi-starter-webmvc is on classpath, it might be auto-generated.
    //     return "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Generated API\", \"version\": \"1.0.0\"}, \"paths\": {}}";
    // }
} 