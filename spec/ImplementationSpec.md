# CodeDocGen – Implementation & Folder Spec
...
// Cursor Spec File for CodeDocGen
// This spec should be input to Cursor AI to scaffold the full-stack application.
// Java 21 (Spring Boot - Maven) Backend + React + Material UI (Primary) Frontend

/*
====================================
📘 HIGH-LEVEL STRUCTURE
====================================
Backend: Java 21 + Spring Boot (Maven)
Frontend: React + Material UI (Primary)
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
│   │   │   ├── DaoAnalysisServiceImpl.java   // Implements DaoAnalysisService, returns DbAnalysisResult
│   │   │   ├── LoggerInsightsServiceImpl.java // Analyzes logs for PII/PCI, patterns loaded from application.yml
│   │   │   ├── PiiPciDetectionServiceImpl.java // New: Scans entire repo for PII/PCI based on configurable patterns
│   │   │   └── YamlParserServiceImpl.java   // New: Basic YAML file parsing service
│   │   ├── GitService.java
│   │   ├── ProjectDetectorService.java
│   │   ├── JavaParserService.java
│   │   ├── EndpointExtractorService.java
│   │   ├── DiagramService.java
│   │   ├── DocumentationService.java
│   │   ├── DaoAnalysisService.java       // Interface method analyzeDbOperations returns DbAnalysisResult
│   │   ├── LoggerInsightsService.java    // Interface for logger analysis
│   │   ├── PiiPciDetectionService.java   // New: Interface for PII/PCI repo scanning
│   │   └── YamlParserService.java        // New: Interface for YAML parsing
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
│       ├── PiiPciFinding.java       // New: Model for a single PII/PCI finding
│       └── DbAnalysisResult.java      // New DTO: holds operationsByClass and classesByEntity maps
├── src/main/resources/
│   └── application.yml              // Includes logging levels for specific classes (e.g., ClassMetadataVisitorLogger), PII/PCI patterns for LoggerInsights and PiiPciDetectionService
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
│   │   ├── EndpointTable.js
│   │   ├── DiagramViewer.js        // Changed from .tsx
│   │   ├── FeatureFileList.js
│   │   └── ProgressIndicator.js    // Shows loading state during analysis
│   ├── pages/
│   │   ├── OverviewPage.js         // Displays project summary, Spring Boot info.
│   │   ├── ApiSpecsPage.js         // Displays OpenAPI/SwaggerUI and detailed WSDL/XSD.
│   │   ├── CallFlowPage.js         // Displays sequence diagrams and formatted, interactive call traces (plaintext, no params, copyable).
│   │   ├── DiagramsPage.js         // General diagrams page (class, component, usecase, ERD).
│   │   ├── DatabasePage.js         // Updated to use DbAnalysisResult for entity-centric view & detailed ops
│   │   ├── GherkinPage.js
│   │   ├── ClassesPage.js
│   │   ├── PiiPciScanPage.js       // New: Page for displaying comprehensive PII/PCI scan results
│   │   └── LoggerInsightsPage.js   // Page for displaying logger insights with new features
│   ├── services/
│   │   └── api.js
│   ├── App.js                    // Main app component, handles overall layout (sidebar + content), routing, analysis results state.
│   ├── index.js                  // Main entry point, renders App.js
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
12. Iteration 12: Logger Insights Feature - ✅ DONE (Patterns configurable via application.yml)
13. Iteration 13: Comprehensive PII/PCI Scanning (New Feature)
    *   **Backend API**: The `/api/analysis/analyze` endpoint's `ParsedDataResponse` now includes `piiPciFindings` (List<PiiPciFinding>).
        ```java
        // com.codedocgen.model.PiiPciFinding
        public class PiiPciFinding {
            private String filePath;
            private int lineNumber;
            private int columnNumber; 
            private String findingType; // e.g., PII_SSN, PCI_CREDIT_CARD
            private String matchedText;
            // ...getters/setters...
        }
        ```
    *   **Backend Logic (`PiiPciDetectionServiceImpl.java`)**:
        *   Scans all text-based files in the cloned repository.
        *   Uses configurable regex patterns for PII (e.g., SSN, Email) and PCI (e.g., Credit Card numbers) data.
        *   Patterns (map of type to regex string) are loaded from `application.yml` via `@Value` (e.g., `pii.patterns.EMAIL`, `pci.patterns.VISA`).
        *   Excludes common binary file types and directories like `.git`, `target`, `build`, `node_modules`.
        *   Populates `PiiPciFinding` objects with details of each match.
    *   **Frontend Route**: `/pii-pci-scan` (Handled by `activeSection` in `App.js` via Sidebar)
    *   **Frontend Components (`PiiPciScanPage.js`)**:
        *   Displays findings in a collapsible table (each row can be expanded to show full matched text).
        *   Provides search/filter capabilities for file path, finding type, and matched text.
        *   Includes "Expand All" / "Collapse All" buttons.
    *   **Sidebar**: New "PCI/PII Scan" tab in `Sidebar.js` navigates to this section.

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

**Enterprise Readiness Features (Latest Update):**

1. **Secure Git Integration (`GitServiceImpl`):**
   * Support for authentication with private enterprise Git repositories.
   * Username/password credentials support via `app.git.username` and `app.git.password` configuration properties.
   * Secure credential handling with masked logging.

2. **Custom Maven Integration (`MavenBuildServiceImpl`):**
   * Support for enterprise Maven `settings.xml` files via `app.maven.settings.path` configuration.
   * Handles both filesystem paths and classpath resources.
   * Support for temporary file management for classpath resources.
   * Properly masks sensitive information in logs.

3. **SSL/TLS Trust Configuration (`TruststoreConfig`):**
   * Robust loading of enterprise truststore.jks for HTTPS operations.
   * Support for classpath and filesystem-based truststore files.
   * Validation of truststore files before use.
   * Early initialization with Spring's `@Order(0)`.
   * Global system property configuration for all HTTPS operations.
   * Fallback to app-level trust store password if server one is not set.

4. **OS-Aware Path Management (`SystemInfoUtil`):**
   * Enhanced OS detection with normalized return values.
   * Path resolution for executables with proper OS-specific handling.
   * Support for Windows, macOS, and Linux.
   * Helper methods for checking if executables are available on PATH.

5. **Graphviz Integration (`PlantUMLRenderer`):**
   * Configurable dot executable path via `app.graphviz.dot.executable.path` property.
   * Automatic fallback to common installation locations on Windows.
   * Validation of the executable presence.
   * System property configuration for PlantUML's Graphviz integration.

**Core Parsing & Analysis Overhaul (Iterative):**

1.  **Symbol Resolution Engine (Backend - `JavaParserServiceImpl`):**
    *   Integrated JavaSymbolSolver with comprehensive type solvers.
    *   Pre-compilation step (`mvn compile`) for generated sources.
        *   Maven commands now utilize a `settings.xml` file if specified in `codedocgen`'s configuration (`app.maven.settings.path`).
        *   Maven execution environment is configured with truststore details (`truststore.jks` path and password) from `codedocgen`'s SSL configuration (`server.ssl.trust-store`, `app.ssl.trust-store-password`). This is managed by the enhanced `TruststoreConfig.java` class and applied by `MavenBuildServiceImpl`.
    *   FQN-centric identification.
    *   Robust fallbacks for unresolved calls.
    *   Accurate Class Type Determination.

2.  **Call Flow Analysis (Backend - `CallFlowAnalyzer`, `DiagramServiceImpl`):**
    *   DFS from entry points to build call sequences.
    *   Sequence diagrams generated from these steps with clean labels.
    *   Frontend (`CallFlowPage.js` and `FlowExplorer.tsx`) now displays call flows as a numbered list of method calls.
        *   The display is simplified: parameters are removed (e.g., `myMethod(int a)` becomes `myMethod()`).
        *   Prefixes like "RESOLVED_CALL:" or "UNRESOLVED:" are not shown in the UI.
        *   A "Copy Trace" button allows users to copy the formatted plaintext trace to the clipboard, suitable for AI assistants.
    *   The `generateFlowDisplayName` function in `CallFlowPage.js` continues to create user-friendly names for flows and diagrams from full Java method signatures.

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

5.  **Logger Insights (Backend `LoggerInsightsServiceImpl`, Frontend `LoggerInsightsPage`):**
    *   Analysis of log statements for PII/PCI risks.
    *   Detection patterns configurable via `application.yml`.
    *   Dedicated UI page with filtering and expandable details.

6.  **Comprehensive PII/PCI Scanning (Backend `PiiPciDetectionServiceImpl`, Frontend `PiiPciScanPage`):**
    *   Scans entire repository (text files) for PII/PCI data using configurable regex patterns from `application.yml`.
    *   Excludes binary files and irrelevant directories.
    *   Results displayed in a new dedicated UI tab (`PiiPciScanPage.js`) with search, filtering, and expandable details for each finding.
    *   New `PiiPciFinding.java` model and `PiiPciDetectionService.java` interface added.
    *   `ParsedDataResponse.java` updated to include `List<PiiPciFinding>`.

7.  **Spring Boot Integration (Backend & Frontend):**
    *   Accurate Spring Boot project and version detection (from `pom.xml`, `build.gradle`, `build.gradle.kts`).
    *   Information displayed in UI and used in summaries.

**TODOs (from codebase - to be addressed):**
*   **Backend:**
    *   `EndpointExtractorServiceImpl.java`: Determine specific HTTP method from `@RequestMapping` if specified (L115) - ✅ DONE; Add support for other SOAP annotations like `@WebMethod` (JAX-WS) (L156) - ✅ DONE.
    *   `ProjectDetectorServiceImpl.java`: Add similar logic for `build.gradle`/`build.gradle.kts` if necessary for Spring Boot version detection (L120) - ✅ DONE (Regex corrected for Java string literals).
    *   `DocumentationServiceImpl.java`: (Method summaries) Add called methods / external calls if data is available (L108) - ✅ DONE; (Project summary) Enhance with common libraries, tech stack details (L279) - ✅ DONE (enhanced with available data, further improvements may require build file access).
    *   `DaoAnalyzer.java`: Handle cases where SQL is in a variable or constructed dynamically (L50) - ☑️ PARTIALLY ADDRESSED (basic variable tracking implemented; complex dynamic SQL remains a challenge).
    *   **`LoggerInsightsServiceImpl.java`**: PII/PCI keyword patterns externalized to `application.yml` - ✅ DONE.
    *   **Deeper YAML Parsing**: Added `YamlParserService` and `YamlParserServiceImpl` for basic YAML file parsing to `Map<String, Object>`. Further integration depends on specific use cases for YAML in target projects - ✅ NEWLY ADDED.
*   **Frontend:**
    *   `FlowExplorer.tsx`: Add more details or a way to expand/explore the flow (L24) - ✅ DONE (implemented basic expand/collapse for flow steps, though this file might be .js or integrated if not a separate .tsx now)
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
*   **`DaoAnalysisService` / `DaoAnalysisServiceImpl`:** // Implements DaoAnalysisService, returns DbAnalysisResult
*   **`LoggerInsightsService` / `LoggerInsightsServiceImpl`:** // Analyzes logs for PII/PCI, patterns loaded from application.yml
*   **`PiiPciDetectionService` / `PiiPciDetectionServiceImpl`:** // New: Scans entire repo for PII/PCI based on configurable patterns
*   **`YamlParserService` / `YamlParserServiceImpl`:** // New: Basic YAML file parsing service

### 2.5. Parsers - `com.codedocgen.parser`

*   **`CallFlowAnalyzer.java`:** Builds detailed call flow sequences.
*   **`DaoAnalyzer.java`:** Utility class for `DaoAnalysisServiceImpl`