# CodeDocGen - Implementation Summary

This document summarizes the current state of the CodeDocGen project, covering both the backend and frontend components.

## 1. Project Overview

*   **Goal:** To analyze Java-based applications from public Git repositories, generate documentation, visualizations (diagrams), and provide insights into the codebase.
*   **Tech Stack:**
    *   **Backend:** Java 21, Spring Boot 3.2.x (Maven), JGit, JavaParser (with JavaSymbolSolver), PlantUML, CommonMark, SpringDoc OpenAPI.
    *   **Frontend:** React (Create React App), Material UI (with potential Tailwind/shadcn elements), Axios, react-zoom-pan-pinch, swagger-ui-react, react-router-dom.
*   **Core Workflow:**
    1.  User provides a public Git repository URL via the frontend.
    2.  Frontend sends the URL to the backend's `/api/analysis/analyze` endpoint.
    3.  Backend clones the repository, extracts project name, performs deep analysis (project type detection, Spring Boot versioning, **advanced Java parsing with symbol resolution**, class/method metadata extraction, **call flow tracing**, **DAO/DB analysis**, endpoint extraction, generation of multiple diagram types), and returns a structured `ParsedDataResponse`.
    4.  Frontend displays the received information in a user-friendly sidebar-navigated interface, correctly loading served diagrams and detailed analysis results.

## 2. Backend Details (`codedocgen-backend`)

### 2.1. Main Application & Configuration

*   **`CodeDocGenApplication.java`:** Main Spring Boot application class.
*   **`pom.xml`:** Manages dependencies, including `com.github.javaparser:javaparser-symbol-solver`.
*   **`application.yml`:** Configures server port, application name, logging levels (including specific `TRACE` levels for parser components like `com.codedocgen.parser.ClassMetadataVisitorLogger`), and configurable paths for `repoStoragePath`, `outputBasePath`, `docsStoragePath`, `diagramsStoragePath`.
*   **`config/WebConfig.java`:** Configures CORS and static resource serving for `/generated-output/**`.

### 2.2. DTOs (Data Transfer Objects) - `com.codedocgen.dto`

*   **`RepoRequest.java`:** `repoUrl` (String).
*   **`ParsedDataResponse.java`:** Main response DTO.
    *   `projectName`, `projectType`, `springBootVersion`, `springBootProject` (boolean, serialized from `isSpringBootProject`).
    *   `classes` (List<ClassMetadata>): All parsed classes.
    *   `endpoints` (List<EndpointMetadata>): Extracted API endpoints.
    *   `diagrams` (Map<String, String>): Map of diagram types (as String keys, e.g., "CLASS_DIAGRAM", "SEQUENCE_DIAGRAM_fqn.method") to server-relative SVG URLs.
    *   `projectSummary` (String): Textual project summary.
    *   `openApiSpec` (String): OpenAPI spec content.
    *   `featureFiles` (List<String>): Gherkin feature file contents.
    *   `wsdlFilesContent` (Map<String, String>): WSDL file contents.
    *   `xsdFilesContent` (Map<String, String>): XSD file contents.
    *   `callFlows` (Map<String, List<String>>): Raw call flow steps for each entry point (FQN key).
    *   `daoOperations` (Map<String, List<DaoOperationDetail>>): DAO operations grouped by class FQN.
    *   `dbDiagramPath` (String): Server-relative path to the database schema diagram.
*   **`MavenExecutionResult.java`:** For results of `mvn` commands (classpath, compile status).

### 2.3. Models - `com.codedocgen.model`

*   **`ClassMetadata.java`:** Name, packageName, type, annotations, methods, fields (List<FieldMetadata>), parentClass, interfaces, filePath, etc.
*   **`MethodMetadata.java`:** Name, returnType, parameters (List<String> `type name`), annotations, exceptions, visibility, static, abstract, `calledMethods` (List<String> - fully resolved or descriptive unresolved FQNs), `parameterAnnotations`, `daoOperations` (List<DaoOperationDetail>), `sqlQueries`, `sqlTables`, `sqlOperations`.
*   **`FieldMetadata.java`:** Name, type, annotations, visibility, static, final, `initializer` (String).
*   **`EndpointMetadata.java`:** Path, httpMethod, handlerMethod, requestBodyType, responseBodyType, etc.
*   **`DiagramType.java` (enum):** `CLASS_DIAGRAM`, `SEQUENCE_DIAGRAM`, `COMPONENT_DIAGRAM`, `USECASE_DIAGRAM`, `DATABASE_DIAGRAM`, `ENTITY_RELATIONSHIP_DIAGRAM`.
*   **`DaoOperationDetail.java`:** Details for a single DAO operation (method name, return type, parameters, SQL query, tables, operation type).

### 2.4. Services - `com.codedocgen.service` & `com.codedocgen.service.impl`

*   **`GitService` / `GitServiceImpl`:** Clones and cleans Git repos.
*   **`ProjectDetectorService` / `ProjectDetectorServiceImpl`:** Detects build tool, Spring Boot presence, and version.
*   **`JavaParserService` / `JavaParserServiceImpl`:**
    *   Core parsing engine using JavaParser and **JavaSymbolSolver**.
    *   Initializes symbol solver with project sources, dependencies (via `mvn dependency:build-classpath`), and compiled classes (`target/classes` after running `mvn compile`).
    *   `ClassMetadataVisitor` extracts detailed class, method, and field metadata.
    *   Uses FQNs and provides descriptive fallbacks for unresolved method calls (e.g., `UNRESOLVED_CALL: com.example.MyClass.myMethod(params)`).
    *   Determines class types accurately (controller, service, repository, entity, model, etc.).
*   **`EndpointExtractorService` / `EndpointExtractorServiceImpl`:** Extracts REST endpoint info.
*   **`DiagramService` / `DiagramServiceImpl`:**
    *   Generates Class, Component, Usecase, Sequence, ERD, and DB Schema diagrams as SVGs using PlantUML.
    *   `generateSequenceDiagram`: Uses raw call flows from `CallFlowAnalyzer` and cleans participant names/labels.
    *   Enhanced Component and Usecase diagram generation for better insights into SOAP/legacy systems and multi-tier architectures.
*   **`DocumentationService` / `DocumentationServiceImpl`:** Generates project summaries, finds feature/WSDL/XSD files, creates Markdown/HTML docs.
*   **`DaoAnalysisService` / `DaoAnalysisServiceImpl`:**
    *   Analyzes repository interfaces and classes identified by `JavaParserServiceImpl`.
    *   Uses `DaoAnalyzer` helper to detect SQL queries and table names from method bodies.
    *   Infers operations from Spring Data method names and extracted SQL.
    *   Provides data for the DB schema diagram and DAO operation listings.

### 2.5. Parsers - `com.codedocgen.parser`

*   **`CallFlowAnalyzer.java`:** Performs DFS on method metadata (from `JavaParserServiceImpl`) to trace and build detailed call flow sequences for entry points.
*   **`DaoAnalyzer.java`:** Utility class for `DaoAnalysisServiceImpl` to find SQL queries and table names in method bodies.
*   **`SoapWsdlParser.java`**, **`YamlParser.java`**: Existing parsers.

### 2.6. Controller - `com.codedocgen.controller`

*   **`AnalysisController.java`:** Orchestrates the entire analysis workflow by calling services in sequence. Populates and returns `ParsedDataResponse` including all analysis results, diagram paths, call flows, and DAO operations.

## 3. Frontend Details (`codedocgen-frontend`)

### 3.1. Project Setup & Key Libraries

*   Create React App based, using Material UI, Axios, `react-router-dom`, `swagger-ui-react`, `react-zoom-pan-pinch`.
*   Environment variables in `.env` for `REACT_APP_API_URL` and `REACT_APP_BACKEND_STATIC_URL`.

### 3.2. Core Components & Pages - `src/`

*   **`App.js`:** Main component, manages state (`analysisResult`, `isLoading`, etc.), routing, and renders page components via `Sidebar` navigation.
*   **`Sidebar.js`:** Main navigation for Overview, API Specs, Call Flow, Classes, Diagrams, Database, Gherkin.
*   **`OverviewPage.js`:** Project summary, Spring Boot status/version.
*   **`ClassesPage.js`:** Detailed view of parsed classes and their members.
*   **`ApiSpecsPage.js`:** Displays OpenAPI specs via SwaggerUI and detailed WSDL/XSD structures.
*   **`CallFlowPage.js`:** Displays sequence diagrams (via `DiagramViewer.js`) and associated raw call steps for each identified call flow.
*   **`DiagramsPage.js`:** Displays general diagrams (Class, Component, Usecase, ERD) via `DiagramViewer.js`.
*   **`DatabasePage.js`:** Displays the Database Schema diagram, lists detected entities, and DAO operations.
*   **`GherkinPage.js`:** Shows Gherkin feature file content.
*   **`DiagramViewer.js`:** Reusable component for rendering SVG diagrams with zoom/pan.
*   **`services/api.js`:** Axios service for backend communication.
*   **`constants/uiConstants.js`:** For `BACKEND_STATIC_BASE_URL`.

## 4. Functionality Achieved (Key Highlights)

*   **Deep Java Analysis:** Robust parsing of Java code using symbol resolution (JavaSymbolSolver) for accurate FQN-based method call tracing, type determination, and metadata extraction.
*   **Comprehensive Call Flow Analysis:** Generation of detailed sequence diagrams and raw call steps, with clear labeling of resolved and unresolved calls (including JDK/framework calls).
*   **Database & DAO Insights:** Detection of entities, DAO/repository operations (Spring Data & basic SQL), and generation of a database schema diagram.
*   **Multiple Diagram Types:** Class, Sequence, Component, Usecase, ERD, and Database Schema diagrams generated as SVGs.
*   **Accurate Spring Boot Detection:** Reliable identification of Spring Boot projects and versions.
*   **User-Friendly Frontend:** Clear presentation of all analysis results via a sidebar-navigated interface, with interactive diagram viewers.
*   **Detailed API Specification Display:** OpenAPI via SwaggerUI and rich, parsed WSDL/XSD views.

## 5. Build Status

*   Backend and Frontend should build and run successfully following the extensive debugging and refinement.

## 6. Known Limitations & TODOs (High-Level)

*   **Advanced Parsing:**
    *   Further refinement of REST endpoint detail extraction (complex request/response bodies).
    *   Deeper YAML parsing if used for project configuration beyond basic Spring Boot.
*   **Diagrams & Visualization:**
    *   More interactive call flow visualization beyond static sequence diagrams.
*   **Frontend Enhancements:**
    *   UI/UX improvements for very large datasets (advanced filtering, searching, pagination).
    *   Dark mode.
*   **Backend Enhancements:**
    *   Configuration for private Git repositories.
    *   Performance optimizations for extremely large codebases.
*   **Export Features:** Confluence publishing, PDF/HTML downloads remain future scope.

## Recent Updates (May 2025)
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
- CORS is enabled for `/generated-output/**` so the frontend and backend can fetch diagrams directly from the backend.
- JavaParser upgraded to 3.26.4 for robust Java 7-21 compatibility.
- If any files fail to parse, a warning is shown in the UI listing the affected files.
- A new `svg-viewer.html` (in the frontend public folder) allows zoomable, fit-to-screen SVG viewing in a new browser tab.
- Sequence diagrams now use quoted fully qualified names (FQNs) for PlantUML participants and arrows, ensuring valid syntax and correct rendering.
- SVG is the default output format for all diagrams.
- No PlantUML upgrade was needed; version 1.2025.2 is current.
- Performance optimization is a future enhancement.
- Entity Relationship diagrams now properly identify JPA entities using annotation-based detection, regardless of naming conventions.
- The Entity tab UI has been simplified to focus on a text-based representation of entities and their fields, removing the direct diagram display that was causing issues.
- Entity details now include highlighted primary keys and field types in a more user-friendly format.
- Entity relationship diagrams remain available through the dedicated Diagrams tab, which provides better rendering and zoom capabilities.
- **Symbol Solver Pre-compilation (Backend)**: Implemented `mvn compile -DskipTests -q` pre-step in `JavaParserServiceImpl` for target Maven projects. This makes generated sources (e.g., JAXB) available, vastly improving symbol resolution for method calls and accuracy of call flow diagrams.
- **WSDL & XSD Deep Parsing (Frontend & Backend)**: Backend now provides XSD content via `ParsedDataResponse.xsdFilesContent`. Frontend's `ApiSpecsPage.js` uses this to parse WSDL-imported XSDs, enabling a detailed, recursive display of SOAP operation message structures, including elements, attributes, and types.
- **Call Flow Page & Sidebar Navigation (Frontend)**: Introduced `CallFlowPage.js` to display sequence diagrams. Main navigation shifted to a `Sidebar.js` component, managing views for Overview, API Specs, Call Flow, Classes, Diagrams, and Gherkin Features.
- **Refined API Specs UI**: Improved `ApiSpecsPage.js` to handle both SwaggerUI for OpenAPI and the new detailed WSDL/XSD viewer for SOAP projects, with appropriate fallbacks and raw XML display options.
- **Enhanced Component Diagram for SOAP/Legacy Applications**: Significantly improved component diagram generation with expanded detection for SOAP services, endpoints and legacy components:
  - Now identifies components based on class name patterns (e.g., *Endpoint, *ServiceImpl), annotations (@WebService, @Endpoint, @SOAPBinding), and conventional stereotypes
  - Adds WSDL representation as interfaces in the diagram
  - Uses component stereotypes (<<endpoint>>, <<service>>, etc.) for better visual categorization
  - Provides meaningful relationships between components (e.g., "delegates to", "implements", "uses")
  - Displays components with simplified names for better readability
  - Visually distinguishes between different component types (controllers, services, repositories, endpoints)
  - Shows hierarchical package organization of components
- **Comprehensive Usecase Diagram for SOAP/Legacy Applications**: Complete overhaul of usecase diagram generation to better support SOAP and legacy applications:
  - Added "External System" actor to represent SOAP service clients
  - Enhanced usecase detection to include SOAP endpoints through annotation and naming pattern checks
  - Includes service-layer methods as a second level of use cases connected by "includes" relationships
  - Added database operations and database actor for repository operations
  - Uses icons to differentiate between SOAP operations (gear), service methods (layers), and database operations
  - Shows multi-tier architecture with proper abstraction levels
  - Automatically connects endpoints to appropriate actors (User or External System) based on their type

## New Feature: DAO/JDBC Analysis (May 2025)
- **Database Operation Analysis**: Implemented advanced analysis of DAO/Repository classes to extract SQL operations:
  - Automatically identifies potential DAO/Repository classes using various detection methods (class name patterns, interface implementations, method name patterns)
  - Extracts SQL queries from method bodies using static code analysis
  - Infers database operations from method names when SQL can't be directly analyzed
  - Categorizes operations into SELECT, INSERT, UPDATE, DELETE types
  - Extracts table names from SQL queries and method names
- **Database Schema Visualization**:
  - Generates Entity-Relationship diagrams based on analyzed DAO operations
  - Shows tables and their relationships based on foreign key patterns
  - Connects DAO/Repository classes to the tables they operate on
  - Visualizes database schema with appropriate UML notation
- **Database Operation UI**:
  - Added new Database page in the frontend to display extracted database operations
  - Shows tables detected in the system
  - Lists all database operations by DAO/Repository class
  - Displays operation type, affected tables, and inferred SQL queries
  - Integrates with existing diagram viewer to show database schema 