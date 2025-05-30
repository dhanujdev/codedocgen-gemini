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
import PiiPciScanPage from './pages/PiiPciScanPage';
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
  const [activeSection, setActiveSection] = useState('home'); // Ensure initial matches a key
  const [repoName, setRepoName] = useState('');

  const extractRepoName = (repoUrl) => {
    const match = repoUrl.match(/github\.com[/:][^/]+\/([^/.]+)(?:\.git)?/);
    return match ? match[1] : repoUrl; // Fallback to full URL if no match
  };

  const handleAnalyze = useCallback(async (repoUrl) => {
    setIsLoading(true);
    setError(null);
    setAnalysisResult(null);
    setRepoName('');

    try {
      // Use API_BASE_URL and specific endpoint
      const response = await axios.post(`${API_BASE_URL}/analysis/analyze`, { repoUrl });
      const data = response.data;
      setAnalysisResult(data); // This now includes logStatements
      
      const extractedName = extractRepoName(repoUrl);
      setRepoName(extractedName || data.projectName || 'Repository');
      
      setActiveSection('overview'); // Use key 'overview'
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to analyze repository.');
      // Clear data on error
      setAnalysisResult(null);
      setRepoName('');
    }
    setIsLoading(false);
  }, []);

  const handleCloseSnackbar = () => setError(null);

  const handleNav = (itemKey) => { // Changed parameter to itemKey for clarity
    setActiveSection(itemKey);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', minHeight: '100vh', fontFamily: 'Quicksand, Inter, Roboto, Arial, sans-serif', bgcolor: '#f8fafc' }}>
        <Sidebar activeSection={activeSection} setActiveSection={setActiveSection} />
        <Box sx={{ flex: 1, p: 0, display: 'flex', flexDirection: 'column', minHeight: '100vh', alignItems: 'center', background: '#f8fafc' }}>
          <Box sx={{ width: '100%', p: 4, pt: 3, pb: 6, background: '#fff', borderRadius: 3, boxShadow: 2, mt: 4, mb: 4, minHeight: 600 }}>
            {activeSection === 'home' && <HomePage onAnalyze={handleAnalyze} isLoading={isLoading} error={error} onCloseError={handleCloseSnackbar} />}
            {activeSection === 'overview' && <OverviewPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'endpoints' && <EndpointsPage endpoints={analysisResult?.endpoints} repoName={repoName}/>}
            {activeSection === 'apiSpecs' && <ApiSpecsPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'callFlow' && <CallFlowPage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'features' && <FeaturesPage features={analysisResult?.featureFiles} repoName={repoName} />}
            {activeSection === 'entities' && <EntitiesPage entities={analysisResult?.classes} endpoints={analysisResult?.endpoints} repoName={repoName} />}
            {activeSection === 'database' && <DatabasePage analysisResult={analysisResult} repoName={repoName} />}
            {activeSection === 'allClasses' && <AllClassesPage entities={analysisResult?.classes} repoName={repoName} />}
            {activeSection === 'diagrams' && 
              <DiagramsPage diagramMap={analysisResult?.diagrams} sequenceDiagrams={analysisResult?.sequenceDiagrams} rawCallFlows={analysisResult?.callFlows} repoName={repoName} />}
            {activeSection === 'loggerInsights' && <LoggerInsightsPage analysisData={analysisResult} analysisLoading={isLoading} analysisError={error} repoUrl={analysisResult?.projectName} /> }
            {activeSection === 'piiPciScan' && <PiiPciScanPage analysisData={analysisResult} />}
            {activeSection === 'publish' && <PublishPage repoName={repoName} analysisResult={analysisResult}/>}
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