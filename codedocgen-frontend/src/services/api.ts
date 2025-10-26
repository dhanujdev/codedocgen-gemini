import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface RepoAnalysisRequest {
  repoUrl: string;
}

// Define more specific response types based on ParsedDataResponse from backend DTO
export interface ClassDetail {
    name: string;
    packageName: string;
    type: string;
    // ... other fields from ClassMetadata
}

export interface EndpointDetail {
    path: string;
    httpMethod: string;
    // ... other fields from EndpointMetadata
}

export interface ParsedData {
  projectName: string;
  projectType: string;
  isSpringBootProject?: boolean;
  springBootVersion?: string;
  classes: ClassDetail[];
  endpoints: EndpointDetail[];
  diagrams?: { [key: string]: string };
  featureFiles?: { [key: string]: string };
  parseWarnings?: string[];
  openApiSpec?: string;
  projectSummary?: string;
  callFlows?: { [key: string]: string[] };
  sequenceDiagrams?: { [key: string]: string };
  wsdlFilesContent?: { [key: string]: string };
}


// Example API service functions
export const analyzeRepository = async (data: RepoAnalysisRequest): Promise<ParsedData> => {
  try {
    const response = await apiClient.post('/analysis/analyze', data);
    return response.data;
  } catch (error) {
    console.error('Error analyzing repository:', error);
    throw error;
  }
};

// Add other API service calls here as needed

export default apiClient; 
