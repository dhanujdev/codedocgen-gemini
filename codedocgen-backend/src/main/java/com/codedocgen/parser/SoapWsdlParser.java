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

            // Get targetNamespace
            String targetNamespace = doc.getDocumentElement().getAttribute("targetNamespace");

            // Find all portType/operation elements
            NodeList portTypes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "portType");
            for (int i = 0; i < portTypes.getLength(); i++) {
                Element portType = (Element) portTypes.item(i);
                NodeList operations = portType.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "operation");
                for (int j = 0; j < operations.getLength(); j++) {
                    Element operation = (Element) operations.item(j);
                    String opName = operation.getAttribute("name");
                    EndpointMetadata endpoint = new EndpointMetadata();
                    endpoint.setType("SOAP");
                    endpoint.setHttpMethod("SOAP");
                    endpoint.setOperationName(opName);
                    endpoint.setPath(targetNamespace + "/" + opName);
                    endpoint.setConsumes("application/soap+xml");
                    endpoint.setProduces("application/soap+xml");
                    endpoint.setWsdlUrl(wsdlPath);

                    // Try to extract input/output message names
                    NodeList inputList = operation.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
                    if (inputList.getLength() > 0) {
                        Element input = (Element) inputList.item(0);
                        String message = input.getAttribute("message");
                        endpoint.setRequestBodyType(message);
                    }
                    NodeList outputList = operation.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
                    if (outputList.getLength() > 0) {
                        Element output = (Element) outputList.item(0);
                        String message = output.getAttribute("message");
                        endpoint.setResponseBodyType(message);
                    }

                    // Try to extract documentation if present
                    NodeList docNodes = operation.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "documentation");
                    if (docNodes.getLength() > 0) {
                        endpoint.setProduces(endpoint.getProduces() + " (" + docNodes.item(0).getTextContent().trim() + ")");
                    }

                    endpoints.add(endpoint);
                }
            }
        } catch (Exception e) {
            // Log and continue
            System.err.println("WSDL parsing error: " + e.getMessage());
        }
        return endpoints;
    }
} 