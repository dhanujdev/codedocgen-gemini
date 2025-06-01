package com.codedocgen.model;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata for a REST API endpoint
 */
@NoArgsConstructor
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

    private String controllerName;
    private String packageName;
    private String description;
    private String returnType;
    private List<ParameterMetadata> parameters = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    
    // Security-related metadata
    private boolean isSecured;
    private List<String> securityRoles = new ArrayList<>();
    private List<String> securityScopes = new ArrayList<>();
    
    // For documentation
    private String javadoc;
    private String responseDescription;
    private List<String> possibleResponseCodes = new ArrayList<>();
    
    public void addParameter(ParameterMetadata parameter) {
        parameters.add(parameter);
    }
    
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }
    
    public void addSecurityRole(String role) {
        securityRoles.add(role);
    }
    
    public void addSecurityScope(String scope) {
        securityScopes.add(scope);
    }
    
    public void addPossibleResponseCode(String responseCode) {
        possibleResponseCodes.add(responseCode);
    }

    /**
     * Get the path of this endpoint
     * @return The path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Set the path of this endpoint
     * @param path The path
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Get the HTTP method of this endpoint
     * @return The HTTP method
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    
    /**
     * Set the HTTP method of this endpoint
     * @param httpMethod The HTTP method
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    /**
     * Get the produces content type of this endpoint
     * @return The produces content type
     */
    public String getProduces() {
        return produces;
    }
    
    /**
     * Set the produces content type of this endpoint
     * @param produces The produces content type
     */
    public void setProduces(String produces) {
        this.produces = produces;
    }
    
    /**
     * Get the consumes content type of this endpoint
     * @return The consumes content type
     */
    public String getConsumes() {
        return consumes;
    }
    
    /**
     * Set the consumes content type of this endpoint
     * @param consumes The consumes content type
     */
    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }
    
    /**
     * Get the type of this endpoint
     * @return The type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the type of this endpoint
     * @param type The type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Get the handler method of this endpoint
     * @return The handler method
     */
    public String getHandlerMethod() {
        return handlerMethod;
    }
    
    /**
     * Set the handler method of this endpoint
     * @param handlerMethod The handler method
     */
    public void setHandlerMethod(String handlerMethod) {
        this.handlerMethod = handlerMethod;
    }
    
    /**
     * Get the WSDL URL of this endpoint
     * @return The WSDL URL
     */
    public String getWsdlUrl() {
        return wsdlUrl;
    }
    
    /**
     * Set the WSDL URL of this endpoint
     * @param wsdlUrl The WSDL URL
     */
    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }
    
    /**
     * Get the operation name of this endpoint
     * @return The operation name
     */
    public String getOperationName() {
        return operationName;
    }
    
    /**
     * Set the operation name of this endpoint
     * @param operationName The operation name
     */
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
    
    /**
     * Get the target namespace of this endpoint
     * @return The target namespace
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }
    
    /**
     * Set the target namespace of this endpoint
     * @param targetNamespace The target namespace
     */
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
    
    /**
     * Get the request body type of this endpoint
     * @return The request body type
     */
    public String getRequestBodyType() {
        return requestBodyType;
    }
    
    /**
     * Set the request body type of this endpoint
     * @param requestBodyType The request body type
     */
    public void setRequestBodyType(String requestBodyType) {
        this.requestBodyType = requestBodyType;
    }
    
    /**
     * Get the response body type of this endpoint
     * @return The response body type
     */
    public String getResponseBodyType() {
        return responseBodyType;
    }
    
    /**
     * Set the response body type of this endpoint
     * @param responseBodyType The response body type
     */
    public void setResponseBodyType(String responseBodyType) {
        this.responseBodyType = responseBodyType;
    }

    /**
     * Get the class name of this endpoint
     * @return The class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Set the class name of this endpoint
     * @param className The class name
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
    /**
     * Get the method name of this endpoint
     * @return The method name
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * Set the method name of this endpoint
     * @param methodName The method name
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    /**
     * Get the headers of this endpoint
     * @return The headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Set the headers of this endpoint
     * @param headers The headers
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    /**
     * Get the SOAP action of this endpoint
     * @return The SOAP action
     */
    public String getSoapAction() {
        return soapAction;
    }
    
    /**
     * Set the SOAP action of this endpoint
     * @param soapAction The SOAP action
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }
    
    /**
     * Get the style of this endpoint
     * @return The style
     */
    public String getStyle() {
        return style;
    }
    
    /**
     * Set the style of this endpoint
     * @param style The style
     */
    public void setStyle(String style) {
        this.style = style;
    }
    
    /**
     * Get the use of this endpoint
     * @return The use
     */
    public String getUse() {
        return use;
    }
    
    /**
     * Set the use of this endpoint
     * @param use The use
     */
    public void setUse(String use) {
        this.use = use;
    }
    
    /**
     * Get the port type name of this endpoint
     * @return The port type name
     */
    public String getPortTypeName() {
        return portTypeName;
    }
    
    /**
     * Set the port type name of this endpoint
     * @param portTypeName The port type name
     */
    public void setPortTypeName(String portTypeName) {
        this.portTypeName = portTypeName;
    }
    
    /**
     * Get the service name of this endpoint
     * @return The service name
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Set the service name of this endpoint
     * @param serviceName The service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    /**
     * Get the port name of this endpoint
     * @return The port name
     */
    public String getPortName() {
        return portName;
    }
    
    /**
     * Set the port name of this endpoint
     * @param portName The port name
     */
    public void setPortName(String portName) {
        this.portName = portName;
    }
    
    /**
     * Get the parameter style of this endpoint
     * @return The parameter style
     */
    public String getParameterStyle() {
        return parameterStyle;
    }
    
    /**
     * Set the parameter style of this endpoint
     * @param parameterStyle The parameter style
     */
    public void setParameterStyle(String parameterStyle) {
        this.parameterStyle = parameterStyle;
    }
    
    /**
     * Get the request wrapper name of this endpoint
     * @return The request wrapper name
     */
    public String getRequestWrapperName() {
        return requestWrapperName;
    }
    
    /**
     * Set the request wrapper name of this endpoint
     * @param requestWrapperName The request wrapper name
     */
    public void setRequestWrapperName(String requestWrapperName) {
        this.requestWrapperName = requestWrapperName;
    }
    
    /**
     * Get the request wrapper class name of this endpoint
     * @return The request wrapper class name
     */
    public String getRequestWrapperClassName() {
        return requestWrapperClassName;
    }
    
    /**
     * Set the request wrapper class name of this endpoint
     * @param requestWrapperClassName The request wrapper class name
     */
    public void setRequestWrapperClassName(String requestWrapperClassName) {
        this.requestWrapperClassName = requestWrapperClassName;
    }
    
    /**
     * Get the response wrapper name of this endpoint
     * @return The response wrapper name
     */
    public String getResponseWrapperName() {
        return responseWrapperName;
    }
    
    /**
     * Set the response wrapper name of this endpoint
     * @param responseWrapperName The response wrapper name
     */
    public void setResponseWrapperName(String responseWrapperName) {
        this.responseWrapperName = responseWrapperName;
    }
    
    /**
     * Get the response wrapper class name of this endpoint
     * @return The response wrapper class name
     */
    public String getResponseWrapperClassName() {
        return responseWrapperClassName;
    }
    
    /**
     * Set the response wrapper class name of this endpoint
     * @param responseWrapperClassName The response wrapper class name
     */
    public void setResponseWrapperClassName(String responseWrapperClassName) {
        this.responseWrapperClassName = responseWrapperClassName;
    }
    
    /**
     * Get the SOAP request header QNames of this endpoint
     * @return The SOAP request header QNames
     */
    public List<String> getSoapRequestHeaderQNames() {
        return soapRequestHeaderQNames;
    }
    
    /**
     * Set the SOAP request header QNames of this endpoint
     * @param soapRequestHeaderQNames The SOAP request header QNames
     */
    public void setSoapRequestHeaderQNames(List<String> soapRequestHeaderQNames) {
        this.soapRequestHeaderQNames = soapRequestHeaderQNames;
    }
    
    /**
     * Get the path variables of this endpoint
     * @return The path variables
     */
    public List<String> getPathVariables() {
        return pathVariables;
    }
    
    /**
     * Set the path variables of this endpoint
     * @param pathVariables The path variables
     */
    public void setPathVariables(List<String> pathVariables) {
        this.pathVariables = pathVariables;
    }
    
    /**
     * Get the request parameters of this endpoint
     * @return The request parameters
     */
    public List<String> getRequestParameters() {
        return requestParameters;
    }
    
    /**
     * Set the request parameters of this endpoint
     * @param requestParameters The request parameters
     */
    public void setRequestParameters(List<String> requestParameters) {
        this.requestParameters = requestParameters;
    }
    
    /**
     * Get the HTTP status of this endpoint
     * @return The HTTP status
     */
    public String getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Set the HTTP status of this endpoint
     * @param httpStatus The HTTP status
     */
    public void setHttpStatus(String httpStatus) {
        this.httpStatus = httpStatus;
    }
    
    /**
     * Get the request parameter details of this endpoint
     * @return The request parameter details
     */
    public Map<String, Map<String, String>> getRequestParamDetails() {
        return requestParamDetails;
    }
    
    /**
     * Set the request parameter details of this endpoint
     * @param requestParamDetails The request parameter details
     */
    public void setRequestParamDetails(Map<String, Map<String, String>> requestParamDetails) {
        this.requestParamDetails = requestParamDetails;
    }
} 