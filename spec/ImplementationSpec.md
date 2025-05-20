# CodeDocGen – Implementation & Folder Spec
...
// Cursor Spec File for CodeDocGen
// This spec should be input to Cursor AI to scaffold the full-stack application.
// Java 21 (Spring Boot - Maven) Backend + React + Material UI Frontend (NOT Bootstrap/Tailwind as previously drafted in early spec)

/*
====================================
📘 HIGH-LEVEL STRUCTURE
====================================
Backend: Java 21 + Spring Boot (Maven)
Frontend: React + Material UI (Note: Previous spec mentioned Tailwind CSS + shadcn/ui, but current implementation uses Material UI)
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
│   │   └── AnalysisController.java    // Renamed from RepoController, orchestrates analysis, extracts project name, handles diagram paths
│   ├── dto/
│   │   ├── RepoRequest.java
│   │   └── ParsedDataResponse.java  // Includes server-relative diagram paths, extracted project name, wsdlFilesContent, xsdFilesContent
│   ├── service/
│   │   ├── impl/                    // Service implementations here
│   │   │   ├── GitServiceImpl.java
│   │   │   ├── ProjectDetectorServiceImpl.java
│   │   │   ├── JavaParserServiceImpl.java // Improved class type detection. Symbol resolution enhanced by pre-compiling target project.
│   │   │   ├── EndpointExtractorServiceImpl.java
│   │   │   ├── DiagramServiceImpl.java      // Generates class and sequence diagrams (SVG)
│   │   │   └── DocumentationServiceImpl.java // Generates summaries, reads WSDL & XSD files.
│   │   ├── GitService.java
│   │   ├── ProjectDetectorService.java
│   │   ├── JavaParserService.java
│   │   ├── EndpointExtractorService.java
│   │   ├── DiagramService.java
│   │   └── DocumentationService.java
│   ├── parser/
│   │   ├── ClassScanner.java
│   │   ├── CallFlowAnalyzer.java
│   │   ├── SoapWsdlParser.java
│   │   ├── DaoAnalyzer.java
│   │   └── YamlParser.java
│   ├── util/
│   │   ├── FileUtils.java
│   │   └── PlantUMLRenderer.java
│   └── model/
│       ├── ClassMetadata.java
│       ├── MethodMetadata.java
│       ├── EndpointMetadata.java
│       └── DiagramType.java
├── src/main/resources/
│   └── application.yml
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
├── src/
│   ├── components/
│   │   ├── RepoForm.js             // Changed from .tsx to .js based on current files
│   │   ├── AnalysisDisplay.js      // Main display, uses TabPanel, handles diagram URLs. Changed from .tsx. Manages activeSection state.
│   │   ├── TabPanel.js             // Newly added for MUI Tabs (though main navigation is now Sidebar)
│   │   ├── Sidebar.js              // Main navigation component for switching between Overview, API Specs, Call Flow, etc.
│   │   ├── EndpointTable.tsx       // Assuming .tsx, but verify if .js
│   │   ├── DiagramViewer.tsx       // Assuming .tsx, but verify if .js. Displays diagrams from server-relative URLs.
│   │   ├── FlowExplorer.tsx        // Assuming .tsx, but verify if .js (Currently CallFlowPage.js serves this purpose)
│   │   ├── FeatureFileList.tsx     // Assuming .tsx, but verify if .js. Displays existing Gherkin files.
│   │   ├── MarkdownExport.tsx      // Assuming .tsx, but verify if .js
│   │   └── ConfluencePublisher.tsx // Assuming .tsx, but verify if .js
│   ├── pages/
│   │   ├── Dashboard.tsx           // If used; current app structure is simpler
│   │   ├── Home.tsx                // If used
│   │   ├── OverviewPage.js         // Displays project summary
│   │   ├── ApiSpecsPage.js         // Displays OpenAPI via SwaggerUI and detailed WSDL/XSD information
│   │   ├── CallFlowPage.js         // Displays sequence diagrams for call flows
│   │   ├── DiagramsPage.js         // General diagrams page (could consolidate or be used for Class/ER diagrams)
│   │   ├── GherkinPage.js          // Displays Gherkin feature files
│   │   └── ClassesPage.js          // Displays detailed class metadata
│   ├── services/
│   │   ├── api.ts                  // If exists
│   │   └── endpoints.ts            // If exists
│   ├── types/
│   │   ├── Repo.ts                 // If exists
│   │   └── ParsedData.ts           // If exists
│   ├── App.js                    // Main app component. Changed from .tsx. Handles API_URL prop.
│   ├── index.js                  // Entry point. Changed from .tsx.
│   ├── index.css                 // MUI setup, global styles. (Bootstrap import removed as MUI is used)
│   └── custom.d.ts               // Optional: type augmentation (if using TypeScript extensively)
├── package.json                    // Includes new deps: tailwindcss-animate, react-router-dom, @types/react-dom, prop-types
└── README.md

/*
====================================
📘 ITERATION ROADMAP FOR CURSOR
====================================
*/

1. Iteration 1: Public Git Repo Input (UI form + backend endpoint)
2. Iteration 2: Clone repo using JGit
3. Iteration 3: Detect Maven/Gradle/Raw + Spring Boot or Legacy
4. Iteration 4: Parse all classes, methods, inheritance
5. Iteration 5: Extract REST and SOAP endpoints from annotations/WSDL. Categorize SOAP endpoint classes as type 'soap', and config/test classes as 'config'/'test'. Improve repository type detection for interfaces with `@Repository`.
6. Iteration 6: Trace full call flow (controller → service → DAO/utils/...) - (Future Enhancement)
7. Iteration 7: ✅ Parse legacy DAO, JDBC, SQL, and table usage - Now implemented with automated detection and diagram generation.
8. Iteration 8: Generate class diagrams with PlantUML (currently implemented). Implement other diagram types (entity, ER, flow) - (Future Enhancement). Ensure diagrams are served correctly by the backend and displayed by the frontend.
9. Iteration 9: Generate Swagger (OpenAPI via SpringDoc for Spring Boot) or SOAP docs + microservice summary. Extract project name from Git URL for summary.
10. Iteration 10: Build full UI with tabs (Overview, Endpoints, Diagrams, DB Schema, Export) using Material UI. Ensure Gherkin files are displayed if found (generation is future scope).
11. Iteration 11: Export to Confluence + PDF/HTML downloads

/*
====================================
📘 README.md BACKEND EXCERPT
====================================
# CodeDocGen Backend

## Features
- Clone public Git repo
- Detect build type and language flavor
- Parse Java classes, methods, WSDL, YAML, DAOs
- Generate Swagger/OpenAPI or SOAP contracts
- Render diagrams (PlantUML)
- Provide REST API to serve frontend

## Run Locally
```bash
mvn spring-boot:run
```

## Build Output
- `/tmp/repos` → Cloned repos
- `/output/docs` → Markdown, Swagger, WSDL summaries
- `/output/diagrams` → PNG/UML diagrams

/*
====================================
📘 README.md FRONTEND EXCERPT
====================================
# CodeDocGen Frontend

## Features
- Submit public Git URL for parsing
- View API endpoints (REST + SOAP)
- Render diagrams (class, entity, flow)
- Download Markdown, feature files
- Publish to Confluence

## Run Locally
```bash
npm install
npm run dev
```

## Uses
- React + Material UI
- Axios for backend integration

/*
====================================
📘 KEY IMPLEMENTATION UPDATES
====================================
- Backend now categorizes each class with a 'type' field (controller, service, repository, entity, enum, interface, abstract, test, class, soap, config, other) in API responses, with improved accuracy for repository types (e.g. `@Repository` on interfaces).
- Frontend dynamically filters and displays classes using the 'type' field from the backend.
- Backend correctly serves generated diagram images (SVG format).
- Frontend correctly constructs image URLs for diagrams using server-relative paths and displays them via `DiagramViewer.js` (or similar).
- Project name is extracted from the Git URL for display and summaries.
- Frontend navigation primarily uses a `Sidebar.js` component to switch between different views (`OverviewPage`, `ApiSpecsPage`, `CallFlowPage`, etc.) managed by `App.js`.
- Resolved several frontend compilation issues by adding missing npm packages and fixing code errors (logger, API_URL prop).
- Manifest icon references (logo192.png, logo512.png) have been removed from the frontend.
- Frontend uses Material UI (not Tailwind/shadcn/ui as in earlier spec drafts) for styling and components.
- Sequence diagrams now use quoted fully qualified names (FQNs) for PlantUML participants and arrows, ensuring valid syntax and correct rendering.
- SVG is the default output format for diagrams (class and sequence).
- No PlantUML upgrade was needed; version 1.2025.2 is current.
- **Symbol Resolution Enhancement (Backend)**: `JavaParserServiceImpl` now attempts to run `mvn compile -DskipTests -q` on the target project before symbol solving. This critical step ensures generated sources (e.g., JAXB classes from XSDs) are available, significantly improving symbol resolution accuracy for method calls and thus the quality of call flow data.
- **XSD Handling (Backend & Frontend)**: 
    - `DocumentationServiceImpl` now includes a `findAndReadXsdFiles` method to locate and read XSD files, typically found alongside WSDLs.
    - `ParsedDataResponse` DTO now includes `xsdFilesContent` (Map<String, String>) to carry this XSD data to the frontend.
    - `AnalysisController` calls the new service method to populate `xsdFilesContent`.
    - `ApiSpecsPage.js` (Frontend) consumes `xsdFilesContent`. When parsing a WSDL, it looks for `<xsd:import>` or `<xs:import>` tags, matches their `schemaLocation` (or namespace) to keys in `xsdFilesContentMap`, parses the XSD string into a DOM, and then uses this to recursively resolve and display element attributes, types, and nested structures within WSDL messages. This provides a rich, detailed view of SOAP API contracts.
- **API Specs Display (Frontend)**: `ApiSpecsPage.js` has been significantly enhanced:
    - For OpenAPI projects, it renders the specification using `<SwaggerUI spec={...} />`.
    - For SOAP projects, it parses WSDL and linked XSDs to display operations, messages, and detailed element structures with attributes, types, and cardinalities. Includes a fallback to show raw WSDL/XSD if parsing has issues or for user reference.
- **Call Flow Display (Frontend)**: `CallFlowPage.js` is now implemented to receive `analysisResult` and display sequence diagrams found in `analysisResult.diagramMap` (for `SEQUENCE_DIAGRAM` type) or `analysisResult.sequenceDiagrams`, using the `DiagramViewer` component.
- Performance tuning for large projects is a future enhancement.
*/

## Recent Updates (May 2025)
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
- CORS is enabled for `/generated-output/**` so the frontend and SVG viewer can fetch diagrams directly from the backend.
- JavaParser upgraded to 3.26.4 for robust Java 7-21 compatibility.
- If any files fail to parse, a warning is shown in the UI listing the affected files.
- A new `svg-viewer.html` (in the frontend public folder) allows zoomable, fit-to-screen SVG viewing in a new browser tab.
- Entity Relationship diagrams now properly identify JPA entities using annotation-based detection.
- The Entity tab UI has been simplified to a single consolidated view, removing the problematic diagram tab.
- Entity relationship rendering and visualization is now exclusively handled through the Diagrams tab.
- Improved entity filtering logic to detect JPA entities with different annotation patterns.
- The Entity tab now provides a cleaner, text-based view with highlighted primary keys.
- **Symbol Solver Pre-compilation**: Implemented a crucial pre-step in `JavaParserServiceImpl` to execute `mvn compile -DskipTests -q` on the target Maven project. This ensures that generated source files (e.g., from JAXB, annotation processors) are compiled and available to the JavaParser symbol solver, dramatically improving its ability to resolve types and method calls, especially those involving generated code. This has led to more accurate call flow analysis.
- **WSDL & XSD Deep Parsing for API Specs**: The frontend's `ApiSpecsPage.js` now performs detailed parsing of WSDL files. It identifies imported XSD schemas, retrieves their content from the backend (which now provides `xsdFilesContent` in the `ParsedDataResponse`), and recursively analyzes these schemas. This allows the UI to display not just WSDL operations, but also the detailed structure of message parts, including element attributes, types (simple and complex), and occurrences, extracted from the XSDs. This provides a comprehensive view of SOAP API contracts.
- **Dedicated Call Flow Page**: A new `CallFlowPage.js` has been implemented in the frontend to display sequence diagrams. It uses the existing `DiagramViewer.js` component and sources diagram data from the `analysisResult`.
- **Sidebar Navigation**: Frontend navigation has been consolidated into a `Sidebar.js` component, improving UI organization and replacing previous horizontal tab-based primary navigation for sections.
- **Enhanced Component Diagram Generation for SOAP/Legacy Systems**: Major improvements to the component diagram generation in `DiagramServiceImpl.java`:
  - **Expanded Component Detection**: Now identifies components through multiple methods:
    - Class name patterns (e.g., `*Endpoint`, `*ServiceImpl`, `*Service`, `*WebService`)
    - Annotations (`@WebService`, `@Endpoint`, `@SOAPBinding`, etc.)
    - Traditional Spring stereotypes (`@Component`, `@Service`, `@Repository`, etc.)
  - **WSDL Interface Representation**: Automatically adds WSDL interfaces to the diagram when SOAP endpoints are detected
  - **Component Role Classification**: Implements intelligent component role detection through the new `determineComponentRole()` method that analyzes class names, annotations, and types
  - **Semantic Relationships**: The new `determineRelationship()` method adds meaningful relationship labels between components (e.g., "delegates to", "uses", "exposes", "implements")
  - **Visual Enhancements**:
    - Uses stereotypes (e.g., `<<endpoint>>`, `<<service>>`) for better visual categorization
    - Displays simplified class names (without package prefixes) for better readability
    - Organizes components hierarchically by package
    - Uses different arrow styles for different relationship types (solid for composition, dotted for method calls)
  - **Field and Method Call Analysis**: Analyzes both field declarations and method calls to establish comprehensive relationships between components
  - **Handles Legacy Code Patterns**: Better recognition of legacy code styles and SOAP service implementations that don't follow modern Spring patterns

- **Comprehensive Usecase Diagram Improvements for SOAP/Legacy Systems**:
  - **Extended Actor Model**: Added "External System" actor to represent SOAP service clients
  - **Multi-level Usecase Detection**:
    - First level: Controller/endpoint public methods representing direct API entry points
    - Second level: Service methods called by controllers/endpoints
    - Added database operations as a special type of usecase
  - **SOAP Endpoint Recognition**: Enhanced detection for SOAP endpoints with specialized handling:
    - Identifies endpoints through name patterns and annotations
    - Connects SOAP operations to the "External System" actor instead of "User"
    - Adds special styling with gear icon (`<&cog>`) for SOAP operations
  - **Service Layer Integration**:
    - Analyzes controller method calls to identify service method invocations
    - Creates "includes" relationships between controller usecases and service usecases
    - Adds layers icon (`<&layers>`) to service method usecases
  - **Database Interaction Visualization**:
    - Added "Database" actor for repository operations
    - Automatically identifies data access methods with naming patterns (get*, find*, save*, etc.)
    - Connects service methods to database operations
    - Uses database icon (`<&database>`) for database operation usecases
  - **Architectural Insights**:
    - Clearly shows the multi-tier architecture (controllers/endpoints → services → repositories)
    - Provides visual distinction between different types of operations
    - Demonstrates external interfaces and integration points
    - Shows data flow between components

## DAO/JDBC Analysis Feature (May 2025)
- **Backend Implementation**:
  - Added `DaoAnalysisService` and `DaoAnalysisServiceImpl` to analyze database operations
  - Enhanced `DaoAnalyzer` to extract SQL queries and infer operations from method names
  - Added `DATABASE_DIAGRAM` type to `DiagramType` enum
  - Updated `PlantUMLRenderer` to support database schema diagrams
  - Added DAO operations to the `ParsedDataResponse` DTO
  - Integrated database analysis into the `AnalysisController` workflow
  - Implemented database schema diagram generation based on extracted operations

- **Frontend Implementation**:
  - Added `Database.js` page in the `pages` directory to display database operations
  - Updated `Sidebar.js` to include a Database navigation item
  - Enhanced `App.js` to render the Database page
  - Implemented table detection display with chips for discovered table names
  - Added database operation listing grouped by DAO/Repository class
  - Color-coded operation types (SELECT, INSERT, UPDATE, DELETE) for easy scanning
  - Integrated database schema diagram display using the existing diagram viewer

- **Key Capabilities**:
  - Automatic detection of database access classes through multiple signals:
    - Class name patterns (containing "DAO", "Repository", "Mapper")
    - Interface implementations (CrudRepository, JpaRepository, etc.)
    - Annotations (@Repository, @DAO)
    - Method name patterns (find*, save*, delete*, etc.)
  - Extraction of SQL operations through both static analysis and naming conventions
  - Inference of database tables and their relationships
  - Visualization of the database schema with UML notation
  - Comprehensive UI for browsing all database operations
