// Centralized API endpoint definitions
// This can be useful if you have many endpoints and want to manage them in one place.
// Alternatively, these can be defined directly in api.ts or within specific service modules.

const API_ROOT = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

export const ENDPOINTS = {
  ANALYZE_REPO: `${API_ROOT}/repo/analyze`,
  GET_CLASSES: (repoPath: string) => `${API_ROOT}/repo/classes?repoPath=${encodeURIComponent(repoPath)}`,
  // Add more endpoints here as they are defined in the backend
  // e.g., GET_DIAGRAMS: (repoPath: string, diagramType: string) => `${API_ROOT}/repo/diagrams?repoPath=${repoPath}&type=${diagramType}`,
};

// Usage example (in api.ts or a component):
// import { ENDPOINTS } from './endpoints';
// apiClient.post(ENDPOINTS.ANALYZE_REPO, { gitUrl });
// apiClient.get(ENDPOINTS.GET_CLASSES('my-cloned-repo-name')); 