import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Select, MenuItem, FormControl, InputLabel } from '@mui/material';
import DiagramViewer from '../components/DiagramViewer';

const BACKEND_STATIC_BASE_URL = 'http://localhost:8080';

const Diagrams = ({ diagramMap, sequenceDiagrams, rawCallFlows, repoName }) => {
  const [selectedDiagramKey, setSelectedDiagramKey] = useState('');

  console.log('[DiagramsPage] Received diagramMap:', JSON.stringify(diagramMap));
  console.log('[DiagramsPage] Received sequenceDiagrams:', JSON.stringify(sequenceDiagrams ? Object.keys(sequenceDiagrams) : null));
  console.log('[DiagramsPage] Received rawCallFlows:', JSON.stringify(rawCallFlows ? Object.keys(rawCallFlows) : null));

  useEffect(() => {
    console.log('[DiagramsPage] useEffect: diagramMap updated:', JSON.stringify(diagramMap));
    console.log('[DiagramsPage] useEffect: sequenceDiagrams updated:', JSON.stringify(sequenceDiagrams ? Object.keys(sequenceDiagrams) : null));
    console.log('[DiagramsPage] useEffect: rawCallFlows updated:', JSON.stringify(rawCallFlows ? Object.keys(rawCallFlows) : null));
  }, [diagramMap, sequenceDiagrams, rawCallFlows]);

  if (!repoName) {
    return <Typography>Please analyze a repository first to see diagrams.</Typography>;
  }

  let allDiagramsData = {};
  let diagramOptions = [];

  if (diagramMap) {
    console.log('[DiagramsPage] Processing diagramMap:', diagramMap);
    Object.entries(diagramMap).forEach(([type, url], index) => {
      const key = `diagram-${type}-${index}`;
      const title = type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
      console.log(`[DiagramsPage] Adding from diagramMap: key=${key}, title=${title}, type=${type}`);
      allDiagramsData[key] = {
        key: key,
        title: title,
        url: `${BACKEND_STATIC_BASE_URL}${url}`,
        type: 'IMAGE'
      };
      diagramOptions.push({ key: key, title: title });
    });
  }

  if (sequenceDiagrams) {
    console.log('[DiagramsPage] Processing sequenceDiagrams, count:', Object.keys(sequenceDiagrams).length);
    Object.entries(sequenceDiagrams).forEach(([fqn, url], index) => {
      const key = `sequence-${index}`;
      // Attempt to create a shorter, more readable title from the FQN
      let shortTitle = fqn;
      try {
        const methodNameMatch = fqn.match(/\.([^.(]+)\(.*\]$/);
        const simpleMethodNameMatch = fqn.match(/\.([^.(]+)\(/);
        const classAndMethodMatch = fqn.match(/([\w$.]+)\.([^.(]+)\(/);

        if (methodNameMatch && methodNameMatch[1]) {
          shortTitle = methodNameMatch[1]; // Method name only
        } else if (simpleMethodNameMatch && simpleMethodNameMatch[1]) {
          shortTitle = simpleMethodNameMatch[1];
        } 
        // If class.method is desired:
        if (classAndMethodMatch && classAndMethodMatch[1] && classAndMethodMatch[2]) {
            const classNamePart = classAndMethodMatch[1];
            const simpleClassName = classNamePart.substring(classNamePart.lastIndexOf('.') + 1);
            shortTitle = `${simpleClassName}.${classAndMethodMatch[2]}`;
        }
      } catch (e) {
        console.warn("Error parsing FQN for short title: ", fqn, e);
        // Fallback to full FQN if parsing fails
        shortTitle = fqn;
      }
      
      console.log(`[DiagramsPage] Adding from sequenceDiagrams: key=${key}, title=${shortTitle} (from FQN=${fqn})`);
      allDiagramsData[key] = {
        key: key,
        title: `Sequence: ${shortTitle}`,
        originalTitle: `Sequence: ${fqn}`, // Keep original FQN if needed elsewhere
        url: `${BACKEND_STATIC_BASE_URL}${url}`,
        type: 'IMAGE'
      };
      diagramOptions.push({ key: key, title: `Sequence: ${shortTitle}` });
    });
  }
  console.log('[DiagramsPage] Final diagramOptions:', JSON.stringify(diagramOptions));

  const handleDiagramChange = (event) => {
    setSelectedDiagramKey(event.target.value);
  };

  let diagramsToDisplay = null;
  if (selectedDiagramKey && allDiagramsData[selectedDiagramKey]) {
    diagramsToDisplay = allDiagramsData[selectedDiagramKey];
  } else if (!selectedDiagramKey && diagramOptions.length > 0) {
    // If nothing selected, diagramsToDisplay remains null, handled by DiagramViewer or the message below.
  }

  if (diagramOptions.length === 0) {
    return <Typography>No diagrams found for this repository.</Typography>;
  }

  return (
    <Box sx={{ fontFamily: 'Quicksand' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', fontFamily: 'Quicksand' }}>System Diagrams</Typography>
        {/* Download All functionality would need implementation */}
        {/* <Button variant="outlined" startIcon={<DownloadIcon />} sx={{ fontFamily: 'Quicksand' }}>Download All Diagrams</Button> */}
      </Box>
      <FormControl fullWidth sx={{ mb: 2 }} variant="outlined">
        <InputLabel id="diagram-select-label">Select Diagram</InputLabel>
        <Select
          labelId="diagram-select-label"
          value={selectedDiagramKey}
          label="Select Diagram"
          onChange={handleDiagramChange}
        >
          <MenuItem value=""><em>Select a diagram to view</em></MenuItem>
          {diagramOptions.map((opt) => (
            <MenuItem key={opt.key} value={opt.key}>{opt.title}</MenuItem>
          ))}
        </Select>
      </FormControl>
      <Paper elevation={2} sx={{ p: 2, borderRadius: 2, minHeight: 300 }}>
        {selectedDiagramKey && diagramsToDisplay ? (
          <DiagramViewer diagram={diagramsToDisplay} />
        ) : (
          <Typography sx={{textAlign: 'center', mt: 4}}>
            {diagramOptions.length === 0 ? 'No diagrams available for this repository.' : 'Please select a diagram from the dropdown above.'}
          </Typography>
        )}
      </Paper>
    </Box>
  );
};

export default Diagrams; 