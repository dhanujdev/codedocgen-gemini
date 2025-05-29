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
import java.util.Optional;

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
                        // TODO: Add called methods / external calls if that data is available - DONE
                        if (method.getCalledMethods() != null && !method.getCalledMethods().isEmpty()) {
                            markdownBuilder.append("  - Called Methods: ").append(method.getCalledMethods().stream().map(cm -> "`" + cm + "`").collect(Collectors.joining(", "))).append("\n");
                        }
                        if (method.getExternalCalls() != null && !method.getExternalCalls().isEmpty()) {
                            markdownBuilder.append("  - External Calls: ").append(method.getExternalCalls().stream().map(ec -> "`" + ec + "`").collect(Collectors.joining(", "))).append("\n");
                        }
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
        
        // Tech stack details
        List<String> techStack = new ArrayList<>();
        if (analysisResult.getProjectType() != null && !analysisResult.getProjectType().equals("Raw Source/Unknown")) {
            techStack.add(analysisResult.getProjectType());
        }

        if (analysisResult.isSpringBootProject()) {
            techStack.add("Spring Boot" + (analysisResult.getSpringBootVersion() != null && !analysisResult.getSpringBootVersion().isEmpty() ? " (" + analysisResult.getSpringBootVersion() + ")" : ""));
        }

        // Infer other technologies based on endpoint types or class types if possible (example)
        boolean hasRest = false;
        boolean hasSoap = false;
        if (analysisResult.getEndpoints() != null) {
            for (EndpointMetadata endpoint : analysisResult.getEndpoints()) {
                if ("REST".equalsIgnoreCase(endpoint.getType())) hasRest = true;
                if ("SOAP".equalsIgnoreCase(endpoint.getType())) hasSoap = true;
            }
        }
        if (hasRest) techStack.add("REST APIs");
        if (hasSoap) techStack.add("SOAP Web Services");

        // Check for common class types like 'repository' or 'service'
        boolean hasRepositories = false;
        boolean hasServices = false;
        if(analysisResult.getClasses() != null){
            for(ClassMetadata cmd : analysisResult.getClasses()){
                if("repository".equalsIgnoreCase(cmd.getType())) hasRepositories = true;
                if("service".equalsIgnoreCase(cmd.getType())) hasServices = true;
            }
        }
        if(hasRepositories) techStack.add("Data Repositories (e.g., Spring Data)");
        if(hasServices) techStack.add("Service Layer");


        if (!techStack.isEmpty()) {
            summary.append("Key Technologies: ").append(String.join(", ", techStack)).append(". ");
        }

        if (analysisResult.isSpringBootProject() && analysisResult.getSpringBootVersion() != null && !analysisResult.getSpringBootVersion().isEmpty()) {
            // This information is already part of techStack, so commenting out the specific line for Spring Boot version here to avoid redundancy.
            // summary.append("Spring Boot Version: ").append(analysisResult.getSpringBootVersion()).append(". ");
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
        // TODO: Enhance with common libraries, tech stack details - Partially Done (more detailed library detection would require pom/gradle access)

        if (analysisResult.getProjectSummary() != null && !analysisResult.getProjectSummary().isEmpty()) {
             summary.append("\n\nUser Provided Summary: ").append(analysisResult.getProjectSummary());
        }

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
            logger.warn("Project directory is null or not a directory. Cannot find WSDL files.");
            return wsdlFilesContent;
        }

        List<Path> searchPaths = new ArrayList<>();
        // Common locations for WSDL files
        searchPaths.add(projectDir.toPath().resolve("src/main/resources/wsdl"));
        searchPaths.add(projectDir.toPath().resolve("src/main/resources/META-INF/wsdl"));
        searchPaths.add(projectDir.toPath().resolve("src/main/resources/service-api-definition")); // For jonashackt/soap-spring-boot-cxf
        searchPaths.add(projectDir.toPath().resolve("src/main/webapp/WEB-INF/wsdl"));
        searchPaths.add(projectDir.toPath().resolve("src/main/resources")); // General resources
        searchPaths.add(projectDir.toPath()); // Project root

        logger.info("Searching for WSDL files in project: {}", projectDir.getName());

        for (Path dir : searchPaths) {
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.walk(dir)) {
                    stream.filter(file -> !Files.isDirectory(file) && file.toString().toLowerCase().endsWith(".wsdl"))
                          .forEach(wsdlFile -> {
                              try {
                                  String content = Files.readString(wsdlFile, StandardCharsets.UTF_8);
                                  String relativePath = projectDir.toPath().relativize(wsdlFile).toString();
                                  wsdlFilesContent.put(relativePath, content);
                                  logger.info("Found and read WSDL file: {}", relativePath);
                              } catch (IOException e) {
                                  logger.warn("Could not read WSDL file {}: {}", wsdlFile, e.getMessage());
                              }
                          });
                } catch (IOException e) {
                    logger.warn("Error walking directory {} for WSDL files: {}", dir, e.getMessage());
                }
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

    public List<EndpointMetadata> combineEndpointData(List<EndpointMetadata> annotationEndpoints, List<EndpointMetadata> wsdlEndpoints) {
        if (wsdlEndpoints == null || wsdlEndpoints.isEmpty()) {
            return annotationEndpoints != null ? annotationEndpoints : new ArrayList<>();
        }
        if (annotationEndpoints == null || annotationEndpoints.isEmpty()) {
            return wsdlEndpoints;
        }

        List<EndpointMetadata> combined = new ArrayList<>();
        Map<String, EndpointMetadata> wsdlMapByTargetNamespaceAndOperation = new HashMap<>();

        for (EndpointMetadata wsdlEp : wsdlEndpoints) {
            if (wsdlEp.getTargetNamespace() != null && !wsdlEp.getTargetNamespace().isEmpty() &&
                wsdlEp.getOperationName() != null && !wsdlEp.getOperationName().isEmpty()) {
                String key = wsdlEp.getTargetNamespace().toLowerCase() + "::" + wsdlEp.getOperationName().toLowerCase();
                wsdlMapByTargetNamespaceAndOperation.put(key, wsdlEp);
            } else {
                 // If TNS or OpName is missing, add WSDL endpoint as is, it cannot be reliably correlated
                logger.warn("WSDL endpoint for WSDL {} operation {} is missing targetNamespace or operationName, adding as-is.", wsdlEp.getWsdlUrl(), wsdlEp.getOperationName());
                combined.add(wsdlEp);
            }
        }
        
        List<EndpointMetadata> annotationEndpointsNotMatched = new ArrayList<>();

        for (EndpointMetadata annEp : annotationEndpoints) {
            String annTargetNamespace = annEp.getTargetNamespace();
            String annOperationName = annEp.getOperationName();

            EndpointMetadata matchedWsdlEp = null;
            if (annTargetNamespace != null && !annTargetNamespace.isEmpty() &&
                annOperationName != null && !annOperationName.isEmpty()) {
                String key = annTargetNamespace.toLowerCase() + "::" + annOperationName.toLowerCase();
                matchedWsdlEp = wsdlMapByTargetNamespaceAndOperation.get(key);
            }

            if (matchedWsdlEp != null) {
                logger.info("Matched WSDL endpoint for TNS '{}' operation '{}' with annotation endpoint {}#{}", 
                            matchedWsdlEp.getTargetNamespace(), matchedWsdlEp.getOperationName(), annEp.getClassName(), annEp.getMethodName());
                
                // Merge: Start with annotation endpoint, then selectively override/add from WSDL
                EndpointMetadata merged = new EndpointMetadata();
                merged.setClassName(annEp.getClassName());
                merged.setMethodName(annEp.getMethodName());
                merged.setHandlerMethod(annEp.getHandlerMethod());
                merged.setHeaders(annEp.getHeaders()); // Keep Java annotations

                // Details from WSDL
                merged.setWsdlUrl(matchedWsdlEp.getWsdlUrl());
                merged.setOperationName(matchedWsdlEp.getOperationName()); // Use WSDL's casing for operation name
                merged.setTargetNamespace(matchedWsdlEp.getTargetNamespace());
                merged.setSoapAction(matchedWsdlEp.getSoapAction());
                merged.setStyle(matchedWsdlEp.getStyle());
                merged.setUse(matchedWsdlEp.getUse());
                merged.setPortTypeName(matchedWsdlEp.getPortTypeName());
                merged.setServiceName(matchedWsdlEp.getServiceName()); // From WSDL Service
                merged.setPortName(matchedWsdlEp.getPortName());       // From WSDL Port
                merged.setPath(matchedWsdlEp.getPath()); // Use WSDL derived path
                merged.setRequestBodyType(matchedWsdlEp.getRequestBodyType()); // WSDL message name
                merged.setResponseBodyType(matchedWsdlEp.getResponseBodyType()); // WSDL message name
                
                // Details from Annotations (if WSDL didn't provide or if annotation is more specific)
                merged.setConsumes(annEp.getConsumes() != null ? annEp.getConsumes() : matchedWsdlEp.getConsumes());
                merged.setProduces(annEp.getProduces() != null ? annEp.getProduces() : matchedWsdlEp.getProduces());
                merged.setType(annEp.getType()); // Should be SOAP from both
                merged.setHttpMethod(annEp.getHttpMethod()); // Should be SOAP from both

                merged.setParameterStyle(annEp.getParameterStyle() != null ? annEp.getParameterStyle() : matchedWsdlEp.getParameterStyle());
                merged.setRequestWrapperName(annEp.getRequestWrapperName());
                merged.setRequestWrapperClassName(annEp.getRequestWrapperClassName());
                merged.setResponseWrapperName(annEp.getResponseWrapperName());
                merged.setResponseWrapperClassName(annEp.getResponseWrapperClassName());
                merged.setHeaders(annEp.getHeaders()); // Use headers from the annotation-derived endpoint metadata
                merged.setSoapRequestHeaderQNames(annEp.getSoapRequestHeaderQNames()); // from @SoapHeader
                merged.setPathVariables(annEp.getPathVariables()); // Usually null for SOAP
                merged.setRequestParameters(annEp.getRequestParameters()); // Usually null for SOAP
                merged.setHttpStatus(annEp.getHttpStatus()); // Usually null for SOAP
                merged.setRequestParamDetails(annEp.getRequestParamDetails());

                combined.add(merged);
                wsdlMapByTargetNamespaceAndOperation.remove(matchedWsdlEp.getTargetNamespace().toLowerCase() + "::" + matchedWsdlEp.getOperationName().toLowerCase()); // Remove from map as it's matched
            } else {
                logger.info("No WSDL match for annotation endpoint: {} {}#{} (TNS: {}, Op: {}). Adding as is.",
                    annEp.getPath(), annEp.getClassName(), annEp.getMethodName(), annEp.getTargetNamespace(), annEp.getOperationName());
                annotationEndpointsNotMatched.add(annEp);
            }
        }

        // Add any WSDL endpoints that were not matched by any annotation endpoint
        combined.addAll(wsdlMapByTargetNamespaceAndOperation.values());
        // Add annotation endpoints that were not matched (these might be REST or non-WSDL SOAP)
        combined.addAll(annotationEndpointsNotMatched);

        logger.info("Combined {} annotation endpoints and {} WSDL endpoints into {} final endpoints.",
            annotationEndpoints != null ? annotationEndpoints.size() : 0,
            wsdlEndpoints != null ? wsdlEndpoints.size() : 0,
            combined.size());

        return combined;
    }
} 