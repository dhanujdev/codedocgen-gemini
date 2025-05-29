import React from 'react';
import { Box, Typography, Paper, Alert } from '@mui/material';

const Overview = ({ analysisResult }) => {
  console.log('[Overview.js] analysisResult:', analysisResult);
  if (!analysisResult) {
    return <Typography variant="h6">No project analyzed yet. Please analyze a repository first.</Typography>;
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '80vh', fontFamily: 'Quicksand' }}>
      <Typography variant="h3" sx={{ fontWeight: 700, color: '#4f46e5', mb: 1, fontFamily: 'Quicksand' }}>
        Project Overview
      </Typography>
      <Paper elevation={3} sx={{ p: 4, minWidth: 400, maxWidth: 520, borderRadius: 3, boxShadow: 4, mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600, mb: 2, fontFamily: 'Quicksand' }}>Summary</Typography>
        <Alert severity="info" sx={{ mb: 2, fontFamily: 'Quicksand' }}>
          <strong>Project Name:</strong> {analysisResult.projectName}<br />
          <strong>Type:</strong> {analysisResult.projectType}<br />
          <strong>Spring Boot:</strong> {analysisResult.springBootProject ? 'Yes' : 'No'}<br />
          {analysisResult.springBootVersion && (<><strong>Spring Boot Version:</strong> {analysisResult.springBootVersion}<br /></>)}
        </Alert>
        <Typography variant="body1" sx={{ fontFamily: 'Quicksand' }}>{analysisResult.projectSummary || 'No summary available.'}</Typography>
      </Paper>
    </Box>
  );
};

export default Overview; 