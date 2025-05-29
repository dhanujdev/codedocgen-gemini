# CodeDocGen Backend

This is the backend service for the Code Documentation Generator. It is a Spring Boot application responsible for cloning Git repositories, parsing source code, analyzing various aspects of the project, generating diagrams, and providing a REST API for the frontend.

## Core Features

-   **Git Repository Cloning:** Clones public Git repositories for analysis.
-   **Project Detection:**
    -   Detects project type (e.g., Maven, Gradle).
    -   Identifies if a project uses Spring Boot and extracts its version (from `pom.xml`, `build.gradle`, `build.gradle.kts`).
-   **Advanced Java Parsing (JavaParser & JavaSymbolSolver):**
    -   Parses Java source files (`.java`).
    -   Utilizes a sophisticated **Symbol Solver** for accurate type resolution and method call linking.
    -   Uses Fully Qualified Names (FQN) for robust identification.
    -   Employs fallback mechanisms for unresolved method calls.
    -   Determines class types (Controller, Service, Repository, Entity, Model, etc.).
-   **Call Flow Analysis:**
    -   Identifies entry points for call flows.
    -   Performs a Depth-First Search (DFS) to trace method calls.
    -   Generates detailed step-by-step call sequences.
-   **DAO & Database Analysis (Enhanced):**
    -   Identifies Spring Data repository interfaces and other DAO patterns.
    *   Extracts entity names from repository generics and class definitions.
    *   Generates synthetic queries for Spring Data methods and extracts SQL from annotations/method bodies using `DaoAnalyzer` (includes basic support for SQL in string variables).
    *   Performs validation of table names against known entities.
    *   **Returns a `DbAnalysisResult` object containing:**
        *   `operationsByClass`: A map of (DAO/Repository FQN -> List of `DaoOperationDetail`).
        *   `classesByEntity`: A map of (Entity Name -> Set of DAO/Repository FQNs operating on that entity).
    *   Filters out redundant interface entries if their implementing class operations are present.
    *   Contributes to Entity-Relationship and Database Schema diagram generation.
-   **Logger Insights (New Feature):**
    *   Scans all Java files for SLF4J logging calls (`log.info`, etc.) and `System.out.println`/`err.println`.
    *   Extracts log message, level, class name, and line number.
    *   Analyzes logged variables and message content for potential PII (Personally Identifiable Information) and PCI (Payment Card Industry) data exposure.
    *   Uses comprehensive and extensible keyword patterns for PII and PCI, including common abbreviations and variations. **These keyword patterns are configurable via `application.yml`.**
    *   Flags logs with separate `isPiiRisk` and `isPciRisk` booleans.
    *   Provides this data as a list of `LogStatement` objects within the main `ParsedDataResponse`.
-   **YAML Parsing (New):**
    *   A basic `YamlParserService` is available to parse YAML files (specified by path) into a `Map<String, Object>` structure. This can be used in the future for deeper analysis of project-specific YAML configurations.
-   **Diagram Generation (PlantUML & Graphviz):**
    -   Renders Class, Component, Usecase, Sequence, ERD, and Database Schema diagrams as SVGs.
    -   Requires Graphviz (`dot` executable).
-   **API Endpoint Extraction:** Identifies REST (`@RequestMapping` with method attribute, other common Spring REST annotations) and SOAP (`@WebMethod`) API endpoints.
-   **Documentation Generation:** Creates project summaries including method call details and basic tech stack information.
-   **Contract Generation:** Generates OpenAPI v3 specification (leveraging `cxf-rt-rs-service-description-swagger` for CXF JAX-RS projects where applicable).
-   **Gherkin Feature File Discovery:** Locates and provides content of `.feature` files.
-   **Static File Serving:** Serves generated diagrams and other static output.
-   **REST API:** Provides endpoints for analysis and results, including the new `dbAnalysis` structure and `logStatements` in `ParsedDataResponse`.
-   **Comprehensive Logging.**

## Run Locally

```bash
mvn spring-boot:run
```
Ensure Graphviz `dot` executable is in your PATH.

## Build Output & Storage

-   Configurable paths for cloned repositories and generated outputs via `application.yml`.

## Recent Updates & Key Improvements

-   **Symbol Resolution Overhaul:** Significant accuracy improvements in method call resolution and FQN usage.
-   **Enhanced Call Flow Analysis:** More accurate sequence diagrams and raw call steps.
-   **Improved DAO & DB Analysis (Major Update):**
    *   Implementation of `DbAnalysisResult` to provide a structured view of database interactions, including mappings between entities and the classes that operate on them (`classesByEntity`), alongside detailed operations per class (`operationsByClass`).
    *   Refined logic for identifying DAOs/Repositories and extracting/validating their operations, including basic support for SQL in string variables (`DaoAnalyzer.java`).
    *   Logic to remove redundant interface entries from the final DAO analysis if an implementing class with operations is also present.
-   **Logger Insights Feature:**
    *   Successfully integrated into the main analysis flow.
    *   PII and PCI keyword patterns significantly expanded for better detection accuracy, including numerous short forms and variations.
    *   **Keyword patterns have been externalized to `application.yml` for easier customization and updates without code changes.**
    *   Backend models (`LogStatement`, `LogVariable`) updated for separate PII/PCI flagging.
-   **YAML Parsing:**
    *   Added `YamlParserService` and `YamlParserServiceImpl` to provide basic YAML file parsing capabilities (file path to `Map<String, Object>`). Future enhancements can use this for projects heavily reliant on YAML for configuration.
-   **Accurate Class Typing:** Better distinction between entities, models, and other class stereotypes.
-   **Lombok Handling:** Improved resolution for Lombok-generated methods.
-   **Diagram Generation:** All diagrams as SVG; cleaner sequence diagram labels.
-   **Spring Boot Detection:** Accurate version detection from `pom.xml` and Gradle build files (`build.gradle`, `build.gradle.kts`), with corrected regex for Java string literals.
-   **Endpoint Extraction:** Added support for `@RequestMapping` method attribute and JAX-WS `@WebMethod` annotations (`EndpointExtractorServiceImpl.java`).
-   **Documentation Generation:** Project summaries now include called methods/external calls, and basic tech stack details (`DocumentationServiceImpl.java`).
-   **Robust Logging:** Extensive `TRACE` and `DEBUG` logging.
-   **Build & Dependency Management:** Correct use of Maven for symbol solver initialization.
-   **Bug Fixes:** Addressed numerous parsing and runtime issues, including URL parsing for Git cloning and compilation errors related to static context in `LoggerInsightsServiceImpl`.
-   **Dependency Updates:** Added `cxf-rt-rs-service-description-swagger` to resolve CXF JAX-RS and Swagger integration issues.

## TODOs (Backend Specific)
*   **`DaoAnalyzer.java`**: Further enhance to handle more complex cases of SQL in variables or constructed dynamically.
*   **`LoggerInsightsServiceImpl.java`**: Keyword patterns are now externalized to `application.yml`. Continue to refine and expand these patterns based on real-world examples and feedback.
*   **Deeper YAML Parsing**: `YamlParserService` is implemented for basic parsing. Future work involves identifying specific YAML files within user projects that require deep parsing for meaningful data extraction and integrating this service into the main analysis flow where appropriate.
*   Support for private Git repositories.
*   Performance optimizations for very large codebases.
*   Further refinement of REST endpoint detail extraction for complex request/response bodies.
*   [MINOR WARNING] Address `Maven project compiled test classes directory ... does not exist` if it becomes problematic (currently considered benign).
*   [MINOR WARNING] Address `Could not detect Java version from pom.xml in [cloned_repo]` if it impacts analysis of diverse projects (main project's Java version is set; this refers to temporary clones). 