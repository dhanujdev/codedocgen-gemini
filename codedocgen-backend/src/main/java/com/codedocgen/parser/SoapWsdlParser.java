package com.codedocgen.parser;

import org.springframework.stereotype.Component;
import com.codedocgen.model.EndpointMetadata;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SoapWsdlParser {
    // Parse WSDL XML content and extract SOAP operations as EndpointMetadata
    public List<EndpointMetadata> parseWsdl(String wsdlContent, String wsdlPath) {
        List<EndpointMetadata> endpoints = new ArrayList<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(wsdlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            String wsdlTargetNamespace = doc.getDocumentElement().getAttribute("targetNamespace");

            NodeList bindingNodes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "binding");
            for (int i = 0; i < bindingNodes.getLength(); i++) {
                Element bindingElement = (Element) bindingNodes.item(i);
                String bindingName = bindingElement.getAttribute("name");
                String portTypeNameWithPrefix = bindingElement.getAttribute("type");
                String portTypeName = portTypeNameWithPrefix.contains(":") ? portTypeNameWithPrefix.substring(portTypeNameWithPrefix.indexOf(":") + 1) : portTypeNameWithPrefix;

                String bindingSoapStyle = null;
                NodeList soapBindingElements = bindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "binding");
                if (soapBindingElements.getLength() == 0) {
                    soapBindingElements = bindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap12/", "binding");
                }
                if (soapBindingElements.getLength() > 0) {
                    bindingSoapStyle = ((Element) soapBindingElements.item(0)).getAttribute("style");
                }

                NodeList operationBindingNodes = bindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "operation");
                for (int j = 0; j < operationBindingNodes.getLength(); j++) {
                    Element opBindingElement = (Element) operationBindingNodes.item(j);
                    String operationName = opBindingElement.getAttribute("name");

                    EndpointMetadata endpoint = new EndpointMetadata();
                    endpoint.setType("SOAP");
                    endpoint.setHttpMethod("SOAP"); // Generic for SOAP
                    endpoint.setOperationName(operationName);
                    endpoint.setPortTypeName(portTypeName); // From binding's type attribute
                    endpoint.setPath(portTypeName + "/" + operationName); // Conceptual path
                    endpoint.setConsumes("application/soap+xml");
                    endpoint.setProduces("application/soap+xml");
                    endpoint.setWsdlUrl(wsdlPath);
                    endpoint.setTargetNamespace(wsdlTargetNamespace); // Set TNS for the endpoint

                    // Attempt to find corresponding service and port for this binding
                    NodeList serviceNodes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "service");
                    String wsdlServiceName = null;
                    String wsdlPortName = null;
                    outerLoop:
                    for (int k = 0; k < serviceNodes.getLength(); k++) {
                        Element serviceElement = (Element) serviceNodes.item(k);
                        NodeList portNodes = serviceElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "port");
                        for (int l = 0; l < portNodes.getLength(); l++) {
                            Element portElement = (Element) portNodes.item(l);
                            String portBinding = portElement.getAttribute("binding");
                            String portBindingName = portBinding.contains(":") ? portBinding.substring(portBinding.indexOf(":") + 1) : portBinding;
                            if (bindingName.equals(portBindingName)) {
                                wsdlServiceName = serviceElement.getAttribute("name");
                                wsdlPortName = portElement.getAttribute("name");
                                break outerLoop;
                            }
                        }
                    }
                    endpoint.setServiceName(wsdlServiceName);
                    endpoint.setPortName(wsdlPortName);

                    String opSoapAction = null;
                    String opSoapStyle = bindingSoapStyle; // Default to binding style
                    NodeList soapOpElements = opBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "operation");
                    if (soapOpElements.getLength() == 0) {
                        soapOpElements = opBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap12/", "operation");
                    }
                    if (soapOpElements.getLength() > 0) {
                        Element soapOpElement = (Element) soapOpElements.item(0);
                        opSoapAction = soapOpElement.getAttribute("soapAction");
                        if (soapOpElement.hasAttribute("style")) {
                            opSoapStyle = soapOpElement.getAttribute("style");
                        }
                    }
                    endpoint.setSoapAction(opSoapAction);
                    endpoint.setStyle(opSoapStyle != null ? opSoapStyle.toUpperCase() : null);

                    // Extract input message, output message, and body 'use' attribute
                    // Input
                    NodeList inputBindingNodes = opBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
                    if (inputBindingNodes.getLength() > 0) {
                        Element inputBindingElement = (Element) inputBindingNodes.item(0);
                        NodeList soapBodyElements = inputBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "body");
                        if (soapBodyElements.getLength() == 0) {
                            soapBodyElements = inputBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap12/", "body");
                        }
                        if (soapBodyElements.getLength() > 0) {
                            endpoint.setUse(((Element) soapBodyElements.item(0)).getAttribute("use").toUpperCase());
                            // Potentially extract parts from soap:body parts attribute if needed for requestBodyType refinement
                        }
                    }
                    // Output (similar logic for 'use', could also set response parameterStyle if different)
                    NodeList outputBindingNodes = opBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
                    if (outputBindingNodes.getLength() > 0) {
                        Element outputBindingElement = (Element) outputBindingNodes.item(0);
                        NodeList soapBodyElements = outputBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "body");
                        if (soapBodyElements.getLength() == 0) {
                             soapBodyElements = outputBindingElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap12/", "body");
                        }
                        if (soapBodyElements.getLength() > 0) {
                            // Assuming 'use' is consistent for request/response for now
                            if (endpoint.getUse() == null) { // Set if not set by input
                                endpoint.setUse(((Element) soapBodyElements.item(0)).getAttribute("use").toUpperCase());
                            }
                        }
                    }
                    
                    // Correlate with portType to get message names for request/responseBodyType
                    Element portTypeElement = findPortTypeElement(doc, portTypeName);
                    if (portTypeElement != null) {
                        Element operationElementInPortType = findOperationInPortType(portTypeElement, operationName);
                        if (operationElementInPortType != null) {
                            NodeList inputMsgList = operationElementInPortType.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
                            if (inputMsgList.getLength() > 0) {
                                endpoint.setRequestBodyType(((Element) inputMsgList.item(0)).getAttribute("message"));
                            }
                            NodeList outputMsgList = operationElementInPortType.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
                            if (outputMsgList.getLength() > 0) {
                                endpoint.setResponseBodyType(((Element) outputMsgList.item(0)).getAttribute("message"));
                            }
                            // Clean up produces field from old logic
                            NodeList docNodes = operationElementInPortType.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "documentation");
                            if (docNodes.getLength() > 0) {
                                // endpoint.setDescription(docNodes.item(0).getTextContent().trim()); // Add description if available
                            }
                        }
                    }
                    endpoints.add(endpoint);
                }
            }
        } catch (Exception e) {
            System.err.println("WSDL parsing error: " + e.getMessage());
            e.printStackTrace(); // More detailed error
        }
        return endpoints;
    }

    // Helper to find a specific portType element by name
    private Element findPortTypeElement(Document doc, String portTypeName) {
        NodeList portTypes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "portType");
        for (int i = 0; i < portTypes.getLength(); i++) {
            Element pt = (Element) portTypes.item(i);
            String name = pt.getAttribute("name");
            if (name.equals(portTypeName)) {
                return pt;
            }
        }
        return null;
    }

    // Helper to find a specific operation within a portType element
    private Element findOperationInPortType(Element portTypeElement, String operationName) {
        NodeList operations = portTypeElement.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "operation");
        for (int i = 0; i < operations.getLength(); i++) {
            Element op = (Element) operations.item(i);
            if (op.getAttribute("name").equals(operationName)) {
                return op;
            }
        }
        return null;
    }
} 