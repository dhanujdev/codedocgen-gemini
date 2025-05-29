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

3.  **DAO & Database Analysis (Backend - `DaoAnalysisServiceImpl`, `DaoAnalyzer`):**
    *   Identifies Spring Data repositories and methods.
    *   Extracts entity names from generics.
    *   Generates synthetic queries.
    *   `DaoAnalyzer` detects SQL and table names.
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
    *   Accurate Spring Boot project and version detection.
    *   Information displayed in UI and used in summaries.

6.  **Frontend Enhancements:**
    *   **Call Flow Page (`CallFlowPage.js`):** Displays sequence diagrams and raw call steps.
    *   **Database Page (`DatabasePage.js`):**
        *   Displays schema diagram.
        *   **Renders new "Entities and Interacting DAO/Repository Classes" section by iterating `analysisResult.dbAnalysis.classesByEntity`.**
        *   **Updates "Detailed Operations by DAO/Repository Class" section using `analysisResult.dbAnalysis.operationsByClass` and adds a method name column.**
        *   Adjusts initial data check to use `analysisResult.dbAnalysis`.
    *   General Diagram Display (`DiagramViewer.tsx`, `DiagramsPage.js`).

**TODOs (from codebase - to be addressed):**
*   **Backend:**
    *   `EndpointExtractorServiceImpl.java`: Determine specific HTTP method from `@RequestMapping` if specified (L115); Add support for other SOAP annotations like `@WebMethod` (JAX-WS) (L156).
    *   `ProjectDetectorServiceImpl.java`: Add similar logic for `build.gradle`/`build.gradle.kts` if necessary for Spring Boot version detection (L120).
    *   `DocumentationServiceImpl.java`: (Method summaries) Add called methods / external calls if data is available (L108); (Project summary) Enhance with common libraries, tech stack details (L279).
    *   `DaoAnalyzer.java`: Handle cases where SQL is in a variable or constructed dynamically (L50).
*   **Frontend:**
    *   `FlowExplorer.tsx`: Add more details or a way to expand/explore the flow (L24).
    *   `ApiSpecsPage.js`: (WSDL/XSD rendering) Future enhancement - if `typeAttr` refers to a global `complexType`, expand it (L247).

*/

## Recent Updates (May 2025) - Historical Snapshot, see consolidated list above
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
// ... (rest of the old "Recent Updates" section can be truncated or reviewed for any unique points not covered above)
...
