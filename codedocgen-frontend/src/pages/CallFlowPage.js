import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, List, ListItem, ListItemText, Divider, CircularProgress, Alert } from '@mui/material';
import DiagramViewer from '../components/DiagramViewer';

const BACKEND_STATIC_BASE_URL = process.env.REACT_APP_BACKEND_BASE_URL || 'http://localhost:8080';

const CallFlowPage = ({ analysisResult, repoName }) => {
  const [displayableDiagrams, setDisplayableDiagrams] = useState([]);
  const [pageIsLoading, setPageIsLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    console.log('[CallFlowPage] Received analysisResult:', analysisResult);
    setPageIsLoading(true);
    setMessage('');
    setDisplayableDiagrams([]);

    if (!analysisResult) {
      setMessage('No analysis data available. Please analyze a repository first.');
      setPageIsLoading(false);
      return;
    }

    let collected = [];
    // Correctly access .diagrams and .sequenceDiagrams from the analysisResult object
    const { diagrams: generalDiagramMap, sequenceDiagrams: specificSequenceDiagrams } = analysisResult;

    console.log('[CallFlowPage] Extracted generalDiagramMap:', generalDiagramMap);
    console.log('[CallFlowPage] Extracted specificSequenceDiagrams:', specificSequenceDiagrams);

    if (generalDiagramMap) {
      Object.entries(generalDiagramMap).forEach(([type, url]) => {
        if (type && (type.toUpperCase() === 'SEQUENCE_DIAGRAM' || type.toUpperCase().includes('SEQUENCE'))) {
          const key = `seq-from-general-${type}`;
          const title = type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
          collected.push({
            key: key,
            title: `${title} (from general map)`,
            url: `${BACKEND_STATIC_BASE_URL}${url}`,
            type: type
          });
        }
      });
    }

    if (specificSequenceDiagrams) {
      Object.entries(specificSequenceDiagrams).forEach(([fqn, url]) => {
        const key = `seq-from-specific-${fqn}`;
        const simpleName = fqn.includes('.') ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
        const title = `Call Flow: ${simpleName}`;
        collected.push({
          key: key,
          title: title,
          originalTitle: `Sequence: ${fqn}`,
          url: `${BACKEND_STATIC_BASE_URL}${url}`,
          type: 'SEQUENCE_DIAGRAM'
        });
      });
    }
    
    console.log('[CallFlowPage] Collected sequence diagrams:', collected);

    if (collected.length > 0) {
      setDisplayableDiagrams(collected);
    } else {
      setMessage('No sequence diagrams (call flows) were found or generated for this repository.');
    }
    setPageIsLoading(false);

  }, [analysisResult]); // Effect runs when analysisResult changes

  if (!repoName && !analysisResult) { // Show this only if no repo context at all
    return <Typography sx={{ fontFamily: 'Quicksand', p:2 }}>Please analyze a repository first.</Typography>;
  }
  
  return (
    <Box sx={{ fontFamily: 'Quicksand', width: '100%' }}>
      <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, fontFamily: 'Quicksand' }}>
        Call Flow Diagrams for {repoName || (analysisResult && analysisResult.projectName) || 'Project'}
      </Typography>

      {pageIsLoading ? (
        <CircularProgress />
      ) : message ? (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}><Alert severity="info">{message}</Alert></Paper>
      ) : displayableDiagrams.length > 0 ? (
        <List>
          {displayableDiagrams.map((diag, index) => (
            <React.Fragment key={diag.key}>
              <ListItem>
                <ListItemText 
                  primary={diag.title} 
                  primaryTypographyProps={{variant: 'h6', fontFamily: 'Quicksand'}}
                />
              </ListItem>
              <Paper elevation={1} sx={{ p: 2, mb: 2, borderRadius: 2, border: '1px solid #eee' }}>
                <DiagramViewer diagram={diag} />
              </Paper>
              {index < displayableDiagrams.length - 1 && <Divider sx={{ my: 2 }} />}
            </React.Fragment>
          ))}
        </List>
      ) : (
        // This case should ideally be caught by the message state, but as a fallback:
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}><Alert severity="info">No call flow diagrams to display.</Alert></Paper>
      )}
    </Box>
  );
};

export default CallFlowPage; 