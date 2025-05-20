# CodeDocGen - Implementation Summary

This document summarizes the current state of the CodeDocGen project, covering both the backend and frontend components.

## 1. Project Overview

*   **Goal:** To analyze Java-based applications from public Git repositories, generate documentation, visualizations (diagrams), and provide insights into the codebase.
*   **Tech Stack:**
    *   **Backend:** Java 21, Spring Boot 3.2.x (Maven), JGit, JavaParser, PlantUML, CommonMark, SpringDoc OpenAPI.
    *   **Frontend:** React (Create React App), Material UI, Axios, react-zoom-pan-pinch, swagger-ui-react, tailwindcss-animate, react-router-dom.
*   **Core Workflow:**
    1.  User provides a public Git repository URL via the frontend.
    2.  Frontend sends the URL to the backend's `/api/analysis/analyze` endpoint.
    3.  Backend clones the repository, extracts the project name from the URL, performs analysis (project type detection, code parsing with improved type classification, endpoint extraction, class diagram generation, documentation creation), and returns a structured `ParsedDataResponse` with server-relative paths for diagrams.
    4.  Frontend displays the received information in a user-friendly tabbed interface, correctly loading served diagrams.

## 2. Backend Details (`codedocgen-backend`)

### 2.1. Main Application & Configuration

*   **`CodeDocGenApplication.java`:** Main Spring Boot application class (`@SpringBootApplication`).
*   **`pom.xml`:** Manages dependencies, including:
    *   `spring-boot-starter-web`, `spring-boot-starter-actuator`
    *   `springdoc-openapi-starter-webmvc-ui` (for OpenAPI/Swagger UI)
    *   `org.eclipse.jgit` (for Git operations)
    *   `com.github.javaparser` (core and symbol-solver for Java code analysis)
    *   `net.sourceforge.plantuml` (for diagram generation)
    *   `org.commonmark` (for Markdown to HTML conversion)
    *   `org.yaml:snakeyaml` (for YAML parsing, if needed directly)
    *   `commons-io` (for file utilities)
    *   `org.projectlombok` (for boilerplate code reduction)
*   **`application.yml`:** Configures:
    *   Server port (`8080`).
    *   Application name.
    *   Logging levels.
    *   `app.repoStoragePath`: Temporary directory for cloning repositories (e.g., `/tmp/codedocgen_repos`).
    *   `app.outputBasePath`: Base directory for generated outputs like diagrams and docs (e.g., `/tmp/codedocgen_output`), served via `/generated-output/`.
*   **`config/WebConfig.java`:** Configures CORS and a static resource handler for serving files from `app.outputBasePath` under the `/generated-output/**` URL pattern.

### 2.2. DTOs (Data Transfer Objects) - `com.codedocgen.dto`

*   **`RepoRequest.java`:**
    *   `repoUrl` (String): The Git repository URL provided by the user.
*   **`ParsedDataResponse.java`:** The main DTO returned by the analysis endpoint.
    *   `projectName` (String): Extracted from the Git repository URL.
    *   `projectType` (String): e.g., "Maven", "Gradle".
    *   `springBootVersion` (String): If applicable.
    *   `isSpringBootProject` (boolean)
    *   `classes` (List<ClassMetadata>): List of all parsed classes.
    *   `endpoints` (List<EndpointMetadata>): List of extracted API endpoints.
    *   `diagrams` (Map<DiagramType, String>): Map of diagram types to their server-relative image URLs (e.g., `/generated-output/docs_id/diagrams/class_diagram.svg`). Includes `CLASS_DIAGRAM` and `SEQUENCE_DIAGRAM`.
    *   `projectSummary` (String): A textual summary of the project.
    *   `openApiSpec` (String): OpenAPI/Swagger spec content or a message indicating its availability.
    *   `featureFiles` (List<String>): Contents of found Gherkin feature files.
    *   `wsdlFilesContent` (Map<String, String>): Content of found WSDL files, keyed by path.
    *   `xsdFilesContent` (Map<String, String>): Content of found XSD files, keyed by path.
    *   `sequenceDiagrams` (List<String>): PlantUML strings for sequence diagrams (alternative to diagramMap).

### 2.3. Models - `com.codedocgen.model`

*   **`ClassMetadata.java`:** Detailed information about a parsed Java class/interface/enum.
    *   `name`, `packageName`, `type` (controller, service, entity, etc.), `annotations`, `methods` (List<MethodMetadata>), `fields`, `parentClass`, `interfaces`, `filePath`.
*   **`MethodMetadata.java`:** Detailed information about a parsed method.
    *   `name`, `returnType`, `parameters`, `annotations`, `exceptionsThrown`, `visibility`, `isStatic`, `isAbstract`, `calledMethods` (List<String> - TODO), `externalCalls` (Map<String, String> - TODO).
*   **`EndpointMetadata.java`:** Information about an API endpoint.
    *   `path`, `httpMethod`, `handlerMethod` (fully qualified), `requestBodyType`, `responseBodyType`, `pathVariables`, `requestParameters`, `consumes`, `produces`, `type` ("REST" or "SOAP"), `wsdlUrl`, `operationName`.
*   **`DiagramType.java` (enum):** Types of diagrams that can be generated (e.g., `CLASS_DIAGRAM`, `SEQUENCE_DIAGRAM`).

### 2.4. Services - `com.codedocgen.service` & `com.codedocgen.service.impl`

*   **`GitService` / `GitServiceImpl`:**
    *   Clones a Git repository using JGit to a specified local path.
    *   Deletes the cloned repository directory.
*   **`ProjectDetectorService` / `ProjectDetectorServiceImpl`:**
    *   Detects the build tool (Maven, Gradle) by checking for `pom.xml` or `build.gradle` files.
    *   Detects if a project is a Spring Boot project by checking build files for Spring Boot dependencies/plugins and by scanning for `@SpringBootApplication` annotation.
    *   Attempts to detect the Spring Boot version from `pom.xml` (parent or property).
*   **`JavaParserService` / `JavaParserServiceImpl`:**
    *   Walks through the project directory to find all `.java` files.
    *   Uses JavaParser to parse each Java file.
    *   A `ClassMetadataVisitor` extracts information about classes, interfaces, enums, methods (including visibility, static/abstract modifiers, parameters, return type, annotations, exceptions), and fields.
    *   Determines class type (controller, service, repository, entity, config, test, etc.) based on annotations (prioritizing stereotype annotations like `@Repository` even for interfaces) and naming conventions.
    *   **Symbol Resolution Enhancement**: To improve accuracy, especially for projects with generated code (e.g., JAXB from XSDs), this service now attempts to execute `mvn compile -DskipTests -q` on the target project *before* parsing. This makes generated sources available to the JavaParser symbol solver.
*   **`EndpointExtractorService` / `EndpointExtractorServiceImpl`:**
    *   Iterates through `ClassMetadata` identified as "controller".
    *   Extracts REST endpoint information from method annotations (e.g., `@GetMapping`, `@PostMapping`, `@RequestMapping`), including HTTP method and path.
    *   Sets default `consumes`/`produces` to `application/json`.
    *   Includes TODOs for more robust parsing (path variables, request/response bodies) and SOAP endpoint extraction.
*   **`DiagramService` / `DiagramServiceImpl`:**
    *   Generates class diagrams and sequence diagrams using PlantUML.
    *   Constructs PlantUML source string from `ClassMetadata` (for class diagrams) or `CallFlows` (for sequence diagrams).
    *   Renders diagrams as SVG images to the specified output directory, returning absolute paths.
    *   **Enhanced Component Diagram Generation:**
        *   Improved detection of components in legacy/SOAP applications by checking:
            *   Class name patterns (e.g., *Endpoint, *ServiceImpl, *Service)
            *   Annotations (@WebService, @Endpoint, @SOAPBinding)
            *   Spring stereotypes (@Component, @Service, @Repository, @Controller, etc.)
        *   Determines component roles (controller, service, repository, endpoint, webservice, etc.) through a dedicated algorithm
        *   Groups components by package for hierarchical organization
        *   Adds descriptive relationships between components ("delegates to", "uses", "implements")
        *   Adds WSDL interfaces to represent web service contracts
        *   Applies stereotypes (<<endpoint>>, <<service>>, etc.) for visual categorization
        *   Uses simplified display names for better readability
    *   **Improved Usecase Diagram Generation:**
        *   Expanded controller detection to include SOAP endpoints through annotation and naming pattern checks
        *   Added second-level service methods as connected usecases (via "includes" relationships)
        *   Implemented External System actor for SOAP clients
        *   Added database actor and operations for repository interactions
        *   Uses icons to differentiate between usecases:
            *   SOAP operations (gear icon)
            *   Service methods (layers icon)
            *   Database operations (database icon)
        *   Intelligently connects usecases to appropriate actors (User or External System)
        *   Shows multi-tier architecture with proper abstraction levels
        *   Analyzes method call chains to establish relationships between controller methods and service methods
*   **`DocumentationService` / `DocumentationServiceImpl`:**
    *   `generateProjectSummary()`: Creates a textual summary based on `ParsedDataResponse`, now reflecting more accurate repository counts due to improved class typing.
    *   `findAndReadFeatureFiles()`: Searches for `.feature` files in common directories (e.g., `src/test/resources/features`) and reads their content.
    *   `findAndReadWsdlFiles()`: Searches for `.wsdl` files in common resource directories and reads their content into `wsdlFilesContent` map.
    *   `findAndReadXsdFiles()`: Searches for `.xsd` files in common resource directories (often alongside WSDLs) and reads their content into `xsdFilesContent` map.
    *   `generateMarkdownDocumentation()`: Creates a comprehensive Markdown document from `ParsedDataResponse`, including project overview, summary, class details, endpoint details, links/embedded diagrams, OpenAPI spec, and feature files.
    *   `generateHtmlDocumentation()`: Converts the generated Markdown to a basic HTML document using the `org.commonmark` library.

### 2.5. Controller - `com.codedocgen.controller`

*   **`AnalysisController.java`:**
    *   Handles API requests at `/api/analysis`.
    *   `@PostMapping("/analyze")`:
        *   Accepts `RepoRequest` (containing `repoUrl`).
        *   Extracts `projectName` from `repoUrl`.
        *   Creates unique temporary directories for cloning and output.
        *   Orchestrates the analysis by calling the various services in sequence:
            1.  `GitService` to clone.
            2.  `ProjectDetectorService` to get project type/Spring Boot info.
            3.  `JavaParserService` to parse all Java files.
            4.  `EndpointExtractorService` to extract endpoints.
            5.  `DiagramService` to generate diagrams (Class Diagrams, Sequence Diagrams from call flows). Converts absolute diagram paths to server-relative URLs (e.g., `/generated-output/...`) before setting them in `ParsedDataResponse`.
            6.  `DocumentationService` to find feature files, WSDL files, and XSD files.
            7.  Sets `openApiSpec` message (points to Spring Boot auto-generated endpoints or indicates TODO for others). SpringDoc auto-configuration is relied upon for Swagger UI at `/swagger-ui.html` and API docs at `/v3/api-docs`.
            8.  `DocumentationService` to generate project summary.
        *   Returns `ResponseEntity<ParsedDataResponse>` with all collected data.
        *   Cleans up the cloned repository in a `finally` block.

## 3. Frontend Details (`codedocgen-frontend`)

### 3.1. Project Setup & Key Libraries

*   **Create React App** based project.
*   **`package.json`:** Manages dependencies and scripts.
    *   `react`, `react-dom`, `react-scripts`
    *   `@mui/material`, `@mui/icons-material`, `@emotion/react`, `@emotion/styled` (for Material UI)
    *   `axios` (for API calls)
    *   `react-zoom-pan-pinch` (for diagram interactivity)
    *   `swagger-ui-react` (for displaying OpenAPI specs)
    *   `tailwindcss-animate`, `react-router-dom` (recent additions for UI and routing)
    *   `prop-types` (for component prop validation)
*   **Scripts:** `npm start`, `npm run build`, `npm test`.
*   **Styling:** Material UI's theming and `CssBaseline`. Basic global styles in `src/index.css`. Some Create React App files might have been converted to `.tsx` for TypeScript usage.
*   **Key fixes**: Resolved issues with missing `tailwindcss-animate`, `react-router-dom`, `@types/react-dom`. Corrected undefined `logger` in `App.js` (using `console.error`) and undefined `API_URL` in `AnalysisDisplay.js` (passed as prop).

### 3.2. Core Components - `src/components`

*   **`App.js` (in `src/`):**
    *   Main application component.
    *   Sets up Material UI `ThemeProvider` and `CssBaseline`.
    *   Manages state for `analysisResult`, `isLoading`, `error`, and `activeSection` (for sidebar navigation).
    *   Includes an `AppBar` with the application title and a `Sidebar` component.
    *   Conditionally renders different page components (`OverviewPage`, `ApiSpecsPage`, `CallFlowPage`, `ClassesPage`, `DiagramsPage`, `GherkinPage`) based on `activeSection`.
    *   `handleAnalyze` function: makes POST request to backend `/api/analysis/analyze` using `axios`.
    *   Handles loading state and displays errors using MUI `Snackbar` and `Alert`.
*   **`RepoForm.js`:**
    *   A simple form with a `TextField` for the Git repository URL and a `Button` to trigger analysis.
    *   Disabled during loading.
*   **`AnalysisDisplay.js`:**
    *   Displays the `analysisResult` from the backend. Previously tabbed, now section-based via `App.js` and `Sidebar.js`.
    *   Receives `API_URL` as a prop.
*   **`Sidebar.js`:**
    *   Provides main navigation using MUI `List` and `ListItem` components.
    *   Allows switching between sections: Overview, API Specs, Call Flow, Classes, Diagrams, Gherkin Features.
*   **`OverviewPage.js`:**
    *   Displays `data.projectSummary`, project type, and Spring Boot version.
*   **`ClassesPage.js`:**
    *   Lists all classes using MUI `Accordion`. Each class accordion expands to show package name, class name, type (as a `Chip`), file path, parent class, implemented interfaces, annotations, fields, and methods (with visibility, static/abstract, return type, name, parameters, method-specific annotations, exceptions).
*   **`ApiSpecsPage.js`:**
    *   **OpenAPI/Swagger Display:** If `data.openApiSpec` is present and parsable as JSON/YAML, it renders the spec using `<SwaggerUI spec={specObject} />`. Provides links to backend auto-generated endpoints if applicable. Handles errors in spec string format.
    *   **WSDL/XSD Display:** If `data.wsdlFilesContent` is present, it uses the first WSDL. It parses WSDL operations, messages, and parts. Crucially, it uses `data.xsdFilesContent` to look up and parse imported XSD schemas (matching by `schemaLocation` or namespace). It then recursively traverses XSD elements and complex types to display detailed attributes, child elements, types, and cardinalities in a structured list format. Provides a fallback to show raw WSDL/XSD content.
*   **`CallFlowPage.js`:**
    *   Receives `analysisResult` and `repoName`.
    *   Extracts sequence diagrams from `analysisResult.diagramMap` (type `SEQUENCE_DIAGRAM`) or `analysisResult.sequenceDiagrams` (PlantUML strings).
    *   Uses `DiagramViewer.js` to display these diagrams.
*   **`DiagramsPage.js`:**
    *   Displays other diagrams (e.g., Class Diagrams) from `data.diagramMap` using `DiagramViewer.js`.
*   **`GherkinPage.js`:**
    *   Displays the content of Gherkin `.feature` files from `data.featureFiles`. Shows "No Gherkin feature files found" if none are present.
*   **`DiagramViewer.js`:**
    *   A reusable component to display SVG diagrams (from URL or direct SVG string) with zoom/pan capabilities (`react-zoom-pan-pinch`).

### 3.3. Entry Point & HTML

*   **`src/index.js`:** React app entry point, renders `<App />` into the 'root' div.
*   **`public/index.html`:** Basic HTML shell, includes link for Roboto font (used by Material UI).

### 3.4. Environment Configuration

*   `API_URL` in `App.js` defaults to `http://localhost:8080/api`.
*   A `README.md` instructs users on optionally creating a `.env` file to override `REACT_APP_API_URL`.

## 4. Functionality Achieved (Iterations 1-10, plus recent fixes)

*   **Repo Input & Cloning:** UI form for Git URL, backend clones via JGit. Project name correctly extracted from URL.
*   **Project Detection:** Backend identifies Maven/Gradle, Spring Boot, and SB version.
*   **Code Parsing:** Comprehensive parsing of Java classes, methods, fields, annotations, inheritance. Improved accuracy in classifying class types (e.g., repository interfaces).
*   **Endpoint Extraction:** Basic REST endpoint details extracted from Spring annotations.
*   **Diagram Generation & Display:** PlantUML class diagrams and sequence diagrams generated as SVGs. Backend serves these, and frontend displays them with interactivity via `DiagramViewer.js`.
*   **Documentation & Summaries:** Project summary, Gherkin feature files, and raw WSDL/XSD are available.
*   **OpenAPI/Swagger:** Displayed via SwaggerUI component for valid specs; links for auto-generated specs.
*   **WSDL/XSD Display**: Rich, structured view of WSDL operations and XSD elements/attributes.
*   **Call Flow Display**: Sequence diagrams for analyzed call flows are displayed.
*   **UI Display:** Frontend presents all gathered information in a structured, sidebar-navigated interface using Material UI.

## 5. Build Status

*   Backend (`codedocgen-backend`): Should compile successfully with Maven (`mvn clean install`) after the recent fixes (missing `StandardCharsets` import and deletion of old `RepoController.java`).
*   Frontend (`codedocgen-frontend`): Should run with `npm start` and build with `npm run build` after recent dependency installations and fixes. Source map warnings from third-party libraries may still appear but are generally non-critical.

## 6. Known Limitations & TODOs (High-Level)

*   **Call Flow Analysis:** Implemented with sequence diagram generation. Accuracy significantly improved, though complex dynamic behaviors or highly abstract code might still pose challenges for static analysis.
*   **SOAP Support:** Significantly enhanced. WSDLs are parsed, and referenced XSDs are processed to display detailed operation contracts including element attributes and types.
*   **Advanced Parsing:**
    *   More robust extraction of REST endpoint details (request/response bodies, path/query parameters) is still an area for improvement.
    *   ✅ Parsing of legacy DAO/JDBC patterns is now implemented, with database operation extraction and table relationship visualization.
    *   YAML file parsing for configuration or OpenAPI specs within the repo.
*   **Diagrams:**
    *   ✅ Entity Relationship (ER) diagrams have been implemented with proper JPA entity annotation detection.
    *   ✅ Component diagrams with enhanced visualization for SOAP/legacy systems have been implemented.
    *   ✅ Usecase diagrams showing system capabilities with connections to actors (User, External System, Database) have been implemented.
    *   ✅ Database schema diagrams showing database tables and relationships extracted from DAO/Repository classes have been implemented.
    *   Class and Sequence diagrams are fully implemented and functional.
*   **Gherkin Feature Files:**
    *   Implement AI-assisted generation of Gherkin feature files from code analysis (future scope).
*   **Frontend Enhancements:**
    *   More sophisticated UI for large datasets (pagination, filtering, searching).
    *   Interactive call flow visualization.
    *   Improved error handling and user feedback.
    *   Dark mode.
*   **Backend Enhancements:**
    *   Configuration for private repositories (authentication).
    *   More robust error handling and recovery.
    *   ✅ Database interaction analysis for DAO/JDBC code has been implemented.
*   **Export/Publishing:** Confluence publishing and PDF/HTML downloads (Iteration 11) are not yet started.

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