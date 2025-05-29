import React, { useState, useEffect } from 'react';
import { Box, Typography, Link, Paper, Alert, List, ListItem, ListItemText, Divider, Chip } from '@mui/material';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css'; // Import default swagger-ui styles

// Align with API_BASE_URL in App.js or api.ts if it exists elsewhere
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const MAX_XSD_RECURSION_DEPTH = 5; // Max depth for XSD element/type resolution

// Helper function to parse WSDL operations
const parseWsdlOperations = (wsdlString, xsdFilesContentMap) => {
  const operations = [];
  const parsedXsdDocs = {}; // Store parsed XSDs by schemaLocation or namespace

  try {
    const parser = new DOMParser();
    const wsdlXmlDoc = parser.parseFromString(wsdlString, 'text/xml');

    const parserError = wsdlXmlDoc.getElementsByTagName('parsererror');
    if (parserError.length > 0) {
      console.error('[ApiSpecsPage] WSDL parsing error:', parserError[0].textContent);
      return [];
    }

    // Attempt to parse imported XSDs
    if (xsdFilesContentMap && typeof xsdFilesContentMap === 'object') {
      console.log('[ApiSpecsPage] processImports: xsdFilesContentMap received:', JSON.parse(JSON.stringify(xsdFilesContentMap))); // Deep copy for logging
      const imports = wsdlXmlDoc.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'import');
      const altImports = wsdlXmlDoc.getElementsByTagName('xsd:import'); // Fallback

      const processImports = (importNodes) => {
        console.log(`[ApiSpecsPage] processImports: Found ${importNodes.length} import nodes.`);
        for (let i = 0; i < importNodes.length; i++) {
          const schemaLocation = importNodes[i].getAttribute('schemaLocation');
          const namespace = importNodes[i].getAttribute('namespace');
          console.log(`[ApiSpecsPage] processImports: Import node ${i + 1}: schemaLocation="${schemaLocation}", namespace="${namespace}"`);

          if (schemaLocation) {
            let xsdKey = schemaLocation; // Try direct match first
            let xsdContent = xsdFilesContentMap[xsdKey];

            if (!xsdContent) {
              // If direct match fails, try to find a key that ends with the schemaLocation
              const matchingKey = Object.keys(xsdFilesContentMap).find(key => key.endsWith(schemaLocation));
              if (matchingKey) {
                xsdKey = matchingKey;
                xsdContent = xsdFilesContentMap[xsdKey];
                console.log(`[ApiSpecsPage] processImports: Found XSD for "${schemaLocation}" using key "${xsdKey}" by endsWith match.`);
              }
            } else {
                 console.log(`[ApiSpecsPage] processImports: Found XSD for "${schemaLocation}" by direct key match.`);
            }

            if (xsdContent) {
              try {
                const xsdXmlDoc = parser.parseFromString(xsdContent, 'text/xml');
                const xsdParserError = xsdXmlDoc.getElementsByTagName('parsererror');
                if (xsdParserError.length > 0) {
                  console.error(`[ApiSpecsPage] processImports: Error parsing XSD for ${xsdKey}:`, xsdParserError[0].textContent);
                } else {
                  // Store by namespace if available, otherwise by schemaLocation (or the matched key)
                  const storeKey = namespace || xsdKey;
                  parsedXsdDocs[storeKey] = xsdXmlDoc;
                  console.log(`[ApiSpecsPage] processImports: Successfully parsed and stored XSD for "${storeKey}" (Original schemaLocation: "${schemaLocation}")`);
                  // Log targetNamespace of the parsed XSD
                  const schemaNode = xsdXmlDoc.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'schema')[0] || xsdXmlDoc.getElementsByTagName('schema')[0];
                  if (schemaNode) {
                    console.log(`[ApiSpecsPage] processImports: Parsed XSD targetNamespace: ${schemaNode.getAttribute('targetNamespace')}`);
                  }
                }
              } catch (e) {
                console.error(`[ApiSpecsPage] processImports: Exception parsing XSD content for ${xsdKey}:`, e);
              }
            } else {
              console.warn(`[ApiSpecsPage] processImports: XSD content for schemaLocation '${schemaLocation}' NOT FOUND in xsdFilesContentMap. Available keys:`, Object.keys(xsdFilesContentMap));
            }
          } else {
            console.warn('[ApiSpecsPage] processImports: Import node has no schemaLocation attribute.');
          }
        }
      };
      processImports(imports);
      if (imports.length === 0 && altImports.length > 0) processImports(altImports);
    } else {
      console.warn('[ApiSpecsPage] parseWsdlOperations: xsdFilesContentMap is null, not an object, or empty.');
    }


    const portTypes = wsdlXmlDoc.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'portType');
    if (!portTypes || portTypes.length === 0) {
        const genericPortTypes = wsdlXmlDoc.getElementsByTagName('portType');
        for (let i = 0; i < genericPortTypes.length; i++) {
            handlePortType(genericPortTypes[i], operations, wsdlXmlDoc, parsedXsdDocs);
        }
    } else {
        for (let i = 0; i < portTypes.length; i++) {
            handlePortType(portTypes[i], operations, wsdlXmlDoc, parsedXsdDocs);
        }
    }

  } catch (e) {
    console.error('[ApiSpecsPage] Error parsing WSDL or XSDs:', e);
  }
  console.log('[ApiSpecsPage] parseWsdlOperations: Final parsedXsdDocs:', JSON.parse(JSON.stringify(parsedXsdDocs)));
  return operations;
};

const handlePortType = (portTypeNode, operations, wsdlXmlDoc, parsedXsdDocs) => {
    const ops = portTypeNode.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'operation');
    if (!ops || ops.length === 0) {
        const genericOps = portTypeNode.getElementsByTagName('operation');
        for (let j = 0; j < genericOps.length; j++) {
            extractOperationDetails(genericOps[j], operations, wsdlXmlDoc, parsedXsdDocs);
        }
    } else {
        for (let j = 0; j < ops.length; j++) {
            extractOperationDetails(ops[j], operations, wsdlXmlDoc, parsedXsdDocs);
        }
    }
};

// Helper to extract prefix and local name
const splitQName = (qName) => {
    if (!qName || !qName.includes(':')) return { prefix: null, localName: qName };
    const parts = qName.split(':');
    return { prefix: parts[0], localName: parts[1] };
};

// Helper to get namespace URI for a prefix from the WSDL doc
const getNamespaceUri = (prefix, wsdlXmlDoc) => {
    if (!prefix || !wsdlXmlDoc || !wsdlXmlDoc.documentElement) return null;
    // Check common XSD/WSDL namespaces first
    if (prefix === 'xs' || prefix === 'xsd') return 'http://www.w3.org/2001/XMLSchema';
    if (prefix === 'wsdl') return 'http://schemas.xmlsoap.org/wsdl/';
    return wsdlXmlDoc.documentElement.getAttributeNS('http://www.w3.org/2000/xmlns/', prefix);
};

const getElementDetailsFromXSD = (elementQName, wsdlXmlDoc, parsedXsdDocs, recursionDepth = 0) => {
    if (recursionDepth > MAX_XSD_RECURSION_DEPTH) {
        console.warn(`[ApiSpecsPage] Max recursion depth reached for XSD element: ${elementQName}`);
        const { localName } = splitQName(elementQName);
        return [{ name: localName, type: elementQName, kind: 'element (recursion limit)' }];
    }

    if (!elementQName) return [];
    const { prefix, localName } = splitQName(elementQName);
    const targetNamespace = getNamespaceUri(prefix, wsdlXmlDoc);
    
    let xsdDocToSearch = null;
    if (targetNamespace && parsedXsdDocs[targetNamespace]) {
        xsdDocToSearch = parsedXsdDocs[targetNamespace];
    } else if (parsedXsdDocs[elementQName]) { // Fallback if stored by full QName (less likely for elements)
        xsdDocToSearch = parsedXsdDocs[elementQName];
    } else if (Object.keys(parsedXsdDocs).length > 0) {
        // If only one XSD, or if targetNamespace is null, try searching in all available XSDs
        // This is a simplified heuristic. Proper XSD handling would involve matching targetNamespace of XSD.
        xsdDocToSearch = Object.values(parsedXsdDocs)[0]; // Default to the first one if specific match fails
        if (Object.keys(parsedXsdDocs).length > 1 && !targetNamespace) {
             console.warn(`[ApiSpecsPage] Ambiguous XSD for element ${elementQName} as targetNamespace is unknown and multiple XSDs exist. Using first one.`);
        }
    }

    if (!xsdDocToSearch) {
        console.warn(`[ApiSpecsPage] No suitable XSD found to search for element: ${elementQName} (targetNS: ${targetNamespace})`);
        return [];
    }

    const details = [];
    // Try with NS, then without
    let elementDef = xsdDocToSearch.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'element');
    if (!elementDef || elementDef.length === 0) {
         elementDef = xsdDocToSearch.getElementsByTagName('xsd:element');
         if (!elementDef || elementDef.length === 0) {
            elementDef = xsdDocToSearch.getElementsByTagName('element'); // most generic
         }
    }
    
    let foundElementNode = null;
    for (let i = 0; i < elementDef.length; i++) {
        if (elementDef[i].getAttribute('name') === localName) {
            foundElementNode = elementDef[i];
            break;
        }
    }

    if (foundElementNode) {
        console.log(`[ApiSpecsPage] Found XSD element definition for: ${localName} (Depth: ${recursionDepth})`);
        const complexTypeNodes = foundElementNode.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'complexType');
        // Add fallbacks for complexType without NS or with xsd: prefix
        const complexTypeNode = complexTypeNodes[0] || foundElementNode.getElementsByTagName('xsd:complexType')[0] || foundElementNode.getElementsByTagName('complexType')[0];


        if (complexTypeNode) {
            // Look for attributes
            const attributes = complexTypeNode.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'attribute');
            const altAttributes = complexTypeNode.getElementsByTagName('xsd:attribute');

            const processAttributes = (attrNodes) => {
                for (let i = 0; i < attrNodes.length; i++) {
                    details.push({
                        name: attrNodes[i].getAttribute('name'),
                        type: attrNodes[i].getAttribute('type') || 'xs:string', // Default type if not specified
                        kind: 'attribute',
                        use: attrNodes[i].getAttribute('use') || 'optional'
                    });
                }
            };
            processAttributes(attributes);
            if(attributes.length === 0 && altAttributes.length > 0) processAttributes(altAttributes);


            // Look for child elements in a sequence
            const sequenceNodes = complexTypeNode.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'sequence');
            const altSequenceNodes = complexTypeNode.getElementsByTagName('xsd:sequence');
             const sequenceNode = sequenceNodes[0] || altSequenceNodes[0] || complexTypeNode.getElementsByTagName('sequence')[0];

            if (sequenceNode) {
                const childElementDefs = sequenceNode.getElementsByTagNameNS('http://www.w3.org/2001/XMLSchema', 'element');
                const altChildElementDefs = sequenceNode.getElementsByTagName('xsd:element');

                const processChildElements = (childElems) => {
                     for (let i = 0; i < childElems.length; i++) {
                        const childNode = childElems[i];
                        const ref = childNode.getAttribute('ref');
                        const nameAttr = childNode.getAttribute('name');
                        const typeAttr = childNode.getAttribute('type');
                        const minOccurs = childNode.getAttribute('minOccurs') || '1';
                        const maxOccurs = childNode.getAttribute('maxOccurs') || '1';

                        if (ref) {
                            const { localName: refLocalName } = splitQName(ref);
                            let childrenDetails = [];
                            if (recursionDepth < MAX_XSD_RECURSION_DEPTH) {
                                childrenDetails = getElementDetailsFromXSD(ref, wsdlXmlDoc, parsedXsdDocs, recursionDepth + 1);
                            } else {
                                console.warn(`[ApiSpecsPage] Max recursion depth reached for XSD ref: ${ref}`);
                            }
                            details.push({
                                name: refLocalName,
                                type: ref,
                                kind: 'element',
                                minOccurs,
                                maxOccurs,
                                isRef: true,
                                children: childrenDetails
                            });
                        } else if (nameAttr) {
                            // TODO: Future enhancement - if typeAttr refers to a global complexType, expand it.
                            // This would involve:
                            // 1. Parsing the typeAttr (e.g., "ns:MyCustomType") to get namespace and local name.
                            // 2. Looking up the namespace URI from the WSDL/XSD imports.
                            // 3. Searching all parsed XSD documents for a global <xsd:complexType name="MyCustomType"> or <xsd:simpleType name="MyCustomType">
                            //    matching the target namespace.
                            // 4. If found, recursively call a function similar to getElementDetailsFromXSD or a new function
                            //    (e.g., getTypeDetailsFromXSD) to parse its structure (sequence, choice, attributes etc.).
                            // 5. This needs careful handling of recursion depth and already visited types to prevent infinite loops.
                            // For now, relies on the type being simple or the complexType being inline for further expansion of attributes/elements.
                            // Acknowledged as a complex feature for future iteration.
                            details.push({
                                name: nameAttr,
                                type: typeAttr || 'xs:string',
                                kind: 'element',
                                minOccurs,
                                maxOccurs,
                                children: [] // Placeholder for future type expansion
                            });
                        }
                    }
                };
                processChildElements(childElementDefs);
                if(childElementDefs.length === 0 && altChildElementDefs.length > 0) processChildElements(altChildElementDefs);
            }
        } else {
             // If element has a 'type' attribute, it might reference a complexType defined elsewhere
            const elementType = foundElementNode.getAttribute('type');
            if (elementType) {
                 details.push({ name: localName, type: elementType, kind: 'element (referenced type)'});
                 // Potentially recurse or look up this type: getElementDetailsFromXSD(elementType, wsdlXmlDoc, parsedXsdDocs)
                 // For now, just show the type.
            } else {
                console.log(`[ApiSpecsPage] XSD element ${localName} is simple type or has no inline/referenced complexType.`);
                 details.push({ name: localName, type: 'Simple or Undefined Structure', kind: 'element'});
            }
        }
    } else {
         console.warn(`[ApiSpecsPage] XSD element definition NOT FOUND for: ${localName} in the searched XSD doc.`);
    }
    return details;
};


const extractOperationDetails = (opNode, operations, wsdlXmlDoc, parsedXsdDocs) => {
    const name = opNode.getAttribute('name');
    let inputMessageName = '';
    let outputMessageName = '';
    let documentation = '';
    let inputPart = null;
    let outputPart = null;
    let inputElementDetails = [];
    let outputElementDetails = [];

    const findPartForMessage = (messageNameValue) => {
        if (!messageNameValue || !wsdlXmlDoc) return null;
        const msgName = messageNameValue.includes(':') ? messageNameValue.split(':')[1] : messageNameValue;
        let messageNode = null;
        // Prefer namespace-aware search
        const messages = wsdlXmlDoc.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'message');
        for (let i = 0; i < messages.length; i++) {
            if ((messages[i].getAttribute('name') === msgName) || (messages[i].getAttribute('name') === messageNameValue)) {
                messageNode = messages[i];
                break;
            }
        }
        // Fallback to generic tag search if not found
        if (!messageNode) {
            const genericMessages = wsdlXmlDoc.getElementsByTagName('message');
            for (let i = 0; i < genericMessages.length; i++) {
                 if ((genericMessages[i].getAttribute('name') === msgName) || (genericMessages[i].getAttribute('name') === messageNameValue)) {
                    messageNode = genericMessages[i];
                    break;
                }
            }
        }

        if (messageNode) {
            const partNode = messageNode.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'part')[0] || messageNode.getElementsByTagName('part')[0];
            if (partNode) {
                return {
                    name: partNode.getAttribute('name'),
                    element: partNode.getAttribute('element') // This is a QName like tns:ElementName
                };
            }
        }
        return null;
    };

    const inputNode = opNode.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'input')[0] || opNode.getElementsByTagName('input')[0];
    if (inputNode) {
        inputMessageName = inputNode.getAttribute('message') || 'N/A';
        inputPart = findPartForMessage(inputMessageName);
        if (inputPart && inputPart.element) {
            inputElementDetails = getElementDetailsFromXSD(inputPart.element, wsdlXmlDoc, parsedXsdDocs);
        }
    }

    const outputNode = opNode.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'output')[0] || opNode.getElementsByTagName('output')[0];
    if (outputNode) {
        outputMessageName = outputNode.getAttribute('message') || 'N/A';
        outputPart = findPartForMessage(outputMessageName);
        if (outputPart && outputPart.element) {
            outputElementDetails = getElementDetailsFromXSD(outputPart.element, wsdlXmlDoc, parsedXsdDocs);
        }
    }

    const docNode = opNode.getElementsByTagNameNS('http://schemas.xmlsoap.org/wsdl/', 'documentation')[0] || opNode.getElementsByTagName('documentation')[0];
    if (docNode) {
        documentation = docNode.textContent || '';
    }
    
    if (name) {
        operations.push({ name, inputMessageName, outputMessageName, documentation, inputPart, outputPart, inputElementDetails, outputElementDetails });
    }
};

const ApiSpecsPage = ({ analysisResult, repoName }) => {
  // Hooks must be at the top level
  const [parsedWsdlOperations, setParsedWsdlOperations] = useState([]);
  const [showRawWsdlAsFallback, setShowRawWsdlAsFallback] = useState(false);
  const [derivedSpecObject, setDerivedSpecObject] = useState(null);
  const [derivedOpenApiStringError, setDerivedOpenApiStringError] = useState(false);
  const [actualWsdlForProcessing, setActualWsdlForProcessing] = useState(null);
  const [detailedSoapEndpoints, setDetailedSoapEndpoints] = useState([]); // New state for annotation-based SOAP details

  useEffect(() => {
    if (!analysisResult) {
      setParsedWsdlOperations([]);
      setShowRawWsdlAsFallback(false);
      setDerivedSpecObject(null);
      setDerivedOpenApiStringError(false);
      setActualWsdlForProcessing(null);
      setDetailedSoapEndpoints([]); // Reset new state
      return;
    }

    const { openApiSpec, wsdlFilesContent, projectType, xsdFilesContent, endpoints } = analysisResult; // Added endpoints
    let wsdlStringToProcess = null;

    console.log('[ApiSpecsPage] useEffect: Received analysisResult.xsdFilesContent:', xsdFilesContent ? JSON.parse(JSON.stringify(xsdFilesContent)) : xsdFilesContent);
    
    // Populate detailedSoapEndpoints from analysisResult.endpoints
    if (endpoints && Array.isArray(endpoints)) {
      const soapEndpoints = endpoints.filter(ep => ep.type === 'SOAP');
      setDetailedSoapEndpoints(soapEndpoints);
      console.log('[ApiSpecsPage] useEffect: Filtered SOAP endpoints:', soapEndpoints);
    } else {
      setDetailedSoapEndpoints([]);
    }

    if (wsdlFilesContent && typeof wsdlFilesContent === 'object' && Object.keys(wsdlFilesContent).length > 0) {
      const firstWsdlFile = Object.keys(wsdlFilesContent)[0];
      const wsdlStr = wsdlFilesContent[firstWsdlFile];
      if (typeof wsdlStr === 'string' && wsdlStr.trim().length > 0) {
        console.log(`[ApiSpecsPage] Identified WSDL from wsdlFilesContent, key: ${firstWsdlFile}`);
        wsdlStringToProcess = wsdlStr;
      }
    } else if (projectType === 'SOAP' && typeof openApiSpec === 'string' && openApiSpec.trim().startsWith('<')) {
      console.log('[ApiSpecsPage] Using openApiSpec as WSDL for SOAP project (fallback).');
      wsdlStringToProcess = openApiSpec;
    }
    setActualWsdlForProcessing(wsdlStringToProcess);

    if (wsdlStringToProcess) {
      const operations = parseWsdlOperations(wsdlStringToProcess, xsdFilesContent);
      setParsedWsdlOperations(operations);
      setShowRawWsdlAsFallback(operations.length === 0); // Show raw if parsing failed or no ops
      setDerivedSpecObject(null); // WSDL takes precedence
      setDerivedOpenApiStringError(false);
    } else {
      setParsedWsdlOperations([]);
      setShowRawWsdlAsFallback(false);
      // If no WSDL, process openApiSpec
      if (typeof openApiSpec === 'string') {
        try {
          const parsed = JSON.parse(openApiSpec);
          if (typeof parsed === 'object' && parsed !== null && parsed.openapi) {
            setDerivedSpecObject(parsed);
            setDerivedOpenApiStringError(false);
          } else {
            setDerivedSpecObject(null);
            setDerivedOpenApiStringError(true);
          }
        } catch (e) {
          setDerivedSpecObject(null);
          setDerivedOpenApiStringError(true);
        }
      } else if (typeof openApiSpec === 'object' && openApiSpec !== null && openApiSpec.openapi) {
        setDerivedSpecObject(openApiSpec);
        setDerivedOpenApiStringError(false);
      } else {
        setDerivedSpecObject(null);
        setDerivedOpenApiStringError(false); // Or true if it implies an error state
      }
    }
  }, [analysisResult]);

  // Early returns after hooks
  if (!repoName) {
    return <Typography sx={{ fontFamily: 'Quicksand' }}>Please analyze a repository first to see API specifications.</Typography>;
  }
  if (!analysisResult) {
    // This case is also handled by useEffect, but good for clarity or if useEffect hadn't run yet
    return <Typography variant="h6" sx={{ fontFamily: 'Quicksand' }}>No analysis data available for API specifications.</Typography>;
  }

  // Props for direct use in rendering, primarily for fallback/OpenAPI string
  const { openApiSpec, isSpringBootProject, projectName } = analysisResult;

  // Recursive component to render element details
  const ElementDetailItem = ({ detail }) => {
    return (
      <ListItem sx={{ p: 0, display: 'block', alignItems: 'flex-start' }}>
        <ListItemText 
          disableTypography
          primary={
            <Typography variant="caption" sx={{ fontFamily: 'Quicksand', display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
              <Chip 
                label={detail.kind === 'attribute' ? `@${detail.name}` : detail.name} 
                size="small" 
                color="primary" 
                variant="outlined" 
                sx={{ mr: 0.5, mb: 0.5 }}
              /> 
              <Chip 
                label={detail.type || 'N/A'} 
                size="small" 
                variant="filled" 
                sx={{ mr: 0.5, mb: 0.5 }} 
              />
              {detail.kind === 'element' && (detail.minOccurs || detail.maxOccurs) && (
                <Chip 
                  label={`[${detail.minOccurs || '1'}..${detail.maxOccurs === 'unbounded' ? '*' : (detail.maxOccurs || '1')}]`} 
                  size="small" 
                  sx={{ mr: 0.5, mb: 0.5 }} 
                />
              )}
              {detail.kind === 'attribute' && detail.use && (
                <Chip 
                  label={detail.use} 
                  size="small" 
                  sx={{ mr: 0.5, mb: 0.5 }} 
                />
              )}
            </Typography>
          }
        />
        {detail.children && detail.children.length > 0 && (
          <List dense sx={{ ml: 2, mt: 0, pl: 1, borderLeft: '1px solid #eee' }}>
            {detail.children.map((childDetail, childIdx) => (
              <ElementDetailItem 
                key={`${childDetail.name}-${childIdx}-${detail.name}-child`} /* Ensure key uniqueness using parent detail name too */
                detail={childDetail}
              />
            ))}
          </List>
        )}
      </ListItem>
    );
  };

  return (
    <Box sx={{ fontFamily: 'Quicksand', width: '100%' }}>
      <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, fontFamily: 'Quicksand' }}>
        API Specifications for {repoName || projectName || 'Project'}
      </Typography>

      {parsedWsdlOperations.length > 0 ? (
        <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>WSDL Operations</Typography>
          <List>
            {parsedWsdlOperations.map((op, index) => {
              const matchedEndpoint = detailedSoapEndpoints.find(ep => 
                ep.methodName === op.name || 
                (ep.path && op.name && ep.path.toLowerCase().includes(op.name.toLowerCase()))
              );
              return (
                <React.Fragment key={op.name + index}>
                  <ListItem alignItems="flex-start">
                    <ListItemText
                      primary={<Typography variant="subtitle1" sx={{ fontWeight: 'bold', fontFamily: 'Quicksand' }}>{op.name}</Typography>}
                      secondaryTypographyProps={{ component: 'div', fontFamily: 'Quicksand' }}
                      secondary={
                        <>
                          {matchedEndpoint && (
                            <Paper elevation={0} sx={{ p: 1, mb: 1, backgroundColor: '#f9f9f9', borderRadius: 1}}>
                              <Typography variant="caption" sx={{ fontWeight: 'bold', display: 'block', color: '#3f51b5' }}>
                                Annotation-derived Details:
                              </Typography>
                              {matchedEndpoint.className && <Typography variant="caption" display="block">Class: {matchedEndpoint.className}</Typography>}
                              {matchedEndpoint.methodName && matchedEndpoint.methodName !== op.name && <Typography variant="caption" display="block">Method: {matchedEndpoint.methodName}</Typography>}
                              {matchedEndpoint.serviceName && <Typography variant="caption" display="block">Service Name: {matchedEndpoint.serviceName}</Typography>}
                              {matchedEndpoint.portName && <Typography variant="caption" display="block">Port Name: {matchedEndpoint.portName}</Typography>}
                              {matchedEndpoint.targetNamespace && <Typography variant="caption" display="block">Target Namespace: {matchedEndpoint.targetNamespace}</Typography>}
                              {matchedEndpoint.soapAction && <Typography variant="caption" display="block">SOAP Action: <Chip label={matchedEndpoint.soapAction} size="small" /></Typography>}
                              {matchedEndpoint.style && <Typography variant="caption" display="block">Style: {matchedEndpoint.style}</Typography>}
                              {matchedEndpoint.use && <Typography variant="caption" display="block">Use: {matchedEndpoint.use}</Typography>}
                              {matchedEndpoint.parameterStyle && <Typography variant="caption" display="block">Parameter Style: {matchedEndpoint.parameterStyle}</Typography>}
                              {matchedEndpoint.headers && Object.keys(matchedEndpoint.headers).length > 0 && (
                                  <Box mt={0.5}>
                                      <Typography variant="caption" sx={{fontWeight: 'bold'}}>Headers:</Typography>
                                      <List dense disablePadding sx={{pl:1}}>
                                          {Object.entries(matchedEndpoint.headers).map(([key, value]) => (
                                              <ListItem key={key} sx={{p:0}}>
                                                  <ListItemText primaryTypographyProps={{variant: 'caption'}} primary={`${key}: ${value}`} />
                                              </ListItem>
                                          ))}
                                      </List>
                                  </Box>
                              )}
                              {matchedEndpoint.requestWrapperName && <Typography variant="caption" display="block">Request Wrapper: {matchedEndpoint.requestWrapperName} (Class: {matchedEndpoint.requestWrapperClassName || 'N/A'})</Typography>}
                              {matchedEndpoint.responseWrapperName && <Typography variant="caption" display="block">Response Wrapper: {matchedEndpoint.responseWrapperName} (Class: {matchedEndpoint.responseWrapperClassName || 'N/A'})</Typography>}
                              {matchedEndpoint.wsdlUrl && <Typography variant="caption" display="block">WSDL Location (from annotation): <Link href={matchedEndpoint.wsdlUrl} target="_blank">{matchedEndpoint.wsdlUrl}</Link></Typography>}
                            </Paper>
                          )}
                          <Typography component="div" variant="body2" color="text.primary">
                            Input Message: <Chip label={op.inputMessageName || 'N/A'} size="small" variant="outlined" />
                            {op.inputPart && (
                              <Typography component="div" variant="caption" sx={{ ml: 2, mt: 0.5, fontFamily: 'Quicksand' }}>
                                ↳ Part: <Chip label={`${op.inputPart.name} (Element: ${op.inputPart.element})`} size="small" variant="outlined" />
                                {op.inputElementDetails && op.inputElementDetails.length > 0 && (
                                  <List dense sx={{ml: 2, mt:0.5}}>
                                    {op.inputElementDetails.map((detail, idx) => (
                                      <ElementDetailItem key={`input-${op.name}-${idx}`} detail={detail} />
                                    ))}
                                  </List>
                                )}
                              </Typography>
                            )}
                          </Typography>
                          
                          <Typography component="div" variant="body2" color="text.primary" sx={{mt:1}}>
                            Output Message: <Chip label={op.outputMessageName || 'N/A'} size="small" variant="outlined" />
                             {op.outputPart && (
                              <Typography component="div" variant="caption" sx={{ ml: 2, mt: 0.5, fontFamily: 'Quicksand' }}>
                                ↳ Part: <Chip label={`${op.outputPart.name} (Element: ${op.outputPart.element})`} size="small" variant="outlined" />
                                 {op.outputElementDetails && op.outputElementDetails.length > 0 && (
                                  <List dense sx={{ml: 2, mt:0.5}}>
                                    {op.outputElementDetails.map((detail, idx) => (
                                     <ElementDetailItem key={`output-${op.name}-${idx}`} detail={detail} />
                                    ))}
                                  </List>
                                )}
                              </Typography>
                            )}
                          </Typography>
                          {op.documentation && <Typography variant="caption" display="block" sx={{ mt: 1, color: 'text.secondary', fontStyle: 'italic' }}>Documentation: {op.documentation}</Typography>}
                          {!matchedEndpoint && (
                              <Typography variant="caption" display="block" sx={{mt: 1, color: 'text.secondary', fontStyle: 'italic' }}>
                                  (No specific annotation-derived metadata found for this WSDL operation name)
                              </Typography>
                          )}
                        </>
                      }
                    />
                  </ListItem>
                  {index < parsedWsdlOperations.length - 1 && <Divider variant="inset" component="li" />}
                </React.Fragment>
              )}
            )}
          </List>
          {actualWsdlForProcessing && (
             <details style={{ marginTop: '20px', cursor: 'pointer' }}>
                <summary style={{ fontFamily: 'Quicksand', color: '#555'}}>View Raw WSDL Content</summary>
                <Box component="pre" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '400px', overflowY: 'auto', p: 2, backgroundColor: '#f5f5f5', border: '1px solid #eee', borderRadius: 1, fontFamily: 'monospace', mt: 1 }}>
                    {actualWsdlForProcessing}
                </Box>
            </details>
          )}
        </Paper>
      )
      : showRawWsdlAsFallback && actualWsdlForProcessing ? (
        <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>WSDL Specification</Typography>
          <Alert severity="info" sx={{ mb: 2, fontFamily: 'Quicksand' }}>Could not extract operations from WSDL, or no operations defined. Displaying raw content.</Alert>
          <Box component="pre" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '600px', overflowY: 'auto', p: 2, backgroundColor: '#f5f5f5', border: '1px solid #eee', borderRadius: 1, fontFamily: 'monospace' }}>
            {actualWsdlForProcessing}
          </Box>
        </Paper>
      )
      : derivedSpecObject ? (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2, '& .swagger-ui .topbar': { display: 'none' } }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>Embedded OpenAPI Specification</Typography>
          <SwaggerUI spec={derivedSpecObject} />
        </Paper>
      )
      : detailedSoapEndpoints.length > 0 ? (
        <Paper elevation={2} sx={{ p:3, mb:3, borderRadius: 2}}>
            <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>SOAP Endpoints (from Annotations)</Typography>
            <Alert severity="info" sx={{ mb: 2, fontFamily: 'Quicksand' }}>
                Displaying SOAP endpoints identified from code annotations. This section appears when WSDL operations could not be parsed or are not available.
            </Alert>
            <List>
                {detailedSoapEndpoints.map((ep, index) => (
                    <React.Fragment key={`annotated-soap-${index}`}>
                        <ListItem alignItems="flex-start">
                            <ListItemText
                                primary={<Typography variant="subtitle1" sx={{ fontWeight: 'bold', fontFamily: 'Quicksand' }}>{ep.methodName || ep.path || 'Unnamed SOAP Endpoint'}</Typography>}
                                secondaryTypographyProps={{ component: 'div', fontFamily: 'Quicksand' }}
                                secondary={
                                    <Paper elevation={0} sx={{ p: 1, mt:0.5, backgroundColor: '#f9f9f9', borderRadius: 1}}>
                                        {ep.className && <Typography variant="caption" display="block">Class: {ep.className}</Typography>}
                                        {ep.serviceName && <Typography variant="caption" display="block">Service Name: {ep.serviceName}</Typography>}
                                        {ep.portName && <Typography variant="caption" display="block">Port Name: {ep.portName}</Typography>}
                                        {ep.targetNamespace && <Typography variant="caption" display="block">Target Namespace: {ep.targetNamespace}</Typography>}
                                        {ep.soapAction && <Typography variant="caption" display="block">SOAP Action: <Chip label={ep.soapAction} size="small" /></Typography>}
                                        {ep.style && <Typography variant="caption" display="block">Style: {ep.style}</Typography>}
                                        {ep.use && <Typography variant="caption" display="block">Use: {ep.use}</Typography>}
                                        {ep.parameterStyle && <Typography variant="caption" display="block">Parameter Style: {ep.parameterStyle}</Typography>}
                                        {ep.headers && Object.keys(ep.headers).length > 0 && (
                                            <Box mt={0.5}>
                                                <Typography variant="caption" sx={{fontWeight: 'bold'}}>Headers:</Typography>
                                                <List dense disablePadding sx={{pl:1}}>
                                                    {Object.entries(ep.headers).map(([key, value]) => (
                                                        <ListItem key={key} sx={{p:0}}>
                                                            <ListItemText primaryTypographyProps={{variant: 'caption'}} primary={`${key}: ${value}`} />
                                                        </ListItem>
                                                    ))}
                                                </List>
                                            </Box>
                                        )}
                                        {ep.requestBodyType && <Typography variant="caption" display="block">Request Type: {ep.requestBodyType}</Typography>}
                                        {ep.responseBodyType && <Typography variant="caption" display="block">Response Type: {ep.responseBodyType}</Typography>}
                                        {ep.requestWrapperName && <Typography variant="caption" display="block">Request Wrapper: {ep.requestWrapperName} (Class: {ep.requestWrapperClassName || 'N/A'})</Typography>}
                                        {ep.responseWrapperName && <Typography variant="caption" display="block">Response Wrapper: {ep.responseWrapperName} (Class: {ep.responseWrapperClassName || 'N/A'})</Typography>}
                                        {ep.wsdlUrl && <Typography variant="caption" display="block">WSDL Location (from annotation): <Link href={ep.wsdlUrl} target="_blank">{ep.wsdlUrl}</Link></Typography>}
                                        {ep.path && <Typography variant="caption" display="block">Path (from annotation): {ep.path}</Typography>}                                        
                                    </Paper>
                                }
                            />
                        </ListItem>
                        {index < detailedSoapEndpoints.length - 1 && <Divider variant="inset" component="li" />}
                    </React.Fragment>
                ))}
            </List>
        </Paper>
      )
      : isSpringBootProject ? (
        <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>OpenAPI / Swagger Documentation (Spring Boot Project)</Typography>
          <Alert severity="info" sx={{ mb: 2, fontFamily: 'Quicksand' }}>This Spring Boot project typically provides auto-generated OpenAPI documentation. If a WSDL for this project exists, it would have been shown above.</Alert>
          <Typography paragraph sx={{ fontFamily: 'Quicksand' }}>Standard Swagger UI path: <Link href={`${API_BASE_URL.replace('/api', '')}/swagger-ui.html`} target="_blank" rel="noopener noreferrer">{` ${API_BASE_URL.replace('/api', '')}/swagger-ui.html`}</Link></Typography>
          <Typography paragraph sx={{ fontFamily: 'Quicksand' }}>Standard OpenAPI JSON path: <Link href={`${API_BASE_URL.replace('/api', '')}/v3/api-docs`} target="_blank" rel="noopener noreferrer">{` ${API_BASE_URL.replace('/api', '')}/v3/api-docs`}</Link></Typography>
          {derivedOpenApiStringError && typeof openApiSpec === 'string' && openApiSpec !== actualWsdlForProcessing && (
            <Box mt={2}>
              <Typography variant="subtitle2" gutterBottom sx={{ fontFamily: 'Quicksand' }}>Additional Information from Backend (OpenAPI related):</Typography>
              <Alert severity="info" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '200px', overflowY: 'auto' }}>{openApiSpec}</Alert>
            </Box>
          )}
        </Paper>
      )
      : derivedOpenApiStringError && typeof openApiSpec === 'string' && openApiSpec !== actualWsdlForProcessing ? (
        <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>API Specification Details</Typography>
          <Alert severity="info" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '300px', overflowY: 'auto' }}>{openApiSpec}</Alert>
        </Paper>
      )
      : (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontFamily: 'Quicksand' }}>API Specification</Typography>
          <Alert severity="warning" sx={{ fontFamily: 'Quicksand' }}>No WSDL, displayable OpenAPI/Swagger specification, or other specific API information was found for this project.</Alert>
        </Paper>
      )}
    </Box>
  );
};

export default ApiSpecsPage; 