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
    private String wsdlUrl; // For SOAP, URL to the WSDL file if specified
    private String operationName; // For SOAP endpoints
    private String httpStatus; // e.g., 200, 201, 404, etc. (from @ResponseStatus)
    private Map<String, Map<String, String>> requestParamDetails; // param name -> {required, defaultValue}

    // SOAP Specific fields
    private String serviceName; // For SOAP, the service name
    private String portName; // For SOAP, the port name
    private String targetNamespace; // For SOAP, the target namespace
    private String soapAction; // For SOAP, the SOAPAction
    private Map<String, String> headers; // HTTP Headers from @WebParam(header=true) or similar
    private String style; // For SOAP (@SOAPBinding style: DOCUMENT or RPC)
    private String use; // For SOAP (@SOAPBinding use: LITERAL or ENCODED)
    private String parameterStyle; // For SOAP (@SOAPBinding parameterStyle: BARE or WRAPPED)
    private String requestWrapperName; // Local name of the request wrapper element, if @RequestWrapper is used
    private String requestWrapperClassName; // Class name of the request wrapper, if @RequestWrapper is used
    private String responseWrapperName; // Local name of the response wrapper element, if @ResponseWrapper is used
    private String responseWrapperClassName; // Class name of the response wrapper, if @ResponseWrapper is used
    private String responseWrapperNamespace; // Namespace for the response wrapper element
    private List<String> soapRequestHeaderQNames; // For Spring-WS, QNames of SOAP headers declared via @SoapHeader on parameters
    private String portTypeName; // For JAX-WS, the wsdl:portType name, usually from @WebService(name=...)

    // For SOAP headers (distinguished from general HTTP headers)
    private Map<String, String> requestHeaderParts; // e.g. from @WebParam(header=true, name="X", partName="Y") partName as key, type as value
    private Map<String, String> responseHeaderParts; // e.g. from @WebResult(header=true, name="X", partName="Y")

    // Potentially other SOAP details like specific JAX-WS/Spring-WS annotations, policies etc.

    // Add more fields as needed, e.g., for security, headers, example requests/responses
} 