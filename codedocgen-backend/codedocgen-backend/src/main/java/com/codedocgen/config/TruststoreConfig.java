package com.codedocgen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

@Configuration
@Order(0) // Ensure this bean is initialized very early in the application startup process
public class TruststoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(TruststoreConfig.class);

    @Value("${server.ssl.trust-store:#{null}}")
    private String trustStorePath; // e.g., classpath:truststore.jks

    @Value("${server.ssl.trust-store-password:#{null}}")
    private String trustStorePassword;
    
    @Value("${app.ssl.trust-store-password:changeit}")
    private String appTrustStorePassword;

    private String effectiveTrustStorePath;
    private String effectiveTrustStorePassword;

    @PostConstruct
    public void init() {
        logger.info("TruststoreConfig init started.");
        logger.info("Initial trustStorePath from properties: {}", trustStorePath);

        // If trustStorePassword is not set, use the app-level one
        if (trustStorePassword == null || trustStorePassword.trim().isEmpty()) {
            effectiveTrustStorePassword = appTrustStorePassword;
            logger.info("Using app-level trust store password because server.ssl.trust-store-password is not set");
        } else {
            effectiveTrustStorePassword = trustStorePassword;
            logger.info("Using server.ssl.trust-store-password");
        }

        if (trustStorePath != null && trustStorePath.startsWith("classpath:")) {
            try {
                String resourceName = trustStorePath.substring("classpath:".length());
                Path tempTrustStore = Files.createTempFile("truststore", ".jks");
                
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                    if (is == null) {
                        logger.error("Truststore resource not found on classpath: {}", resourceName);
                        throw new RuntimeException("Truststore resource not found: " + resourceName);
                    }
                    try (FileOutputStream fos = new FileOutputStream(tempTrustStore.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > -1) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                this.effectiveTrustStorePath = tempTrustStore.toAbsolutePath().toString();
                // Ensure the temporary file is deleted on JVM exit
                tempTrustStore.toFile().deleteOnExit();
                logger.info("Copied classpath truststore {} to temporary file: {}", resourceName, this.effectiveTrustStorePath);

                // Validate the truststore to ensure it's a valid keystore file
                validateTrustStore(this.effectiveTrustStorePath, this.effectiveTrustStorePassword);

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
            
            // Validate the truststore to ensure it's a valid keystore file
            validateTrustStore(this.effectiveTrustStorePath, this.effectiveTrustStorePassword);
        } else {
            this.effectiveTrustStorePath = null; // Explicitly null if not provided
            logger.info("No truststore path provided.");
        }
        
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
                    
                    // Set system properties to apply truststore settings globally
                    System.setProperty("javax.net.ssl.trustStore", this.effectiveTrustStorePath);
                    System.setProperty("javax.net.ssl.trustStorePassword", this.effectiveTrustStorePassword);
                    logger.info("Set system properties for truststore: javax.net.ssl.trustStore={}", this.effectiveTrustStorePath);
                    logger.info("Set system property javax.net.ssl.trustStorePassword (value hidden)");
                    
                    // Additional property for truststore type (JKS is the most common)
                    System.setProperty("javax.net.ssl.trustStoreType", "JKS");
                    logger.info("Set system property javax.net.ssl.trustStoreType=JKS");
                } else {
                    logger.info("Truststore password is not set (null). This may cause issues if the truststore is password-protected.");
                }
            }
        }
        
        logger.info("TruststoreConfig initialization complete.");
    }

    /**
     * Validates that the truststore file exists and can be loaded with the given password.
     * 
     * @param trustStorePath Path to the truststore file
     * @param password Password for the truststore
     */
    private void validateTrustStore(String trustStorePath, String password) {
        if (trustStorePath == null || password == null) {
            return;
        }
        
        File tsFile = new File(trustStorePath);
        if (!tsFile.exists()) {
            logger.warn("Truststore file does not exist at path: {}", trustStorePath);
            return;
        }
        
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream is = Files.newInputStream(tsFile.toPath())) {
                keyStore.load(is, password.toCharArray());
                int certCount = keyStore.size();
                logger.info("Successfully validated truststore at {}. Contains {} certificates.", trustStorePath, certCount);
            }
        } catch (Exception e) {
            logger.error("Failed to validate truststore at {}: {}", trustStorePath, e.getMessage(), e);
        }
    }

    public String getEffectiveTrustStorePath() {
        return effectiveTrustStorePath;
    }

    public String getEffectiveTrustStorePassword() {
        return effectiveTrustStorePassword;
    }
} 