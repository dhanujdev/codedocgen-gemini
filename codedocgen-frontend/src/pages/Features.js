import React from 'react';
import { Box, Typography, Paper, Button, Card, CardContent, CircularProgress } from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';

const Features = ({ features, repoName }) => {
  if (!repoName) {
    return <Typography>Please analyze a repository first to see features.</Typography>;
  }

  if (features === null) {
    return <Typography>No feature files found or an error occurred during analysis.</Typography>;
  }
  
  const featureEntries = Object.entries(features);

  if (featureEntries.length === 0) {
    return <Typography>No .feature files were found in this repository.</Typography>;
  }

  return (
    <Box sx={{ fontFamily: 'Quicksand' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', fontFamily: 'Quicksand' }}>Feature Files (.feature)</Typography>
      </Box>
      {featureEntries.map(([fileName, fileContent], idx) => (
        <Card key={idx} sx={{ mb: 3, borderRadius: 2 }}>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, color: '#333' }}>{fileName}</Typography>
            <Paper variant="outlined" sx={{ p: 2, mt: 1, background: '#f8fafc', whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 14, maxHeight: '400px', overflowY: 'auto' }}>
              {fileContent}
            </Paper>
          </CardContent>
        </Card>
      ))}
    </Box>
  );
};

export default Features; 