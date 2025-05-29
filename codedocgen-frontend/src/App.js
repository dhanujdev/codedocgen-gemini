import React, { useState, useCallback } from 'react';
import axios from 'axios';
import { Box, CssBaseline, createTheme, ThemeProvider } from '@mui/material';
import Sidebar from './components/Sidebar';
import OverviewPage from './pages/Overview';
import EndpointsPage from './pages/Endpoints';
import FeaturesPage from './pages/Features';
import EntitiesPage from './pages/Entities';
import AllClassesPage from './pages/AllClassesPage';
import DiagramsPage from './pages/Diagrams';
import PublishPage from './pages/Publish';
import HomePage from './pages/Home';
import ApiSpecsPage from './pages/ApiSpecsPage';
import CallFlowPage from './pages/CallFlowPage';
import DatabasePage from './pages/Database';
import LoggerInsightsPage from './pages/LoggerInsightsPage';
import '@fontsource/quicksand/400.css';
import '@fontsource/quicksand/700.css';

// Basic theme
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1976d2' },
    secondary: { main: '#43a047' },
    background: { default: '#f8fafc', paper: '#fff' },
  },
  typography: {
    fontFamily: 'Quicksand, Inter, Roboto, Arial, sans-serif',
    h1: { fontSize: '2.2rem', fontWeight: 600 },
    h6: { fontWeight: 500 },
  },
});

// Align with API_BASE_URL in api.ts
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

function App() {
  const [analysisResult, setAnalysisResult] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  // const [aboutOpen, setAboutOpen] = useState(false); // Assuming this is for a modal, not directly related to API calls
  const [activeSection, setActiveSection] = useState('Home'); // Changed initial to 'Home'
  const [repoName, setRepoName] = useState('');
  // Removed individual states for endpoints, features, entities, diagramMap as they are part of analysisResult
  // const [endpoints, setEndpoints] = useState([]); 
  // const [features, setFeatures] = useState(null); 
  // const [entities, setEntities] = useState([]); 
  // const [diagramMap, setDiagramMap] = useState(null);

  const extractRepoName = (repoUrl) => {
    const match = repoUrl.match(/github\.com[/:][^/]+\/([^/.]+)(?:\.git)?/);
    return match ? match[1] : repoUrl; // Fallback to full URL if no match
  };

  const handleAnalyze = useCallback(async (repoUrl) => {
    setIsLoading(true);
    setError(null);
    setAnalysisResult(null);
    // setEndpoints([]); // No longer needed
    // setFeatures(null); // No longer needed
    // setEntities([]); // No longer needed
    // setDiagramMap(null); // No longer needed
    // console.log('[App.js] handleAnalyze: diagramMap reset to null'); // No longer needed
    setRepoName('');

    try {
      // Use API_BASE_URL and specific endpoint
      const response = await axios.post(`${API_BASE_URL}/analysis/analyze`, { repoUrl });
      const data = response.data;
      setAnalysisResult(data); // This now includes logStatements
      
      const extractedName = extractRepoName(repoUrl);
      setRepoName(extractedName || data.projectName || 'Repository');
      
      // These are now accessed via analysisResult directly in child components if needed
      // setEndpoints(data.endpoints || []);
      // setFeatures(data.featureFiles || null); 
      // setEntities(data.classes || []); 
      // console.log('[App.js] handleAnalyze: Attempting to set diagramMap from data.diagrams:', JSON.stringify(data.diagrams));
      // setDiagramMap(data.diagrams || null); 
      
      setActiveSection('Overview');
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to analyze repository.');
      // Clear data on error
      setAnalysisResult(null);
      // setEndpoints([]); // No longer needed
      // setFeatures(null); // No longer needed
      // setEntities([]); // No longer needed
      // setDiagramMap(null); // No longer needed
      // console.log('[App.js] handleAnalyze error: diagramMap reset to null'); // No longer needed
      setRepoName('');
    }
    setIsLoading(false);
  }, []);

  const handleCloseSnackbar = () => setError(null);

  const handleNav = (item) => {
    // if (item.label === 'About') setAboutOpen(true);
    // else setActiveSection(item.label);
    setActiveSection(item.label); // Simplified for now
  };

  // Log diagramMap before rendering DiagramsPage
  // if (activeSection === 'Diagrams') { // No longer needed as diagramMap is part of analysisResult
  //   console.log('[App.js] Rendering DiagramsPage, current diagramMap state:', JSON.stringify(diagramMap));
  // }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', minHeight: '100vh', fontFamily: 'Quicksand, Inter, Roboto, Arial, sans-serif', bgcolor: '#f8fafc' }}>
        <Sidebar activeSection={activeSection} setActiveSection={setActiveSection} onNav={handleNav} />
        <Box sx={{ flex: 1, p: 0, display: 'flex', flexDirection: 'column', minHeight: '100vh', alignItems: 'center', background: '#f8fafc' }}>
          <Box sx={{ width: '100%', p: 4, pt: 3, pb: 6, background: '#fff', borderRadius: 3, boxShadow: 2, mt: 4, mb: 4, minHeight: 600 }}>
            {activeSection === 'Home' && <HomePage onAnalyze={handleAnalyze} isLoading={isLoading} error={error} onCloseError={handleCloseSnackbar} />}
            {activeSection === 'Overview' && <OverviewPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'Endpoints' && <EndpointsPage endpoints={analysisResult?.endpoints} repoName={repoName}/>} {/* Pass from analysisResult */}
            {activeSection === 'API Specs' && <ApiSpecsPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'Call Flow' && <CallFlowPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'Features' && <FeaturesPage features={analysisResult?.featureFiles} repoName={repoName} />} {/* Pass from analysisResult */}
            {activeSection === 'Entities' && <EntitiesPage entities={analysisResult?.classes} endpoints={analysisResult?.endpoints} repoName={repoName} />} {/* Pass from analysisResult */}
            {activeSection === 'Database' && <DatabasePage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'All Classes' && <AllClassesPage entities={analysisResult?.classes} repoName={repoName} />} {/* Pass from analysisResult */}
            {activeSection === 'Diagrams' && 
              <DiagramsPage diagramMap={analysisResult?.diagrams} sequenceDiagrams={analysisResult?.sequenceDiagrams} rawCallFlows={analysisResult?.callFlows} repoName={repoName} />}
            {activeSection === 'Logger Insights' && <LoggerInsightsPage analysisData={analysisResult} analysisLoading={isLoading} analysisError={error} repoUrl={analysisResult?.projectName} /> } {/* Added LoggerInsightsPage */}
            {activeSection === 'Publish' && <PublishPage repoName={repoName} analysisResult={analysisResult}/>} {/* PublishPage might need full analysisResult*/}
          </Box>
          <Box sx={{ textAlign: 'center', color: '#aaa', fontSize: 14, mb: 2, mt: 'auto' }}>
            © {new Date().getFullYear()} CodeDocGen. All rights reserved.
          </Box>
        </Box>
      </Box>
      {/* Simple error display, can be improved with a Snackbar component */}
      {error && (
        <Box sx={{ position: 'fixed', bottom: 20, left: '50%', transform: 'translateX(-50%)', background: 'red', color: 'white', padding: '10px 20px', borderRadius: '5px' }}>
          {error}
          <button onClick={handleCloseSnackbar} style={{ marginLeft: '15px', color: 'white', background: 'transparent', border: 'none' }}>X</button>
        </Box>
      )}
    </ThemeProvider>
  );
}

export default App; 