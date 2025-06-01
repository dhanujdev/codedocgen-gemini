package com.codedocgen.service.impl;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.EndpointMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.service.EndpointExtractorService;
import com.codedocgen.parser.SoapWsdlParser;
import com.codedocgen.service.DocumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Corrected JavaParser related imports for current logic:
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.Node;
// These are the problematic symbol solver imports.
// If javaparser-symbol-solver-core is correctly on classpath, they should resolve.
// They are not directly used in the current WSDLDefinitionInfo extraction logic
// but might be used elsewhere or by transitive JavaParser functionalities.
// For now, let's REMOVE them as a diagnostic step.
// REMOVE: import com.github.javaparser.symbolsolver.model.resolution.SymbolReference; 
// REMOVE: import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
// Imports that might be needed if we do deeper symbol resolution in the future for non-literal args:
// import com.github.javaparser.resolution.SymbolResolver; 
// import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
// import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
// import com.github.javaparser.resolution.types.ResolvedType;

@Service
public class EndpointExtractorServiceImpl implements EndpointExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointExtractorServiceImpl.class);

    @Autowired
    private SoapWsdlParser soapWsdlParser;
    @Autowired
    private DocumentationService documentationService;

    @Override
    public List<EndpointMetadata> extractEndpoints(List<ClassMetadata> allClassMetadata, File projectDir) {
        List<EndpointMetadata> endpoints = new ArrayList<>();
        Map<String, WsdlDefinitionInfo> wsdlInfosByTargetNamespace = new HashMap<>();
        Map<String, WsdlDefinitionInfo> wsdlInfosByBeanName = new HashMap<>();

        logger.info("Phase 1: Scanning for DefaultWsdl11Definition beans...");
        for (ClassMetadata clazz : allClassMetadata) {
            if (clazz.getAnnotations() != null && clazz.getAnnotations().stream().anyMatch(a -> "Configuration".equals(a))) {
                logger.debug("Found @Configuration class: {}", clazz.getName());
                for (MethodMetadata method : clazz.getMethods()) {
                    if (method.getAnnotations() != null && method.getAnnotations().stream().anyMatch(a -> "Bean".equals(a))) {
                        String returnType = method.getReturnType(); // Assuming this is a String like "org.example.MyType"
                        logger.debug("Found @Bean method: {} in class: {}, Return Type: {}", method.getName(), clazz.getName(), returnType);

                        if (returnType != null && returnType.endsWith("DefaultWsdl11Definition")) { // Simplified check for FQN
                            logger.info("Found DefaultWsdl11Definition bean method: {} in {}", method.getName(), clazz.getName());
                            WsdlDefinitionInfo wsdlInfo = new WsdlDefinitionInfo();
                            wsdlInfo.setBeanName(method.getName());
                            wsdlInfo.setServiceName(method.getName()); // Tentative service name from bean name

                            MethodDeclaration beanMethodNode = method.getResolvedMethodNode();
                            if (beanMethodNode != null && beanMethodNode.getBody().isPresent()) {
                                for (Statement stmt : beanMethodNode.getBody().get().getStatements()) {
                                    if (stmt.isExpressionStmt()) {
                                        ExpressionStmt exprStmt = stmt.asExpressionStmt();
                                        if (exprStmt.getExpression().isMethodCallExpr()) {
                                            MethodCallExpr calledExpr = exprStmt.getExpression().asMethodCallExpr();
                                            String calledMethodName = calledExpr.getNameAsString();

                                            // We are interested in setters like setPortTypeName, setTargetNamespace, setLocationUri
                                            if (calledExpr.getArguments().size() == 1 && 
                                                (calledMethodName.equals("setPortTypeName") || 
                                                 calledMethodName.equals("setTargetNamespace") || 
                                                 calledMethodName.equals("setLocationUri"))) {
                                                
                                                Node argNode = calledExpr.getArguments().get(0);
                                                String argumentValue = null;

                                                if (argNode instanceof StringLiteralExpr) {
                                                    argumentValue = ((StringLiteralExpr) argNode).getValue();
                                                } else {
                                                    // Attempt to resolve if it's not a direct string literal (e.g., a constant)
                                                    // This part can be complex and might need the full symbol solver context
                                                    // For now, we'll log if it's not a string literal
                                                    logger.trace("    Argument for {} in bean {} is not a direct string literal: {}. Type: {}", 
                                                                 calledMethodName, method.getName(), argNode.toString(), argNode.getClass().getSimpleName());
                                                    // Try to get its string representation as a fallback, might be variable name
                                                    // argumentValue = argNode.toString(); 
                                                }
                                                
                                                logger.trace("    Call to {} in bean {} with argument: '{}'", calledMethodName, method.getName(), argumentValue);

                                                if (argumentValue != null && !argumentValue.isEmpty()) {
                                                    if ("setPortTypeName".equals(calledMethodName)) {
                                                        wsdlInfo.setPortName(argumentValue);
                                                        logger.info("    Extracted PortTypeName for bean '{}': {}", wsdlInfo.getBeanName(), argumentValue);
                                                    } else if ("setTargetNamespace".equals(calledMethodName)) {
                                                        wsdlInfo.setTargetNamespace(argumentValue);
                                                        logger.info("    Extracted TargetNamespace for bean '{}': {}", wsdlInfo.getBeanName(), argumentValue);
                                                    } else if ("setLocationUri".equals(calledMethodName)) {
                                                        wsdlInfo.setWsdlUrl(argumentValue);
                                                        logger.info("    Extracted LocationUri for bean '{}' (used as wsdlUrl): {}", wsdlInfo.getBeanName(), argumentValue);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                logger.warn("Could not find method body or resolved node for @Bean method {} in {}", method.getName(), clazz.getName());
                            }
                            if (wsdlInfo.getTargetNamespace() != null && !wsdlInfo.getTargetNamespace().isEmpty()) {
                                wsdlInfosByTargetNamespace.put(wsdlInfo.getTargetNamespace(), wsdlInfo);
                            } else {
                                // Store by bean name if targetNamespace is not available or empty, though less reliable for matching
                                wsdlInfosByBeanName.put(wsdlInfo.getBeanName(), wsdlInfo);
                                logger.warn("WSDL info for bean '{}' stored by bean name due to missing targetNamespace.", wsdlInfo.getBeanName());
                            }
                        }
                    }
                }
            }
        }
        logger.info("Phase 1: Scan Complete. Found {} WSDL definition infos by TNS, {} by bean name.", wsdlInfosByTargetNamespace.size(), wsdlInfosByBeanName.size());

        logger.info("Phase 2: Processing all classes for endpoint annotations...");
        for (ClassMetadata cmd : allClassMetadata) {
            // Populate class-level SOAP metadata first
            String classTargetNamespace = null;
            String classPortTypeName = null; // This is operation name for JAX-WS, or PortType for WSDL
            String classPortName = null; // This is PortName in WSDL context, or from @WebService(portName)
            String classBindingStyle = null;
            String classParameterStyle = null;
            String classServiceName = null;
            String classWsdlLocation = null;
            String classSeiName = null; 

            // Final effective values to be used for methods in this class
            String effectiveTargetNamespace = null;
            String effectivePortTypeNameForClass = null; // JAX-WS @WebService(name=) can be on class or SEI
            String effectivePortNameForClass = null; // JAX-WS @WebService(portName=) can be on class or SEI
            String effectiveServiceNameForClass = null; // JAX-WS @WebService(serviceName=) can be on class or SEI
            String effectiveWsdlLocation = null;


            for (String clsAnnotation : cmd.getAnnotations()) {
                if (clsAnnotation.startsWith("@WebService")) {
                    classTargetNamespace = extractAnnotationAttribute(clsAnnotation, "targetNamespace");
                    classPortTypeName = extractAnnotationAttribute(clsAnnotation, "name");
                    classPortName = extractAnnotationAttribute(clsAnnotation, "portName");
                    classServiceName = extractAnnotationAttribute(clsAnnotation, "serviceName");
                    classWsdlLocation = extractAnnotationAttribute(clsAnnotation, "wsdlLocation");
                    classSeiName = extractAnnotationAttribute(clsAnnotation, "endpointInterface");
                } else if (clsAnnotation.startsWith("@SOAPBinding")) {
                    classBindingStyle = extractAnnotationAttribute(clsAnnotation, "style");
                    classParameterStyle = extractAnnotationAttribute(clsAnnotation, "parameterStyle");
                }
            }

            effectiveTargetNamespace = classTargetNamespace;
            effectivePortTypeNameForClass = classPortTypeName;
            effectivePortNameForClass = classPortName;
            effectiveServiceNameForClass = classServiceName;
            effectiveWsdlLocation = classWsdlLocation;

            ClassMetadata seiClassMetadata = null;
            if (classSeiName != null && !classSeiName.isEmpty()) {
                final String finalClassSeiName = classSeiName;
                seiClassMetadata = allClassMetadata.stream()
                        .filter(c -> finalClassSeiName.equals(c.getPackageName() + "." + c.getName()))
                        .findFirst().orElse(null);
                if (seiClassMetadata != null) {
                    logger.info("Found SEI {} for class {}", classSeiName, cmd.getName());
                    for (String seiAnnotationString : seiClassMetadata.getAnnotations()) {
                        if (seiAnnotationString.startsWith("@WebService")) {
                            String seiTargetNamespace = extractAnnotationAttribute(seiAnnotationString, "targetNamespace");
                            if (seiTargetNamespace != null && !seiTargetNamespace.isEmpty()) {
                                effectiveTargetNamespace = seiTargetNamespace; // SEI TNS overrides class TNS
                                logger.info("Overriding targetNamespace with SEI value: {} for class {}", effectiveTargetNamespace, cmd.getName());
                            }
                            String seiName = extractAnnotationAttribute(seiAnnotationString, "name"); 
                            if (seiName != null && !seiName.isEmpty()) {
                                effectivePortTypeNameForClass = seiName; // SEI name overrides class name for PortType
                            }
                            String seiPortName = extractAnnotationAttribute(seiAnnotationString, "portName");
                            if (seiPortName != null && !seiPortName.isEmpty()) {
                                effectivePortNameForClass = seiPortName; // SEI portName overrides
                            }
                            String seiServiceName = extractAnnotationAttribute(seiAnnotationString, "serviceName");
                            if (seiServiceName != null && !seiServiceName.isEmpty()) {
                                effectiveServiceNameForClass = seiServiceName; // SEI serviceName overrides
                            }
                            // WSDL location is typically on the implementation or deployment descriptor
                        }
                    }
                }
            }

            if ("controller".equalsIgnoreCase(cmd.getType())) {
                // REST endpoint extraction
                for (MethodMetadata method : cmd.getMethods()) {
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
                            
                            String httpMethod = "ANY"; 
                            String methodAttribute = extractAnnotationAttribute(annotation, "method");
                            if (methodAttribute != null && !methodAttribute.isEmpty()) {
                                if (methodAttribute.contains(".")) { 
                                    httpMethod = methodAttribute.substring(methodAttribute.lastIndexOf('.') + 1);
                                } else {
                                    httpMethod = methodAttribute; 
                                }
                            }
                            endpoint.setHttpMethod(httpMethod.toUpperCase());

                            extractPathFromAnnotation(annotation, endpoint);
                            extractRestParams(method, endpoint);
                            extractResponseStatus(method, endpoint);
                            extractRequestParamDetails(method, endpoint);
                            endpoints.add(endpoint);
                        }
                    });
                }
            } else if ("soap".equalsIgnoreCase(cmd.getType()) || cmd.getAnnotations().stream().anyMatch(ann -> ann.startsWith("@WebService"))) {
                 // Enhanced SOAP endpoint extraction
                for (MethodMetadata method : cmd.getMethods()) {
                    boolean isSoapMethod = false;
                    EndpointMetadata endpoint = new EndpointMetadata();
                    String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                    String methodName = method.getName();

                    endpoint.setHandlerMethod(fullClassName + "." + methodName);
                    endpoint.setClassName(fullClassName);
                    endpoint.setMethodName(methodName);
                    endpoint.setOperationName(methodName); // Default operation name to method name
                    endpoint.setType("SOAP");
                    endpoint.setConsumes("application/soap+xml"); // Default
                    endpoint.setProduces("application/soap+xml"); // Default

                    // Apply class-level JAX-WS and SOAPBinding info
                    endpoint.setTargetNamespace(effectiveTargetNamespace);
                    endpoint.setPortTypeName(effectivePortTypeNameForClass);
                    endpoint.setPortName(effectivePortNameForClass);
                    endpoint.setServiceName(effectiveServiceNameForClass); // Apply serviceName
                    endpoint.setWsdlUrl(effectiveWsdlLocation);    // Apply wsdlUrl

                    endpoint.setStyle(classBindingStyle != null ? classBindingStyle.toUpperCase() : null);
                    endpoint.setParameterStyle(classParameterStyle != null ? classParameterStyle.toUpperCase() : null);

                    // Effective values, starting with class-level, potentially overridden by method-level

                    Map<String, String> requestHeaders = new HashMap<>();
                    Map<String, String> responseHeaders = new HashMap<>();

                    for (String annotation : method.getAnnotations()) {
                        if (annotation.startsWith("@WebMethod")) {
                            isSoapMethod = true;
                            String operationName = extractAnnotationAttribute(annotation, "operationName");
                            if (operationName == null || operationName.isEmpty()) {
                                operationName = methodName;
                            }
                            endpoint.setOperationName(operationName);
                            endpoint.setSoapAction(extractAnnotationAttribute(annotation, "action"));
                            // @WebMethod's targetNamespace would override class's if present, but it's not a common attribute here.
                        } else if (annotation.startsWith("@PayloadRoot")) { // Spring-WS
                            isSoapMethod = true;
                            endpoint.setTargetNamespace(extractAnnotationAttribute(annotation, "namespace"));
                            endpoint.setOperationName(extractAnnotationAttribute(annotation, "localPart"));
                        } else if (annotation.startsWith("@SoapAction")) { // Spring-WS
                            isSoapMethod = true;
                            endpoint.setSoapAction(extractAnnotationAttribute(annotation, "value"));
                        } else if (annotation.startsWith("@SOAPBinding")) { // Method-level override
                            isSoapMethod = true; // If a method has SOAPBinding, it's likely a SOAP method
                            String style = extractAnnotationAttribute(annotation, "style");
                            String paramStyle = extractAnnotationAttribute(annotation, "parameterStyle");
                            if (style != null) endpoint.setStyle(style.toUpperCase());
                            if (paramStyle != null) endpoint.setParameterStyle(paramStyle.toUpperCase());
                        } else if (annotation.startsWith("@RequestWrapper")) {
                            isSoapMethod = true;
                            endpoint.setRequestWrapperName(extractAnnotationAttribute(annotation, "localName"));
                            endpoint.setRequestWrapperClassName(extractAnnotationAttribute(annotation, "className"));
                        } else if (annotation.startsWith("@ResponseWrapper")) {
                            isSoapMethod = true;
                            endpoint.setResponseWrapperName(extractAnnotationAttribute(annotation, "localName"));
                            endpoint.setResponseWrapperClassName(extractAnnotationAttribute(annotation, "className"));
                        } else if (annotation.startsWith("@WebResult")) {
                            isSoapMethod = true; // A method with @WebResult in a @WebService class is a SOAP method
                            if (Boolean.parseBoolean(extractAnnotationAttribute(annotation, "header"))) {
                                String headerName = extractAnnotationAttribute(annotation, "name");
                                if (headerName == null || headerName.isEmpty()) headerName = "return"; // Default
                                responseHeaders.put(headerName, method.getReturnType());
                            }
                             // Other attributes like name, targetNamespace could refine responseBodyType if BARE
                        }
                         // Allow any method in a @WebService class to be implicitly a SOAP op if not excluded
                        if (cmd.getAnnotations().stream().anyMatch(ann -> ann.startsWith("@WebService")) && !method.getAnnotations().stream().anyMatch(ann -> ann.startsWith("@WebMethod") && "true".equalsIgnoreCase(extractAnnotationAttribute(ann, "exclude")))) {
                            isSoapMethod = true;
                            if (endpoint.getOperationName() == null) { // if not set by @WebMethod or @PayloadRoot
                                endpoint.setOperationName(methodName);
                            }
                        }
                    }

                    // Process parameter annotations for @WebParam and @SoapHeader
                    if (method.getParameterAnnotations() != null && method.getParameters() != null) {
                        for (int i = 0; i < method.getParameters().size(); i++) {
                            String paramTypeAndName = method.getParameters().get(i);
                            String paramType = paramTypeAndName.contains(" ") ? paramTypeAndName.substring(0, paramTypeAndName.indexOf(" ")) : paramTypeAndName;
                            String paramName = paramTypeAndName.contains(" ") ? paramTypeAndName.substring(paramTypeAndName.indexOf(" ") + 1) : ("arg" + i) ;


                            if (method.getParameterAnnotations().size() > i) {
                                for (String paramAnn : method.getParameterAnnotations().get(i)) {
                                    if (paramAnn.startsWith("@WebParam")) {
                                        isSoapMethod = true; // A method with @WebParam in a @WebService class is a SOAP method
                                        if (Boolean.parseBoolean(extractAnnotationAttribute(paramAnn, "header"))) {
                                            String headerName = extractAnnotationAttribute(paramAnn, "name");
                                            // If partName is present, it might be preferred for SOAP headers
                                            String partName = extractAnnotationAttribute(paramAnn, "partName");
                                            if (partName != null && !partName.isEmpty()) headerName = partName;
                                            
                                            if (headerName == null || headerName.isEmpty()) headerName = paramName;
                                            requestHeaders.put(headerName, paramType);
                                        }
                                        // Other attributes like name, targetNamespace could refine requestBodyType if BARE or for wrapper elements
                                    } else if (paramAnn.startsWith("@SoapHeader")) { // Spring-WS
                                        isSoapMethod = true;
                                        // Example: @SoapHeader("{http://mycompany.com/headers}authKey") String authKey
                                        // Need a more robust way to parse qualified name from value()
                                        String headerFullName = extractAnnotationAttribute(paramAnn, "value");
                                        if(headerFullName != null && !headerFullName.isEmpty()){
                                            if(headerFullName.startsWith("{")) {
                                                int endNsIdx = headerFullName.indexOf("}");
                                                if(endNsIdx > 0) {
                                                    // String ns = headerFullName.substring(1, endNsIdx);
                                                    headerFullName = headerFullName.substring(endNsIdx + 1);
                                                }
                                            }
                                            requestHeaders.put(headerFullName, paramType);
                                        } else {
                                             requestHeaders.put(paramName, paramType); // Fallback if value is not there
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!requestHeaders.isEmpty()) endpoint.setRequestHeaderParts(requestHeaders);
                    if (!responseHeaders.isEmpty()) endpoint.setResponseHeaderParts(responseHeaders);


                    if (isSoapMethod) {
                        // Set default operation name if still null
                        if (endpoint.getOperationName() == null) {
                            endpoint.setOperationName(methodName);
                        }

                        // Construct path (conceptual for SOAP)
                        String serviceNameForPath = cmd.getAnnotations().stream()
                            .filter(ann -> ann.startsWith("@WebService"))
                            .map(ann -> extractAnnotationAttribute(ann, "serviceName"))
                            .filter(sn -> sn != null && !sn.isEmpty())
                            .findFirst().orElse(cmd.getName());
                        endpoint.setPath(serviceNameForPath + "/" + endpoint.getOperationName());


                        // Request/Response body types (simplified for now, can be enhanced by BARE/WRAPPED style)
                        // If @RequestWrapper is present, className could be the requestBodyType
                        if (endpoint.getRequestWrapperClassName() != null && !endpoint.getRequestWrapperClassName().isEmpty()) {
                            endpoint.setRequestBodyType(endpoint.getRequestWrapperClassName());
                        } else if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                             // For BARE, might be the first non-header param. For WRAPPED, this is heuristic.
                             // This part needs to be smarter based on parameterStyle.
                            if (!"BARE".equalsIgnoreCase(endpoint.getParameterStyle())) { // Wrapped or unknown
                                if (method.getParameters().size() == 1 && (endpoint.getRequestHeaderParts() == null || !endpoint.getRequestHeaderParts().containsKey(method.getParameters().get(0).split(" ")[1]))) {
                                   endpoint.setRequestBodyType(method.getParameters().get(0).split(" ")[0]);
                                } else if (method.getParameters().size() > 1) {
                                    // Heuristic: if multiple params and wrapped, often implies a generated wrapper not explicitly annotated.
                                    // For now, leave it null or use a placeholder like "Multiple Parameters (Wrapped)"
                                     endpoint.setRequestBodyType(methodName + "Request"); // Placeholder
                                }
                            } else { // BARE
                                for (int i=0; i < method.getParameters().size(); i++) {
                                    String paramFullName = method.getParameters().get(i);
                                    String currentParamName = paramFullName.contains(" ") ? paramFullName.substring(paramFullName.indexOf(" ") + 1) : ("arg" + i);
                                    boolean isHeader = false;
                                    if (endpoint.getRequestHeaderParts() != null && endpoint.getRequestHeaderParts().containsKey(currentParamName)) {
                                        isHeader = true;
                                    }
                                    if (!isHeader) {
                                        endpoint.setRequestBodyType(paramFullName.split(" ")[0]);
                                        break; // Found the first non-header param
                                    }
                                }
                            }
                        }

                        // If @ResponseWrapper is present, className could be the responseBodyType
                        if (endpoint.getResponseWrapperClassName() != null && !endpoint.getResponseWrapperClassName().isEmpty()) {
                            endpoint.setResponseBodyType(endpoint.getResponseWrapperClassName());
                        } else {
                            if (!"void".equalsIgnoreCase(method.getReturnType()) && ("BARE".equalsIgnoreCase(endpoint.getParameterStyle()) || (endpoint.getResponseHeaderParts() == null || endpoint.getResponseHeaderParts().isEmpty())) ){
                                endpoint.setResponseBodyType(method.getReturnType());
                            } else if (!"void".equalsIgnoreCase(method.getReturnType()) && !"BARE".equalsIgnoreCase(endpoint.getParameterStyle())) {
                                // Wrapped response, not void, and no explicit @ResponseWrapper
                                endpoint.setResponseBodyType(methodName + "Response"); // Placeholder
                            }
                        }
                        
                        endpoints.add(endpoint);
                        logger.debug("Extracted SOAP endpoint: {} from class {}", endpoint.getOperationName(), cmd.getName());
                    }
                }
            }

            // JAX-WS Service Implementation Bean (often marked as @Service or @Component, not just @WebService)
            // or a class that implements an SEI.
            boolean isJaxWsImplementation = cmd.getAnnotations().stream().anyMatch(a -> a.startsWith("@WebService"));
            if (!isJaxWsImplementation && classSeiName != null && !classSeiName.isEmpty()) {
                // If it doesn't have @WebService directly but implements an SEI found earlier,
                // it's effectively a JAX-WS implementation.
                isJaxWsImplementation = true;
            }

            if (isJaxWsImplementation && ("service".equalsIgnoreCase(cmd.getType()) || "component".equalsIgnoreCase(cmd.getType()) || "soap".equalsIgnoreCase(cmd.getType()) || classSeiName != null)) {
                logger.info("Processing JAX-WS style class: {} (or class implementing SEI: {})", cmd.getName(), classSeiName);

                final ClassMetadata finalSeiClassMetadata = seiClassMetadata; // effectively final for lambda
                final String finalEffectiveTargetNamespace = effectiveTargetNamespace;
                final String finalEffectivePortTypeNameForClass = effectivePortTypeNameForClass;
                final String finalEffectivePortNameForClass = effectivePortNameForClass;
                final String finalEffectiveServiceNameForClass = effectiveServiceNameForClass;
                final String finalEffectiveWsdlLocation = effectiveWsdlLocation;
                final String finalClassBindingStyle = classBindingStyle;
                final String finalClassParameterStyle = classParameterStyle;

                for (MethodMetadata method : cmd.getMethods()) {
                    List<String> combinedAnnotations = new ArrayList<>(method.getAnnotations());
                    String seiMethodSignature = method.getName() + "(" + method.getParameters().size() + ")";

                    if (finalSeiClassMetadata != null) {
                        MethodMetadata seiMethod = finalSeiClassMetadata.getMethods().stream()
                            .filter(m -> (m.getName() + "(" + m.getParameters().size() + ")").equals(seiMethodSignature))
                            .findFirst().orElse(null);
                        if (seiMethod != null) {
                            logger.debug("Merging annotations from SEI method {} for impl method {}", seiMethod.getName(), method.getName());
                            combinedAnnotations.addAll(seiMethod.getAnnotations());
                        }
                    }

                    boolean isWebMethod = false;
                    String operationName = method.getName(); // Default operation name
                    String action = "";
                    String methodBindingStyle = null;
                    String methodUse = null;
                    String methodParameterStyle = null;
                    String requestWrapperName = null;
                    String requestWrapperClassName = null;
                    String responseWrapperName = null;
                    String responseWrapperClassName = null;
                    // Map<String, String> headers = new HashMap<>(); // This was for general headers, JAX-WS uses @WebParam for SOAP headers. EndpointMetadata already has a field for this.
                    List<String> soapRequestHeaderQNames = new ArrayList<>(); // For Spring-WS @SoapHeader

                    // These are specific to the current method being processed for JAX-WS headers
                    Map<String, String> methodRequestHeaders = new HashMap<>();
                    Map<String, String> methodResponseHeaders = new HashMap<>();
                    List<String> requestHeaderParts = new ArrayList<>(); // To match SoapWsdlParser structure if needed later
                    List<String> responseHeaderParts = new ArrayList<>();

                    for (String ann : combinedAnnotations) {
                        if (ann.startsWith("@WebMethod")) {
                            isWebMethod = true;
                            String opNameAttr = extractAnnotationAttribute(ann, "operationName");
                            if (opNameAttr != null && !opNameAttr.isEmpty()) operationName = opNameAttr;
                            String actionAttr = extractAnnotationAttribute(ann, "action");
                            if (actionAttr != null) action = actionAttr;
                        } else if (ann.startsWith("@SOAPBinding")) {
                            methodBindingStyle = extractAnnotationAttribute(ann, "style");
                            methodUse = extractAnnotationAttribute(ann, "use");
                            methodParameterStyle = extractAnnotationAttribute(ann, "parameterStyle");
                        } else if (ann.startsWith("@RequestWrapper")) {
                            requestWrapperName = extractAnnotationAttribute(ann, "localName");
                            requestWrapperClassName = extractAnnotationAttribute(ann, "className");
                        } else if (ann.startsWith("@ResponseWrapper")) {
                            responseWrapperName = extractAnnotationAttribute(ann, "localName");
                            responseWrapperClassName = extractAnnotationAttribute(ann, "className");
                        }
                        // @WebParam for headers is processed below with parameters
                    }

                    if (isWebMethod) {
                        EndpointMetadata endpoint = new EndpointMetadata();
                        String fullClassName = cmd.getPackageName() + "." + cmd.getName();
                        endpoint.setHandlerMethod(fullClassName + "." + method.getName());
                        endpoint.setClassName(fullClassName);
                        endpoint.setMethodName(method.getName());
                        endpoint.setType("SOAP");
                        endpoint.setHttpMethod("SOAP"); // Generic for SOAP
                        endpoint.setOperationName(operationName);
                        endpoint.setSoapAction(action);
                        
                        // Apply class/SEI level defaults, then method-level overrides
                        endpoint.setTargetNamespace(finalEffectiveTargetNamespace);
                        endpoint.setPortTypeName(finalEffectivePortTypeNameForClass); // This is the SEI/Class @WebService name (PortType)
                        endpoint.setPortName(finalEffectivePortNameForClass);     // This is the SEI/Class @WebService portName
                        endpoint.setServiceName(finalEffectiveServiceNameForClass); // This is the SEI/Class @WebService serviceName
                        endpoint.setWsdlUrl(finalEffectiveWsdlLocation);

                        endpoint.setStyle(methodBindingStyle != null ? methodBindingStyle.toUpperCase() : (finalClassBindingStyle != null ? finalClassBindingStyle.toUpperCase() : null));
                        endpoint.setUse(methodUse != null ? methodUse.toUpperCase() : null); // Use typically comes from method @SOAPBinding or WSDL body
                        endpoint.setParameterStyle(methodParameterStyle != null ? methodParameterStyle.toUpperCase() : (finalClassParameterStyle != null ? finalClassParameterStyle.toUpperCase() : null));

                        endpoint.setRequestWrapperName(requestWrapperName);
                        endpoint.setRequestWrapperClassName(requestWrapperClassName);
                        endpoint.setResponseWrapperName(responseWrapperName);
                        endpoint.setResponseWrapperClassName(responseWrapperClassName);

                        // Process parameter annotations for @WebParam and @SoapHeader
                        if (method.getParameterAnnotations() != null && method.getParameters() != null) {
                            for (int i = 0; i < method.getParameters().size(); i++) {
                                String paramTypeAndName = method.getParameters().get(i);
                                String paramType = paramTypeAndName.contains(" ") ? paramTypeAndName.substring(0, paramTypeAndName.lastIndexOf(' ')) : paramTypeAndName;
                                String paramName = paramTypeAndName.contains(" ") ? paramTypeAndName.substring(paramTypeAndName.lastIndexOf(' ') + 1) : "arg" + i;

                                if (method.getParameterAnnotations() != null && method.getParameterAnnotations().size() > i) {
                                    for (String paramAnn : method.getParameterAnnotations().get(i)) {
                                        if (paramAnn.startsWith("@WebParam")) {
                                            isWebMethod = true; 
                                            if (Boolean.parseBoolean(extractAnnotationAttribute(paramAnn, "header"))) {
                                                String headerName = extractAnnotationAttribute(paramAnn, "name");
                                                if (headerName == null || headerName.isEmpty()) headerName = paramName;
                                                String partName = extractAnnotationAttribute(paramAnn, "partName");
                                                methodRequestHeaders.put(headerName, paramType);
                                                if(partName !=null && !partName.isEmpty()) requestHeaderParts.add(partName);
                                                else requestHeaderParts.add(headerName); // Fallback to name if partName isn't there
                                            }
                                        } else if (paramAnn.startsWith("@SoapHeader")) { 
                                            isWebMethod = true;
                                            String qNameStr = extractAnnotationAttribute(paramAnn, "value");
                                            if (qNameStr != null && !qNameStr.isEmpty()) {
                                                soapRequestHeaderQNames.add(qNameStr);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Process return type annotation for @WebResult(header=true)
                        if (method.getReturnTypeAnnotations() != null) {
                            for (String retAnn : method.getReturnTypeAnnotations()) {
                                if (retAnn.startsWith("@WebResult")) {
                                     if (Boolean.parseBoolean(extractAnnotationAttribute(retAnn, "header"))) {
                                        String headerName = extractAnnotationAttribute(retAnn, "name");
                                        if (headerName == null || headerName.isEmpty()) headerName = "return"; 
                                        String partName = extractAnnotationAttribute(retAnn, "partName");
                                        methodResponseHeaders.put(headerName, method.getReturnType());
                                        if(partName !=null && !partName.isEmpty()) responseHeaderParts.add(partName);
                                        else responseHeaderParts.add(headerName); // Fallback to name if partName isn't there
                                    }
                                }
                            }
                        }

                        if (!methodRequestHeaders.isEmpty()) endpoint.setRequestHeaderParts(methodRequestHeaders);
                        // endpoint.setHeaders(methodRequestHeaders); // Setting to the specific requestHeaderParts instead for now
                        if (!methodResponseHeaders.isEmpty()) endpoint.setResponseHeaderParts(methodResponseHeaders);
                        if (!soapRequestHeaderQNames.isEmpty()) endpoint.setSoapRequestHeaderQNames(soapRequestHeaderQNames);


                        if (isWebMethod) {
                            // Set default operation name if still null
                            if (endpoint.getOperationName() == null) {
                                endpoint.setOperationName(method.getName());
                            }

                            // Construct path (conceptual for SOAP)
                            String serviceNameForPath = cmd.getAnnotations().stream()
                                .filter(ann -> ann.startsWith("@WebService"))
                                .map(ann -> extractAnnotationAttribute(ann, "serviceName"))
                                .filter(sn -> sn != null && !sn.isEmpty())
                                .findFirst().orElse(cmd.getName());
                            endpoint.setPath(serviceNameForPath + "/" + endpoint.getOperationName());


                            // Request/Response body types (simplified for now, can be enhanced by BARE/WRAPPED style)
                            // If @RequestWrapper is present, className could be the requestBodyType
                            if (endpoint.getRequestWrapperClassName() != null && !endpoint.getRequestWrapperClassName().isEmpty()) {
                                endpoint.setRequestBodyType(endpoint.getRequestWrapperClassName());
                            } else if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                                 // For BARE, might be the first non-header param. For WRAPPED, this is heuristic.
                                 // This part needs to be smarter based on parameterStyle.
                                if (!"BARE".equalsIgnoreCase(endpoint.getParameterStyle())) { // Wrapped or unknown
                                    if (method.getParameters().size() == 1 && (endpoint.getRequestHeaderParts() == null || !endpoint.getRequestHeaderParts().containsKey(method.getParameters().get(0).split(" ")[1]))) {
                                       endpoint.setRequestBodyType(method.getParameters().get(0).split(" ")[0]);
                                    } else if (method.getParameters().size() > 1) {
                                        // Heuristic: if multiple params and wrapped, often implies a generated wrapper not explicitly annotated.
                                        // For now, leave it null or use a placeholder like "Multiple Parameters (Wrapped)"
                                         endpoint.setRequestBodyType(method.getName() + "Request"); // Placeholder
                                    }
                                } else { // BARE
                                    for (int i=0; i < method.getParameters().size(); i++) {
                                        String paramFullName = method.getParameters().get(i);
                                        String currentParamName = paramFullName.contains(" ") ? paramFullName.substring(paramFullName.indexOf(" ") + 1) : ("arg" + i);
                                        boolean isHeader = false;
                                        if (endpoint.getRequestHeaderParts() != null && endpoint.getRequestHeaderParts().containsKey(currentParamName)) {
                                            isHeader = true;
                                        }
                                        if (!isHeader) {
                                            endpoint.setRequestBodyType(paramFullName.split(" ")[0]);
                                            break; // Found the first non-header param
                                        }
                                    }
                                }
                            }

                            // If @ResponseWrapper is present, className could be the responseBodyType
                            if (endpoint.getResponseWrapperClassName() != null && !endpoint.getResponseWrapperClassName().isEmpty()) {
                                endpoint.setResponseBodyType(endpoint.getResponseWrapperClassName());
                            } else {
                                if (!"void".equalsIgnoreCase(method.getReturnType()) && ("BARE".equalsIgnoreCase(endpoint.getParameterStyle()) || (endpoint.getResponseHeaderParts() == null || endpoint.getResponseHeaderParts().isEmpty())) ){
                                    endpoint.setResponseBodyType(method.getReturnType());
                                } else if (!"void".equalsIgnoreCase(method.getReturnType()) && !"BARE".equalsIgnoreCase(endpoint.getParameterStyle())) {
                                    // Wrapped response, not void, and no explicit @ResponseWrapper
                                    endpoint.setResponseBodyType(method.getName() + "Response"); // Placeholder
                                }
                            }
                            
                            endpoints.add(endpoint);
                            logger.debug("Extracted SOAP endpoint: {} from class {}", endpoint.getOperationName(), cmd.getName());
                        }
                    }
                }
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
            // Corrected string literal for finding a simple quote: changed \\" to \"
            int start = annotation.indexOf("\""); 
            int end = annotation.lastIndexOf("\""); 
            
            String pathVal = null;

            // Attempt to parse path="<value>"
            String pathKey = "path = \""; // Corrected: \\" to \"
            int pathKeyIndex = annotation.indexOf(pathKey);
            if (pathKeyIndex != -1) {
                int pathStart = pathKeyIndex + pathKey.length();
                int pathEnd = annotation.indexOf("\"", pathStart); // Corrected: \\" to \"
                if (pathEnd != -1 && pathStart <= pathEnd) {
                    pathVal = annotation.substring(pathStart, pathEnd);
                }
            }

            // If not found by path="...", attempt to parse value="<value>"
            if (pathVal == null) {
                String valueKey = "value = \""; // Corrected: \\" to \"
                int valueKeyIndex = annotation.indexOf(valueKey);
                if (valueKeyIndex != -1) {
                    int valueStart = valueKeyIndex + valueKey.length();
                    int valueEnd = annotation.indexOf("\"", valueStart); // Corrected: \\" to \"
                    if (valueEnd != -1 && valueStart <= valueEnd) {
                        pathVal = annotation.substring(valueStart, valueEnd);
                    }
                }
            }

            // Fallback: If no "path=..." or "value=..." attribute was found and parsed,
            // and if the initial generic quote search found something plausible.
            // This handles cases like @GetMapping("/just/the/path")
            if (pathVal == null && start != -1 && end != -1 && start < end) {
                 String potentialPath = annotation.substring(start + 1, end);
                 // Basic check to see if it looks like a simple path rather than complex attributes
                 if (!potentialPath.contains("=") && !potentialPath.contains(",") && !potentialPath.contains("\"")) {
                    pathVal = potentialPath;
                 }
            }
            
            if (pathVal != null) {
                endpoint.setPath(pathVal);
            }

        } catch (Exception e) {
            logger.warn("Could not parse REST path from annotation: {}. Error: {}", annotation, e.getMessage());
        }
    }

    private String extractAnnotationAttribute(String annotation, String attributeName) {
        // Basic parser for annotation attributes like namespace="...", localPart="..."
        // Example: @PayloadRoot(namespace = "http://www.jpworks.com/employee", localPart = "EmployeeByNameRequest")
        // Handles attributes like: attributeName = "value", attributeName = true, attributeName = some.CONSTANT_VALUE
        String attributePatternPrefix = attributeName + "\\s*=\\s*";
        int startIdx = annotation.indexOf(attributePatternPrefix);

        if (startIdx != -1) {
            int valueStart = startIdx + attributePatternPrefix.length();
            char firstChar = annotation.charAt(valueStart);

            if (firstChar == '"') { // String literal
                valueStart++; // Skip the opening quote
                int endIdx = annotation.indexOf('"', valueStart);
                if (endIdx != -1) {
                    return annotation.substring(valueStart, endIdx);
                }
            } else { // Boolean, enum, or other literal without quotes
                int endIdx = valueStart;
                while (endIdx < annotation.length() && !Character.isWhitespace(annotation.charAt(endIdx)) && annotation.charAt(endIdx) != ',' && annotation.charAt(endIdx) != ')') {
                    endIdx++;
                }
                return annotation.substring(valueStart, endIdx);
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

    // Helper class for storing extracted WSDL definition details
    // @lombok.Data // If Lombok was available and configured for internal use here
    class WsdlDefinitionInfo {
        private String beanName;    // Name of the @Bean method
        private String portName;    // From setPortTypeName()
        private String targetNamespace; // From setTargetNamespace()
        private String wsdlUrl;     // From setLocationUri()
        private String serviceName; // Tentatively the bean name, or if found otherwise

        // Standard getters and setters
        public String getBeanName() { return beanName; }
        public void setBeanName(String beanName) { this.beanName = beanName; }
        public String getPortName() { return portName; }
        public void setPortName(String portName) { this.portName = portName; }
        public String getTargetNamespace() { return targetNamespace; }
        public void setTargetNamespace(String targetNamespace) { this.targetNamespace = targetNamespace; }
        public String getWsdlUrl() { return wsdlUrl; }
        public void setWsdlUrl(String wsdlUrl) { this.wsdlUrl = wsdlUrl; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    }
    // Make sure this class is either a static nested class if inside EndpointExtractorServiceImpl, or in its own file if public.
    // For simplicity here, assuming it can be a non-public helper class in the same file or a static nested one.
} 