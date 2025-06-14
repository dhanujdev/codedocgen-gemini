package com.codedocgen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.outputBasePath:/tmp/codedocgen_output}")
    private String outputBasePath;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Apply to all endpoints
                        .allowedOrigins("*") // Allow all origins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow common methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(false); // Allow credentials if needed, false for wildcard origin
            }
        };
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure path ends with a slash if it's a directory
        String resolvedOutputBasePath = new java.io.File(outputBasePath).toURI().toString();
        if (!resolvedOutputBasePath.endsWith("/")) {
            resolvedOutputBasePath += "/";
        }
        
        registry.addResourceHandler("/generated-output/**") // URL path to access the files
                .addResourceLocations(resolvedOutputBasePath); // Physical path on the server
        
        // Handler for serving diagrams directly if a simpler structure is preferred in future
        // registry.addResourceHandler("/diagrams/**")
        //         .addResourceLocations(resolvedOutputBasePath + "diagrams/"); // Assuming diagrams are in outputBasePath/diagrams
    }
} 