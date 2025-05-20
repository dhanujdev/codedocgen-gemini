package com.codedocgen.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class EndpointMetadata {
    private String path;
    private String httpMethod; // GET, POST, PUT, DELETE, etc. or SOAP Action
    private String handlerMethod; // Fully qualified name of the controller/handler method
    private String className; // Class containing the handler method (e.g., controller or SOAP service class)
    private String methodName; // Name of the method handling the endpoint
    private String requestBodyType; // Class name of request body
    private String responseBodyType; // Class name of response body
    private List<String> pathVariables;
    private List<String> requestParameters;
    private String consumes; // e.g., application/json, application/xml
    private String produces; // e.g., application/json, application/xml
    private String type; // REST or SOAP
    private String wsdlUrl; // For SOAP endpoints
    private String operationName; // For SOAP endpoints
    private String httpStatus; // e.g., 200, 201, 404, etc. (from @ResponseStatus)
    private Map<String, Map<String, String>> requestParamDetails; // param name -> {required, defaultValue}
    // Add more fields as needed, e.g., for security, headers, example requests/responses
} 