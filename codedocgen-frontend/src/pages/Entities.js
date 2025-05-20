import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Table, TableHead, TableRow, TableCell, TableBody, Tabs, Tab, List, ListItem, ListItemText, CircularProgress, Button } from '@mui/material';

// Updated helper to check for entity annotation (case-insensitive for the core "Entity" part)
const isJpaEntity = (entity) => {
  if (!entity.annotations || !Array.isArray(entity.annotations)) return false;
  return entity.annotations.some(annString => {
    if (!annString || typeof annString !== 'string') return false;
    // Remove leading @ and any parameters like (...) for a simpler check
    const coreAnnotation = annString.substring(annString.startsWith('@') ? 1 : 0).split('(')[0];
    // Check if the core annotation name is 'Entity' or ends with '.Entity' for FQNs
    return coreAnnotation.endsWith('Entity'); // Catches Entity, javax.persistence.Entity, etc.
  });
};

const Entities = ({ entities, endpoints, repoName }) => {
  // Hooks: useState
  const [tab, setTab] = useState(0);

  // Derived state/constants
  const actualEntities = entities && entities.length > 0 ? entities.filter(e => e.type === 'entity' || (e.type === 'CLASS' && isJpaEntity(e))) : [];

  // Early returns - AFTER all hook calls
  if (!repoName) {
    return <Typography>Please analyze a repository first to see entities.</Typography>;
  }

  if (actualEntities.length === 0) {
    return <Typography>No JPA entities (classes annotated with @Entity or similar) found.</Typography>;
  }

  // Function to find endpoints using a given entity
  const findAssociatedEndpoints = (entityName) => {
    if (!endpoints || endpoints.length === 0) return [];
    return endpoints.filter(ep => 
      ep.requestBodyType === entityName || 
      ep.responseBodyType === entityName ||
      (ep.parameters && ep.parameters.some(p => p.type === entityName)) ||
      (ep.className && ep.className === entityName) ||
      (ep.handlerMethod && ep.handlerMethod.includes(entityName))
    );
  };

  return (
    <Box sx={{ fontFamily: 'Quicksand' }}>
      <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, fontFamily: 'Quicksand' }}>Data Entities (e.g., @Entity)</Typography>
      {actualEntities.map((entity, idx) => {
        const entityFQN = `${entity.packageName}.${entity.name}`;
        const associatedEndpointsByName = findAssociatedEndpoints(entity.name); 
        const associatedEndpointsByFQN = findAssociatedEndpoints(entityFQN);
        const allAssociatedEndpoints = [...new Map(
          [...associatedEndpointsByName, ...associatedEndpointsByFQN].map(ep => [`${ep.httpMethod}-${ep.path}-${ep.handlerMethod}`, ep])
        ).values()];

        return (
          <Paper key={`jpa-entity-${idx}`} elevation={2} sx={{ mb: 4, p: 2, borderRadius: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>{entityFQN}</Typography>
            {entity.fields && entity.fields.length > 0 ? (
              <Table size="small" sx={{mb: 2}}>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Field Name</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Type</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Annotations</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {entity.fields.map((field, i) => (
                    <TableRow key={`jpa-field-${i}`}>
                      <TableCell>{field.name}</TableCell>
                      <TableCell>{field.type}</TableCell>
                      <TableCell>{field.annotations ? field.annotations.join(', ') : 'N/A'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <Typography variant="body2" sx={{ fontStyle: 'italic', mb: 2 }}>No fields found for this entity.</Typography>
            )}
            {allAssociatedEndpoints.length > 0 && (
              <Box mt={1}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Used by Endpoints:</Typography>
                <List dense>
                  {allAssociatedEndpoints.map((ep, epIdx) => (
                    <ListItem key={`jpa-ep-${epIdx}`} disablePadding>
                      <ListItemText 
                        primary={`${ep.httpMethod} ${ep.path}`} 
                        secondary={`Class: ${ep.className || 'N/A'}, Method: ${ep.methodName || 'N/A'}`} 
                      />
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}
          </Paper>
        );
      })}

      <Typography variant="h4" sx={{ fontWeight: 700, color: '#4f46e5', mb: 2, mt:4, fontFamily: 'Quicksand' }}>Entity Relationship Overview</Typography>
      <Paper elevation={2} sx={{ p: 2, borderRadius: 2 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
          <Tab label="Entity List" />
        </Tabs>
        {/* Tab Panel for Entity List (Index 0) */}
        {tab === 0 && (
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>Entities (e.g., @Entity)</Typography>
            <ul>
              {actualEntities.map((entity, idx) => (
                <li key={`er-entity-${idx}`}>{entity.packageName}.{entity.name}</li>
              ))}
            </ul>
            
            {/* Adding entity details directly in the entity list tab */}
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1, mt: 3 }}>Entity Details</Typography>
            {actualEntities.map((entity, idx) => (
              <Box key={`er-detail-${idx}`} sx={{ mb: 2, border: '1px solid #eee', p: 1, borderRadius: 1 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>{entity.name}</Typography>
                <Typography variant="caption" sx={{ display: 'block', mb: 1, color: 'text.secondary' }}>
                  {entity.fields?.length || 0} fields
                </Typography>
                {entity.fields && entity.fields.length > 0 && (
                  <Box component="ul" sx={{ pl: 2, mt: 0 }}>
                    {entity.fields.map((field, i) => {
                      // Check if field is a primary key
                      const isPrimaryKey = field.annotations && 
                        Array.isArray(field.annotations) && 
                        field.annotations.some(ann => typeof ann === 'string' && ann.toLowerCase().includes('id'));
                      
                      return (
                        <Box component="li" key={`er-field-${idx}-${i}`} sx={{ 
                          fontSize: '0.85rem', 
                          fontWeight: isPrimaryKey ? 'bold' : 'normal',
                          color: isPrimaryKey ? 'primary.main' : 'text.primary'
                        }}>
                          {field.name}: {field.type}
                          {isPrimaryKey && ' (PK)'}
                        </Box>
                      );
                    })}
                  </Box>
                )}
              </Box>
            ))}
          </Box>
        )}
      </Paper>
    </Box>
  );
};

export default Entities; 