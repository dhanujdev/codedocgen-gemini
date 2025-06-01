# CodeDocGen – Product Requirements Document (PRD)

---

### 📌 Overview

**CodeDocGen** is a documentation and visualization generator for Java-based applications hosted in public Git repositories. It supports modern Spring Boot projects as well as legacy Java apps. The tool analyzes the project comprehensively to build interactive and visual documentation for API contracts, entity relationships, class diagrams, call flows, database interactions, and architectural summaries.

---

### 🎯 Objectives

1.  Accept a **public Git repository URL** via UI.
2.  Clone and scan the Java codebase. Project name is extracted from the URL.
3.  Identify **all classes**, including controllers, services, entities, utility classes, configuration files, DAOs, WSDL definitions, YAML files. Handles single and multi-module Maven projects efficiently.
4.  Generate:
    *   Complete **class diagrams**.
    *   Entity diagrams and ER diagrams for JPA entities.
    *   Component diagrams with enhanced visualization for SOAP/legacy systems.
    *   Usecase diagrams with actors connected to appropriate operations.
    *   End-to-end **method call flow diagrams** (Sequence Diagrams).
    *   API contract documentation (Swagger/OpenAPI, detailed WSDL/XSD views).
    *   **Visual representations of database interactions, including schema diagrams and an entity-centric view of DAO/Repository operations.**
    *   A **microservice/project capability overview**.
    *   Gherkin feature files display.
    *   **Comprehensive PCI/PII scan results, identifying potential sensitive data exposure across the entire codebase (not limited to logs).**
5.  Provide a **centralized documentation dashboard** with sidebar navigation.

---

### 🧱 Tech Stack

*   **Backend**: Java 21, Spring Boot (Maven)
*   **Frontend**: React, Material UI (Primary), Axios
*   **Parser Tools**: JavaParser (with symbol resolution), Reflection, Custom WSDL/XML parsers.
    *   Maven execution during Java parsing now respects a configured `settings.xml`, utilizes an OS-aware Maven executable path, and applies `codedocgen`'s truststore settings (`truststore.jks` and password).
    *   Optimized for memory usage and performance when analyzing large or multi-module projects.
*   **Diagram Generator**: PlantUML (rendered to SVG), utilizing an OS-aware Graphviz `dot` executable path.
*   **API Visualization**: Swagger UI + custom WSDL/XSD explorer.
*   **Build Tool**: Maven

---

### 🔁 Core Features (with legacy + modern support)

#### 🔹 Iteration 1–3: Repo Onboarding
*   Accept Git URL, clone, extract project name, detect build type, Spring Boot status.

#### 🔹 Iteration 4–6: Code Parsing
*   Parse all classes, YAML, WSDL, XSD.
*   Identify endpoints, class relationships, method call chains, DAO-layer queries/procedures.

#### 🔹 Iteration 7–9: Documentation Generation
*   **API Docs**: Swagger/OpenAPI for REST; Detailed WSDL/XSD viewer for SOAP.
*   **Class/Entity Diagrams**: PlantUML class diagrams; ER diagrams.
*   **Call Flow**: Sequence Diagrams from method call chains.
*   **Gherkin Feature Files**: Display existing files.
*   **Database Analysis**: Generate schema diagrams; Extract DAO operations; **Provide entity-to-class mappings.**

#### 🔹 Iteration 10–12: UI and Publishing
*   Dashboard UI (Material UI based, with sidebar navigation):
    *   Overview (project summary)
    *   API Specs (SwaggerUI, WSDL/XSD view)
    *   Call Flow (Sequence diagram viewer)
    *   Diagrams (Class, Component, Usecase, ERD, DB Schema)
    *   **Database View (Entity-centric view of DAO interactions, detailed operations table)**
    *   Classes (Detailed class browser)
    *   Gherkin Features
    *   **PCI/PII Scan (New): Displays findings of potential PCI/PII data across the repository, with details on file, line, and matched text. Patterns are robustly configured via `application.yml` using a type-safe `@ConfigurationProperties` approach.**
*   Export options (Future Enhancement)
*   Publish to Confluence (Future Enhancement)
*   **Logger Insights**:
    *   Provides a comprehensive overview of all logging statements in the codebase.
    *   Identifies insecure or excessive logging, especially any exposure of **Personally Identifiable Information (PII)** and **Payment Card Industry (PCI)** data. Utilizes an extensive keyword pattern matching system, including common abbreviations and variations, to detect potential risks. **These keyword patterns are now robustly configured via the backend's `application.yml` (using `@ConfigurationProperties`), allowing for easier customization and updates.**
    *   Helps security consultants audit logs easily.
    *   Displays logs in a table with class/filename, line number, log level, message, variables, and separate **PII Risk** and **PCI Risk** flags. Each log entry can be expanded to view associated variables.
    *   Includes "Expand All" and "Collapse All" buttons for log entry details.
    *   Allows filtering by class, **log level (via a dropdown menu)**, **PII risk**, and **PCI risk**.
    *   Includes toggles to show/hide logs flagged for PII or PCI risk.
    *   Option to download a PDF report of the filtered view.

---

### Output Structure (Conceptual)
- The backend produces a comprehensive JSON (`ParsedDataResponse`) containing all extracted metadata, diagram paths, call flows, and the new `DbAnalysisResult` (which itself contains `operationsByClass` and `classesByEntity`).
- The frontend consumes this JSON to render the various views.

## Recent Updates (Reflecting latest state)
- Diagrams are generated and served as SVG.
- CORS enabled for `/generated-output/**`.
- JavaParser upgraded for Java 7-21 compatibility.
- UI warnings for files failing to parse.
- Standalone SVG viewer (`svg-viewer.html`).
- **WSDL/XSD Parsing**: Deep parsing for detailed WSDL/XSD display.
- **Symbol Resolution Enhancement**: `mvn compile` pre-step for improved accuracy.
    *   This step now also incorporates the project-specific `settings.xml` (if configured), uses an OS-aware Maven executable, and applies the `codedocgen` truststore for secure connections during dependency resolution or plugin execution.
    *   Enhanced to fully support multi-module Maven projects by parsing the root `pom.xml` and including all specified modules in the analysis scope.
    *   Optimized classpath resolution using `mvn dependency:build-classpath -DincludeScope=compile`.
- **Call Flow Display**: Dedicated page with sequence diagrams.
- **Enhanced Component Diagram Generation**: Better SOAP/legacy support.
- **Complete Usecase Diagram Redesign**: Multi-level use cases, SOAP support.
- **Entity Visualization**: Identification, details view, primary key highlighting, ERDs.
- **Database Interaction Analysis (Major Update)**:
    *   **DAO/Repository Detection**: Automatic identification (Spring Data, traditional DAOs).
    *   **Database Operation Extraction**: SQL query extraction, method name analysis, CRUD classification, table name extraction.
    *   **Database Schema Visualization**: Generation of schema diagrams, DAO-to-table connections.
    *   **Database Analysis UI (`DatabasePage.js`):**
        *   Displays schema diagram.
        *   **Presents an "Entities and Interacting Classes" view using `analysisResult.dbAnalysis.classesByEntity`.**
        *   **Shows a "Detailed Operations by DAO/Repository Class" table (with method names) using `analysisResult.dbAnalysis.operationsByClass`.**
        *   **Backend `DaoAnalysisServiceImpl` now returns `DbAnalysisResult` and filters redundant interfaces.**
- **PCI/PII Scanning (Enhanced)**: Comprehensive PCI/PII scanning analyzes all text-based files in the repository. Findings are presented in a dedicated UI tab. Detection patterns for PII/PCI data are robustly managed via a type-safe `@ConfigurationProperties` class (`PiiPciProperties`) populated from `application.yml`, ensuring clear and maintainable configuration.

## Known Limitations & TODOs (Summary)

*   **Backend Analysis:**
    *   Dynamic SQL in DAOs (`DaoAnalyzer.java`).
    *   More specific REST endpoint HTTP methods (`EndpointExtractorServiceImpl.java`).
    *   Broader SOAP annotation support (`EndpointExtractorServiceImpl.java`).
    *   Gradle support for Spring Boot version detection (`ProjectDetectorServiceImpl.java`).
    *   Richer project/method summaries (`DocumentationServiceImpl.java`).
    *   **Deeper YAML Parsing**: Basic YAML parsing capability added (`YamlParserService`); future enhancements could leverage this for projects using YAML for significant configuration (beyond simple properties).
*   **Frontend Display:**
    *   Expandable call flows (`FlowExplorer.tsx`).
    *   Global `complexType` expansion in WSDL/XSD view (`ApiSpecsPage.js`).
    *   Advanced UI for large datasets (filtering, pagination).
    *   Dark mode.
*   **General:**
    *   Private Git repository support.
    *   Performance for very large codebases.
    *   Export features (Confluence, PDF, HTML).

(PRD is a higher-level document, so detailed line numbers for TODOs are omitted here but are present in ImplementationSummary/Spec and linked to code.)
