import React, { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  Card,
  CardContent,
  CardHeader,
  List,
  ListItem,
  ListItemText,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Link,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Tooltip,
  Modal,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { TransformWrapper, TransformComponent } from "react-zoom-pan-pinch";
import FullscreenIcon from '@mui/icons-material/Fullscreen';
import CloseIcon from '@mui/icons-material/Close';
import SwaggerUI from "swagger-ui-react";
import "swagger-ui-react/swagger-ui.css";

function AnalysisDisplay({ data, API_URL, activeSection }) {
  const [currentTab, setCurrentTab] = React.useState(0);
  const [diagramType, setDiagramType] = useState('CLASS_DIAGRAM');
  const [fullscreen, setFullscreen] = useState(false);

  const handleTabChange = (event, newValue) => {
    setCurrentTab(newValue);
  };

  const handleDiagramChange = (e) => setDiagramType(e.target.value);
  const handleFullscreen = () => setFullscreen(true);
  const handleCloseFullscreen = () => setFullscreen(false);

  if (!data) {
    return <Typography>No analysis data available.</Typography>;
  }

  // Simple check if the openApiSpec is a URL or actual spec content
  const isOpenApiUrl = data.openApiSpec && (data.openApiSpec.startsWith('http') || data.openApiSpec.includes('api-docs'));
  const isOpenApiContent = data.openApiSpec && (data.openApiSpec.startsWith('{') || data.openApiSpec.startsWith('openapi:') || data.openApiSpec.startsWith('swagger:'));

  return (
    <Box sx={{ width: '100%', fontFamily: 'Inter, Roboto, Arial, sans-serif', transition: 'all 0.3s' }}>
      {data.parseWarnings && data.parseWarnings.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Some files could not be parsed and were skipped. This may affect the completeness of the analysis.
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            {data.parseWarnings.slice(0, 10).map((msg, idx) => (
              <li key={idx} style={{ fontSize: '0.9em' }}>{msg}</li>
            ))}
            {data.parseWarnings.length > 10 && <li>...and {data.parseWarnings.length - 10} more</li>}
          </ul>
        </Alert>
      )}
      <Typography variant="h4" gutterBottom component="div">
        Analysis Results for: {data.projectName}
      </Typography>

      <Paper elevation={2} sx={{mb: 2, p:2}}>
        <Typography variant="h5" gutterBottom>Project Summary</Typography>
        <Typography variant="body1" sx={{whiteSpace: 'pre-wrap'}}>{data.projectSummary}</Typography>
        <Typography variant="body2" color="textSecondary" sx={{mt:1}}>
            Project Type: {data.projectType} 
            {data.isSpringBootProject && `(Spring Boot ${data.springBootVersion || 'version not detected'})`}
        </Typography>
      </Paper>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={currentTab} onChange={handleTabChange} aria-label="analysis tabs example" variant="scrollable" scrollButtons="auto">
          <Tab label="Classes" />
          <Tab label="Endpoints" />
          <Tab label="Diagrams" />
          <Tab label="Call Flows" />
          <Tab label="OpenAPI/Swagger" />
          <Tab label="Feature Files" />
          {data.wsdlFilesContent && Object.keys(data.wsdlFilesContent).length > 0 && <Tab label="WSDL Files" />}
        </Tabs>
      </Box>

      {activeSection === 'Classes' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>Classes & Interfaces</Typography>
          {data.classes && data.classes.length > 0 ? (
            data.classes.map((cls, index) => (
              <Accordion key={index} sx={{ mb: 1}}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography sx={{ width: '70%', flexShrink: 0 }}>
                    {cls.packageName}.<strong>{cls.name}</strong>
                  </Typography>
                  <Chip label={cls.type || 'N/A'} size="small" color="primary" variant="outlined" sx={{ml: 'auto'}} />
                </AccordionSummary>
                <AccordionDetails sx={{backgroundColor: '#f9f9f9'}}>
                  <Typography variant="body2"><strong>File Path:</strong> {cls.filePath}</Typography>
                  {cls.parentClass && <Typography variant="body2"><strong>Extends:</strong> {cls.parentClass}</Typography>}
                  {cls.interfaces && cls.interfaces.length > 0 && 
                    <Typography variant="body2"><strong>Implements:</strong> {cls.interfaces.join(', ')}</Typography>}
                  
                  <Typography variant="subtitle1" sx={{mt:1, mb:0.5}}>Annotations:</Typography>
                  {cls.annotations && cls.annotations.length > 0 ? 
                      (<List dense disablePadding>{cls.annotations.map((ann, i) => <ListItem key={i} disablePadding><ListItemText secondary={`@${ann}`} /></ListItem>)}</List>) : 
                      <Typography variant="body2" color="textSecondary">None</Typography>}

                  <Typography variant="subtitle1" sx={{mt:1, mb:0.5}}>Fields:</Typography>
                  {cls.fields && cls.fields.length > 0 ? 
                      (<List dense disablePadding>{cls.fields.map((field, i) => <ListItem key={i} disablePadding><ListItemText secondary={field} /></ListItem>)}</List>) : 
                      <Typography variant="body2" color="textSecondary">None</Typography>}

                  <Typography variant="subtitle1" sx={{mt:1, mb:0.5}}>Methods:</Typography>
                  {cls.methods && cls.methods.length > 0 ? (
                    cls.methods.map((method, methodIdx) => (
                      <Paper variant="outlined" sx={{p:1, my: 0.5}} key={methodIdx}>
                          <Typography variant="body2" component="div">
                              <code>{method.visibility} {method.static ? 'static' : ''} {method.abstract ? 'abstract' : ''} {method.returnType} <strong>{method.name}</strong>({method.parameters ? method.parameters.join(', ') : ''})</code>
                          </Typography>
                          {method.annotations && method.annotations.length > 0 && 
                              <Typography variant="caption" display="block" color="textSecondary">Annotations: {method.annotations.map(a => `@${a}`).join(', ')}</Typography>}
                           {method.exceptionsThrown && method.exceptionsThrown.length > 0 && 
                              <Typography variant="caption" display="block" color="textSecondary">Throws: {method.exceptionsThrown.join(', ')}</Typography>}
                          {cls.type === 'repository' && method.sqlQueries && method.sqlQueries.length === 0 && (
                            <Box sx={{mt:1, mb:1, p:1, background:'#fafbfc', border:'1px dashed #b3c6e0', borderRadius:1}}>
                              <Typography variant="body2" sx={{color:'#888'}}>No SQL/DB usage detected.</Typography>
                            </Box>
                          )}
                      </Paper>
                    ))
                  ) : <Typography variant="body2" color="textSecondary">None</Typography>}
                </AccordionDetails>
              </Accordion>
            ))
          ) : <Typography>No classes found.</Typography>}
        </Box>
      )}

      {activeSection === 'Endpoints' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>API Endpoints</Typography>
          {data.endpoints && data.endpoints.length > 0 ? (
            <>
              {/* Group endpoints by type for clarity */}
              {['REST', 'SOAP'].map((type) => (
                <Box key={type} sx={{mb: 3}}>
                  <Typography variant="h6" sx={{mb:1}}>{type} Endpoints</Typography>
                  {data.endpoints.filter(ep => (ep.type || '').toUpperCase() === type).length === 0 && (
                    <Typography variant="body2" color="textSecondary">No {type} endpoints found.</Typography>
                  )}
                  {data.endpoints.filter(ep => (ep.type || '').toUpperCase() === type).map((ep, index) => (
                    <Card key={index} sx={{ mb: 2, borderLeft: type === 'SOAP' ? '6px solid #1976d2' : undefined }} variant="outlined">
                      <CardHeader 
                        titleTypographyProps={{variant:'h6'}}
                        title={ep.httpMethod && ep.httpMethod !== 'SOAP' ? `${ep.httpMethod} ${ep.path}` : ep.path}
                        subheader={<>
                          Type: {ep.type} {type === 'SOAP' && <Chip label="SOAP" color="primary" size="small" sx={{ml:1}} />}
                        </>}
                        sx={{pb:0}}
                      />
                      <CardContent>
                        {type === 'SOAP' ? (
                          <>
                            {ep.operationName && <Typography variant="body2"><strong>Operation:</strong> {ep.operationName}</Typography>}
                            {ep.wsdlUrl && data.wsdlFilesContent && data.wsdlFilesContent[ep.wsdlUrl] ? (
                              <Typography variant="body2">
                                <strong>WSDL:</strong> <Link href="#wsdl-{ep.wsdlUrl}" onClick={e => {
                                  e.preventDefault();
                                  // Scroll to WSDL file section if present
                                  const el = document.getElementById(`wsdl-${ep.wsdlUrl.replace(/[^a-zA-Z0-9]/g, '')}`);
                                  if (el) el.scrollIntoView({behavior: 'smooth'});
                                }}>{ep.wsdlUrl}</Link>
                              </Typography>
                            ) : ep.wsdlUrl && <Typography variant="body2"><strong>WSDL:</strong> {ep.wsdlUrl}</Typography>}
                            {ep.requestBodyType && <Typography variant="body2"><strong>Input Message:</strong> {ep.requestBodyType}</Typography>}
                            {ep.responseBodyType && <Typography variant="body2"><strong>Output Message:</strong> {ep.responseBodyType}</Typography>}
                            <Typography variant="body2"><strong>Consumes:</strong> {ep.consumes || 'N/A'}</Typography>
                            <Typography variant="body2"><strong>Produces:</strong> {ep.produces || 'N/A'}</Typography>
                          </>
                        ) : (
                          <>
                            <Typography variant="body2"><strong>Handler:</strong> {ep.handlerMethod}</Typography>
                            {ep.requestBodyType && <Typography variant="body2"><strong>Request Body:</strong> {ep.requestBodyType}</Typography>}
                            {ep.responseBodyType && <Typography variant="body2"><strong>Response Body:</strong> {ep.responseBodyType}</Typography>}
                            <Typography variant="body2"><strong>Consumes:</strong> {ep.consumes || 'N/A'}</Typography>
                            <Typography variant="body2"><strong>Produces:</strong> {ep.produces || 'N/A'}</Typography>
                            {ep.httpStatus && (
                              <Typography variant="body2"><strong>HTTP Status:</strong> {ep.httpStatus}</Typography>
                            )}
                            {ep.requestParamDetails && Object.keys(ep.requestParamDetails).length > 0 && (
                              <Box sx={{mt:1}}>
                                <Typography variant="body2" sx={{fontWeight:'bold'}}>Request Parameters:</Typography>
                                <List dense disablePadding>
                                  {Object.entries(ep.requestParamDetails).map(([param, details], i) => (
                                    <ListItem key={i} sx={{pl:2}}>
                                      <ListItemText
                                        primary={<>
                                          <strong>{param}</strong>
                                          {details.required && (
                                            <Chip label={details.required === 'true' ? 'required' : 'optional'} size="small" color={details.required === 'true' ? 'error' : 'default'} sx={{ml:1}} />
                                          )}
                                          {details.defaultValue && (
                                            <span style={{marginLeft:8, color:'#888'}}>default: <code>{details.defaultValue}</code></span>
                                          )}
                                        </>}
                                      />
                                    </ListItem>
                                  ))}
                                </List>
                              </Box>
                            )}
                          </>
                        )}
                      </CardContent>
                    </Card>
                  ))}
                </Box>
              ))}
            </>
          ) : <Typography>No endpoints found.</Typography>}
        </Box>
      )}

      {activeSection === 'Diagrams' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>Diagrams</Typography>
          <FormControl sx={{ minWidth: 220, mb: 2 }}>
            <InputLabel id="diagram-type-label">Diagram Type</InputLabel>
            <Select labelId="diagram-type-label" value={diagramType} label="Diagram Type" onChange={handleDiagramChange}>
              {data.diagrams && Object.keys(data.diagrams).map((type) => (
                <MenuItem key={type} value={type}>{type.replace(/_/g, ' ')}</MenuItem>
              ))}
            </Select>
          </FormControl>
          {data.diagrams && data.diagrams[diagramType] && (
            <Card sx={{mb:3, borderLeft:'6px solid #43a047', position:'relative'}} variant="outlined">
              <CardHeader title={diagramType.replace(/_/g, ' ')} sx={{background:'#e8f5e9'}}
                action={
                  <Tooltip title="Full Screen">
                    <IconButton onClick={handleFullscreen}><FullscreenIcon /></IconButton>
                  </Tooltip>
                }
              />
              <CardContent>
                <TransformWrapper>
                  <TransformComponent>
                    <img src={`${API_URL.replace("/api","")}${data.diagrams[diagramType]}`} alt={diagramType} style={{ maxWidth: '100%', border: '1px solid #ddd' }}/>
                  </TransformComponent>
                </TransformWrapper>
                <Box sx={{mt: 1}}>
                  <Link href={`/svg-viewer.html?file=${encodeURIComponent(`${API_URL.replace("/api","")}${data.diagrams[diagramType]}`)}`} target="_blank" rel="noopener noreferrer">
                    View SVG in new tab
                  </Link>
                </Box>
              </CardContent>
            </Card>
          )}
          <Modal open={fullscreen} onClose={handleCloseFullscreen} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'rgba(0,0,0,0.85)' }}>
            <Box sx={{ position: 'relative', width: '90vw', height: '90vh', bgcolor: '#fff', borderRadius: 2, boxShadow: 24, p: 2 }}>
              <IconButton onClick={handleCloseFullscreen} sx={{ position: 'absolute', top: 8, right: 8, zIndex: 10 }}><CloseIcon /></IconButton>
              {data.diagrams && data.diagrams[diagramType] && (
                <TransformWrapper>
                  <TransformComponent>
                    <img src={`${API_URL.replace("/api","")}${data.diagrams[diagramType]}`} alt={diagramType} style={{ maxHeight: '85vh', maxWidth: '100%', border: '1px solid #ddd' }}/>
                  </TransformComponent>
                </TransformWrapper>
              )}
            </Box>
          </Modal>
        </Box>
      )}

      {activeSection === 'Call Flows' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>Call Flows</Typography>
          {data.callFlows && Object.keys(data.callFlows).length > 0 ? (
            <List>
              {Object.entries(data.callFlows).map(([entry, flow], idx) => (
                <ListItem key={idx} alignItems="flex-start" sx={{display:'block'}}>
                  <Typography variant="subtitle1" sx={{fontWeight:'bold'}}>{entry}</Typography>
                  <List dense disablePadding sx={{ml:2}}>
                    {flow.map((f, i) => <ListItem key={i} disablePadding><ListItemText primary={f} /></ListItem>)}
                  </List>
                  {/* Sequence diagram for this entrypoint, if available */}
                  {data.sequenceDiagrams && data.sequenceDiagrams[entry] && (
                    <Box sx={{mt:2, mb:2}}>
                      <Typography variant="subtitle2">Sequence Diagram</Typography>
                      <TransformWrapper>
                        <TransformComponent>
                          <img src={`${API_URL.replace("/api","")}${data.sequenceDiagrams[entry]}`} alt={`Sequence Diagram for ${entry}`} style={{ maxWidth: '100%', border: '1px solid #ddd' }}/>
                        </TransformComponent>
                      </TransformWrapper>
                      <Box sx={{mt: 1}}>
                        <Link href={`/svg-viewer.html?file=${encodeURIComponent(`${API_URL.replace("/api","")}${data.sequenceDiagrams[entry]}`)}`} target="_blank" rel="noopener noreferrer">
                          View SVG in new tab
                        </Link>
                      </Box>
                    </Box>
                  )}
                </ListItem>
              ))}
            </List>
          ) : <Typography>No call flow data available.</Typography>}
        </Box>
      )}

      {activeSection === 'OpenAPI/Swagger' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>OpenAPI / Swagger Specification</Typography>
          {isOpenApiUrl ? (
            <Box>
              <Typography paragraph>
                An OpenAPI specification for this Spring Boot project is likely available through the backend. 
                You can typically access the Swagger UI directly via the backend URL:
              </Typography>
              <Link href={`${API_URL.replace('/api','' )}/swagger-ui.html`} target="_blank" rel="noopener noreferrer">
                {`${API_URL.replace('/api','' )}/swagger-ui.html`}
              </Link>
               <Typography paragraph sx={{mt:1}}>
                Or the raw spec at:
              </Typography>
               <Link href={`${API_URL.replace('/api','' )}/v3/api-docs`} target="_blank" rel="noopener noreferrer">
                {`${API_URL.replace('/api','' )}/v3/api-docs`}
              </Link>
              <Typography variant="body2" color="textSecondary" sx={{mt:2}}>{data.openApiSpec}</Typography>
            </Box>
          ) : isOpenApiContent ? (
              <SwaggerUI spec={data.openApiSpec} />
          ) : (
            <Typography>{data.openApiSpec || 'No OpenAPI specification available or could not be determined.'}</Typography>
          )}
        </Box>
      )}

      {activeSection === 'Feature Files' && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 700, color: '#1976d2', mb: 2 }}>Feature Files (Gherkin)</Typography>
          {data.featureFiles && data.featureFiles.length > 0 ? (
            data.featureFiles.map((content, index) => (
              <Paper key={index} variant="outlined" sx={{mb:2, p:2, backgroundColor: '#f5f5f5'}}>
                  <Typography component="pre" sx={{whiteSpace: 'pre-wrap', fontFamily: 'monospace'}}>
                      {content}
                  </Typography>
              </Paper>
            ))
          ) : <Typography>No Gherkin feature files found.</Typography>}
        </Box>
      )}

      {data.wsdlFilesContent && Object.keys(data.wsdlFilesContent).length > 0 && (
        <Box sx={{p: 2, mb: 3, borderRadius: 2, boxShadow: 1, background: '#fafbfc', fontFamily: 'Quicksand'}}>
          <Typography variant="h5" gutterBottom>WSDL Files</Typography>
          {Object.entries(data.wsdlFilesContent).map(([filePath, content], index) => (
            <Accordion key={index} sx={{ mb: 1}}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography sx={{ width: '70%', flexShrink: 0 }}>
                  {filePath}
                </Typography>
              </AccordionSummary>
              <AccordionDetails sx={{backgroundColor: '#f9f9f9'}}>
                <Paper variant="outlined" sx={{mb:2, p:2, backgroundColor: '#fff', maxHeight: '400px', overflowY: 'auto'}}>
                    <Typography component="pre" sx={{whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.875rem'}}>
                        {content}
                    </Typography>
                </Paper>
              </AccordionDetails>
            </Accordion>
          ))}
        </Box>
      )}
    </Box>
  );
}

export default AnalysisDisplay; 