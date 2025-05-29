import React from 'react';
import { Box, Typography, Paper, Table, TableHead, TableRow, TableCell, TableBody, Chip } from '@mui/material';

const methodColor = (method) => {
  switch (method) {
    case 'POST': return 'primary';
    case 'GET': return 'success';
    case 'PUT': return 'warning';
    case 'DELETE': return 'error';
    default: return 'default';
  }
};

const Endpoints = ({ endpoints, repoName }) => {
  if (!repoName) {
    return <Typography>Please analyze a repository first to see endpoints.</Typography>;
  }

  if (!endpoints || endpoints.length === 0) {
    return <Typography>No endpoints found for this repository or an error occurred during analysis.</Typography>;
  }

  return (
    <Box sx={{ fontFamily: 'Quicksand' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', fontFamily: 'Quicksand' }}>API Endpoints</Typography>
      </Box>
      <Paper elevation={2} sx={{ p: 2, mb: 4, borderRadius: 2, overflowX: 'auto' }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Controller Name</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Method Name</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>HTTP Method</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Path</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Request Body</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Response Body</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Parameters</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {endpoints.map((ep, idx) => (
              <TableRow key={idx}>
                <TableCell>{ep.className || 'N/A'}</TableCell>
                <TableCell>{ep.methodName || 'N/A'}</TableCell>
                <TableCell><Chip label={ep.httpMethod} color={methodColor(ep.httpMethod)} size="small" /></TableCell>
                <TableCell sx={{ wordBreak: 'break-all' }}>{ep.path}</TableCell>
                <TableCell>{ep.requestBodyType || 'N/A'}</TableCell>
                <TableCell>{ep.responseBodyType || 'N/A'}</TableCell>
                <TableCell>
                  {ep.parameters && ep.parameters.length > 0 
                    ? ep.parameters.map(p => `${p.name}: ${p.type}`).join(', ') 
                    : 'N/A'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
};

export default Endpoints; 