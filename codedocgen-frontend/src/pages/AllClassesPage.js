import React, { useState } from 'react';
import { Box, Typography, Accordion, AccordionSummary, AccordionDetails, List, ListItem, ListItemText, Chip, Table, TableHead, TableRow, TableCell, TableBody, Paper } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const AllClassesPage = ({ entities, repoName }) => {
  const [expandedClass, setExpandedClass] = useState(false);

  const handleAccordionChange = (panel) => (event, isExpanded) => {
    setExpandedClass(isExpanded ? panel : false);
  };

  if (!repoName) {
    return <Typography>Please analyze a repository first to see parsed class information.</Typography>;
  }
  
  if (!entities || entities.length === 0) {
    return <Typography sx={{mt:2}}>No classes were parsed from the repository.</Typography>;
  }

  return (
    <Box sx={{ fontFamily: 'Quicksand', p: 2 }}>
      <Typography variant="h4" sx={{ fontWeight: 700, color: '#3b82f6', mb: 2, fontFamily: 'Quicksand' }}>
        All Parsed Classes & Methods
      </Typography>
      {entities.map((cls, index) => (
        <Accordion 
          key={`cls-accordion-${index}`} 
          expanded={expandedClass === `panel${index}`} 
          onChange={handleAccordionChange(`panel${index}`)}
          sx={{ mb: 1 }}
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon />}
            aria-controls={`panel${index}bh-content`}
            id={`panel${index}bh-header`}
          >
            <Typography sx={{ width: '60%', flexShrink: 0, fontWeight: 500 }}>
              {cls.packageName ? `${cls.packageName}.${cls.name}` : cls.name}
            </Typography>
            <Typography component="div" sx={{ color: 'text.secondary' }}>
              <Chip label={cls.type || 'CLASS'} size="small" variant="outlined" />
            </Typography>
          </AccordionSummary>
          <AccordionDetails sx={{ display: 'flex', flexDirection: 'column', gap: 2, backgroundColor: '#f9fafb' }}>
            <Box>
              <Typography variant="subtitle2" gutterBottom sx={{fontWeight: 'bold'}}>File Path:</Typography>
              <Typography variant="body2" color="text.secondary">{cls.filePath || 'N/A'}</Typography>
            </Box>
            {cls.annotations && cls.annotations.length > 0 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom sx={{fontWeight: 'bold'}}>Class Annotations:</Typography>
                <List dense disablePadding>
                  {cls.annotations.map((ann, i) => (
                    <ListItem key={`cls-ann-${index}-${i}`} sx={{py: 0.2, px:0}}>
                      <ListItemText primaryTypographyProps={{ variant: 'body2', fontFamily:'monospace' }} primary={ann} />
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}
            {cls.parentClass && (
               <Box>
                <Typography variant="subtitle2" gutterBottom sx={{fontWeight: 'bold'}}>Parent Class:</Typography>
                <Typography variant="body2" color="text.secondary">{cls.parentClass}</Typography>
              </Box>
            )}
            {cls.interfaces && cls.interfaces.length > 0 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom sx={{fontWeight: 'bold'}}>Implemented Interfaces:</Typography>
                 <List dense disablePadding>
                  {cls.interfaces.map((iface, i) => (
                    <ListItem key={`cls-iface-${index}-${i}`} sx={{py: 0.2, px:0}}>
                      <ListItemText primaryTypographyProps={{ variant: 'body2' }} primary={iface} />
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}
            
            {/* Fields Display */}
            {cls.fields && cls.fields.length > 0 && (
              <Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1, mb: 1 }}>Fields</Typography>
                <Paper variant="outlined" sx={{ width: '100%', overflowX: 'auto' }}>
                  <Table size="small" sx={{ minWidth: 400 }}>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 'bold' }}>Name</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Visibility</TableCell>
                        <TableCell sx={{ fontWeight: 'bold' }}>Annotations</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {cls.fields.map((field, i) => (
                        <TableRow key={`field-${index}-${i}`}>
                          <TableCell>{field.name}</TableCell>
                          <TableCell>{field.type}</TableCell>
                          <TableCell>{field.visibility || 'default'}</TableCell>
                          <TableCell>{field.annotations ? field.annotations.join(', ') : 'N/A'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Paper>
              </Box>
            )}

            {/* Methods Display including DAO Operations */}
            {cls.methods && cls.methods.length > 0 ? (
              <Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 2, mb: 1 }}>Methods</Typography>
                {cls.methods.map((method, methodIdx) => (
                  <Accordion key={`method-accordion-${index}-${methodIdx}`} sx={{ boxShadow: 'none', border: '1px solid #e0e0e0', '&:before': { display: 'none' } }}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                      <Typography variant="body1" sx={{ fontWeight: 500 }}>
                        {method.visibility && `${method.visibility} `}{method.name}({(method.parameters || []).join(', ')}) : {method.returnType}
                      </Typography>
                    </AccordionSummary>
                    <AccordionDetails sx={{ display: 'flex', flexDirection: 'column', gap: 1, backgroundColor: '#f0f4f8' }}>
                      {method.annotations && method.annotations.length > 0 && (
                         <Box>
                              <Typography variant="caption" display="block" gutterBottom sx={{fontWeight: 'bold'}}>Method Annotations:</Typography>
                              {method.annotations.map((ann, i) => <Chip key={`meth-ann-${i}`} label={ann} size="small" sx={{mr:0.5, mb:0.5, fontFamily:'monospace'}} />)}
                         </Box>
                      )}

                      {/* DAO Operations Display */}
                      {method.daoOperations && method.daoOperations.length > 0 && (
                        <Box mt={1} p={1} sx={{ border: '1px dashed #ccc', borderRadius: 1, backgroundColor: '#e9ecef' }}>
                          <Typography variant="overline" display="block" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                            Database Operations:
                          </Typography>
                          <List dense disablePadding>
                            {method.daoOperations.map((daoOp, daoIdx) => (
                              <ListItem key={`dao-${index}-${methodIdx}-${daoIdx}`} sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', py:0.5 }}>
                                <Box sx={{display:'flex', alignItems:'center', gap:1, mb:0.5}}>
                                  <Chip label={daoOp.operationType || 'UNKNOWN'} color="secondary" size="small" variant="filled" />
                                  <Typography variant="caption" sx={{fontFamily: 'monospace', backgroundColor:'#fff', p:0.5, borderRadius:1, border:'1px solid #ddd' }}>
                                    {daoOp.sqlQuery}
                                  </Typography>
                                </Box>
                                {daoOp.tables && daoOp.tables.length > 0 && (
                                  <Typography variant="caption" color="text.secondary">
                                    Tables: {daoOp.tables.join(', ')}
                                  </Typography>
                                )}
                              </ListItem>
                            ))}
                          </List>
                        </Box>
                      )}
                       {(!method.daoOperations || method.daoOperations.length === 0) && (
                          <Typography variant="caption" color="text.secondary" sx={{mt:1}}>No direct database operations detected in this method.</Typography>
                      )}
                    </AccordionDetails>
                  </Accordion>
                ))}
              </Box>
            ) : (
               <Typography variant="body2" sx={{ fontStyle: 'italic', mt: 2 }}>No methods found in this class.</Typography>
            )}
          </AccordionDetails>
        </Accordion>
      ))}
    </Box>
  );
};

export default AllClassesPage; 