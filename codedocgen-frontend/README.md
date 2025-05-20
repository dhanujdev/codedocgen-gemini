# CodeDocGen Frontend

This project is the frontend for the Code Documentation Generator, built with React and Material UI.

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
4.  **Environment Variables (Optional but Recommended):**
    Create a `.env` file in the `codedocgen-frontend` root directory.
    Add the following line, adjusting the URL if your backend runs elsewhere:
    ```
    REACT_APP_API_URL=http://localhost:8080/api
    ```
    If this file is not present, the app will default to `http://localhost:8080/api`.

## Available Scripts

In the project directory, you can run:

### `npm start` or `yarn start`

Runs the app in development mode.
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.
You may also see any lint errors in the console.

The backend server (codedocgen-backend) must be running for the API calls to work.

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

- Input a Git repository URL.
- View a summary of the analyzed project.
- Browse class and interface metadata.
- View extracted API endpoints.
- Display generated PlantUML diagrams (e.g., class diagrams) with zoom/pan capabilities.
- View OpenAPI/Swagger UI for Spring Boot projects.
- Display Gherkin feature files.

## Further Enhancements (TODO)

- Visualization for call flows (sequence diagrams).
- More detailed display for SOAP services.
- Enhanced error handling and user feedback.
- More sophisticated UI for displaying complex metadata.
- Options to trigger re-analysis or clear results.
- Dark mode theme.

## Run Locally
```bash
npm install
npm start
```

## Uses
- React + TypeScript
- Tailwind CSS + shadcn/ui
- Axios for backend integration 

## Recent Updates
- SVG diagrams are now supported and can be opened in a zoomable, fit-to-screen viewer (svg-viewer.html).
- Backend now supports CORS for static files, so SVGs and other diagrams can be fetched directly by the frontend and viewer.
- If any files fail to parse, a warning is shown in the UI listing the affected files.
- JavaParser backend upgraded for Java 7-21 compatibility. 