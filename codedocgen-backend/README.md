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

## Recent Updates
- Diagrams are now generated and served as SVG for best quality and Confluence publishing.
- CORS is enabled for `/generated-output/**` so the frontend and SVG viewer can fetch diagrams directly from the backend.
- JavaParser upgraded to 3.26.4 for robust Java 7-21 compatibility.
- If any files fail to parse, a warning is shown in the UI listing the affected files.
- A new `svg-viewer.html` (in the frontend public folder) allows zoomable, fit-to-screen SVG viewing in a new browser tab. 