package com.codedocgen.service.impl;

import com.codedocgen.service.ProjectDetectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ProjectDetectorServiceImpl implements ProjectDetectorService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDetectorServiceImpl.class);

    @Override
    public String detectBuildTool(File projectDir) {
        if (new File(projectDir, "pom.xml").exists()) {
            logger.info("Detected Maven project in {}", projectDir.getAbsolutePath());
            return "Maven";
        }
        if (new File(projectDir, "build.gradle").exists() || new File(projectDir, "build.gradle.kts").exists()) {
            logger.info("Detected Gradle project in {}", projectDir.getAbsolutePath());
            return "Gradle";
        }
        logger.info("No known build tool (Maven/Gradle) detected in {}. Assuming Raw Source/Unknown.", projectDir.getAbsolutePath());
        return "Raw Source/Unknown";
    }

    @Override
    public boolean isSpringBootProject(File projectDir) {
        // Check in pom.xml for Spring Boot starter parent or common dependencies
        File pomFile = new File(projectDir, "pom.xml");
        if (pomFile.exists()) {
            try {
                String content = Files.readString(pomFile.toPath());
                if (content.contains("spring-boot-starter-parent") || content.contains("org.springframework.boot")) {
                    logger.info("Detected Spring Boot project (Maven) in {}", projectDir.getAbsolutePath());
                    return true;
                }
            } catch (IOException e) {
                logger.error("Error reading pom.xml in {}: {}", projectDir.getAbsolutePath(), e.getMessage());
            }
        }

        // Check in build.gradle or build.gradle.kts for Spring Boot plugins or dependencies
        File gradleFile = new File(projectDir, "build.gradle");
        File gradleKtsFile = new File(projectDir, "build.gradle.kts");
        File targetGradleFile = gradleFile.exists() ? gradleFile : (gradleKtsFile.exists() ? gradleKtsFile : null);

        if (targetGradleFile != null) {
            try {
                String content = Files.readString(targetGradleFile.toPath());
                if (content.contains("org.springframework.boot") || content.contains("spring-boot-gradle-plugin")) {
                    logger.info("Detected Spring Boot project (Gradle) in {}", projectDir.getAbsolutePath());
                    return true;
                }
            } catch (IOException e) {
                logger.error("Error reading build.gradle/kts in {}: {}", projectDir.getAbsolutePath(), e.getMessage());
            }
        }
        
        // Fallback: Scan Java files for @SpringBootApplication annotation
        // This is more intensive and should be a secondary check if build files don't give a clear answer.
        // Consider using JavaParserService for this if it's already available and configured.
        try (Stream<Path> stream = Files.walk(projectDir.toPath())) {
            Optional<Path> springBootAppFile = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> {
                    try {
                        return Files.readString(path).contains("@SpringBootApplication");
                    } catch (IOException e) {
                        logger.warn("Could not read file {} during Spring Boot detection: {}", path, e.getMessage());
                        return false;
                    }
                })
                .findFirst();
            if (springBootAppFile.isPresent()) {
                logger.info("Detected Spring Boot project (via @SpringBootApplication annotation) in {}", projectDir.getAbsolutePath());
                return true;
            }
        } catch (IOException e) {
            logger.error("Error walking file tree for Spring Boot detection in {}: {}", projectDir.getAbsolutePath(), e.getMessage());
        }

        logger.info("Spring Boot not detected in {}", projectDir.getAbsolutePath());
        return false;
    }

    @Override
    public String detectSpringBootVersion(File projectDir) {
        // Attempt to find Spring Boot version from pom.xml
        File pomFile = new File(projectDir, "pom.xml");
        if (pomFile.exists()) {
            try {
                String content = Files.readString(pomFile.toPath());
                // Regex for <parent><version>xxx</version></parent> specific to Spring Boot
                Pattern parentVersionPattern = Pattern.compile("<parent>.*?<groupId>org.springframework.boot</groupId>.*?<artifactId>spring-boot-starter-parent</artifactId>.*?<version>(.*?)</version>.*?</parent>", Pattern.DOTALL);
                Matcher parentMatcher = parentVersionPattern.matcher(content);
                if (parentMatcher.find()) {
                    logger.info("Detected Spring Boot version {} (from parent) in {}", parentMatcher.group(1), projectDir.getAbsolutePath());
                    return parentMatcher.group(1);
                }
                // Regex for <spring-boot.version>xxx</spring-boot.version> property
                Pattern propertyVersionPattern = Pattern.compile("<spring-boot.version>(.*?)</spring-boot.version>");
                 Matcher propertyMatcher = propertyVersionPattern.matcher(content);
                if (propertyMatcher.find()) {
                    logger.info("Detected Spring Boot version {} (from property) in {}", propertyMatcher.group(1), projectDir.getAbsolutePath());
                    return propertyMatcher.group(1);
                }
            } catch (IOException e) {
                logger.error("Error reading pom.xml for Spring Boot version detection in {}: {}", projectDir.getAbsolutePath(), e.getMessage());
            }
        }

        // Attempt to find Spring Boot version from build.gradle or build.gradle.kts
        File gradleFile = new File(projectDir, "build.gradle");
        File gradleKtsFile = new File(projectDir, "build.gradle.kts");
        File targetGradleFile = gradleFile.exists() ? gradleFile : (gradleKtsFile.exists() ? gradleKtsFile : null);

        if (targetGradleFile != null) {
            try {
                String content = Files.readString(targetGradleFile.toPath());
                // Regex for id 'org.springframework.boot' version 'x.y.z'
                // Target Java String: "(?:id[\\s\\(]['\\\"]org\\.springframework\\.boot['\\\"]\\)?(?:\\s*version\\s*['\\\"](.*?)['\\\"'])?)
                Pattern pluginVersionPattern = Pattern.compile("(?:id[\\s\\(]['\\\"']org\\.springframework\\.boot['\\\"']\\)?(?:\\s*version\\s*['\\\"'](.*?)['\\\"'])?)", Pattern.CASE_INSENSITIVE);
                Matcher pluginMatcher = pluginVersionPattern.matcher(content);
                if (pluginMatcher.find() && pluginMatcher.group(1) != null) {
                    logger.info("Detected Spring Boot version {} (from Gradle plugin) in {}", pluginMatcher.group(1), projectDir.getAbsolutePath());
                    return pluginMatcher.group(1);
                }

                // Regex for springBootVersion = 'x.y.z' or springBootVersion = "x.y.z"
                // And for ext['springBootVersion'] = 'x.y.z' or ext["springBootVersion"] = "x.y.z"
                // Target Java String: "(?:ext\\s*\\[['\\\"]springBootVersion['\\\"]\\]\\s*=|springBootVersion\\s*=)\\s*['\\\"](.*?)['\\\"]"
                Pattern extVersionPattern = Pattern.compile("(?:ext\\s*\\[['\\\"']springBootVersion['\\\"']\\]\\s*=|springBootVersion\\s*=)\\s*['\\\"'](.*?)['\\\"']");
                Matcher extMatcher = extVersionPattern.matcher(content);
                if (extMatcher.find()) {
                    logger.info("Detected Spring Boot version {} (from Gradle ext property) in {}", extMatcher.group(1), projectDir.getAbsolutePath());
                    return extMatcher.group(1);
                }
                
                // Regex for plugins { id("org.springframework.boot") version "x.y.z" }
                // Target Java String: "id\\(\\\"org\\.springframework\\.boot\\\"\\)\\s*version\\s*\\\"(.*?)\\\""
                Pattern kotlinDslPluginPattern = Pattern.compile("id\\(\\\"org\\.springframework\\.boot\\\"\\)\\s*version\\s*\\\"(.*?)\\\"");
                Matcher kotlinDslMatcher = kotlinDslPluginPattern.matcher(content);
                if (kotlinDslMatcher.find()) {
                    logger.info("Detected Spring Boot version {} (from Gradle Kotlin DSL plugin) in {}", kotlinDslMatcher.group(1), projectDir.getAbsolutePath());
                    return kotlinDslMatcher.group(1);
                }

            } catch (IOException e) {
                logger.error("Error reading build.gradle/kts for Spring Boot version detection in {}: {}", projectDir.getAbsolutePath(), e.getMessage());
            }
        }
        // TODO: Add similar logic for build.gradle/build.gradle.kts if necessary - DONE
        logger.warn("Could not determine Spring Boot version for {}", projectDir.getAbsolutePath());
        return null; // Or a default/unknown string
    }
} 