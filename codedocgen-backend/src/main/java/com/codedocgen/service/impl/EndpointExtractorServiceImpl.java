package com.codedocgen.service.impl;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;
import com.codedocgen.service.EndpointExtractorService;
import com.codedocgen.parser.SoapWsdlParser;
import com.codedocgen.service.DocumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class EndpointExtractorServiceImpl implements EndpointExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointExtractorServiceImpl.class);

    @Autowired
    private SoapWsdlParser soapWsdlParser;
    @Autowired
    private DocumentationService documentationService;

    @Override
    public List<EndpointMetadata> extractEndpoints(List<ClassMetadata> classMetadataList, File projectDir) {
        logger.info("Starting endpoint extraction for project in directory: {}", projectDir.getAbsolutePath());
        List<EndpointMetadata> endpoints = new ArrayList<>();

        for (ClassMetadata cmd : classMetadataList) {
            if ("controller".equalsIgnoreCase(cmd.getType())) {
                // REST endpoint extraction
                cmd.getMethods().forEach(method -> {
                    method.getAnnotations().forEach(annotation -> {
                        if (annotation.startsWith("@GetMapping")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);
                            endpoint.setProduces("application/json"); 
                            endpoint.setConsumes("application/json"); 
                            endpoint.setType("REST");
                            endpoint.setHttpMethod("GET");
                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            endpoints.add(endpoint);
                        } else if (annotation.startsWith("@PostMapping")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);
                            endpoint.setProduces("application/json"); 
                            endpoint.setConsumes("application/json"); 
                            endpoint.setType("REST");
                            endpoint.setHttpMethod("POST");
                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            endpoints.add(endpoint);
                        } else if (annotation.startsWith("@PutMapping")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);
                            endpoint.setProduces("application/json"); 
                            endpoint.setConsumes("application/json"); 
                            endpoint.setType("REST");
                            endpoint.setHttpMethod("PUT");
                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            endpoints.add(endpoint);
                        } else if (annotation.startsWith("@DeleteMapping")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);
                            endpoint.setProduces("application/json"); 
                            endpoint.setConsumes("application/json"); 
                            endpoint.setType("REST");
                            endpoint.setHttpMethod("DELETE");
                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            endpoints.add(endpoint);
                        } else if (annotation.startsWith("@RequestMapping")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);
                            endpoint.setProduces("application/json"); 
                            endpoint.setConsumes("application/json"); 
                            endpoint.setType("REST");
                            endpoint.setHttpMethod("ANY");
                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            // TODO: Determine specific HTTP method from @RequestMapping if specified
                            endpoints.add(endpoint);
                        }
                        // Do NOT call extractRestParams outside these branches!
                    });
                });
            } else if ("soap".equalsIgnoreCase(cmd.getType())) {
                // SOAP endpoint extraction
                cmd.getMethods().forEach(method -> {
                    method.getAnnotations().forEach(annotation -> {
                        if (annotation.startsWith("@PayloadRoot")) {
                            EndpointMetadata endpoint = new EndpointMetadata();
                            String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                            String methodName = method.getName();
                            endpoint.setType("SOAP");
                            endpoint.setHttpMethod("SOAP");
                            endpoint.setHandlerMethod(fullClassName + "." + methodName);
                            endpoint.setClassName(fullClassName);
                            endpoint.setMethodName(methodName);

                            // Extract namespace and localPart from @PayloadRoot
                            String namespace = extractAnnotationAttribute(annotation, "namespace");
                            String localPart = extractAnnotationAttribute(annotation, "localPart");
                            
                            endpoint.setOperationName(localPart);
                            // Construct a conceptual path/identifier for the SOAP operation
                            endpoint.setPath( (namespace != null ? namespace : "") + "/" + (localPart != null ? localPart : method.getName()) );
                            
                            // Set Request/Response types from method parameters/return type
                            if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                                endpoint.setRequestBodyType(method.getParameters().get(0).split(" ")[0]); // Assuming first param is request object
                            }
                            endpoint.setResponseBodyType(method.getReturnType());
                            
                            // Consumes/Produces for SOAP are typically XML based
                            endpoint.setConsumes("application/soap+xml"); // Or text/xml
                            endpoint.setProduces("application/soap+xml"); // Or text/xml

                            endpoints.add(endpoint);
                            logger.debug("Extracted SOAP endpoint: {} from class {}", localPart, cmd.getName());
                        }
                        // TODO: Add support for other SOAP annotations like @WebMethod (JAX-WS)
                    });
                });
            }
        }

        // --- New: WSDL-based SOAP endpoint extraction ---
        try {
            // Find WSDL files and parse them
            java.util.Map<String, String> wsdlFiles = documentationService.findAndReadWsdlFiles(projectDir);
            if (wsdlFiles != null && !wsdlFiles.isEmpty()) {
                for (java.util.Map.Entry<String, String> wsdlEntry : wsdlFiles.entrySet()) {
                    String wsdlPath = wsdlEntry.getKey();
                    String wsdlContent = wsdlEntry.getValue();
                    List<EndpointMetadata> wsdlEndpoints = soapWsdlParser.parseWsdl(wsdlContent, wsdlPath);
                    if (wsdlEndpoints != null && !wsdlEndpoints.isEmpty()) {
                        endpoints.addAll(wsdlEndpoints);
                        logger.info("Extracted {} SOAP endpoints from WSDL file: {}", wsdlEndpoints.size(), wsdlPath);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error during WSDL-based SOAP endpoint extraction: {}", e.getMessage());
        }

        logger.info("Found {} endpoints.", endpoints.size());
        return endpoints;
    }

    private void extractPathFromAnnotation(String annotation, EndpointMetadata endpoint) {
        // Simple path extraction for REST
        try {
            int start = annotation.indexOf("\"");
            int end = annotation.lastIndexOf("\"");
            if (start != -1 && end != -1 && start < end) {
                endpoint.setPath(annotation.substring(start + 1, end));
            } else if (annotation.contains("path = \"")){
                 start = annotation.indexOf("path = \"") + "path = \"".length();
                 end = annotation.indexOf("\"", start);
                 endpoint.setPath(annotation.substring(start, end));
            } else if (annotation.contains("value = \"")) { // Also check for value attribute
                start = annotation.indexOf("value = \"") + "value = \"".length();
                end = annotation.indexOf("\"", start);
                endpoint.setPath(annotation.substring(start, end));
            }
        } catch (Exception e) {
            logger.warn("Could not parse REST path from annotation: {}", annotation, e);
        }
    }

    private String extractAnnotationAttribute(String annotation, String attributeName) {
        // Basic parser for annotation attributes like namespace="...", localPart="..."
        // Example: @PayloadRoot(namespace = "http://www.jpworks.com/employee", localPart = "EmployeeByNameRequest")
        String attributePattern = attributeName + " = \"";
        int start = annotation.indexOf(attributePattern);
        if (start != -1) {
            start += attributePattern.length();
            int end = annotation.indexOf("\"", start);
            if (end != -1) {
                return annotation.substring(start, end);
            }
        }
        return null;
    }

    private boolean isPrimitiveOrCommon(String type) {
        String t = type.toLowerCase();
        return t.equals("string") || t.equals("int") || t.equals("integer") || t.equals("long") || t.equals("double") || t.equals("float") || t.equals("boolean") || t.equals("char") || t.equals("byte") || t.equals("short") || t.startsWith("list<") || t.startsWith("set<") || t.startsWith("map<");
    }

    private void extractRestParams(com.codedocgen.model.MethodMetadata method, EndpointMetadata endpoint) {
        if (endpoint.getHttpMethod() == null) return; // Guard against NPE
        List<String> pathVars = new ArrayList<>();
        List<String> reqParams = new ArrayList<>();
        String reqBodyType = null;
        if (method.getParameterAnnotations() != null && method.getParameters() != null) {
            for (int i = 0; i < method.getParameters().size(); i++) {
                String param = method.getParameters().get(i);
                String paramType = param.contains(" ") ? param.substring(0, param.indexOf(" ")) : param;
                String paramName = param.contains(" ") ? param.substring(param.indexOf(" ") + 1) : param;
                List<String> anns = method.getParameterAnnotations().size() > i ? method.getParameterAnnotations().get(i) : new ArrayList<>();
                boolean isPathVar = anns.stream().anyMatch(a -> a.contains("@PathVariable"));
                boolean isReqParam = anns.stream().anyMatch(a -> a.contains("@RequestParam"));
                boolean isReqBody = anns.stream().anyMatch(a -> a.contains("@RequestBody"));
                if (isPathVar) pathVars.add(paramName);
                if (isReqParam) reqParams.add(paramName);
                if (isReqBody) reqBodyType = paramType;
                // Heuristic: for POST/PUT, if not primitive/well-known and not annotated, treat as body
                if (("POST".equals(endpoint.getHttpMethod()) || "PUT".equals(endpoint.getHttpMethod())) && !isReqBody && !isPathVar && !isReqParam) {
                    if (!isPrimitiveOrCommon(paramType)) reqBodyType = paramType;
                }
            }
        }
        endpoint.setPathVariables(pathVars);
        endpoint.setRequestParameters(reqParams);
        if (reqBodyType != null) endpoint.setRequestBodyType(reqBodyType);
        endpoint.setResponseBodyType(method.getReturnType());
    }

    private void extractResponseStatus(com.codedocgen.model.MethodMetadata method, EndpointMetadata endpoint) {
        if (method.getAnnotations() != null) {
            for (String ann : method.getAnnotations()) {
                if (ann.startsWith("@ResponseStatus")) {
                    // Try to extract code or value attribute
                    String code = extractAnnotationAttribute(ann, "code");
                    if (code == null) code = extractAnnotationAttribute(ann, "value");
                    if (code != null) endpoint.setHttpStatus(code);
                }
            }
        }
    }

    private void extractRequestParamDetails(com.codedocgen.model.MethodMetadata method, EndpointMetadata endpoint) {
        // Map param name -> details (required, defaultValue)
        java.util.Map<String, java.util.Map<String, String>> paramDetails = new java.util.HashMap<>();
        if (method.getParameterAnnotations() != null && method.getParameters() != null) {
            for (int i = 0; i < method.getParameters().size(); i++) {
                String param = method.getParameters().get(i);
                String paramName = param.contains(" ") ? param.substring(param.indexOf(" ") + 1) : param;
                java.util.Map<String, String> details = new java.util.HashMap<>();
                if (method.getParameterAnnotations().size() > i) {
                    for (String ann : method.getParameterAnnotations().get(i)) {
                        if (ann.startsWith("@RequestParam")) {
                            // Extract required and defaultValue attributes
                            String required = extractAnnotationAttribute(ann, "required");
                            String defaultValue = extractAnnotationAttribute(ann, "defaultValue");
                            if (required != null) details.put("required", required);
                            if (defaultValue != null) details.put("defaultValue", defaultValue);
                        }
                    }
                }
                if (!details.isEmpty()) paramDetails.put(paramName, details);
            }
        }
        if (!paramDetails.isEmpty()) endpoint.setRequestParamDetails(paramDetails);
    }
} 