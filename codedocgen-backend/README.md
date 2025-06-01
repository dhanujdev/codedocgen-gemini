# CodeDocGen Backend

This is the backend service for the Code Documentation Generator. It is a Spring Boot application responsible for cloning Git repositories, parsing source code, analyzing various aspects of the project, generating diagrams, and providing a REST API for the frontend.

## Core Features

- **Git Repository Cloning:** 
  - Clones Git repositories for analysis
  - **Supports private enterprise repositories with authentication**
  - Configurable username/password via properties
  - Progress monitoring for large repositories

- **Project Detection:**
  - Detects project type (e.g., Maven, Gradle)
  - Identifies if a project uses Spring Boot and extracts its version
  - **Supports multi-module Maven projects**

- **Advanced Java Parsing:**
  - Parses Java source code using JavaParser and JavaSymbolSolver
  - Extracts class metadata, methods, fields, annotations
  - Resolves fully qualified names, method signatures, and dependencies
  - **Enhanced symbol resolution for complex projects**
  - **Option to exclude test directories**

- **Diagram Generation:**
  - Generates PlantUML diagrams (class diagrams, sequence diagrams, etc.)
  - Renders diagrams to SVG using configured Graphviz dot executable
  - **Configurable dot executable path for cross-platform compatibility**

- **REST API Documentation:**
  - Extracts REST endpoints from Spring MVC and JAX-RS annotations
  - Generates OpenAPI specifications

- **SOAP API Documentation:**
  - Extracts SOAP endpoints from JAX-WS annotations
  - Parses WSDL files

- **Database Analysis:**
  - Analyzes Spring Data JPA repositories and Hibernate entities
  - Extracts SQL from native queries and MyBatis mappers

## Enterprise Features

- **Secure Git Integration:**
  - Authentication for private enterprise repositories
  - Configurable via application properties or environment variables

- **Custom Maven Settings:**
  - Support for enterprise Maven settings.xml
  - Configurable Maven executable path

- **Truststore Integration:**
  - Proper loading and application of truststore for secure HTTPS operations
  - Configurable truststore path and password

- **OS-Specific Path Handling:**
  - Support for Windows and Unix-based systems
  - Configurable executable paths

- **Multi-Module Project Support:**
  - Recursive analysis of multi-module Maven projects
  - Enhanced symbol resolution across modules

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- GraphViz (for diagram generation)

### Building the Application

```bash
cd codedocgen-backend
mvn clean package
```

### Running the Application

```bash
java -jar target/codedocgen-backend.jar
```

### Configuration

The application can be configured via `application.yml` or environment variables:

```yaml
app:
  repoStoragePath: /tmp/repos
  outputBasePath: ./output
  docsStoragePath: ${app.outputBasePath}/docs
  diagramsStoragePath: ${app.outputBasePath}/diagrams
  git:
    username: ${GIT_USERNAME:}
    password: ${GIT_PASSWORD:}
  maven:
    settings:
      path: ${MAVEN_SETTINGS_PATH:}
    executable:
      path: ${MAVEN_EXECUTABLE_PATH:mvn}
  graphviz:
    dot:
      executable:
        path: ${GRAPHVIZ_DOT_PATH:dot}
  truststore:
    path: ${TRUSTSTORE_PATH:classpath:truststore.jks}
    password: ${TRUSTSTORE_PASSWORD:changeit}
```

## API Endpoints

- `POST /api/repos/clone`: Clone a Git repository
- `GET /api/repos/{repoId}`: Get repository information
- `GET /api/repos/{repoId}/classes`: Get class metadata
- `GET /api/repos/{repoId}/diagrams`: Get generated diagrams
- `GET /api/repos/{repoId}/endpoints`: Get REST endpoints
- `GET /api/repos/{repoId}/soap`: Get SOAP endpoints
- `GET /api/repos/{repoId}/db`: Get database metadata

## Enterprise Deployment Considerations

See [DEPLOYMENT.md](../DEPLOYMENT.md) for detailed deployment instructions, including:

- Secure Git integration
- Custom Maven settings configuration
- Truststore configuration
- OS-specific path configuration
- Docker deployment 