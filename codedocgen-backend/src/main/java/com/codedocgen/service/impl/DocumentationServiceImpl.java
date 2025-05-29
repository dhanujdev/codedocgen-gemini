package com.codedocgen.service.impl;

import com.codedocgen.dto.ParsedDataResponse;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.service.DocumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;
import java.util.HashMap;

@Service
public class DocumentationServiceImpl implements DocumentationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationServiceImpl.class);

    @Override
    public String generateMarkdownDocumentation(ParsedDataResponse parsedData, String outputDir) throws IOException {
        if (parsedData == null) {
            logger.warn("ParsedDataResponse is null, cannot generate Markdown documentation.");
            return null;
        }
        StringBuilder markdownBuilder = new StringBuilder();

        // Project Overview
        markdownBuilder.append("# Project Overview: ").append(parsedData.getProjectName()).append("\n\n");
        markdownBuilder.append("**Project Type:** ").append(parsedData.getProjectType()).append("\n");
        if (parsedData.isSpringBootProject()) {
            markdownBuilder.append("**Spring Boot Version:** ").append(parsedData.getSpringBootVersion()).append("\n");
        }
        markdownBuilder.append("\n## Project Summary\n");
        markdownBuilder.append(parsedData.getProjectSummary()).append("\n\n");

        // Classes
        if (parsedData.getClasses() != null && !parsedData.getClasses().isEmpty()) {
            markdownBuilder.append("## Classes\n\n");
            for (ClassMetadata cmd : parsedData.getClasses()) {
                markdownBuilder.append("### ").append(cmd.getPackageName()).append(".").append(cmd.getName()).append(" (`").append(cmd.getType()).append("`)\n");
                markdownBuilder.append("**File Path:** `").append(cmd.getFilePath()).append("`\n");
                if (cmd.getParentClass() != null) {
                    markdownBuilder.append("**Extends:** `").append(cmd.getParentClass()).append("`\n");
                }
                if (cmd.getInterfaces() != null && !cmd.getInterfaces().isEmpty()) {
                    markdownBuilder.append("**Implements:** ").append(cmd.getInterfaces().stream().map(i -> "`" + i + "`").collect(Collectors.joining(", "))).append("\n");
                }
                if (cmd.getAnnotations() != null && !cmd.getAnnotations().isEmpty()) {
                    markdownBuilder.append("**Annotations:** ").append(cmd.getAnnotations().stream().map(a -> "`" + a + "`").collect(Collectors.joining(", "))).append("\n");
                }
                markdownBuilder.append("#### Fields\n");
                if (cmd.getFields() != null && !cmd.getFields().isEmpty()) {
                    cmd.getFields().forEach(field -> {
                        StringBuilder fieldStr = new StringBuilder();
                        if (field.getVisibility() != null && !field.getVisibility().isEmpty()) {
                            fieldStr.append(field.getVisibility()).append(" ");
                        }
                        if (field.isStatic()) {
                            fieldStr.append("static ");
                        }
                        if (field.isFinal()) {
                            fieldStr.append("final ");
                        }
                        if (field.getType() != null && !field.getType().isEmpty()) {
                            fieldStr.append(field.getType().replace("<", "&lt;").replace(">", "&gt;")).append(" "); // Escape HTML in type
                        }
                        if (field.getName() != null && !field.getName().isEmpty()) {
                            fieldStr.append(field.getName());
                        }
                        // Optionally, add annotations:
                        // if (field.getAnnotations() != null && !field.getAnnotations().isEmpty()) {
                        //     fieldStr.append(" // Annotations: ").append(String.join(", ", field.getAnnotations()));
                        // }
                        markdownBuilder.append("- `").append(fieldStr.toString().trim()).append("`\n");
                    });
                } else {
                    markdownBuilder.append("- None\n");
                }
                markdownBuilder.append("#### Methods\n");
                if (cmd.getMethods() != null && !cmd.getMethods().isEmpty()) {
                    for (MethodMetadata method : cmd.getMethods()) {
                        markdownBuilder.append("- **`").append(method.getVisibility()).append(" ")
                                .append(method.isStatic() ? "static " : "")
                                .append(method.isAbstract() ? "abstract " : "")
                                .append(method.getReturnType()).append(" ")
                                .append(method.getName()).append("(")
                                .append(String.join(", ", method.getParameters()))
                                .append(")`**");
                        if (method.getAnnotations() != null && !method.getAnnotations().isEmpty()) {
                            markdownBuilder.append("\n  - Annotations: ").append(method.getAnnotations().stream().map(a -> "`" + a + "`").collect(Collectors.joining(", "))).append("\n");
                        }
                        if (method.getExceptionsThrown() != null && !method.getExceptionsThrown().isEmpty()) {
                            markdownBuilder.append("  - Throws: ").append(method.getExceptionsThrown().stream().map(e -> "`" + e + "`").collect(Collectors.joining(", "))).append("\n");
                        }
                        // TODO: Add called methods / external calls if that data is available
                        markdownBuilder.append("\n"); 
                    }
                } else {
                    markdownBuilder.append("- None\n");
                }
                markdownBuilder.append("\n");
            }
        }

        // Endpoints
        if (parsedData.getEndpoints() != null && !parsedData.getEndpoints().isEmpty()) {
            markdownBuilder.append("## API Endpoints\n\n");
            for (EndpointMetadata endpoint : parsedData.getEndpoints()) {
                markdownBuilder.append("### `").append(endpoint.getHttpMethod()).append(" ").append(endpoint.getPath()).append("`\n");
                markdownBuilder.append("- **Type:** ").append(endpoint.getType()).append("\n");
                markdownBuilder.append("- **Handler:** `").append(endpoint.getHandlerMethod()).append("`\n");
                if (endpoint.getRequestBodyType() != null) {
                    markdownBuilder.append("- **Request Body:** `").append(endpoint.getRequestBodyType()).append("`\n");
                }
                if (endpoint.getResponseBodyType() != null) {
                    markdownBuilder.append("- **Response Body:** `").append(endpoint.getResponseBodyType()).append("`\n");
                }
                markdownBuilder.append("- **Consumes:** `").append(endpoint.getConsumes()).append("`\n");
                markdownBuilder.append("- **Produces:** `").append(endpoint.getProduces()).append("`\n");
                if (endpoint.getType().equalsIgnoreCase("SOAP")){
                    markdownBuilder.append("- **WSDL:** `").append(endpoint.getWsdlUrl()).append("`\n");
                    markdownBuilder.append("- **Operation:** `").append(endpoint.getOperationName()).append("`\n");
                }
                markdownBuilder.append("\n");
            }
        }
        
        // Diagrams
        if (parsedData.getDiagrams() != null && !parsedData.getDiagrams().isEmpty()) {
            markdownBuilder.append("## Diagrams\n\n");
            parsedData.getDiagrams().forEach((type, path) -> {
                markdownBuilder.append("### ").append(type).append("\n");
                // Assuming path is a local file path that can be referenced or embedded
                markdownBuilder.append("![").append(type).append("](").append(path.replace("\\", "/")).append(")\n\n");
            });
        }
        
        // Call Flows
        if (parsedData.getCallFlows() != null && !parsedData.getCallFlows().isEmpty()) {
            markdownBuilder.append("## Call Flows\n\n");
            for (Map.Entry<String, List<String>> entry : parsedData.getCallFlows().entrySet()) {
                markdownBuilder.append("### ").append(entry.getKey()).append("\n");
                for (String step : entry.getValue()) {
                    markdownBuilder.append("- ").append(step).append("\n");
                }
                markdownBuilder.append("\n");
            }
            if (parsedData.getDiagrams() != null && parsedData.getDiagrams().containsKey(com.codedocgen.model.DiagramType.SEQUENCE_DIAGRAM)) {
                markdownBuilder.append("### Sequence Diagram (first controller method)\n");
                String seqPath = parsedData.getDiagrams().get(com.codedocgen.model.DiagramType.SEQUENCE_DIAGRAM);
                markdownBuilder.append("![]("
                    + seqPath.replace("\\", "/") + ")\n\n");
            }
        }
        
        // OpenAPI/Swagger Specification
        if (parsedData.getOpenApiSpec() != null && !parsedData.getOpenApiSpec().isEmpty()) {
            markdownBuilder.append("## OpenAPI Specification\n\n");
            markdownBuilder.append("```yaml\n").append(parsedData.getOpenApiSpec()).append("\n```\n\n");
        }
        
        // Feature Files (Gherkin)
        if (parsedData.getFeatureFiles() != null && !parsedData.getFeatureFiles().isEmpty()) {
            markdownBuilder.append("## Feature Files (Gherkin)\n\n");
            for (String featureContent : parsedData.getFeatureFiles()) {
                markdownBuilder.append("```gherkin\n").append(featureContent).append("\n```\n\n");
            }
        }

        File outputFile = new File(outputDir, parsedData.getProjectName() + "_documentation.md");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(markdownBuilder.toString());
            logger.info("Markdown documentation generated: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Error writing Markdown documentation to file: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public String generateHtmlDocumentation(ParsedDataResponse parsedData, String outputDir) throws IOException {
        String markdownContent = generateMarkdownDocumentation(parsedData, outputDir); // Generate MD first
        if (markdownContent == null) {
            logger.warn("Markdown content is null, cannot generate HTML.");
            return null;
        }
        // Read the generated markdown file (or use the string directly if preferred)
        File mdFile = new File(markdownContent);
        String mdText = Files.readString(mdFile.toPath());

        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlContent = renderer.render(document);
        
        // Basic HTML structure with some styling
        String fullHtml = "<!DOCTYPE html>\n" +
                          "<html>\n<head>\n<meta charset=\"UTF-8\">\n" +
                          "<title>" + parsedData.getProjectName() + " Documentation</title>\n" +
                          "<style>body { font-family: sans-serif; line-height: 1.6; padding: 20px; } " +
                          "h1, h2, h3 { color: #333; } pre { background-color: #f4f4f4; padding: 10px; border-radius: 5px; overflow-x: auto; } " +
                          "code { background-color: #eee; padding: 2px 4px; border-radius: 3px;} img { max-width: 100%; height: auto; } " +
                          "table { border-collapse: collapse; width: 100%; margin-bottom: 1em; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } "+
                          "th { background-color: #f2f2f2; } </style>\n" +
                          "</head>\n<body>\n" +
                          htmlContent +
                          "</body>\n</html>";

        File htmlOutputFile = new File(outputDir, parsedData.getProjectName() + "_documentation.html");
        try (FileWriter writer = new FileWriter(htmlOutputFile)) {
            writer.write(fullHtml);
            logger.info("HTML documentation generated: {}", htmlOutputFile.getAbsolutePath());
            return htmlOutputFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Error writing HTML documentation to file: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String generateProjectSummary(ParsedDataResponse analysisResult) {
        if (analysisResult == null) {
            return "No analysis data available to generate summary.";
        }

        logger.info("[SummaryGen] isSpringBootProject: {}", analysisResult.isSpringBootProject());
        logger.info("[SummaryGen] springBootVersion: {}", analysisResult.getSpringBootVersion());

        StringBuilder summary = new StringBuilder();
        summary.append("Project Name: ").append(analysisResult.getProjectName()).append(". ");
        summary.append("Type: ").append(analysisResult.getProjectType()).append(". ");
        
        if (analysisResult.isSpringBootProject() && analysisResult.getSpringBootVersion() != null && !analysisResult.getSpringBootVersion().isEmpty()) {
            summary.append("Spring Boot Version: ").append(analysisResult.getSpringBootVersion()).append(". ");
        } else if (analysisResult.isSpringBootProject()) {
            summary.append("Spring Boot: Yes (version not detected). ");
        } 
        else {
            summary.append("Spring Boot: No. ");
        }

        int classCount = analysisResult.getClasses() != null ? analysisResult.getClasses().size() : 0;
        summary.append("Contains ").append(classCount).append(" classes/interfaces/enums.");
        long controllerCount = analysisResult.getClasses().stream().filter(c -> "controller".equalsIgnoreCase(c.getType())).count();
        long serviceCount = analysisResult.getClasses().stream().filter(c -> "service".equalsIgnoreCase(c.getType())).count();
        long repositoryCount = analysisResult.getClasses().stream().filter(c -> "repository".equalsIgnoreCase(c.getType())).count();
        summary.append(" (Controllers: ").append(controllerCount)
               .append(", Services: ").append(serviceCount)
               .append(", Repositories: ").append(repositoryCount).append(").\n");
        if (analysisResult.getEndpoints() != null) {
            summary.append("Exposes ").append(analysisResult.getEndpoints().size()).append(" API endpoints.");
            long restEndpoints = analysisResult.getEndpoints().stream().filter(e -> "REST".equalsIgnoreCase(e.getType())).count();
            long soapEndpoints = analysisResult.getEndpoints().stream().filter(e -> "SOAP".equalsIgnoreCase(e.getType())).count();
            if (restEndpoints > 0) summary.append(" (").append(restEndpoints).append(" REST");
            if (soapEndpoints > 0) summary.append(restEndpoints > 0 ? ", " : " (").append(soapEndpoints).append(" SOAP");
            if (restEndpoints > 0 || soapEndpoints > 0) summary.append(").");
            summary.append("\n");
        }
        if (analysisResult.getFeatureFiles() != null && !analysisResult.getFeatureFiles().isEmpty()) {
             summary.append("Includes ").append(analysisResult.getFeatureFiles().size()).append(" Gherkin feature files for BDD testing.\n");
        }
        if (analysisResult.getOpenApiSpec() != null && !analysisResult.getOpenApiSpec().isEmpty()){
            summary.append("An OpenAPI (Swagger) specification is available for REST APIs.\n");
        }
        // TODO: Could be enhanced with more insights, e.g., common libraries, tech stack details.
        return summary.toString();
    }
    
    @Override
    public List<String> findAndReadFeatureFiles(File projectDir) {
        List<String> featureFileContents = new ArrayList<>();
        if (projectDir == null || !projectDir.isDirectory()) {
            logger.warn("Project directory is invalid for feature file search.");
            return featureFileContents;
        }

        // Common locations for feature files
        String[] commonDirs = {"src/test/resources/features", "src/main/resources/features", "features"};

        for (String dir : commonDirs) {
            File featureDir = new File(projectDir, dir);
            if (featureDir.exists() && featureDir.isDirectory()) {
                try (Stream<Path> paths = Files.walk(featureDir.toPath())) {
                    paths.filter(Files::isRegularFile)
                         .filter(path -> path.toString().endsWith(".feature"))
                         .forEach(path -> {
                             try {
                                 featureFileContents.add(Files.readString(path, StandardCharsets.UTF_8));
                                 logger.info("Read feature file: {}", path.toString());
                             } catch (IOException e) {
                                 logger.warn("Failed to read feature file {}: {}", path.toString(), e.getMessage());
                             }
                         });
                } catch (IOException e) {
                    logger.warn("Error walking directory {}: {}", featureDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
        if (featureFileContents.isEmpty()) {
            logger.info("No Gherkin feature files found in common locations for project: {}", projectDir.getName());
        }
        return featureFileContents;
    }

    @Override
    public Map<String, String> findAndReadWsdlFiles(File projectDir) {
        Map<String, String> wsdlFilesContent = new HashMap<>();
        if (projectDir == null || !projectDir.isDirectory()) {
            logger.warn("Project directory is invalid for WSDL file search.");
            return wsdlFilesContent;
        }

        // Common locations for WSDL files
        String[] commonDirs = {"src/main/resources/wsdl", "src/main/webapp/WEB-INF/wsdl", "wsdl"};

        for (String dir : commonDirs) {
            File wsdlDir = new File(projectDir, dir);
            if (wsdlDir.exists() && wsdlDir.isDirectory()) {
                try (Stream<Path> paths = Files.walk(wsdlDir.toPath())) {
                    paths.filter(Files::isRegularFile)
                         .filter(path -> path.toString().toLowerCase().endsWith(".wsdl"))
                         .forEach(path -> {
                             try {
                                 String content = Files.readString(path, StandardCharsets.UTF_8);
                                 // Store relative path as key for better identification
                                 String relativePath = projectDir.toPath().relativize(path).toString().replace("\\", "/");
                                 wsdlFilesContent.put(relativePath, content);
                                 logger.info("Read WSDL file: {}", path.toString());
                             } catch (IOException e) {
                                 logger.warn("Failed to read WSDL file {}: {}", path.toString(), e.getMessage());
                             }
                         });
                } catch (IOException e) {
                    logger.warn("Error walking directory {}: {}", wsdlDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
         // Also search in src/main/resources directly
        File resourcesDir = new File(projectDir, "src/main/resources");
        if (resourcesDir.exists() && resourcesDir.isDirectory()) {
            try (Stream<Path> paths = Files.walk(resourcesDir.toPath(), 1)) { // Max depth 1 to only search current dir
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".wsdl"))
                        .forEach(path -> {
                            try {
                                String content = Files.readString(path, StandardCharsets.UTF_8);
                                String relativePath = projectDir.toPath().relativize(path).toString().replace("\\", "/");
                                if (!wsdlFilesContent.containsKey(relativePath)) { // Avoid duplicates if already found
                                   wsdlFilesContent.put(relativePath, content);
                                   logger.info("Read WSDL file from resources root: {}", path.toString());
                                }
                            } catch (IOException e) {
                                logger.warn("Failed to read WSDL file {}: {}", path.toString(), e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.warn("Error walking directory {}: {}", resourcesDir.getAbsolutePath(), e.getMessage());
            }
        }

        if (wsdlFilesContent.isEmpty()) {
            logger.info("No WSDL files found in common locations for project: {}", projectDir.getName());
        }
        return wsdlFilesContent;
    }

    @Override
    public Map<String, String> findAndReadXsdFiles(File projectDir) {
        Map<String, String> xsdFilesContent = new HashMap<>();
        if (projectDir == null || !projectDir.isDirectory()) {
            logger.warn("Project directory is invalid for XSD file search.");
            return xsdFilesContent;
        }

        // Common locations for XSD files
        String[] commonDirs = {"src/main/resources/xsd", "src/main/resources/schemas", "src/main/resources/wsdl", "src/main/resources"};

        for (String dir : commonDirs) {
            File xsdDir = new File(projectDir, dir);
            if (xsdDir.exists() && xsdDir.isDirectory()) {
                try (Stream<Path> paths = Files.walk(xsdDir.toPath())) {
                    paths.filter(Files::isRegularFile)
                         .filter(path -> path.toString().toLowerCase().endsWith(".xsd"))
                         .forEach(path -> {
                             try {
                                 String content = Files.readString(path, StandardCharsets.UTF_8);
                                 String relativePath = projectDir.toPath().relativize(path).toString().replace("\\", "/");
                                 // Use schemaLocation as key, which might be the file name or a more complex URI
                                 // For now, using relative path. Frontend might need to match this against <xsd:import schemaLocation=...>
                                 if (!xsdFilesContent.containsKey(relativePath)) { // Avoid duplicates if already found by previous dir scans
                                     xsdFilesContent.put(relativePath, content);
                                     logger.info("Read XSD file: {}", path.toString());
                                 }
                             } catch (IOException e) {
                                 logger.warn("Failed to read XSD file {}: {}", path.toString(), e.getMessage());
                             }
                         });
                } catch (IOException e) {
                    logger.warn("Error walking directory {}: {}", xsdDir.getAbsolutePath(), e.getMessage());
                }
            }
        }

        if (xsdFilesContent.isEmpty()) {
            logger.info("No XSD files found in common locations for project: {}", projectDir.getName());
        }
        return xsdFilesContent;
    }

    @Override
    public String generateOpenApiSpecFromEndpoints(List<EndpointMetadata> endpoints, String projectName) {
        // Minimal OpenAPI 3.0 JSON spec
        Map<String, Object> openApi = new HashMap<>();
        openApi.put("openapi", "3.0.0");
        Map<String, Object> info = new HashMap<>();
        info.put("title", projectName != null ? projectName : "Analyzed Project API");
        info.put("version", "1.0.0");
        openApi.put("info", info);
        Map<String, Object> paths = new HashMap<>();

        for (EndpointMetadata ep : endpoints) {
            if (ep.getType() != null && ep.getType().equalsIgnoreCase("REST") && ep.getPath() != null) {
                String path = ep.getPath().startsWith("/") ? ep.getPath() : "/" + ep.getPath();
                Map<String, Object> methodMap = new HashMap<>();
                String httpMethod = ep.getHttpMethod() != null ? ep.getHttpMethod().toLowerCase() : "get";
                Map<String, Object> op = new HashMap<>();
                op.put("summary", ep.getHandlerMethod());
                op.put("operationId", ep.getHandlerMethod());
                op.put("responses", Map.of("200", Map.of("description", "Success")));
                methodMap.put(httpMethod, op);
                if (!paths.containsKey(path)) {
                    paths.put(path, methodMap);
                } else {
                    ((Map<String, Object>) paths.get(path)).putAll(methodMap);
                }
            }
        }
        openApi.put("paths", paths);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(openApi);
        } catch (Exception e) {
            logger.error("Error generating OpenAPI spec from endpoints: {}", e.getMessage(), e);
            return "{}";
        }
    }
} 