# Deployment Instructions for CodeDocGen

This document provides step-by-step instructions to build and deploy the CodeDocGen application using Docker.

## 1. Prerequisites

Before you begin, ensure you have the following installed on your deployment environment:

*   **Docker:** The application is containerized using Docker. Installation instructions can be found on the [official Docker website](https://docs.docker.com/engine/install/).
*   **Git:** (Optional, for cloning the repository if not already present) If you need to clone the project repository first.
*   **Internet Access:** Required for pulling base Docker images and downloading dependencies during the build process.

## 2. Project Structure Overview

The project consists of two main parts:

*   `codedocgen-frontend`: A React application.
*   `codedocgen-backend`: A Java Spring Boot application.

The `Dockerfile` in the project root is a multi-stage Dockerfile that builds both the frontend and backend, then bundles them into a single runnable image where the Spring Boot backend serves the compiled frontend and provides the API.

## 3. Building the Docker Image

1.  **Clone the Repository (if needed):**
    If you haven't cloned the project repository yet, do so:
    ```bash
    git clone <your-repository-url>
    cd codedocgen-gemini 
    ```
    Replace `<your-repository-url>` with the actual URL of your Git repository.

2.  **Navigate to the Project Root:**
    Ensure your terminal is in the root directory of the `codedocgen-gemini` project (where the `Dockerfile` is located).

3.  **Build the Docker Image:**
    Execute the following command to build the Docker image. This command tags the image as `codedocgen-app`.
    ```bash
    docker build -t codedocgen-app .
    ```
    *   `-t codedocgen-app`: Tags the image with the name `codedocgen-app` and default tag `latest`.
    *   `.`: Specifies that the build context is the current directory.

    This process might take some time, especially on the first build, as it needs to download base images and all project dependencies for both frontend and backend.

4.  **Verify the Image (Optional):**
    After the build completes, you can list your Docker images to verify that `codedocgen-app` was created:
    ```bash
    docker images
    ```
    You should see `codedocgen-app` in the list.

## 4. Running the Application using Docker

1.  **Run the Docker Container:**
    Once the image is built, you can run it as a container using the following command:
    ```bash
    docker run -d -p 8080:8080 --name codedocgen-instance codedocgen-app
    ```
    *   `-d`: Runs the container in detached mode (in the background).
    *   `-p 8080:8080`: Maps port `8080` on the host machine to port `8080` inside the container (which is where the Spring Boot application listens, as defined by `ENV PORT=8080` in the Dockerfile).
    *   `--name codedocgen-instance`: Assigns a name to the running container for easier management.
    *   `codedocgen-app`: The name of the image to run.

2.  **Accessing the Application:**
    Open your web browser and navigate to `http://localhost:8080` (or `http://<your-server-ip>:8080` if deploying on a remote server).
    You should see the CodeDocGen frontend.

3.  **Viewing Container Logs (Optional):**
    If you need to check the application logs (e.g., Spring Boot startup messages or runtime errors), you can view the logs of the running container:
    ```bash
    docker logs codedocgen-instance
    ```
    To follow the logs in real-time:
    ```bash
    docker logs -f codedocgen-instance
    ```

4.  **Stopping the Container:**
    To stop the running container:
    ```bash
    docker stop codedocgen-instance
    ```

5.  **Removing the Container (Optional):**
    If you want to remove the stopped container (e.g., before running a new instance with the same name):
    ```bash
    docker rm codedocgen-instance
    ```

## 5. Configuration

### Environment Variables

The Dockerfile sets the following default environment variables for the runtime stage:

*   `PORT=8080`: The port the Spring Boot application will listen on inside the container.
*   `SPRING_PROFILES_ACTIVE=production`: Sets the active Spring profile to `production`.
*   `GRAPHVIZ_DOT=/usr/bin/dot`: Specifies the path to the Graphviz `dot` executable for PlantUML.

You can override these (except `GRAPHVIZ_DOT` which is tied to the installation path) or add other Spring Boot properties when running the `docker run` command using the `-e` or `--env` flag. For example, to change the port and add a custom Spring Boot property:

```bash
docker run -d -p 8081:8081 --name codedocgen-instance \
  -e PORT=8081 \
  -e SPRING_APPLICATION_JSON='{"custom.property":"value"}' \
  codedocgen-app
```
(Note: `SPRING_APPLICATION_JSON` is a convenient way to pass multiple Spring Boot properties as a JSON string.)

### Enterprise Configuration

For enterprise deployments, you may need to configure the following additional environment variables:

```bash
docker run -d -p 8080:8080 --name codedocgen-instance \
  -e GIT_USERNAME='your-git-username' \
  -e GIT_PASSWORD='your-git-password' \
  -e MAVEN_SETTINGS_PATH='/path/to/settings.xml' \
  -e MAVEN_EXECUTABLE_PATH='/path/to/mvn' \
  -e GRAPHVIZ_DOT_PATH='/path/to/dot' \
  -e SSL_TRUST_STORE_PASSWORD='your-truststore-password' \
  -v /path/to/host/truststore.jks:/app/truststore.jks \
  -v /path/to/host/settings.xml:/app/settings.xml \
  codedocgen-app
```

You would also need to update the Spring Boot configuration to use these mounted files:

```bash
-e SPRING_APPLICATION_JSON='{"server.ssl.trust-store":"/app/truststore.jks","app.maven.settings.path":"/app/settings.xml"}'
```

### Application Properties

Spring Boot properties can be configured through `application-production.yml` (or `.properties`) if you include such a file in `codedocgen-backend/src/main/resources/` before building the Docker image. The `SPRING_PROFILES_ACTIVE=production` environment variable will ensure this profile-specific configuration is loaded.

Key properties you might want to manage:

*   `app.repoStoragePath`: Path where cloned repositories are temporarily stored during analysis.
*   `app.outputBasePath`: Path where generated diagrams and documents are stored if they need to be persisted or accessed outside the application flow (though in the current Docker setup, these are mostly transient within the container unless volumes are used).

## 6. Persistent Storage (Optional)

The current Docker setup does **not** configure persistent storage for:

*   Cloned repositories (`app.repoStoragePath`).
*   Generated output files (`app.outputBasePath`) if these are intended to be stored long-term outside the analysis lifecycle.

If you require persistence for these (e.g., for auditing, caching, or serving generated files independently), you should configure Docker volumes. This involves modifying the `docker run` command to mount host directories or named volumes into the container at the paths specified by `app.repoStoragePath` and `app.outputBasePath`.

Example using host-mounted volumes:
```bash
docker run -d -p 8080:8080 --name codedocgen-instance \
  -v /path/on/host/for/repos:/configured/repoStoragePath \
  -v /path/on/host/for/output:/configured/outputBasePath \
  codedocgen-app
```
Replace `/configured/repoStoragePath` and `/configured/outputBasePath` with the actual paths your Spring Boot application is configured to use (from `application-production.yml` or defaults).

## 7. Enterprise Security Considerations

When deploying in an enterprise environment, consider the following:

### SSL/TLS Configuration

For secure HTTPS operations (especially when interacting with enterprise Git repositories), configure a truststore:

1. **Create or obtain a truststore file** (typically `truststore.jks`) containing your organization's certificates.
2. **Mount the truststore** into the container as shown in the enterprise configuration above.
3. **Set the appropriate system properties** through environment variables:
   - `server.ssl.trust-store=/path/to/mounted/truststore.jks`
   - `app.ssl.trust-store-password=your-password`

### Git Authentication

For private repository access:

1. **Securely provide Git credentials** via environment variables `GIT_USERNAME` and `GIT_PASSWORD`.
2. **Consider using Docker secrets** for more secure credential management.
3. **For GitHub Enterprise or similar** ensure your truststore contains their certificate chain.

### Maven Configuration

For enterprise Maven repositories:

1. **Prepare a custom settings.xml** with your enterprise repository configuration.
2. **Mount the settings.xml** into the container.
3. **Configure the application** to use it via `app.maven.settings.path`.

## 8. Troubleshooting

*   **Build Failures:**
    *   Check for errors in the Docker build output. Common issues include network problems preventing dependency downloads, incorrect file paths, or syntax errors in the `Dockerfile`.
    *   Ensure the Maven build (`mvn package`) and Node build (`npm run build`) run successfully locally before attempting a Docker build.
*   **Container Not Starting:**
    *   Use `docker logs codedocgen-instance` to check for application startup errors (e.g., Java exceptions, Spring Boot configuration issues).
    *   Ensure that the port you are trying to map (e.g., `8080`) is not already in use on your host machine.
*   **Diagram Generation Issues:**
    *   If diagrams are not generating correctly at runtime, it might be related to the Graphviz installation or `GRAPHVIZ_DOT` path. The current `Dockerfile` attempts to handle this by installing Graphviz in the final image.
    *   For enterprise deployments, ensure the configured `app.graphviz.dot.executable.path` is correct for your environment.
*   **SSL/Trust Issues:**
    *   If you encounter SSL validation errors when connecting to enterprise repositories, check that your truststore is correctly configured and mounted.
    *   Verify the system properties are correctly set via environment variables.
*   **Maven Repository Access:**
    *   If you experience issues accessing enterprise Maven repositories, verify your `settings.xml` configuration and ensure network connectivity to the repository.

## 9. Updating the Application

To deploy an updated version of the application:

1.  **Pull the latest code changes** (if applicable) from your Git repository.
2.  **Rebuild the Docker image** using the `docker build -t codedocgen-app .` command. Docker will cache unchanged layers, so subsequent builds are often faster.
3.  **Stop and remove the old container** (if it's running):
    ```bash
    docker stop codedocgen-instance
    docker rm codedocgen-instance
    ```
4.  **Run a new container** from the updated image using the `docker run` command described in Section 4.

This completes the deployment instructions. Adjust paths and commands as per your specific environment and requirements. 