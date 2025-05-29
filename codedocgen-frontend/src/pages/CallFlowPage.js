import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, List, ListItem, ListItemText, Divider, CircularProgress, Alert } from '@mui/material';
import DiagramViewer from '../components/DiagramViewer';
import FlowExplorer from '../components/FlowExplorer.tsx';

const BACKEND_STATIC_BASE_URL = process.env.REACT_APP_BACKEND_BASE_URL || 'http://localhost:8080';

const generateFlowDisplayName = (signature) => {
  if (!signature || typeof signature !== 'string') return "Unknown Flow";

  // Standard Java method signature: com.package.Class.method(params) or Class.method(params)
  // Or constructor: com.package.Class.<init>(params)
  // Regex breakdown:
  // ^(?:([\w$.<>]+)\.)? : Optionally matches a fully qualified class name ending with a dot. $ matches literal $ in class/method names
  // ([\w$<>]+)           : Matches the method name (can include $ for inner classes or <> for generics if part of name like special scala methods, though typically <init> or <clinit>)
  // \((.*?)\)$            : Matches parameters within parentheses at the end. (Changed from .* to .*?)
  const methodMatch = signature.match(/^(?:([\w$.<>]+)\.)?([\w$<>]+)\((.*?)\)$/);

  if (!methodMatch) {
    // Fallback for non-standard signatures or simple names
    if (signature.includes('(') && signature.includes(')')) return signature; // Already somewhat formatted
    const spacedName = signature.replace(/([A-Z])/g, ' $1').trim();
    const finalName = spacedName.charAt(0).toUpperCase() + spacedName.slice(1);
    return finalName.replace(/_/g, ' ');
  }

  const classFqnOrNull = methodMatch[1];
  const methodName = methodMatch[2];
  const paramsString = methodMatch[3];

  const simplifyType = (fullType) => {
    if (!fullType) return '';
    // Simplifies types like java.util.List<com.example.MyClass> to List<MyClass>
    // Also handles arrays like java.lang.String[] to String[]
    let simplified = fullType.replace(/([a-zA-Z0-9_]+\\.)+([a-zA-Z0-9_$\\<\\>\\[\\]]+)/g, '$2'); // Outer types
    simplified = simplified.replace(/<([^>]+)>/g, (match, inner) => { // Generic parameters
        const innerSimplified = inner.split(',')
            .map(part => part.trim().replace(/([a-zA-Z0-9_]+\\.)+([a-zA-Z0-9_$\\<\\>\\[\\]]+)/g, '$2'))
            .join(', ');
        return `<${innerSimplified}>`;
    });
    return simplified;
  };

  const processParams = (pString) => {
    if (!pString) return "";
    return pString.split(',')
      .map(p => p.trim())
      .filter(p => p)
      .map(p => {
        // Match "type name" or just "type"
        // Handles types with spaces like "unsigned int" or generics "Map<String, String>"
        const parts = p.match(/^(.*?\s+)?([\w$]+)(\[\])*$/); // Changed (.*\s+)? to (.*?\s+)?
        if (parts) {
            const typeCandidate = (parts[1] || "").trim(); // Everything before the last word
            const nameOrLastPartOfType = parts[2];
            const arrayBrackets = parts[3] || "";


            if (typeCandidate) { // Likely "type name[]" or "type name"
                return `${simplifyType(typeCandidate)} ${nameOrLastPartOfType}${arrayBrackets}`;
            } else { // Likely just "type[]" or "type"
                 return `${simplifyType(nameOrLastPartOfType)}${arrayBrackets}`;
            }
        }
        return simplifyType(p); // Fallback for complex cases not caught by regex
      }).join(', ');
  };

  const processedParams = processParams(paramsString);

  if (methodName === "<init>") {
    const className = classFqnOrNull ? classFqnOrNull.substring(classFqnOrNull.lastIndexOf('.') + 1) : "Object";
    return `new ${className}(${processedParams})`;
  }
  if (methodName === "<clinit>") {
    const className = classFqnOrNull ? classFqnOrNull.substring(classFqnOrNull.lastIndexOf('.') + 1) : "";
    return `${className} static initializer`.trim();
  }

  return `${methodName}(${processedParams})`;
};

const CallFlowPage = ({ analysisResult, repoName }) => {
  const [displayableDiagramsAndFlows, setDisplayableDiagramsAndFlows] = useState([]);
  const [pageIsLoading, setPageIsLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    console.log('[CallFlowPage] Received analysisResult:', analysisResult);
    setPageIsLoading(true);
    setMessage('');
    setDisplayableDiagramsAndFlows([]);

    if (!analysisResult) {
      setMessage('No analysis data available. Please analyze a repository first.');
      setPageIsLoading(false);
      return;
    }

    let collected = [];
    const { diagrams: generalDiagramMap, sequenceDiagrams: specificSequenceDiagrams, callFlows: rawCallFlows } = analysisResult;

    // Process specific sequence diagrams and their raw flows first
    if (specificSequenceDiagrams) {
      Object.entries(specificSequenceDiagrams).forEach(([fqn, url]) => {
        const displayName = generateFlowDisplayName(fqn);
        const flowSteps = rawCallFlows && rawCallFlows[fqn] ? rawCallFlows[fqn] : [];
        collected.push({
          key: `flow-${fqn}`,
          diagramTitle: `Sequence Diagram: ${displayName}`,
          diagramUrl: `${BACKEND_STATIC_BASE_URL}${url}`,
          diagramType: 'IMAGE',
          flowDataForExplorer: [
            {
              name: `Detailed Steps for: ${displayName}`,
              description: `Call flow starting from ${fqn}`,
              steps: flowSteps 
            }
          ]
        });
      });
    }

    // Add other sequence diagrams from the general map if they weren't already processed
    if (generalDiagramMap) {
      Object.entries(generalDiagramMap).forEach(([type, url]) => {
        if (type && (type.toUpperCase() === 'SEQUENCE_DIAGRAM' || type.toUpperCase().includes('SEQUENCE'))) {
          // Check if this diagram (based on URL) was already added from specificSequenceDiagrams
          const alreadyAdded = collected.some(c => c.diagramUrl === `${BACKEND_STATIC_BASE_URL}${url}`);
          if (!alreadyAdded) {
            const title = type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
            collected.push({
              key: `general-seq-${type}`,
              diagramTitle: `${title} (General)`,
              diagramUrl: `${BACKEND_STATIC_BASE_URL}${url}`,
              diagramType: type, 
              flowDataForExplorer: [] // No specific raw flow steps for these general diagrams usually
            });
          }
        }
      });
    }
    
    console.log('[CallFlowPage] Collected diagrams and flows:', collected);

    if (collected.length > 0) {
      setDisplayableDiagramsAndFlows(collected);
    } else {
      setMessage('No sequence diagrams or call flows were found or generated for this repository.');
    }
    setPageIsLoading(false);

  }, [analysisResult]);

  if (!repoName && !analysisResult) {
    return <Typography sx={{ fontFamily: 'Quicksand', p:2 }}>Please analyze a repository first.</Typography>;
  }
  
  return (
    <Box sx={{ fontFamily: 'Quicksand', width: '100%' }}>
      <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, fontFamily: 'Quicksand' }}>
        Call Flow Analysis for {repoName || (analysisResult && analysisResult.projectName) || 'Project'}
      </Typography>

      {pageIsLoading ? (
        <CircularProgress />
      ) : message ? (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}><Alert severity="info">{message}</Alert></Paper>
      ) : displayableDiagramsAndFlows.length > 0 ? (
        <List>
          {displayableDiagramsAndFlows.map((item, index) => (
            <React.Fragment key={item.key}>
              <ListItem>
                <ListItemText 
                  primary={item.diagramTitle} 
                  primaryTypographyProps={{variant: 'h6', fontFamily: 'Quicksand'}}
                />
              </ListItem>
              <Paper elevation={1} sx={{ p: 2, mb: 2, borderRadius: 2, border: '1px solid #eee' }}>
                {item.diagramUrl && <DiagramViewer diagram={{ url: item.diagramUrl, type: item.diagramType, title: item.diagramTitle}} />}
                {item.flowDataForExplorer && item.flowDataForExplorer.length > 0 && (
                  <Box sx={{ mt: 2, pt: 2, borderTop: item.diagramUrl ? '1px dashed #ccc' : 'none' }}>
                    <FlowExplorer flows={item.flowDataForExplorer} />
                  </Box>
                )}
              </Paper>
              {index < displayableDiagramsAndFlows.length - 1 && <Divider sx={{ my: 2 }} />}
            </React.Fragment>
          ))}
        </List>
      ) : (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}><Alert severity="info">No call flow diagrams or detailed steps to display.</Alert></Paper>
      )}
    </Box>
  );
};

export default CallFlowPage; 