import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8000';

const Publish = ({ repoName }) => {
  const [pageTitle, setPageTitle] = useState('');
  const [spaceKey, setSpaceKey] = useState('');
  const [confluenceUrl, setConfluenceUrl] = useState('');
  const [username, setUsername] = useState('');
  const [apiToken, setApiToken] = useState('');
  const [parentPage, setParentPage] = useState('');
  const [loading, setLoading] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [previewContent, setPreviewContent] = useState('');
  
  // Section selection options
  const [selectedSections, setSelectedSections] = useState({
    api_docs: true,
    features: true, 
    diagrams: true,
    flows: true
  });
  
  // Status indicators for each section
  const [sectionStatus, setSectionStatus] = useState({
    api_docs: 'unknown',
    features: 'unknown',
    diagrams: 'unknown',
    flows: 'unknown'
  });

  // Check available sections when repo changes
  useEffect(() => {
    if (repoName) {
      checkAvailableSections();
    }
  }, [repoName]);

  // Check which sections are available for this repo
  const checkAvailableSections = async () => {
    if (!repoName) return;
    
    setPreviewLoading(true);
    
    try {
      // Check API docs
      try {
        const endpoints = await axios.get(`${API_BASE_URL}/api/repo/endpoints/${repoName}`);
        setSectionStatus(prev => ({ ...prev, api_docs: endpoints.data.controllers && Object.keys(endpoints.data.controllers).length > 0 ? 'available' : 'empty' }));
      } catch (e) {
        setSectionStatus(prev => ({ ...prev, api_docs: 'error' }));
      }
      
      // Check features
      try {
        const features = await axios.get(`${API_BASE_URL}/api/repo/features/${repoName}`);
        setSectionStatus(prev => ({ ...prev, features: features.data.features && features.data.features.length > 0 ? 'available' : 'empty' }));
      } catch (e) {
        setSectionStatus(prev => ({ ...prev, features: 'error' }));
      }
      
      // Check flows
      try {
        const flows = await axios.get(`${API_BASE_URL}/api/repo/flows/${repoName}`);
        setSectionStatus(prev => ({ ...prev, flows: flows.data.flows && Object.keys(flows.data.flows).length > 0 ? 'available' : 'empty' }));
      } catch (e) {
        setSectionStatus(prev => ({ ...prev, flows: 'error' }));
      }
      
      // Diagrams always available
      setSectionStatus(prev => ({ ...prev, diagrams: 'available' }));
      
    } catch (err) {
      console.error("Error checking sections:", err);
    } finally {
      setPreviewLoading(false);
    }
  };
  
  // Generate preview content
  const generatePreview = async () => {
    if (!repoName) return;
    
    setPreviewLoading(true);
    
    try {
      // For now we'll just create a simple markdown preview
      // In a real implementation, this would call a backend API to generate HTML
      let preview = `# ${pageTitle || 'API Documentation'}\n\n`;
      preview += `Documentation for repository: **${repoName}**\n\n`;
      
      const selectedSectionsList = Object.entries(selectedSections)
        .filter(([_, isSelected]) => isSelected)
        .map(([sectionKey, _]) => sectionKey);
      
      if (selectedSectionsList.length === 0) {
        preview += "*No sections selected for publishing*";
      } else {
        preview += "## Selected Sections\n\n";
        
        if (selectedSections.api_docs) {
          preview += "### API Documentation\n";
          preview += "REST API endpoints and their descriptions\n\n";
        }
        
        if (selectedSections.features) {
          preview += "### Feature Files\n";
          preview += "Behavioral specifications and scenarios\n\n";
        }
        
        if (selectedSections.diagrams) {
          preview += "### System Diagrams\n";
          preview += "Visual representations of system architecture\n\n";
        }
        
        if (selectedSections.flows) {
          preview += "### Flow Summaries\n";
          preview += "End-to-end flow descriptions\n\n";
        }
      }
      
      setPreviewContent(preview);
    } catch (err) {
      console.error("Error generating preview:", err);
      setPreviewContent("Error generating preview content");
    } finally {
      setPreviewLoading(false);
    }
  };

  // Update preview when selections change
  useEffect(() => {
    if (repoName) {
      generatePreview();
    }
  }, [repoName, pageTitle, selectedSections]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    
    try {
      // Get the selected sections as an array of string IDs
      const selected_sections = Object.entries(selectedSections)
        .filter(([_, isSelected]) => isSelected)
        .map(([sectionKey, _]) => sectionKey);

      if (selected_sections.length === 0) {
        setResult({
          status: 'error',
          message: 'Please select at least one section to publish'
        });
        setLoading(false);
        return;
      }
      
      const response = await axios.post(`${API_BASE_URL}/api/repo/publish/confluence`, {
        repo_name: repoName,
        page_title: pageTitle || `API Documentation for ${repoName}`,
        space_key: spaceKey,
        parent_page: parentPage || undefined,
        confluence_url: confluenceUrl,
        username: username,
        api_token: apiToken,
        selected_sections: selected_sections
      });
      
      setResult({
        status: response.data.status,
        message: response.data.message,
        url: response.data.url
      });
    } catch (err) {
      setResult({
        status: 'error',
        message: err.response?.data?.message || err.message || 'Failed to publish to Confluence'
      });
    } finally {
      setLoading(false);
    }
  };

  // Helper function to get status label
  const getStatusLabel = (status) => {
    switch (status) {
      case 'available': return { text: 'Available', color: 'text-green-700 bg-green-100' };
      case 'empty': return { text: 'No content', color: 'text-yellow-700 bg-yellow-100' };
      case 'error': return { text: 'Error', color: 'text-red-700 bg-red-100' };
      default: return { text: 'Unknown', color: 'text-gray-700 bg-gray-100' };
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm">
      <h2 className="text-xl font-semibold text-gray-800 mb-4">Review & Publish to Confluence</h2>
      
      {!repoName ? (
        <p className="text-gray-500">Please submit a repository to publish documentation.</p>
      ) : (
        <div className="space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* Left column - Content selection */}
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-3">Documentation Content</h3>
              <div className="bg-gray-50 p-4 rounded-md">
                <div className="mb-4">
                  <label htmlFor="pageTitle" className="block text-sm font-medium text-gray-700 mb-1">
                    Page Title
                  </label>
                  <input
                    type="text"
                    id="pageTitle"
                    value={pageTitle}
                    onChange={(e) => setPageTitle(e.target.value)}
                    placeholder={`API Documentation for ${repoName}`}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>
              
                <div className="mt-4">
                  <h4 className="font-medium text-sm text-gray-700 mb-2">
                    Sections to Include
                  </h4>
                  <div className="space-y-2">
                    {/* API Docs checkbox */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <input
                          id="api_docs"
                          name="api_docs"
                          type="checkbox"
                          checked={selectedSections.api_docs}
                          onChange={(e) => setSelectedSections(prev => ({ ...prev, api_docs: e.target.checked }))}
                          className="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        />
                        <label htmlFor="api_docs" className="ml-2 block text-sm text-gray-700">
                          API Documentation
                        </label>
                      </div>
                      <span className={`text-xs px-2 py-1 rounded-full ${getStatusLabel(sectionStatus.api_docs).color}`}>
                        {getStatusLabel(sectionStatus.api_docs).text}
                      </span>
                    </div>
                    
                    {/* Features checkbox */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <input
                          id="features"
                          name="features"
                          type="checkbox"
                          checked={selectedSections.features}
                          onChange={(e) => setSelectedSections(prev => ({ ...prev, features: e.target.checked }))}
                          className="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        />
                        <label htmlFor="features" className="ml-2 block text-sm text-gray-700">
                          Feature Files
                        </label>
                      </div>
                      <span className={`text-xs px-2 py-1 rounded-full ${getStatusLabel(sectionStatus.features).color}`}>
                        {getStatusLabel(sectionStatus.features).text}
                      </span>
                    </div>
                    
                    {/* Diagrams checkbox */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <input
                          id="diagrams"
                          name="diagrams"
                          type="checkbox"
                          checked={selectedSections.diagrams}
                          onChange={(e) => setSelectedSections(prev => ({ ...prev, diagrams: e.target.checked }))}
                          className="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        />
                        <label htmlFor="diagrams" className="ml-2 block text-sm text-gray-700">
                          System Diagrams
                        </label>
                      </div>
                      <span className={`text-xs px-2 py-1 rounded-full ${getStatusLabel(sectionStatus.diagrams).color}`}>
                        {getStatusLabel(sectionStatus.diagrams).text}
                      </span>
                    </div>
                    
                    {/* Flows checkbox */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <input
                          id="flows"
                          name="flows"
                          type="checkbox"
                          checked={selectedSections.flows}
                          onChange={(e) => setSelectedSections(prev => ({ ...prev, flows: e.target.checked }))}
                          className="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                        />
                        <label htmlFor="flows" className="ml-2 block text-sm text-gray-700">
                          Flow Summaries
                        </label>
                      </div>
                      <span className={`text-xs px-2 py-1 rounded-full ${getStatusLabel(sectionStatus.flows).color}`}>
                        {getStatusLabel(sectionStatus.flows).text}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Right column - Preview */}
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-3">Content Preview</h3>
              <div className="bg-gray-50 p-4 rounded-md h-96 overflow-auto">
                {previewLoading ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="spinner inline-block w-8 h-8 border-4 border-t-indigo-500 border-gray-200 rounded-full animate-spin"></div>
                  </div>
                ) : (
                  <div className="markdown-preview prose max-w-none whitespace-pre-wrap">
                    {previewContent ? (
                      <pre className="text-sm text-gray-800 whitespace-pre-wrap">{previewContent}</pre>
                    ) : (
                      <p className="text-gray-500 italic text-center">No content preview available</p>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* Confluence settings */}
          <h3 className="text-lg font-medium text-gray-900 mb-3">Confluence Settings</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="spaceKey" className="block text-sm font-medium text-gray-700 mb-1">
                  Space Key <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  id="spaceKey"
                  value={spaceKey}
                  onChange={(e) => setSpaceKey(e.target.value)}
                  placeholder="TEAM"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              <div>
                <label htmlFor="parentPage" className="block text-sm font-medium text-gray-700 mb-1">
                  Parent Page ID (Optional)
                </label>
                <input
                  type="text"
                  id="parentPage"
                  value={parentPage}
                  onChange={(e) => setParentPage(e.target.value)}
                  placeholder="123456"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
            </div>
            
            <div>
              <label htmlFor="confluenceUrl" className="block text-sm font-medium text-gray-700 mb-1">
                Confluence URL <span className="text-red-500">*</span>
              </label>
              <input
                type="url"
                id="confluenceUrl"
                value={confluenceUrl}
                onChange={(e) => setConfluenceUrl(e.target.value)}
                placeholder="https://your-domain.atlassian.net/wiki"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                required
              />
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                  Username (Email) <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="your-email@company.com"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              <div>
                <label htmlFor="apiToken" className="block text-sm font-medium text-gray-700 mb-1">
                  API Token <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  id="apiToken"
                  value={apiToken}
                  onChange={(e) => setApiToken(e.target.value)}
                  placeholder="••••••••••••••••"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
            </div>
            
            <div className="pt-4">
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-300"
              >
                {loading ? 'Publishing...' : 'Publish to Confluence'}
              </button>
            </div>
          </form>
          
          {result && (
            <div className={`mt-4 p-3 rounded-md ${
              result.status === 'success' ? 'bg-green-50 text-green-800' : 
              result.status === 'error' ? 'bg-red-50 text-red-800' : 
              'bg-blue-50 text-blue-800'
            }`}>
              <p>{result.message}</p>
              {result.url && (
                <p className="mt-2">
                  <a 
                    href={result.url} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="text-indigo-600 hover:text-indigo-800 underline"
                  >
                    View published page
                  </a>
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Publish; 