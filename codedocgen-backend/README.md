# CodeDocGen Backend

This is the backend service for the Code Documentation Generator. It is a Spring Boot application responsible for cloning Git repositories, parsing source code, analyzing various aspects of the project, generating diagrams, and providing a REST API for the frontend.

## Core Features

-   **Git Repository Cloning:** Clones public Git repositories for analysis.
-   **Project Detection:**
    -   Detects project type (e.g., Maven).
    -   Identifies if a project uses Spring Boot and extracts its version.
-   **Advanced Java Parsing (JavaParser & JavaSymbolSolver):**
    -   Parses Java source files (`.java`).
    -   Utilizes a sophisticated **Symbol Solver** (JavaSymbolSolver) configured with project sources, compiled classes (`target/classes`), and all dependencies (from `mvn dependency:build-classpath`) to achieve accurate type resolution and method call linking.
    -   Uses Fully Qualified Names (FQN) for robust identification of classes, methods, and method calls.
    -   Employs fallback mechanisms to provide descriptive names for unresolved method calls (e.g., from JDK, external libraries, or complex Lombok patterns).
    -   Determines class types (Controller, Service, Repository, Entity, Model, Interface, Enum, Annotation, etc.) based on annotations and naming conventions.
-   **Call Flow Analysis:**
    -   Identifies entry points for call flows (e.g., public methods in Spring Controllers).
    -   Performs a Depth-First Search (DFS) to trace method calls across the codebase.
    -   Generates detailed step-by-step call sequences for each entry point.
    -   Handles resolved internal calls, framework calls (Spring Data, common JDK/Libs), and provides descriptive labels for unresolved calls.
-   **DAO & Database Analysis:**
    -   Identifies Spring Data repository interfaces and their methods.
    -   Extracts entity names from repository generics (e.g., `CrudRepository<MyEntity, Long>`).
    -   Generates synthetic queries for common Spring Data methods (e.g., `findBy...`, `save`, `delete`).
    -   Detects basic SQL queries from known annotations or method calls within DAO/Repository classes.
    -   Contributes to Entity-Relationship and Database Schema diagram generation by identifying entities.
-   **Diagram Generation (PlantUML & Graphviz):**
    -   Renders various diagrams as SVG files:
        -   Class Diagram
        -   Component Diagram (based on package structure and class types)
        -   Usecase Diagram (from REST endpoints)
        -   Sequence Diagrams (for each identified call flow)
        -   Entity-Relationship Diagram (ERD - primarily based on `@Entity` relationships, basic version)
        -   Database Schema Diagram (from `@Entity` annotations)
    -   Requires Graphviz (`dot` executable) to be installed and available in the system PATH or via `GRAPHVIZ_DOT` environment variable for PlantUML to function correctly.
-   **API Endpoint Extraction:** Identifies REST API endpoints from Spring MVC annotations.
-   **Contract Generation (Basic):**
    -   Generates an OpenAPI v3 specification from extracted REST endpoints.
    -   (Future: WSDL/SOAP contract summarization if WSDLs are found).
-   **Gherkin Feature File Discovery:** Locates and provides content of `.feature` files.
-   **Static File Serving:** Serves generated diagrams and other static output for the frontend.
-   **REST API:** Provides endpoints for the frontend to trigger analysis and retrieve results.
-   **Comprehensive Logging:** Includes detailed TRACE and DEBUG level logging for parsing, symbol resolution, and analysis steps to aid in troubleshooting.

## Run Locally

```bash
mvn spring-boot:run
```
Ensure Graphviz `dot` executable is in your PATH.

## Build Output & Storage

-   **Cloned Repositories:** `/tmp/repos` (configurable via `app.repoStoragePath` in `application.yml`)
-   **Generated Documentation & Diagrams:**
    -   Base Output Path: `./output` (relative to project root, configurable via `app.outputBasePath`)
    -   Markdown Summaries, OpenAPI specs: `${app.outputBasePath}/docs/<repo_id>/`
    -   SVG Diagrams: `${app.outputBasePath}/diagrams/<repo_id>/`
    -   Note: The `<repo_id>` is a unique identifier generated for each analysis run.

## Recent Updates & Key Improvements

-   **Symbol Resolution Overhaul:** Implemented JavaSymbolSolver with comprehensive type solvers (source, target/classes, dependencies) significantly improving the accuracy of method call resolution, FQN usage, and reducing unresolved calls. This forms the backbone of more accurate call flow and DAO analysis.
-   **Enhanced Call Flow Analysis:**
    -   `CallFlowAnalyzer` now correctly processes detailed method call information (including unresolved calls with descriptive names) from the improved parser.
    -   Sequence diagrams and raw call steps are more accurate and reflect the parser's findings.
    -   Cleaner handling of unresolved/framework call prefixes in the output.
-   **Improved DAO & DB Analysis:**
    -   Better identification of Spring Data repository methods and their associated entities.
    -   More robust database schema diagram generation using all identified entities.
-   **Accurate Class Typing:** Refined logic in `JavaParserServiceImpl.determineClassType` to better distinguish between entities, models, and other class stereotypes, using package information and annotation priority.
-   **Lombok Handling:** Added `target/classes` to the symbol solver to aid in resolving Lombok-generated methods, though complex fluent builders may still appear as unresolved calls (with descriptive names).
-   **Diagram Generation:**
    -   All diagrams are now generated as SVG.
    -   `DiagramServiceImpl` includes logic to clean participant names and call labels for sequence diagrams for better readability.
-   **Spring Boot Detection:** Correctly detects Spring Boot projects and their versions, reflected in the analysis output.
-   **Robust Logging:** Added extensive `TRACE` and `DEBUG` logging throughout the parsing and analysis pipeline, particularly in `JavaParserServiceImpl$ClassMetadataVisitor` and `CallFlowAnalyzer`, crucial for diagnosing resolution issues.
-   **Build & Dependency Management:** Ensured Maven (`mvn compile` and `mvn dependency:build-classpath`) is used during symbol solver initialization to make all necessary classes (including generated ones) and dependencies available.
-   **Bug Fixes:** Addressed numerous bugs related to PlantUML rendering, `null` pointers during analysis, incorrect data propagation, and compilation errors introduced during iterative development. 