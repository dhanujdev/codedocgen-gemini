# CodeDocGen - Enterprise Java Code Documentation Generator

CodeDocGen is a full-stack application designed to analyze Java codebases, generate comprehensive documentation, and produce visual representations of the system architecture. It is built to handle enterprise-grade projects with robust security features.

## Features

- **Advanced Code Analysis**: Parse Java source code using JavaParser and JavaSymbolSolver for accurate symbol resolution.
- **Visualization**: Generate class diagrams, sequence diagrams, ER diagrams, and more using PlantUML.
- **REST API Documentation**: Extract and document REST endpoints and generate OpenAPI specifications.
- **SOAP/WSDL Documentation**: Parse and document SOAP web services.
- **Database Analysis**: Analyze DAO/Repository classes to understand database operations.
- **Security Analysis**: Detect PII/PCI data in source code and logs.
- **Modern UI**: Clean, responsive interface built with React and Material UI.

## Enterprise-Ready Features

CodeDocGen has been enhanced with the following enterprise-specific features:

1. **Secure Git Integration**: 
   - Support for private repositories with username/password authentication
   - Configurable credentials via environment variables

2. **Custom Maven Integration**:
   - Support for enterprise Maven settings.xml files 
   - Works with both filesystem and classpath resources
   - Secure handling of sensitive information in logs

3. **SSL/TLS Trust Configuration**:
   - Robust handling of enterprise truststore.jks for HTTPS operations
   - Support for classpath and filesystem-based truststore files
   - Truststore validation and early initialization

4. **OS-Aware Path Management**:
   - Enhanced path handling for executables across different operating systems
   - Support for Windows, macOS, and Linux
   - Automatic detection and validation of required executables

5. **Configurable Diagram Generation**:
   - OS-aware executable path handling for Graphviz dot
   - Configurable path via application properties
   - Automatic fallback for Windows installations

## Project Structure

The project consists of two main components:

- **codedocgen-backend**: Spring Boot Java application that performs code analysis and provides REST APIs
- **codedocgen-frontend**: React application that provides a user interface for the system

## Getting Started

For detailed setup and deployment instructions, please refer to the following documents:

- [Backend Documentation](codedocgen-backend/README.md)
- [Deployment Guide](DEPLOYMENT.md)

## Configuration

Enterprise deployments can be configured via `application.yml` with the following properties:

```yaml
app:
  git:
    username: ${GIT_USERNAME:} # Allow override via env var, empty by default
    password: ${GIT_PASSWORD:} # Allow override via env var, empty by default
  maven:
    settings:
      path: ${MAVEN_SETTINGS_PATH:} # e.g., /path/to/enterprise/settings.xml or classpath:enterprise-settings.xml
    executable:
      path: ${MAVEN_EXECUTABLE_PATH:mvn} # Defaults to 'mvn' assuming it's on PATH
  graphviz:
    dot:
      executable:
        path: ${GRAPHVIZ_DOT_PATH:dot} # Defaults to 'dot' assuming it's on PATH
  ssl:
    trust-store-password: ${SSL_TRUST_STORE_PASSWORD:changeit} # Default truststore password
```

Additionally, you can configure the server's SSL trust store:

```yaml
server:
  ssl:
    trust-store: classpath:truststore.jks # or file:/path/to/truststore.jks
    trust-store-password: ${SERVER_TRUST_STORE_PASSWORD:changeit}
```

## Docker Deployment

The application can be built and deployed as a Docker container. See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions, including enterprise security considerations.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [JavaParser](https://javaparser.org/) for Java code parsing
- [PlantUML](https://plantuml.com/) for diagram generation
- [Spring Boot](https://spring.io/projects/spring-boot) for backend framework
- [React](https://reactjs.org/) for frontend framework
- [Material UI](https://material-ui.com/) for UI components 