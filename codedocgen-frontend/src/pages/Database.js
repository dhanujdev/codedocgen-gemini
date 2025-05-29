import React from 'react';
import { Typography, Box, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Divider } from '@mui/material';
import { styled } from '@mui/material/styles';
import DiagramViewer from '../components/DiagramViewer';

// Custom styles
const StyledTableContainer = styled(TableContainer)(({ theme }) => ({
  maxHeight: 440,
  marginBottom: theme.spacing(4),
}));

const StyledPaper = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  marginBottom: theme.spacing(3),
}));

const operationTypeColors = {
  'SELECT': 'primary',
  'INSERT': 'success',
  'UPDATE': 'warning',
  'DELETE': 'error',
  'UNKNOWN': 'default'
};

const Database = ({ analysisResult }) => {
  if (!analysisResult || !analysisResult.daoOperations) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Database Analysis
        </Typography>
        <Typography variant="body1">
          No database operations found in the codebase.
        </Typography>
        <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
          This could be because:
          <ul>
            <li>The project doesn't use DAOs or repositories</li>
            <li>The project uses an ORM without direct SQL queries</li>
            <li>SQL queries are built dynamically or stored in external files</li>
          </ul>
        </Typography>
      </Box>
    );
  }

  const daoOperations = analysisResult.daoOperations;
  const dbDiagramPath = analysisResult.dbDiagramPath || (analysisResult.diagrams && analysisResult.diagrams['DATABASE_DIAGRAM']);
  
  // Extract all unique entity names from allClassMetadata
  const allEntities = new Set();
  if (analysisResult.classes) {
    analysisResult.classes.forEach(cls => {
      const isEntity = cls.type === 'entity' || (cls.annotations && cls.annotations.some(ann => ann.includes('@Entity')));
      if (isEntity && cls.name) {
        allEntities.add(cls.name); // Add the class name, which should correspond to the entity/table name
      }
    });
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Database Analysis
      </Typography>
      
      {dbDiagramPath && (
        <StyledPaper elevation={3}>
          <Typography variant="h5" gutterBottom>
            Database Schema Diagram
          </Typography>
          <DiagramViewer 
            diagramPath={dbDiagramPath} 
            alt="Database Schema" 
            style={{ width: '100%', border: '1px solid #ccc' }}
          />
        </StyledPaper>
      )}
      
      <StyledPaper elevation={3}>
        <Typography variant="h5" gutterBottom>
          Tables Detected
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, my: 2 }}>
          {Array.from(allEntities).map(entityName => (
            <Chip 
              key={entityName} 
              label={entityName} // Display entity name
              variant="outlined" 
              color="primary"
            />
          ))}
        </Box>
      </StyledPaper>
      
      <Typography variant="h5" gutterBottom>
        Database Operations by DAO/Repository Class
      </Typography>
      
      {Object.entries(daoOperations).map(([className, operations]) => {
        const simpleName = className.substring(className.lastIndexOf('.') + 1);
        
        return (
          <StyledPaper key={className} elevation={3}>
            <Typography variant="h6" gutterBottom>
              {simpleName}
              <Typography variant="caption" color="textSecondary" sx={{ ml: 1 }}>
                {className}
              </Typography>
            </Typography>
            
            <Divider sx={{ my: 2 }} />
            
            <StyledTableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Operation Type</TableCell>
                    <TableCell>Tables</TableCell>
                    <TableCell>SQL Query</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {operations.map((operation, index) => (
                    <TableRow key={index} hover>
                      <TableCell>
                        <Chip 
                          label={operation.operationType} 
                          color={operationTypeColors[operation.operationType] || 'default'} 
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {operation.tables && operation.tables.map(table => (
                          <Chip 
                            key={table} 
                            label={table} 
                            variant="outlined" 
                            size="small" 
                            sx={{ m: 0.5 }}
                          />
                        ))}
                      </TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        {operation.sqlQuery}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </StyledTableContainer>
          </StyledPaper>
        );
      })}
    </Box>
  );
};

export default Database; 