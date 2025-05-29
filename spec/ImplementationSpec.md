# CodeDocGen – Implementation & Folder Spec
...
// Cursor Spec File for CodeDocGen
// This spec should be input to Cursor AI to scaffold the full-stack application.
// Java 21 (Spring Boot - Maven) Backend + React + Material UI Frontend (Note: Earlier drafts might have mentioned Tailwind, current setup is primarily Material UI with some potential Tailwind/shadcn components based on package.json)

/*
====================================
📘 HIGH-LEVEL STRUCTURE
====================================
Backend: Java 21 + Spring Boot (Maven)
Frontend: React + Material UI (with potential Tailwind CSS + shadcn/ui elements)
*/

/*
====================================
📁 BACKEND PROJECT STRUCTURE (Java)
====================================
Project: codedocgen-backend
*/

codedocgen-backend/
├── src/main/java/com/codedocgen/
│   ├── CodeDocGenApplication.java
│   ├── config/
│   │   └── WebConfig.java             // Handles CORS and static resource serving for generated diagrams/docs
│   ├── controller/
│   │   └── AnalysisController.java    // Orchestrates analysis, uses DbAnalysisResult from service
│   ├── dto/
│   │   ├── RepoRequest.java
│   │   ├── ParsedDataResponse.java  // Includes a field of type DbAnalysisResult
│   │   └── MavenExecutionResult.java
│   ├── service/
│   │   ├── impl/                    // Service implementations here
│   │   │   ├── GitServiceImpl.java
│   │   │   ├── ProjectDetectorServiceImpl.java // Detects project type, Spring Boot version
│   │   │   ├── JavaParserServiceImpl.java // Core parsing logic, symbol resolution, class/method metadata extraction. Uses JavaSymbolSolver.
│   │   │   ├── EndpointExtractorServiceImpl.java
│   │   │   ├── DiagramServiceImpl.java      // Generates class, sequence, component, usecase, ERD, DB schema diagrams (SVG)
│   │   │   ├── DocumentationServiceImpl.java // Generates project summaries, reads WSDL/XSD, Gherkin.
│   │   │   └── DaoAnalysisServiceImpl.java   // Implements DaoAnalysisService, returns DbAnalysisResult
│   │   ├── GitService.java
│   │   ├── ProjectDetectorService.java
│   │   ├── JavaParserService.java
│   │   ├── EndpointExtractorService.java
│   │   ├── DiagramService.java
│   │   ├── DocumentationService.java
│   │   └── DaoAnalysisService.java       // Interface method analyzeDbOperations returns DbAnalysisResult
│   ├── parser/
│   │   ├── CallFlowAnalyzer.java      // Performs DFS to build call flows from method metadata.
│   │   ├── SoapWsdlParser.java        // (Existing, may need updates for XSD deep parsing integration if not already done)
│   │   ├── DaoAnalyzer.java           // Helper for DaoAnalysisServiceImpl, identifies SQL, table names from method bodies.
│   │   └── YamlParser.java            // (Existing)
│   ├── util/
│   │   ├── FileUtils.java
│   │   └── PlantUMLRenderer.java      // Utility to render PlantUML source to SVG using Graphviz.
│   └── model/
│       ├── ClassMetadata.java
│       ├── MethodMetadata.java
│       ├── FieldMetadata.java         // Added for field details including initializers (e.g., for annotation members)
│       ├── EndpointMetadata.java
│       ├── DiagramType.java
│       ├── DaoOperationDetail.java  // Model for DAO operations
│       └── DbAnalysisResult.java      // New DTO: holds operationsByClass and classesByEntity maps
├── src/main/resources/
│   └── application.yml              // Includes logging levels for specific classes (e.g., ClassMetadataVisitorLogger)
├── pom.xml
└── README.md

/*
====================================
📁 FRONTEND PROJECT STRUCTURE (React + Material UI)
====================================
Project: codedocgen-frontend
*/

codedocgen-frontend/
├── public/
│   └── svg-viewer.html            // For standalone SVG viewing
├── src/
│   ├── components/
│   │   ├── RepoForm.js
│   │   ├── AnalysisDisplay.js      // Main display, uses Sidebar for navigation.
│   │   ├── Sidebar.js              // Main navigation component.
│   │   ├── EndpointTable.js        // (from .tsx)
│   │   ├── DiagramViewer.tsx        // Note: .tsx extension
│   │   ├── FeatureFileList.js      // (from .tsx)
│   │   └── ProgressIndicator.js    // Shows loading state during analysis
│   ├── pages/
│   │   ├── OverviewPage.js         // Displays project summary, Spring Boot info.
│   │   ├── ApiSpecsPage.js         // Displays OpenAPI/SwaggerUI and detailed WSDL/XSD.
│   │   ├── CallFlowPage.js         // Displays sequence diagrams and raw call steps.
│   │   ├── DiagramsPage.js         // General diagrams page (class, component, usecase, ERD).
│   │   ├── DatabasePage.js         // Updated to use DbAnalysisResult for entity-centric view & detailed ops
│   │   ├── GherkinPage.js
│   │   └── ClassesPage.js
│   ├── services/
│   │   └── api.js                  // (from .ts) Axios API service for backend communication.
│   ├── App.js                    // Main app component, handles routing, analysis results state.
│   ├── index.js
│   ├── index.css
│   ├── constants/
│   │   └── uiConstants.js        // For things like BACKEND_STATIC_BASE_URL
│   └── util/
│       └── diagramUtils.js       // Helpers for diagram titles, etc.
├── .env                            // For REACT_APP_API_URL, REACT_APP_BACKEND_STATIC_URL
├── package.json
└── README.md

/*
====================================
📘 ITERATION ROADMAP FOR CURSOR (Reflects current state & future)
====================================
*/

1. Iteration 1: Public Git Repo Input - ✅ DONE
2. Iteration 2: Clone repo using JGit - ✅ DONE
3. Iteration 3: Detect Maven/Gradle/Raw + Spring Boot - ✅ DONE
4. Iteration 4: Parse all classes, methods, inheritance with Advanced Symbol Resolution - ✅ DONE
5. Iteration 5: Extract REST and SOAP endpoints. Categorize classes. - ✅ DONE
6. Iteration 6: Trace full call flow - ✅ DONE
7. Iteration 7: Parse legacy DAO, JDBC, SQL, and table usage - ✅ DONE (Enhanced with entity-centric views and improved DAO analysis via DbAnalysisResult)
8. Iteration 8: Generate class diagrams and other diagram types (SVG) - ✅ DONE
9. Iteration 9: Generate Swagger/OpenAPI + project summary. - ✅ DONE
10. Iteration 10: Build full UI with sidebar navigation. - ✅ DONE (Database page updated for new DbAnalysisResult structure)
11. Iteration 11: Export to Confluence + PDF/HTML - (Future Enhancement)

/*
====================================
📘 KEY IMPLEMENTATION UPDATES (Consolidated & Chronological - Reflecting Latest)
====================================
**Initial Phase & Foundation:**
- Backend categorizes classes.
- Backend serves generated SVG diagrams; Frontend displays them via `DiagramViewer.tsx`.
- Project name extracted.
- Frontend navigation via `Sidebar.js`.
- WSDL/XSD display in `ApiSpecsPage.js`, OpenAPI via SwaggerUI.

**Core Parsing & Analysis Overhaul (Iterative):**

1.  **Symbol Resolution Engine (Backend - `JavaParserServiceImpl`):**
    *   Integrated JavaSymbolSolver with comprehensive type solvers.
    *   Pre-compilation step (`mvn compile`) for generated sources.
    *   FQN-centric identification.
    *   Robust fallbacks for unresolved calls.
    *   Accurate Class Type Determination.

2.  **Call Flow Analysis (Backend - `CallFlowAnalyzer`, `DiagramServiceImpl`):**
    *   DFS from entry points to build call sequences.
    *   Sequence diagrams generated from these steps with clean labels.
    *   Frontend (`CallFlowPage.js`) now uses a sophisticated `generateFlowDisplayName` function to create user-friendly names for flows and diagrams from full Java method signatures, including parsing parameter types and names for better readability.

3.  **DAO & Database Analysis (Backend - `DaoAnalysisServiceImpl`, `DaoAnalyzer`):**
    *   Identifies Spring Data repositories and methods.
    *   Extracts entity names from generics.
    *   Generates synthetic queries.
    *   `DaoAnalyzer` detects SQL and table names, with basic support for SQL in string variables.
    *   `generateDbDiagram` uses all known entities.
    *   **Returns `DbAnalysisResult` (a new DTO in `com.codedocgen.model`) containing `operationsByClass` (DAO FQN -> Ops) and `classesByEntity` (Entity Name -> Set of operating DAO FQNs).**
    *   **`DaoAnalysisServiceImpl.analyzeDbOperations` method now returns `DbAnalysisResult`.**
    *   **`DaoAnalysisService` interface updated for `analyzeDbOperations` to return `DbAnalysisResult`.**
    *   **Filters redundant service interfaces from `operationsByClass` if their implementation is present.**
    *   **`AnalysisController` updated to receive `DbAnalysisResult` and set it in `ParsedDataResponse.dbAnalysis`.**
    *   **`ParsedDataResponse` DTO updated with a `dbAnalysis` field of type `DbAnalysisResult`.**

4.  **Diagram Generation (Backend - `DiagramServiceImpl`, `PlantUMLRenderer`):**
    *   Generates Class, Component, Usecase, Sequence, ERD, Database Schema diagrams as SVGs.
    *   Enhanced Component and Usecase diagrams for SOAP/legacy systems.

5.  **Spring Boot Integration (Backend & Frontend):**
    *   Accurate Spring Boot project and version detection (from `pom.xml`, `build.gradle`, `build.gradle.kts`).
    *   Information displayed in UI and used in summaries.

6.  **Frontend Enhancements:**
    *   **Call Flow Page (`CallFlowPage.js`):**
        *   Displays sequence diagrams and raw call steps.
        *   Integrates `FlowExplorer.tsx` for expandable/collapsible detailed flow steps.
        *   **Features `generateFlowDisplayName` helper function to parse full Java method signatures (including parameters with types and names) and generate human-readable titles for diagrams and flow step details. This addresses previous issues with overly long or unparsed signature displays.**
    *   **Database Page (`DatabasePage.js`):**
        *   Displays schema diagram.
        *   **Renders new "Entities and Interacting DAO/Repository Classes" section by iterating `analysisResult.dbAnalysis.classesByEntity`.**
        *   **Updates "Detailed Operations by DAO/Repository Class" section using `analysisResult.dbAnalysis.operationsByClass` and adds a method name column.**
        *   Adjusts initial data check to use `analysisResult.dbAnalysis`.
    *   General Diagram Display (`DiagramViewer.tsx`, `DiagramsPage.js`).

**TODOs (from codebase - to be addressed):**
*   **Backend:**
    *   `EndpointExtractorServiceImpl.java`: Determine specific HTTP method from `@RequestMapping` if specified (L115) - ✅ DONE; Add support for other SOAP annotations like `@WebMethod` (JAX-WS) (L156) - ✅ DONE.
    *   `ProjectDetectorServiceImpl.java`: Add similar logic for `build.gradle`/`build.gradle.kts` if necessary for Spring Boot version detection (L120) - ✅ DONE (Regex corrected for Java string literals).
    *   `DocumentationServiceImpl.java`: (Method summaries) Add called methods / external calls if data is available (L108) - ✅ DONE; (Project summary) Enhance with common libraries, tech stack details (L279) - ✅ DONE (enhanced with available data, further improvements may require build file access).
    *   `DaoAnalyzer.java`: Handle cases where SQL is in a variable or constructed dynamically (L50) - ☑️ PARTIALLY ADDRESSED (basic variable tracking implemented; complex dynamic SQL remains a challenge).
*   **Frontend:**
    *   `FlowExplorer.tsx`: Add more details or a way to expand/explore the flow (L24) - ✅ DONE (implemented basic expand/collapse for flow steps).
    *   `ApiSpecsPage.js`: (WSDL/XSD rendering) Future enhancement - if `typeAttr` refers to a global `complexType`, expand it (L247) - 📝 ACKNOWLEDGED & DETAILED (complex, for future iteration; detailed comment in code outlines steps).
    *   `CallFlowPage.js`: Ensure display names for flows/diagrams are user-friendly and correctly parsed from signatures - ✅ DONE (Implemented `generateFlowDisplayName` with robust parsing).

*/

## Recent Updates (May 2025) - Historical Snapshot, see consolidated list above
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
// ... (rest of the old "Recent Updates" section can be truncated or reviewed for any unique points not covered above)
...

## 1. Project Overview

*   **Goal:** To analyze Java-based applications from public Git repositories, generate documentation, visualizations (diagrams), and provide insights into the codebase.
*   **Tech Stack:**
    *   **Backend:** Java 21, Spring Boot 3.2.x (Maven), JGit, JavaParser (with JavaSymbolSolver), PlantUML, CommonMark, SpringDoc OpenAPI.
    *   **Frontend:** React (Create React App), Material UI, Axios, react-router-dom, swagger-ui-react, react-zoom-pan-pinch.
*   **Core Workflow:**
    1.  User provides a public Git repository URL via the frontend.
    2.  Frontend sends the URL to the backend's `/api/analysis/analyze` endpoint.
    3.  Backend clones the repository, extracts project name, performs deep analysis (project type detection, Spring Boot versioning from pom.xml/build.gradle, **advanced Java parsing with symbol resolution**, class/method metadata extraction, **call flow tracing**, **comprehensive DAO/DB analysis including entity-to-class mappings and basic SQL variable tracking**, endpoint extraction, generation of multiple diagram types), and returns a structured `ParsedDataResponse`.
    4.  Frontend displays the received information in a user-friendly sidebar-navigated interface, correctly loading served diagrams and detailed analysis results, including an entity-centric database view and **call flows with human-readable names parsed from full Java signatures**.

## 2. Backend Details (`codedocgen-backend`)

### 2.4. Services - `com.codedocgen.service` & `com.codedocgen.service.impl`

*   **`GitService` / `GitServiceImpl`:** Clones and cleans Git repos.
*   **`ProjectDetectorService` / `ProjectDetectorServiceImpl`:** Detects build tool, Spring Boot presence, and version (supports `pom.xml`, `build.gradle`, `build.gradle.kts`).
*   **`JavaParserService` / `JavaParserServiceImpl`:** Core parsing engine using JavaParser and **JavaSymbolSolver**.
*   **`EndpointExtractorService` / `EndpointExtractorServiceImpl`:** Extracts REST and SOAP (e.g. `@WebMethod`) endpoint info.
*   **`DiagramService` / `DiagramServiceImpl`:** Generates Class, Component, Usecase, Sequence, ERD, and DB Schema diagrams as SVGs.
*   **`DocumentationService` / `DocumentationServiceImpl`:** Generates project summaries (including called methods, external calls, and tech stack details from available data), finds feature/WSDL/XSD files.
*   **`DaoAnalysisService` / `DaoAnalysisServiceImpl`:**

### 2.5. Parsers - `com.codedocgen.parser`

*   **`CallFlowAnalyzer.java`:** Builds detailed call flow sequences.
*   **`DaoAnalyzer.java`:** Utility class for `DaoAnalysisServiceImpl` to find SQL queries and table names (includes basic support for SQL in string variables).
*   **`SoapWsdlParser.java`**, **`YamlParser.java`**: Existing parsers.

## 3. Frontend Details (`codedocgen-frontend`)

*   **`Sidebar.js`:** Main navigation.
*   **`OverviewPage.js`:** Project summary.
*   **`ClassesPage.js`:** Detailed view of parsed classes.
*   **`ApiSpecsPage.js`:** Displays OpenAPI specs and detailed WSDL/XSD structures.
*   **`CallFlowPage.js`:**
    *   Displays sequence diagrams and raw call steps.
    *   Integrates `FlowExplorer.tsx` for interactive exploration of flow steps.
    *   **Features a `generateFlowDisplayName` helper function to parse full Java method signatures (including parameters with types and names) and generate human-readable titles for diagrams and flow details.**
*   **`DiagramsPage.js`:** Displays general diagrams.
*   **`DatabasePage.js`:**

## 4. Functionality Achieved (Key Highlights)

*   **Deep Java Analysis:** Robust parsing with symbol resolution.
*   **Comprehensive Call Flow Analysis:**
    *   Detailed sequence diagrams and raw call steps.
    *   **User-friendly display names for call flows in the UI, parsed from complex Java signatures.**
*   **Database & DAO Insights:**
    *   Detection of entities, DAO/repository operations (with basic SQL variable tracking).
*   **Accurate Spring Boot Detection (Maven & Gradle).**
*   **User-Friendly Frontend:** Clear presentation, interactive diagram viewers.
*   **Detailed API Specification Display.**
*   **Support for JAX-WS `@WebMethod` and improved `@RequestMapping` handling.**
*   **Enhanced documentation summaries with method calls and tech stack details.**

## 5. Build Status

## 6. Known Limitations & TODOs (High-Level & Code-Level)

*   **Advanced Parsing & Analysis (Backend):**
    *   Further refinement of REST endpoint detail extraction (complex request/response bodies).
    *   Deeper YAML parsing if used for project configuration.
    *   **`DaoAnalyzer.java`**: Handle more complex cases of SQL in variables or constructed dynamically (currently basic support).
*   **Diagrams & Visualization:**
    *   More interactive call flow visualization beyond current expand/collapse.
*   **Frontend Enhancements:**
    *   UI/UX improvements for very large datasets (filtering, searching, pagination).
    *   Dark mode.
    *   **`ApiSpecsPage.js`** (WSDL/XSD rendering): Future enhancement - if `typeAttr` refers to a global `complexType`, expand it (acknowledged, complex).
*   **Backend Enhancements:**
    *   Configuration for private Git repositories.
    *   Performance optimizations for extremely large codebases.
*   **Export Features:** Confluence publishing, PDF/HTML downloads (future scope).

## Recent Updates (Reflecting latest changes)
- Diagrams generated as SVG for quality and Confluence.
- CORS enabled for `/generated-output/**`.
- JavaParser upgraded for Java 7-21 compatibility.
- UI warnings for files failing to parse.
- `svg-viewer.html` for standalone SVG viewing.
- Sequence diagrams use quoted FQNs.
- **Symbol Solver & Pre-compilation (Backend)**: `mvn compile` pre-step for enhanced symbol resolution.
- **WSDL & XSD Deep Parsing (Frontend & Backend)**.
- **Call Flow Page & Sidebar Navigation (Frontend)**:
    - Integration of `FlowExplorer.tsx` for better step navigation.
    - **Implementation of `generateFlowDisplayName` in `CallFlowPage.js` for significantly improved, human-readable names for call flows and sequence diagrams, correctly parsing method signatures including parameters.**
- **Refined API Specs UI**.
- **Enhanced Component Diagram for SOAP/Legacy Applications**.
- **Comprehensive Usecase Diagram for SOAP/Legacy Applications**.
- **DAO/JDBC Analysis & Database Schema Visualization**:
    - Automatic identification of DAO/Repository classes.
    *   Extraction of SQL queries (with basic variable support) and inference from method names.
    *   Categorization of operations (SELECT, INSERT, UPDATE, DELETE) and table name extraction.
    *   Generation of Database Schema diagrams linking DAOs to tables.
    *   **New `DbAnalysisResult` DTO in backend providing `operationsByClass` and `classesByEntity` maps.**
    *   **Frontend `DatabasePage.js` refactored to display an entity-centric view ("Entities and Interacting Classes") and a detailed DAO operation view (now including method names).**
    *   **Backend logic to remove redundant service interfaces from DAO listings if their implementation is present.**
- **Backend TODOs Addressed:**
    - `EndpointExtractorServiceImpl.java`: Support for `@RequestMapping` method attribute and `@WebMethod` (JAX-WS).
    - `ProjectDetectorServiceImpl.java`: Added Gradle support for Spring Boot version detection (with regex fixes).
    - `DocumentationServiceImpl.java`: Method summaries now include called methods/external calls; project summary enhanced.
    - `DaoAnalyzer.java`: Basic support for SQL in variables.

(Removed the old "New Feature: DAO/JDBC Analysis" section as its content is now integrated above and in the main sections) 
