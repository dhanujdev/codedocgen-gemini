import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import Publish from '../components/Publish';

const PublishPage = ({ repoName }) => (
  <Box sx={{ fontFamily: 'Quicksand' }}>
    <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, fontFamily: 'Quicksand' }}>Publish Documentation</Typography>
    <Paper elevation={2} sx={{ p: 2, borderRadius: 2 }}>
      <Publish repoName={repoName} />
    </Paper>
  </Box>
);

export default PublishPage; 