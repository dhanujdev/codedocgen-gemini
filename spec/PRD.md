# CodeDocGen – Product Requirements Document (PRD)
...


## ✅ CodeDocGen – Product Requirements Document (PRD)

---

### 📌 Overview

**CodeDocGen** is a documentation and visualization generator for Java-based applications hosted in public Git repositories. It supports modern Spring Boot projects as well as legacy Java apps (e.g., SOAP, JDBC/DAO). The tool analyzes the project comprehensively to build interactive and visual documentation for API contracts, entity relationships, class diagrams (currently implemented), call flows (future), and architectural summaries.

---

### 🎯 Objectives

1. Accept a **public Git repository URL** via UI.
2. Clone and scan the Java codebase (including Spring Boot or legacy SOAP/DAO projects). Project name is extracted from the URL.
3. Identify **all classes**, including controllers, services, entities (with improved type detection for repositories), utility classes, configuration files, DAOs, WSDL definitions, YAML files.
4. Generate:

   * Complete **class diagrams** (currently implemented). 
   * ✅ Entity diagrams and ER diagrams for JPA entities with relationship visualization.
   * ✅ Component diagrams with enhanced visualization for SOAP/legacy systems.
   * ✅ Usecase diagrams with actors (User, External System, Database) connected to appropriate operations.
   * End-to-end **method call flow diagrams** (Sequence Diagrams, significantly improved accuracy due to backend enhancements).
   * API contract documentation (Swagger/OpenAPI via SpringDoc for Spring Boot, or **detailed WSDL operation views with parsed XSD element/attribute information** for SOAP).
   * ✅ Visual representations of database interactions from JDBC/DAO layers, including database schema diagrams and operation details.
   * A **microservice/project capability overview** for non-technical stakeholders.
   * Gherkin feature files: Display existing `.feature` files found in the repository. AI-assisted generation of Gherkin files is a future scope.
5. Provide a **centralized documentation dashboard** without role-based filters, with diagrams served and displayed correctly. UI primarily uses a sidebar for navigation.

---

### 🧱 Tech Stack

* **Backend**: Java 21, Spring Boot (Maven)
* **Frontend**: React, Material UI, Axios
* **Parser Tools**: JavaParser (symbol resolution significantly improved by pre-compiling target projects), Reflection, Custom WSDL/XML parsers (including XSD parsing for attributes and complex types).
* **Diagram Generator**: PlantUML (rendered to SVG, for Class and Sequence Diagrams)
* **API Visualization**: Swagger UI (via SpringDoc) + custom WSDL/XSD explorer.
* **Build Tool**: Maven

---

### 🔁 Core Features (with legacy + modern support)

#### 🔹 Iteration 1–3: Repo Onboarding

* Accept Git URL input (public repo only)
* Clone repo to server, extract project name from URL, and detect build type (Maven, Gradle, or raw source)
* Check if it's a Spring Boot or legacy project

#### 🔹 Iteration 4–6: Code Parsing

* Parse:

  * All classes (including controllers, services, utils, daos, enums, configs) with improved type detection for repositories.
  * YAML files (e.g., application.yml, openapi specs)
  * WSDL files (for SOAP endpoint detection and detailed operation/XSD parsing)
  * XSD files (referenced by WSDLs, for element and attribute details)
* Identify:

  * Endpoints (REST from annotations, SOAP from WSDL/XSD)
  * Class relationships
  * Method call chains (static, accuracy significantly improved for main application logic)
  * DAO-layer queries or procedures (table mapping for JDBC)

#### 🔹 Iteration 7–9: Documentation Generation

* **API Docs**:

  * Swagger/OpenAPI from REST annotations (via SpringDoc for Spring Boot projects), displayed using SwaggerUI.
  * **Detailed WSDL/XSD viewer**: Parses WSDL operations, messages, parts, and resolves referenced XSD elements and attributes, displaying them in a structured, readable format. Fallback to raw XML display is provided.
* **Class/Entity Diagrams**:

  * PlantUML class diagrams with zoomable output (currently implemented).
  * Entity diagrams, ER diagrams, and other types are future enhancements.
* **Call Flow**:

  * Method call chain from entrypoint (e.g., Controller or SOAP handler) to leaf methods, visualized as Sequence Diagrams. Accuracy for main application logic is significantly improved.
  * Include utility, constants, configs
* **Gherkin Feature Files**:
  * Display existing `.feature` files found in the repository.
  * (AI-assisted generation of Gherkin files is a future scope).

#### 🔹 Iteration 10–12: UI and Publishing

* Dashboard UI (sidebar navigation, sections for Overview, API Specs, Call Flow, Diagrams, etc.):

  * Overview (project summary, features)
  * API Specs (Interactive SwaggerUI for REST, detailed WSDL/XSD tree view for SOAP)
  * Call Flow (Sequence diagram viewer)
  * Features (from flow/methods) - (Future Enhancement for flow-based features)
  * Diagrams (Class Diagrams, Sequence Diagrams; others like entity, call flow are future enhancements)
  * Database View (DAO interaction map) - (Future Enhancement)
  * Publish (Confluence or Export)
* Export options: Markdown, HTML, PDF
* Final publish to Confluence with preview/edit mode

- Backend API responses now include a 'type' field for each class (controller, service, repository, entity, enum, interface, abstract, test, class, soap, config, other) for dynamic categorization, with improved accuracy for repository types.
- Frontend dynamically filters and displays classes using the 'type' field.
- Backend now serves generated diagrams correctly, and frontend displays them.
- Frontend uses Material UI and includes components like `TabPanel.js` for tabbed navigation and a `Sidebar.js` for main navigation.
- Project name is now correctly extracted from the Git URL.
- Manifest icon references (logo192.png, logo512.png) have been removed from the frontend.
- Frontend handles null/empty class lists gracefully to prevent runtime errors.
- Backend includes API logging for request/response tracing and improved CORS configuration.
- The /api/repo/classes endpoint now uses a query parameter (?repoPath=...) instead of a path variable.
- Sequence diagrams now use quoted fully qualified names (FQNs) for PlantUML participants and arrows, ensuring valid syntax and correct rendering.
- SVG is the default output format for all diagrams.
- No PlantUML upgrade was needed; version 1.2025.2 is current.
- Backend Symbol Solver for JavaParser now pre-compiles Maven projects (`mvn compile`) to ensure generated sources (e.g., JAXB) are available, significantly improving call flow accuracy.
- Backend now parses XSD files referenced in WSDLs and includes their content in the analysis response, enabling detailed WSDL element display in the frontend.
- Performance optimization is a future enhancement.

---

###  Output Structure

## Recent Updates (May 2025)
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
- CORS is enabled for `/generated-output/**` so the frontend and SVG viewer can fetch diagrams directly from the backend.
- JavaParser upgraded to 3.26.4 for robust Java 7-21 compatibility.
- If any files fail to parse, a warning is shown in the UI listing the affected files.
- A new `svg-viewer.html` (in the frontend public folder) allows zoomable, fit-to-screen SVG viewing in a new browser tab.
- **WSDL/XSD Parsing**: Frontend now deeply parses WSDL files and their imported XSD schemas to display detailed operation information, including element attributes and complex type structures.
- **Symbol Resolution Enhancement**: Backend JavaParser service now executes a Maven compile (`mvn compile -DskipTests -q`) on the target project prior to analysis. This makes generated sources (e.g., JAXB classes) available to the symbol solver, dramatically improving the accuracy of method call resolution and, consequently, the generated call flow diagrams.
- **Call Flow Display**: A dedicated "Call Flow" page now displays generated sequence diagrams.
- **Enhanced Component Diagram Generation**: Significantly improved visualization of SOAP and legacy application architectures:
  - Now accurately detects SOAP endpoints, services, repositories, and other components through multiple strategies:
    - Class name patterns (e.g., `*Endpoint`, `*ServiceImpl`, `*WebService`)
    - Annotations (`@WebService`, `@Endpoint`, `@SOAPBinding`, etc.)
    - Traditional Spring stereotypes
  - Represents WSDL interfaces in the diagram to show service contracts
  - Uses component stereotypes for clear visual categorization (`<<endpoint>>`, `<<service>>`)
  - Shows meaningful relationships between components with descriptive labels
  - Organizes components hierarchically by package
  - Simplifies class names for better readability
  - Analyzes both field declarations and method call patterns to establish comprehensive relationships
  - Handles various legacy code patterns and SOAP service implementations

- **Complete Usecase Diagram Redesign**: Overhauled usecase diagram generation to better support SOAP/legacy applications:
  - Added "External System" actor to represent SOAP service clients
  - Created multi-level usecase representation:
    - First level: Controller/endpoint public methods (direct API entry points)
    - Second level: Service methods called by controllers/endpoints
    - Database operations as specialized usecases
  - Introduced visual differentiation using icons:
    - SOAP operations (gear icon)
    - Service methods (layers icon)
    - Database operations (database icon)
  - Analyzes method call chains to establish relationships between usecases
  - Intelligently connects usecases to appropriate actors (User, External System, or Database)
  - Clearly visualizes multi-tier architecture and data flows
  - Provides architectural insights showing integration points and component interactions

## Entity Visualization
The system provides comprehensive entity visualization:

- **Entity Identification**: Automatically identifies JPA entities in the codebase through annotation detection
- **Entity Details View**: Displays all entity fields with types and annotations in a clean, text-based format
- **Primary Key Highlighting**: Clearly identifies primary key fields
- **Entity Relationships**: Visualizes entity relationships in the Diagrams tab using PlantUML-based ER diagrams
- **Field Type Analysis**: Shows field types for better understanding of data structures

## Database Interaction Analysis (May 2025)
- **DAO/Repository Detection**: 
  - Automatic identification of data access classes through name patterns, annotations, and interface implementations
  - Support for both Spring Data repositories and traditional DAO patterns
  - Recognition of both explicit SQL queries and method name conventions

- **Database Operation Extraction**:
  - SQL query extraction from string literals in method bodies
  - Method name analysis for repositories using naming conventions (findBy*, save*, delete*, etc.)
  - Classification of operations as SELECT, INSERT, UPDATE, DELETE
  - Table name extraction from SQL queries and method patterns
  - Parameter usage analysis for better understanding of query conditions

- **Database Schema Visualization**:
  - Generation of database schema diagrams showing tables and relationships
  - Foreign key relationship identification from method parameters and SQL queries
  - DAO/Repository connections to their relevant tables
  - Visual operation indicators showing CRUD patterns

- **Database Analysis UI**:
  - Dedicated Database page accessible from the sidebar
  - Comprehensive table list showing all detected database entities
  - Detailed operation view grouped by DAO/Repository class
  - Color-coded operation types for better visual scanning
  - Integration with the diagram viewer for database schema visualization

```