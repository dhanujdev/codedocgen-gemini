# CodeDocGen - Implementation Summary

This document summarizes the current state of the CodeDocGen project, covering both the backend and frontend components.

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

### 2.1. Main Application & Configuration

*   **`CodeDocGenApplication.java`:** Main Spring Boot application class.
*   **`pom.xml`:** Manages dependencies, including `com.github.javaparser:javaparser-symbol-solver`.
*   **`application.yml`:** Configures server port, application name, logging levels, and configurable paths.
*   **`config/WebConfig.java`:** Configures CORS and static resource serving for `/generated-output/**`.

### 2.2. DTOs (Data Transfer Objects) - `com.codedocgen.dto`

*   **`RepoRequest.java`:** `repoUrl` (String).
*   **`ParsedDataResponse.java`:** Main response DTO.
    *   `projectName`, `projectType`, `springBootVersion`, `isSpringBootProject`.
    *   `classes` (List<ClassMetadata>): All parsed classes.
    *   `endpoints` (List<EndpointMetadata>): Extracted API endpoints.
    *   `diagrams` (Map<DiagramType, String>): Map of diagram types to server-relative SVG URLs.
    *   `projectSummary` (String): Textual project summary.
    *   `openApiSpec` (String): OpenAPI spec content.
    *   `featureFiles` (List<String>): Gherkin feature file contents.
    *   `wsdlFilesContent` (Map<String, String>): WSDL file contents.
    *   `xsdFilesContent` (Map<String, String>): XSD file contents.
    *   `callFlows` (Map<String, List<String>>): Raw call flow steps for each entry point.
    *   `sequenceDiagrams` (Map<String, String>): FQN to sequence diagram URL.
    *   `daoOperations` (Map<String, List<DaoOperationDetail>>): Legacy field, operations grouped by class FQN (still populated for compatibility/detailed view).
    *   `dbDiagramPath` (String): Server-relative path to the database schema diagram.
    *   **`dbAnalysis` (DbAnalysisResult): New composite DTO containing:**
        *   `operationsByClass` (Map<String, List<DaoOperationDetail>>): DAO operations grouped by class FQN.
        *   `classesByEntity` (Map<String, Set<String>>): Map of Entity Name to a Set of FQNs of classes operating on that entity.
*   **`MavenExecutionResult.java`:** For results of `mvn` commands.

### 2.3. Models - `com.codedocgen.model`

*   **`ClassMetadata.java`:** Name, packageName, type, annotations, methods, fields, parentClass, interfaces, filePath, etc.
*   **`MethodMetadata.java`:** Name, returnType, parameters, annotations, exceptions, visibility, static, abstract, `calledMethods`, `parameterAnnotations`, `daoOperations`, `sqlQueries`, `sqlTables`, `sqlOperations`.
*   **`FieldMetadata.java`:** Name, type, annotations, visibility, static, final, `initializer`.
*   **`EndpointMetadata.java`:** Path, httpMethod, handlerMethod, requestBodyType, responseBodyType, etc.
*   **`DiagramType.java` (enum):** `CLASS_DIAGRAM`, `SEQUENCE_DIAGRAM`, `COMPONENT_DIAGRAM`, `USECASE_DIAGRAM`, `DATABASE_DIAGRAM`, `ENTITY_RELATIONSHIP_DIAGRAM`.
*   **`DaoOperationDetail.java`:** Details for a single DAO operation (method name, SQL query, tables, operation type).
*   **`DbAnalysisResult.java`:** New DTO holding `operationsByClass` and `classesByEntity` maps (see above).

### 2.4. Services - `com.codedocgen.service` & `com.codedocgen.service.impl`

*   **`GitService` / `GitServiceImpl`:** Clones and cleans Git repos.
*   **`ProjectDetectorService` / `ProjectDetectorServiceImpl`:** Detects build tool, Spring Boot presence, and version (supports `pom.xml`, `build.gradle`, `build.gradle.kts`).
*   **`JavaParserService` / `JavaParserServiceImpl`:** Core parsing engine using JavaParser and **JavaSymbolSolver**.
*   **`EndpointExtractorService` / `EndpointExtractorServiceImpl`:** Extracts REST and SOAP (e.g. `@WebMethod`) endpoint info.
*   **`DiagramService` / `DiagramServiceImpl`:** Generates Class, Component, Usecase, Sequence, ERD, and DB Schema diagrams as SVGs.
*   **`DocumentationService` / `DocumentationServiceImpl`:** Generates project summaries (including called methods, external calls, and tech stack details from available data), finds feature/WSDL/XSD files.
*   **`DaoAnalysisService` / `DaoAnalysisServiceImpl`:**
    *   Analyzes repository interfaces and classes.
    *   Uses `DaoAnalyzer` helper to detect SQL queries and table names.
    *   Infers operations from Spring Data method names and extracted SQL.
    *   **Returns `DbAnalysisResult` containing both `operationsByClass` and the new `classesByEntity` mapping.**
    *   Filters redundant interfaces if their implementations are present.
    *   Provides data for the DB schema diagram and DAO operation listings.

### 2.5. Parsers - `com.codedocgen.parser`

*   **`CallFlowAnalyzer.java`:** Builds detailed call flow sequences.
*   **`DaoAnalyzer.java`:** Utility class for `DaoAnalysisServiceImpl` to find SQL queries and table names (includes basic support for SQL in string variables).
*   **`SoapWsdlParser.java`**, **`YamlParser.java`**: Existing parsers.

### 2.6. Controller - `com.codedocgen.controller`

*   **`AnalysisController.java`:** Orchestrates analysis, populates and returns `ParsedDataResponse` including `dbAnalysis` (with `operationsByClass` and `classesByEntity`).

## 3. Frontend Details (`codedocgen-frontend`)

### 3.1. Project Setup & Key Libraries

*   Create React App based, Material UI, Axios, `react-router-dom`, `swagger-ui-react`, `react-zoom-pan-pinch`.
*   Environment variables in `.env` for API and static content URLs.

### 3.2. Core Components & Pages - `src/`

*   **`App.js`:** Main component, manages state, routing.
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
    *   Displays the Database Schema diagram.
    *   **Shows "Entities and Interacting DAO/Repository Classes" using `dbAnalysis.classesByEntity`.**
    *   **Shows "Detailed Operations by DAO/Repository Class" using `dbAnalysis.operationsByClass`, now including method names.**
*   **`GherkinPage.js`:** Shows Gherkin feature file content.
*   **`DiagramViewer.tsx`:** Reusable component for rendering SVG diagrams. (Note: extension is .tsx)
*   **`services/api.js`:** Axios service.
*   **`constants/uiConstants.js`:** For `BACKEND_STATIC_BASE_URL`.

## 4. Functionality Achieved (Key Highlights)

*   **Deep Java Analysis:** Robust parsing with symbol resolution.
*   **Comprehensive Call Flow Analysis:**
    *   Detailed sequence diagrams and raw call steps.
    *   **User-friendly display names for call flows in the UI, parsed from complex Java signatures.**
*   **Database & DAO Insights:**
    *   Detection of entities, DAO/repository operations (with basic SQL variable tracking).
    *   Generation of a database schema diagram.
    *   **Entity-centric view showing which classes operate on each entity.**
    *   Detailed breakdown of operations per DAO/Repository class.
*   **Multiple Diagram Types:** Class, Sequence, Component, Usecase, ERD, and Database Schema diagrams.
*   **Accurate Spring Boot Detection (Maven & Gradle).**
*   **User-Friendly Frontend:** Clear presentation, interactive diagram viewers.
*   **Detailed API Specification Display.**
*   **Support for JAX-WS `@WebMethod` and improved `@RequestMapping` handling.**
*   **Enhanced documentation summaries with method calls and tech stack details.**

## 5. Build Status

*   Backend and Frontend should build and run successfully.

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