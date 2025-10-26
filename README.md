# CodeDocGen - Enterprise Java Code Documentation Generator

CodeDocGen is a full-stack application designed to analyze Java codebases, generate comprehensive documentation, and produce visual representations of the system architecture. It is built to handle enterprise-grade projects with robust security features.

## Features

- **Advanced Code Analysis**: Parse Java source code using JavaParser and JavaSymbolSolver for accurate symbol resolution.
- **Visualization**: Generate class diagrams, sequence diagrams, ER diagrams, and more using PlantUML.
- **REST API Documentation**: Extract and document REST endpoints and generate OpenAPI specifications.
- **SOAP/WSDL Documentation**: Parse and document SOAP web services.
- **Database Analysis**: Analyze DAO/Repository classes to understand database operations.
- **Security Analysis**: Detect PII/PCI data in source code and logs using configurable patterns.
- **Modern UI**: Clean, responsive interface built with React and Material UI.
- **Multi-Module Maven Support**: Analyze complex, multi-module Maven projects.

## Enterprise-Ready Features

CodeDocGen has been enhanced with the following enterprise-specific features:

1. **Secure Git Integration**: 
   - Support for private repositories with username/password authentication
   - Configurable credentials via environment variables

2. **Custom Maven Integration**:
   - Support for enterprise `settings.xml` files (via `MAVEN_SETTINGS_PATH`)
   - Support for custom Maven executable paths (via `MAVEN_EXECUTABLE_PATH`)
   - OS-aware handling of Maven executable (`mvn` vs. `mvn.cmd`)
   - Enhanced support for multi-module Maven projects, automatically identifying and parsing all sub-modules.
   - Optimized dependency resolution for faster analysis.

3. **SSL/TLS Trust Configuration**:
   - Robust handling of enterprise truststore.jks for HTTPS operations
   - Support for classpath and filesystem-based truststore files
   - Truststore validation and early initialization

4. **OS-Aware Path Management**:
   - Enhanced path handling for executables across different operating systems
   - Support for Windows, macOS, and Linux
   - Automatic detection and validation of required executables (Maven, Git, Graphviz dot)

5. **Configurable Diagram Generation**:
   - OS-aware executable path handling for Graphviz dot (via `GRAPHVIZ_DOT_PATH`)
   - Configurable path via application properties
   - Automatic fallback for Windows installations

6. **Robust PII/PCI Detection**:
   - Flexible PII/PCI pattern definition via `application.yml` using a type-safe configuration properties class (`PiiPciProperties.java`).
   - Clear logging of loaded patterns during application startup.

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
      path: ${MAVEN_EXECUTABLE_PATH:mvn} # Defaults to 'mvn' (or 'mvn.cmd' on Windows) assuming it's on PATH
  graphviz:
    dot:
      executable:
        path: ${GRAPHVIZ_DOT_PATH:dot} # Defaults to 'dot' (or 'dot.exe' on Windows) assuming it's on PATH
  ssl:
    trust-store-password: ${SSL_TRUST_STORE_PASSWORD:changeit} # Default truststore password
  # PII and PCI patterns are configured directly in application.yml under app.pii.patterns and app.pci.patterns
  # Example:
  # pii:
  #   patterns:
  #     ALL_PII: "\b(ssn|socialsecurity|...)\b"
  # pci:
  #   patterns:
  #     ALL_PCI: "\b(creditcard|cardnum|...)\b"

# For PII/PCI pattern examples, refer to the PiiPciProperties.java class or the default application.yml.
# The keys under app.pii.patterns and app.pci.patterns (e.g., ALL_PII) are arbitrary identifiers
# used in PiiPciDetectionServiceImpl. The values are the regex patterns.

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

## Vercel Deployment (Frontend)

Vercel can host the React UI while the Spring Boot backend runs elsewhere (e.g., your own server, Render, Fly.io, etc.).

1. Commit the included `vercel.json` so Vercel knows to build the app from `codedocgen-frontend` and publish the static `build` output.
2. In the Vercel dashboard, create a new project from this repository. The default settings picked up from `vercel.json` (install → `npm install`, build → `npm run build`, output → `codedocgen-frontend/build`) work as-is.
3. Under **Settings → Environment Variables**, set at least:
   - `REACT_APP_API_URL` to the externally accessible backend URL ending in `/api` (for example, `https://your-backend.example.com/api`).
   - Optional: `REACT_APP_PUBLISH_API_URL` if you run the optional publishing service on a different host; otherwise it falls back to `REACT_APP_API_URL`.
4. Trigger a deployment. Vercel serves the compiled SPA and automatically rewrites unknown routes back to `index.html`, so client-side routing continues to work.

> Note: Vercel does not run the Java backend. Make sure your backend is reachable from the deployed frontend, and that CORS is configured accordingly.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [JavaParser](https://javaparser.org/) for Java code parsing
- [PlantUML](https://plantuml.com/) for diagram generation
- [Spring Boot](https://spring.io/projects/spring-boot) for backend framework
- [React](https://reactjs.org/) for frontend framework
- [Material UI](https://material-ui.com/) for UI components 
