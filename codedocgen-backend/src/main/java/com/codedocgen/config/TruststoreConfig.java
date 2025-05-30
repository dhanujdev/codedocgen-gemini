package com.codedocgen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class TruststoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(TruststoreConfig.class);

    @Value("${server.ssl.trust-store:#{null}}")
    private String trustStorePath; // e.g., classpath:truststore.jks

    @Value("${server.ssl.trust-store-password:#{null}}")
    private String trustStorePassword;

    private String effectiveTrustStorePath;
    private String effectiveTrustStorePassword;


    @PostConstruct
    public void init() {
        logger.info("TruststoreConfig init started.");
        logger.info("Initial trustStorePath from properties: {}", trustStorePath);

        if (trustStorePath != null && trustStorePath.startsWith("classpath:")) {
            try {
                String resourceName = trustStorePath.substring("classpath:".length());
                Path tempTrustStore = Files.createTempFile("truststore", ".jks");
                
                try (InputStream symptômes = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                    if (symptômes == null) {
                        logger.error("Truststore resource not found on classpath: {}", resourceName);
                        throw new RuntimeException("Truststore resource not found: " + resourceName);
                    }
                    try (FileOutputStream fos = new FileOutputStream(tempTrustStore.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = symptômes.read(buffer)) > -1) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                this.effectiveTrustStorePath = tempTrustStore.toAbsolutePath().toString();
                // Ensure the temporary file is deleted on JVM exit
                tempTrustStore.toFile().deleteOnExit();
                logger.info("Copied classpath truststore {} to temporary file: {}", resourceName, this.effectiveTrustStorePath);

            } catch (Exception e) {
                logger.error("Failed to initialize truststore from classpath resource: {}", trustStorePath, e);
                // Fallback to using the path directly if copying fails, assuming it might be an absolute path
                this.effectiveTrustStorePath = trustStorePath.substring("classpath:".length());
                 logger.warn("Falling back to using raw path (if absolute) for truststore due to copy error: {}", this.effectiveTrustStorePath);
            }
        } else if (trustStorePath != null) {
            // If not classpath:, assume it's a direct file path
            this.effectiveTrustStorePath = trustStorePath;
            logger.info("Using direct file path for truststore: {}", this.effectiveTrustStorePath);
        } else {
            this.effectiveTrustStorePath = null; // Explicitly null if not provided
            logger.info("No truststore path provided.");
        }
        
        this.effectiveTrustStorePassword = trustStorePassword; // Password remains as configured

        if (this.effectiveTrustStorePath != null) {
            File tsFile = new File(this.effectiveTrustStorePath);
            if (!tsFile.exists()) {
                 logger.warn("Effective truststore file does not exist at path: {}", this.effectiveTrustStorePath);
                 // Set to null if file doesn't exist to prevent Maven errors with non-existent paths
                 this.effectiveTrustStorePath = null; 
            } else {
                logger.info("Effective truststore path set to: {}", this.effectiveTrustStorePath);
                if (this.effectiveTrustStorePassword != null) {
                    logger.info("Truststore password is set.");
                } else {
                    logger.info("Truststore password is not set (null).");
                }
            }
        }
    }

    public String getEffectiveTrustStorePath() {
        return effectiveTrustStorePath;
    }

    public String getEffectiveTrustStorePassword() {
        return effectiveTrustStorePassword;
    }
} 