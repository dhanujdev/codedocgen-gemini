# CodeDocGen Frontend

This project is the frontend for the Code Documentation Generator, built with React and Material UI (as the primary UI library).

## Prerequisites

- Node.js (v18 or later recommended)
- npm or yarn

## Setup

1.  **Clone the repository (if you haven't already for the backend).**
2.  **Navigate to the `codedocgen-frontend` directory:**
    ```bash
    cd codedocgen-frontend
    ```
3.  **Install dependencies:**
    Using npm:
    ```bash
    npm install
    ```
    Or using yarn:
    ```bash
    yarn install
    ```
4.  **Environment Variables (Recommended):**
    Create a `.env` file in the `codedocgen-frontend` root directory with:
    ```
    REACT_APP_API_URL=http://localhost:8080/api
    REACT_APP_BACKEND_STATIC_URL=http://localhost:8080
    ```
    Defaults are provided if `.env` is not used.

## Available Scripts

In the project directory, you can run:

### `npm start` or `yarn start`

Runs the app in development mode.
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.
You may also see any lint errors in the console.

The backend server (codedocgen-backend) must be running for the API calls to work and diagrams to be served.

### `npm test` or `yarn test`

Launches the test runner in interactive watch mode.

### `npm run build` or `yarn build`

Builds the app for production to the `build` folder.
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.
Your app is ready to be deployed!

### `npm run eject`

**Note: this is a one-way operation. Once you `eject`, you can't go back!**

If you aren't satisfied with the build tool and configuration choices, you can `eject` at any time. This command will remove the single build dependency from your project.

Instead, it will copy all the configuration files and the transitive dependencies (webpack, Babel, ESLint, etc.) right into your project so you have full control over them. All of the commands except `eject` will still work, but they will point to the copied scripts so you can tweak them. At this point, you're on your own.

## Features

- **Git Repository Analysis:**
    - Input a Git repository URL for analysis
    - **Support for private enterprise repositories through backend authentication**
    - Analysis of complex enterprise Java applications

- View a project overview including name, type, and detected Spring Boot version.
- Browse detailed metadata for classes, interfaces, enums, and annotations.
- View extracted REST API endpoints.
- Display various generated PlantUML diagrams with zoom/pan capabilities:
    - Class Diagram
    - Component Diagram
    - Usecase Diagram
    - Entity-Relationship Diagram (ERD)
    - Database Schema Diagram

- **Call Flow Analysis (`CallFlowPage.js` & `FlowExplorer.tsx`):**
    - View generated Sequence Diagrams for key controller/service entry points.
    - Explore associated call flows, now displayed as a clean, numbered plaintext list where method parameters are removed (e.g., `myMethod()` instead of `myMethod(int arg)`).
    - Easily copy the formatted plaintext call trace to the clipboard using a dedicated "Copy Trace" button, ideal for pasting into AI assistants or other documentation.
    - Continues to display user-friendly names for overall flows and diagrams, generated by parsing full Java method signatures using the `generateFlowDisplayName` helper function.

- **Database Analysis (Enhanced):**
    - View a database schema diagram.
    - **Display an "Entities and Interacting DAO/Repository Classes" section, showing which DAOs/Repositories operate on each detected Entity (uses `analysisResult.dbAnalysis.classesByEntity`).**
    - **Display a "Detailed Operations by DAO/Repository Class" table, listing specific SQL operations (including method name, type, tables, and query) for each class (uses `analysisResult.dbAnalysis.operationsByClass`).**

- **Logger Insights Page (`LoggerInsightsPage.js`):**
    - Displays a filterable and sortable table of all detected logging statements from the analyzed project.
    - Shows class name, line number, log level, the log message content, and extracted variables. Each log entry is expandable to show variables.
    - Includes "Expand All" and "Collapse All" buttons to manage the expansion state of all log entries.
    - Clearly flags potential **PII (Personally Identifiable Information)** and **PCI (Payment Card Industry)** risks based on backend analysis (which uses comprehensive, **now server-configurable**, keyword patterns).
    - Allows users to filter logs by PII risk, PCI risk, **log level (via a dropdown menu)**, and search terms within class names or messages.
    - Provides an option to download the filtered list of logs as a PDF report.

- View OpenAPI/Swagger UI if an OpenAPI specification is generated by the backend.
- Display Gherkin feature files if present in the analyzed project.
- View parse warnings if some files encounter issues during backend analysis.

- **Comprehensive PII/PCI Scan Page (`PiiPciScanPage.js`):**
    - Displays a filterable table of all potential PII/PCI findings from the analyzed repository (beyond just logs).
    - Shows file path, line number, column number, finding type (e.g., `PII_EMAIL`, `PCI_VISA`), and a preview of the matched text.
    - Each finding is expandable to show the full matched text context.
    - Includes "Expand All" and "Collapse All" buttons.
    - Allows searching by file path, finding type, or matched text.
    - Leverages backend analysis which uses configurable regex patterns from the backend's `application.yml`.

## Enterprise Support

While the frontend itself doesn't have direct enterprise integration features, it works seamlessly with the backend's enterprise capabilities:

- **Private Repository Analysis:** The UI allows analyzing private Git repositories through the backend's authentication system.

- **Secure Communication:** All API calls to the backend respect the server's SSL/TLS configuration.

- **Diagram Generation:** The frontend displays diagrams generated by the backend's OS-aware PlantUML/Graphviz integration.

- **Cross-Platform Compatibility:** The application has been tested on Windows, macOS, and Linux environments.

## Further Enhancements (TODO - Frontend Specific)

- More detailed display for SOAP services (WSDL/XSD views are present, but could be more interactive).
- Enhanced error handling and user feedback.
- More sophisticated UI for complex metadata (e.g., filtering, searching, pagination for large datasets).
- Options to trigger re-analysis or clear results.
- Dark mode theme.
- **`ApiSpecsPage.js`**: (WSDL/XSD rendering) Future enhancement - if `typeAttr` refers to a global `complexType`, expand it (acknowledged as complex, detailed in spec).
- **Enhancement: Add UI controls for enterprise backend configuration** such as Git credentials, Maven settings, and truststore settings.

## Run Locally
```bash
npm install
npm start
```

## Uses
- React (JavaScript, not primarily TypeScript for components)
- Material UI (primary styling and component library)
- Axios for backend integration

## Recent Updates (Reflecting Backend & Frontend Sync)

- **Enterprise Readiness Improvements:**
    - The backend now includes comprehensive support for enterprise environments, including:
        - Private Git repository authentication
        - Custom Maven settings.xml support
        - SSL/TLS truststore integration
        - OS-aware executable path handling
    - The frontend UI seamlessly works with these enterprise backend features

- **UI Framework Standardization & Responsiveness:**
    - The frontend has been standardized to primarily use Material UI for components, styling, and layout.
    - Significant improvements to responsiveness, ensuring the main content area utilizes available screen space effectively, especially on wider screens and when zoomed out.

- **Major Backend Overhaul for Accuracy:**
    - Implemented robust Java symbol resolution, significantly improving method call detection, type resolution, and overall code understanding.
      - Backend Maven commands for parsing now utilize a configured `settings.xml` and apply the project's truststore settings, ensuring correct behavior in secured environments.
    - Enhanced `CallFlowAnalyzer` to produce more accurate sequence diagrams and raw call steps.
    - Refined class type determination and DAO analysis.

- **Frontend Diagram & Call Flow Improvements:**
    - **`CallFlowPage.js` now features significantly improved display names for sequence diagrams and call flow details. The `generateFlowDisplayName` function robustly parses full Java method signatures, including parameter types and names, to create human-readable titles, resolving previous issues with long or unparsed names.**
    - `FlowExplorer.tsx` component integrated into `CallFlowPage.js` for better navigation of call steps (expand/collapse functionality).
    - Ensured all diagram types (Class, Component, Usecase, Sequence, ERD, DB Schema) are rendered correctly.
    - Spring Boot version display on the Overview page is accurate.

- **Frontend Diagram & Call Flow UI Enhancements:**
    - **Call Flow Display (in `FlowExplorer.tsx`):**
        - Individual call steps are now presented as a clean, numbered plaintext list.
        - Method parameters are removed from the displayed steps for brevity (e.g., `myMethod(int arg)` becomes `myMethod()`).
        - A "Copy Trace" button allows users to copy the formatted plaintext trace to their clipboard.
    - **`CallFlowPage.js` continues to use `generateFlowDisplayName` for human-readable titles for the overall flow and associated sequence diagrams.**
    - `FlowExplorer.tsx` (used within `CallFlowPage.js`) provides expand/collapse functionality for each flow.
    - Ensured all diagram types (Class, Component, Usecase, Sequence, ERD, DB Schema) are rendered correctly.
    - Spring Boot version display on the Overview page is accurate.

- **General Stability:**
    - Addressed various bugs related to diagram loading, data passing, and backend parsing errors.
    - JavaParser backend upgraded for Java 7-21 compatibility.
    - Improved logging in the backend.

- **Frontend Database Page Enhancements:**
    - **Consumes the new `dbAnalysis` object from the backend.**
    - **Renders an entity-centric view and a detailed table of database operations per DAO/Repository.**

- **Logger Insights Page:**
    - UI is implemented to display PII/PCI risks separately.
    - Filtering (including by level dropdown, PII/PCI toggles, search), PDF export, and Expand All/Collapse All functionality are in place.

- **General Stability & Display Improvements:**
    - Call Flow page correctly displays sequence diagrams and raw steps with improved naming.
    - All diagram types are rendered correctly.
    - Accurate Spring Boot version display.
    - Addressed various bugs in data passing and display.
    - Backend compilation issues related to CXF JAX-RS and Swagger resolved, ensuring more stable data for frontend consumption. 