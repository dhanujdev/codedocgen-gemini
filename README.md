# CodeDocGen - Enterprise Java Code Documentation Generator

CodeDocGen is a full-stack application designed to analyze Java codebases, generate comprehensive documentation, and produce visual representations of the system architecture. It has been enhanced with enterprise readiness features for integration with corporate environments.

## Features

- **Advanced Code Analysis**: Parse Java source code using JavaParser and JavaSymbolSolver for accurate symbol resolution
- **Visualization**: Generate class diagrams, sequence diagrams, ER diagrams using PlantUML
- **REST API Documentation**: Extract and document REST endpoints and generate OpenAPI specifications
- **SOAP/WSDL Documentation**: Parse and document SOAP web services
- **Database Analysis**: Analyze DAO/Repository classes to understand database operations
- **Multi-Module Support**: Analyze complex multi-module Maven projects

## Enterprise Features

- **Secure Git Integration**: Support for authentication with private enterprise Git repositories
- **Custom Maven Settings**: Support for enterprise Maven settings.xml and repositories
- **Truststore Integration**: Properly load and apply truststore for secure HTTPS operations
- **OS-Specific Path Handling**: Support for Windows and Unix-based systems with configurable executable paths
- **Test Exclusion**: Option to exclude test directories from analysis for performance optimization

## Project Structure

- **codedocgen-backend**: Spring Boot backend service for code analysis and diagram generation
- **codedocgen-frontend**: React frontend for visualization and navigation
- **spec**: Project specifications and documentation

## Getting Started

See the README files in the respective directories for setup instructions:

- [Backend Setup](codedocgen-backend/README.md)
- [Frontend Setup](codedocgen-frontend/README.md)

For deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

For detailed enterprise features specification, see [EnterpriseFeatures.md](spec/EnterpriseFeatures.md).

## Enterprise Configuration

Enterprise features can be configured via application properties or environment variables:

```yaml
app:
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

## Requirements

- Java 17 or higher
- Node.js 18 or higher
- Maven 3.8 or higher
- GraphViz (for diagram generation) 