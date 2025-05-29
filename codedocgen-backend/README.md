# CodeDocGen Backend

This is the backend service for the Code Documentation Generator. It is a Spring Boot application responsible for cloning Git repositories, parsing source code, analyzing various aspects of the project, generating diagrams, and providing a REST API for the frontend.

## Core Features

-   **Git Repository Cloning:** Clones public Git repositories for analysis.
-   **Project Detection:**
    -   Detects project type (e.g., Maven).
    -   Identifies if a project uses Spring Boot and extracts its version.
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
    *   Generates synthetic queries for Spring Data methods and extracts SQL from annotations/method bodies using `DaoAnalyzer`.
    *   Performs validation of table names against known entities.
    *   **Returns a `DbAnalysisResult` object containing:**
        *   `operationsByClass`: A map of (DAO/Repository FQN -> List of `DaoOperationDetail`).
        *   `classesByEntity`: A map of (Entity Name -> Set of DAO/Repository FQNs operating on that entity).
    *   Filters out redundant interface entries if their implementing class operations are present.
    *   Contributes to Entity-Relationship and Database Schema diagram generation.
-   **Diagram Generation (PlantUML & Graphviz):**
    -   Renders Class, Component, Usecase, Sequence, ERD, and Database Schema diagrams as SVGs.
    -   Requires Graphviz (`dot` executable).
-   **API Endpoint Extraction:** Identifies REST API endpoints.
-   **Contract Generation:** Generates OpenAPI v3 specification.
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
    *   Refined logic for identifying DAOs/Repositories and extracting/validating their operations.
    *   Logic to remove redundant interface entries from the final DAO analysis if an implementing class with operations is also present.
-   **Accurate Class Typing:** Better distinction between entities, models, and other class stereotypes.
-   **Lombok Handling:** Improved resolution for Lombok-generated methods.
-   **Diagram Generation:** All diagrams as SVG; cleaner sequence diagram labels.
-   **Spring Boot Detection:** Accurate version detection.
-   **Robust Logging:** Extensive `TRACE` and `DEBUG` logging.
-   **Build & Dependency Management:** Correct use of Maven for symbol solver initialization.
-   **Bug Fixes:** Addressed numerous parsing and runtime issues.

## TODOs (Backend Specific)
*   **`EndpointExtractorServiceImpl.java`**: Determine specific HTTP method from `@RequestMapping`; Add support for `@WebMethod` (JAX-WS).
*   **`ProjectDetectorServiceImpl.java`**: Add Gradle build file parsing for Spring Boot version.
*   **`DocumentationServiceImpl.java`**: Add called methods to summaries; Enhance project summary with tech stack.
*   **`DaoAnalyzer.java`**: Handle SQL in variables or constructed dynamically.
*   Support for private Git repositories.
*   Performance optimizations for very large codebases. 