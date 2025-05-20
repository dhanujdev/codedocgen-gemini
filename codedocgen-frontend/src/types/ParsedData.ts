// Mirroring backend DTOs and Models for frontend type safety

// From backend model/MethodMetadata.java
export interface MethodMetadata {
    name: string;
    returnType: string;
    parameters: string[];
    thrownExceptions: string[];
    annotations: Record<string, string>; 
    isPublic: boolean;
    isStatic: boolean;
}

// From backend model/ClassMetadata.java
export interface ClassMetadata {
    name: string;
    packageName: string;
    type: string; // controller, service, repository, entity, etc.
    methods: MethodMetadata[];
    fields: string[];
    superClass?: string;
    interfaces: string[];
    annotations: Record<string, string>;
    filePath: string;
}

// From backend model/EndpointMetadata.java
export interface EndpointMetadata {
    path: string;
    httpMethod: string;
    methodName: string;
    className: string;
    requestParams: string[];
    requestBodyType?: string;
    responseBodyType?: string;
    consumes?: string;
    produces?: string;
    description?: string;
    isLegacySoap: boolean;
}

// From backend model/DiagramType.java (simplified for frontend, maybe just strings)
export type DiagramType = 
    | "CLASS_DIAGRAM"
    | "ENTITY_RELATIONSHIP_DIAGRAM"
    | "CALL_FLOW_DIAGRAM"
    | "SEQUENCE_DIAGRAM";

export interface DiagramData {
    title: string;
    type: DiagramType | string; // Allow for other custom diagram types
    content?: string; // Raw PlantUML/Mermaid text or other data
    url?: string;     // URL to a generated image (e.g., PNG/SVG)
}

// From backend dto/ParsedDataResponse.java
export interface ParsedDataResponse {
    projectName: string;
    projectType: string;
    classes: ClassMetadata[];
    endpoints: EndpointMetadata[];
    diagrams?: DiagramData[]; // Assuming diagrams will be added
    architecturalSummary?: string; // Assuming this will be added
}

// Maybe a simplified/transformed version for direct UI use if needed
export type UIEnhancedParsedData = ParsedDataResponse & {
    // Add any frontend-specific enhancements or computed properties here
    // For example, a map of classes by type for easier filtering
    classesByType?: Record<string, ClassMetadata[]>;
}; 