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
-   **Diagram Generation (PlantUML & Graphviz):**
    -   Renders Class, Component, Usecase, Sequence, ERD, and Database Schema diagrams as SVGs.
    -   Requires Graphviz (`dot` executable).
-   **API Endpoint Extraction:** Identifies REST (`@RequestMapping` with method attribute, other common Spring REST annotations) and SOAP (`@WebMethod`) API endpoints.
-   **Documentation Generation:** Creates project summaries including method call details and basic tech stack information.
-   **Contract Generation:** Generates OpenAPI v3 specification (leveraging `cxf-rt-rs-service-description-swagger` for CXF JAX-RS projects where applicable).
-   **Gherkin Feature File Discovery:** Locates and provides content of `.feature` files.
-   **Static File Serving:** Serves generated diagrams and other static output.
-   **REST API:** Provides endpoints for analysis and results, including the new `dbAnalysis` structure in `ParsedDataResponse`.
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
-   **Accurate Class Typing:** Better distinction between entities, models, and other class stereotypes.
-   **Lombok Handling:** Improved resolution for Lombok-generated methods.
-   **Diagram Generation:** All diagrams as SVG; cleaner sequence diagram labels.
-   **Spring Boot Detection:** Accurate version detection from `pom.xml` and Gradle build files (`build.gradle`, `build.gradle.kts`), with corrected regex for Java string literals.
-   **Endpoint Extraction:** Added support for `@RequestMapping` method attribute and JAX-WS `@WebMethod` annotations (`EndpointExtractorServiceImpl.java`).
-   **Documentation Generation:** Project summaries now include called methods/external calls, and basic tech stack details (`DocumentationServiceImpl.java`).
-   **Robust Logging:** Extensive `TRACE` and `DEBUG` logging.
-   **Build & Dependency Management:** Correct use of Maven for symbol solver initialization.
-   **Bug Fixes:** Addressed numerous parsing and runtime issues.
-   **Dependency Updates:** Added `cxf-rt-rs-service-description-swagger` to resolve CXF JAX-RS and Swagger integration issues.

## TODOs (Backend Specific)
*   **`DaoAnalyzer.java`**: Further enhance to handle more complex cases of SQL in variables or constructed dynamically.
*   Support for private Git repositories.
*   Performance optimizations for very large codebases.
*   Deeper YAML parsing if used for project configuration.
*   Further refinement of REST endpoint detail extraction for complex request/response bodies.
*   [MINOR WARNING] Address `Maven project compiled test classes directory ... does not exist` if it becomes problematic (currently considered benign).
*   [MINOR WARNING] Address `Could not detect Java version from pom.xml in [cloned_repo]` if it impacts analysis of diverse projects (main project's Java version is set; this refers to temporary clones). 