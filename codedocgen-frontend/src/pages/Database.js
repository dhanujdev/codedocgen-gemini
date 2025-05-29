import React from 'react';
import { Typography, Box, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Divider, List, ListItem, ListItemText } from '@mui/material';
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

const BACKEND_STATIC_BASE_URL = process.env.REACT_APP_BACKEND_BASE_URL || 'http://localhost:8080';

const Database = ({ analysisResult }) => {
  if (!analysisResult || !analysisResult.dbAnalysis || 
      (!analysisResult.dbAnalysis.operationsByClass && !analysisResult.dbAnalysis.classesByEntity)) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Database Analysis
        </Typography>
        <Typography variant="body1">
          No database analysis results found.
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

  const operationsByClass = analysisResult.dbAnalysis.operationsByClass || {};
  const classesByEntity = analysisResult.dbAnalysis.classesByEntity || {};
  
  const dbDiagramPath = analysisResult.dbDiagramPath || (analysisResult.diagrams && analysisResult.diagrams['DATABASE_DIAGRAM']);
  
  let databaseDiagramObject = null;
  if (dbDiagramPath) {
    databaseDiagramObject = {
      key: 'database-schema',
      title: 'Database Schema Diagram',
      url: dbDiagramPath.startsWith('http') ? dbDiagramPath : `${BACKEND_STATIC_BASE_URL}${dbDiagramPath}`,
      type: 'IMAGE'
    };
  }

  const allEntityNamesFromClassesByEntity = Object.keys(classesByEntity);

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Database Analysis
      </Typography>
      
      {databaseDiagramObject && (
        <StyledPaper elevation={3}>
          <Typography variant="h5" gutterBottom>
            Database Schema Diagram
          </Typography>
          <DiagramViewer diagram={databaseDiagramObject} />
        </StyledPaper>
      )}
      
      <Typography variant="h5" gutterBottom sx={{ mt: 4 }}>
        Entities and Interacting DAO/Repository Classes
      </Typography>
      {allEntityNamesFromClassesByEntity.length > 0 ? (
        allEntityNamesFromClassesByEntity.map(entityName => (
          <StyledPaper key={entityName} elevation={3}>
            <Typography variant="h6" gutterBottom>
              Entity: {entityName}
            </Typography>
            <Typography variant="subtitle1" color="textSecondary" gutterBottom>
              Interacting Classes:
            </Typography>
            <List dense>
              {(classesByEntity[entityName] || []).map(interactingClassFqn => {
                const simpleName = interactingClassFqn.substring(interactingClassFqn.lastIndexOf('.') + 1);
                return (
                  <ListItem key={interactingClassFqn}>
                    <ListItemText 
                      primary={simpleName} 
                      secondary={interactingClassFqn} 
                    />
                  </ListItem>
                );
              })}
            </List>
          </StyledPaper>
        ))
      ) : (
        <StyledPaper elevation={3}>
          <Typography variant="body1">
            No specific entity-to-class interactions found.
          </Typography>
        </StyledPaper>
      )}

      <Typography variant="h5" gutterBottom sx={{ mt: 4 }}>
        Detailed Operations by DAO/Repository Class
      </Typography>
      
      {Object.entries(operationsByClass).length > 0 ? (
        Object.entries(operationsByClass).map(([className, operations]) => {
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
                      <TableCell>Method Name</TableCell>
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
                        <TableCell>{operation.methodName || 'N/A'}</TableCell>
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
        })
      ) : (
        <StyledPaper elevation={3}>
          <Typography variant="body1">
            No DAO/Repository operations found.
          </Typography>
        </StyledPaper>
      )}
    </Box>
  );
};

export default Database; 