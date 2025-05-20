// Types related to Repository input and basic metadata

export interface RepoInput {
    gitUrl: string;
}

export interface ClonedRepoInfo {
    id: string; // Could be a hash of the URL or a path segment
    projectName: string;
    localPath: string; // Path on the server where it was cloned
    detectedType?: string; // e.g., "Spring Boot (Maven)", "Legacy SOAP"
} 