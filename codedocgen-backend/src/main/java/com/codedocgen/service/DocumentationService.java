package com.codedocgen.service;

import com.codedocgen.dto.ParsedDataResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentationService {
    String generateMarkdownDocumentation(ParsedDataResponse parsedData, String outputDir) throws IOException;
    String generateHtmlDocumentation(ParsedDataResponse parsedData, String outputDir) throws IOException;    
    String generateProjectSummary(ParsedDataResponse parsedData);

    // Helper to find and read feature files
    List<String> findAndReadFeatureFiles(File projectDir);

    // Helper to find and read WSDL files
    Map<String, String> findAndReadWsdlFiles(File projectDir);

    // Helper to find and read XSD files
    Map<String, String> findAndReadXsdFiles(File projectDir);

    // Generate OpenAPI 3.0 spec JSON from endpoint metadata
    String generateOpenApiSpecFromEndpoints(List<com.codedocgen.model.EndpointMetadata> endpoints, String projectName);
} 