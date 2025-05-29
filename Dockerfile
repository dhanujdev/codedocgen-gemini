# Stage 1: Build Frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend

# Copy package.json and package-lock.json (or yarn.lock)
COPY codedocgen-frontend/package.json codedocgen-frontend/package-lock.json* ./

# Install dependencies
# If you use yarn, replace with: RUN yarn install --frozen-lockfile
RUN npm install --ci

# Copy the rest of frontend application source code
COPY codedocgen-frontend/ ./ 

# Build the frontend application
# If you use yarn, replace with: RUN yarn build
RUN npm run build

# Stage 2: Build Backend and Bundle Frontend
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app/backend

# Copy pom.xml
COPY codedocgen-backend/pom.xml .

# Download Maven dependencies (this will leverage Docker layer caching)
RUN mvn dependency:go-offline -B

# Copy the backend application source code
COPY codedocgen-backend/src ./src

# Copy built frontend static assets from the frontend-builder stage
# Spring Boot will serve these from src/main/resources/static
COPY --from=frontend-builder /app/frontend/build ./src/main/resources/static

# Package the application (skip tests for faster build)
RUN mvn package -DskipTests -B

# Stage 3: Runtime Environment
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Graphviz for PlantUML dynamic diagram generation
#hadolint ignore=DL3008
RUN apt-get update && \
    apt-get install -y --no-install-recommends graphviz && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user and group
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the JAR file from the backend-builder stage
# Adjust the JAR name if your pom.xml produces a different artifactId or version
COPY --from=backend-builder /app/backend/target/codedocgen-backend-*.jar app.jar

# Change ownership to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Set environment variables (optional, can be overridden at runtime)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=production
ENV GRAPHVIZ_DOT=/usr/bin/dot

# Expose the application port
EXPOSE ${PORT}

# Entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 