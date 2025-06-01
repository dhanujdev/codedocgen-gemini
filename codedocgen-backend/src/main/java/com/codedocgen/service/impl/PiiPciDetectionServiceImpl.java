package com.codedocgen.service.impl;

import com.codedocgen.config.PiiPciProperties;
import com.codedocgen.model.PiiPciFinding;
import com.codedocgen.service.PiiPciDetectionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class PiiPciDetectionServiceImpl implements PiiPciDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(PiiPciDetectionServiceImpl.class);

    private final PiiPciProperties piiPciProperties;
    private Map<String, String> piiPatternStrings; // Keep for init logic, populated from piiPciProperties
    private Map<String, String> pciPatternStrings; // Keep for init logic, populated from piiPciProperties

    private final Map<String, Pattern> compiledPiiPatterns = new HashMap<>();
    private final Map<String, Pattern> compiledPciPatterns = new HashMap<>();

    // Constructor injection for PiiPciProperties
    public PiiPciDetectionServiceImpl(PiiPciProperties piiPciProperties) {
        this.piiPciProperties = piiPciProperties;
    }

    @PostConstruct
    public void init() {
        if (piiPciProperties != null && piiPciProperties.getPii() != null && piiPciProperties.getPii().getPatterns() != null) {
            this.piiPatternStrings = piiPciProperties.getPii().getPatterns();
        } else {
            logger.warn("[PiiPciDetectionService-Init] PII patterns not found in configuration properties, initializing to empty map.");
            this.piiPatternStrings = new HashMap<>();
        }

        if (piiPciProperties != null && piiPciProperties.getPci() != null && piiPciProperties.getPci().getPatterns() != null) {
            this.pciPatternStrings = piiPciProperties.getPci().getPatterns();
        } else {
            logger.warn("[PiiPciDetectionService-Init] PCI patterns not found in configuration properties, initializing to empty map.");
            this.pciPatternStrings = new HashMap<>();
        }

        logger.info("[PiiPciDetectionService-Init] Initializing compiled PII patterns from: {}", this.piiPatternStrings);
        this.piiPatternStrings.forEach((key, value) -> {
            try {
                Pattern compiledPattern = Pattern.compile(value);
                this.compiledPiiPatterns.put("PII_" + key, compiledPattern);
                logger.debug("[PiiPciDetectionService-Init] Initialized compiled PII pattern: {} -> {}", "PII_" + key, value);
            } catch (Exception e) {
                logger.error("[PiiPciDetectionService-Init] Error compiling PII pattern '{}' during init: {}", key, e.getMessage());
            }
        });

        logger.info("[PiiPciDetectionService-Init] Initializing compiled PCI patterns from: {}", this.pciPatternStrings);
        this.pciPatternStrings.forEach((key, value) -> {
            try {
                Pattern compiledPattern = Pattern.compile(value);
                this.compiledPciPatterns.put("PCI_" + key, compiledPattern);
                logger.debug("[PiiPciDetectionService-Init] Initialized compiled PCI pattern: {} -> {}", "PCI_" + key, value);
            } catch (Exception e) {
                logger.error("[PiiPciDetectionService-Init] Error compiling PCI pattern '{}' during init: {}", key, e.getMessage());
            }
        });
        logger.info("[PiiPciDetectionService-Init] Initialization complete. Compiled PII patterns: {}, Compiled PCI patterns: {}", compiledPiiPatterns.size(), compiledPciPatterns.size());
    }

    @Override
    public List<PiiPciFinding> scanRepository(Path repoPath, Map<String, Pattern> customPatterns) {
        logger.info("[PiiPciDetectionService] Starting repository scan for: {}", repoPath);
        final List<PiiPciFinding> allFindings = new ArrayList<>();
        final Map<String, Pattern> effectivePatterns = new HashMap<>();

        if (customPatterns != null && !customPatterns.isEmpty()) {
            logger.info("[PiiPciDetectionService] Using custom patterns provided ({} patterns).", customPatterns.size());
            effectivePatterns.putAll(customPatterns);
        } else {
            logger.info("[PiiPciDetectionService] No custom patterns provided, using pre-compiled configured patterns. PII: {}, PCI: {}", compiledPiiPatterns.size(), compiledPciPatterns.size());
            effectivePatterns.putAll(this.compiledPiiPatterns);
            effectivePatterns.putAll(this.compiledPciPatterns);
        }
        
        logger.info("[PiiPciDetectionService] Effective patterns to be used for scanning ({} total): {}", effectivePatterns.size(), effectivePatterns.keySet());

        if (effectivePatterns.isEmpty()) {
            logger.warn("[PiiPciDetectionService] No PII/PCI patterns are configured or provided. Scan will not find any items.");
            return allFindings; // Return empty list if no patterns
        }

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                    String filePathString = path.toString().toLowerCase(); // use toLowerCase for robust suffix checking
                    // Exclude .git directory, common binary files, target directories etc.
                    if (filePathString.contains("/.git/") || 
                        filePathString.contains("/target/") || 
                        filePathString.contains("/build/") ||
                        filePathString.contains("/dist/") ||
                        filePathString.contains("/node_modules/") ||
                        filePathString.endsWith(".jar") ||
                        filePathString.endsWith(".class") ||
                        filePathString.endsWith(".png") ||
                        filePathString.endsWith(".jpg") ||
                        filePathString.endsWith(".jpeg") ||
                        filePathString.endsWith(".gif") ||
                        filePathString.endsWith(".bmp") ||
                        filePathString.endsWith(".tiff") ||
                        filePathString.endsWith(".zip") ||
                        filePathString.endsWith(".tar") ||
                        filePathString.endsWith(".gz") ||
                        filePathString.endsWith(".rar") ||
                        filePathString.endsWith(".7z") ||
                        filePathString.endsWith(".exe") ||
                        filePathString.endsWith(".dll") ||
                        filePathString.endsWith(".so") ||
                        filePathString.endsWith(".dylib") ||
                        filePathString.endsWith(".o") ||
                        filePathString.endsWith(".obj") ||
                        filePathString.endsWith(".pdf") ||
                        filePathString.endsWith(".doc") ||
                        filePathString.endsWith(".docx") ||
                        filePathString.endsWith(".xls") ||
                        filePathString.endsWith(".xlsx") ||
                        filePathString.endsWith(".ppt") ||
                        filePathString.endsWith(".pptx") ||
                        filePathString.endsWith(".odt") ||
                        filePathString.endsWith(".ods") ||
                        filePathString.endsWith(".odp") ||
                        // filePathString.endsWith(".svg") || // SVGs can contain text, might be relevant
                        filePathString.endsWith(".lock") ||
                        filePathString.endsWith(".log") || // Logs might be too noisy, but could contain PII/PCI. User request was "not just logs, but any elements"
                        filePathString.endsWith(".min.js") || // minified JS
                        filePathString.endsWith(".min.css")) { // minified CSS
                        // logger.trace("[PiiPciDetectionService] Filtering out file based on path/extension: {}", filePath);
                        return false;
                    }
                    return true;
                })
                .forEach(filePath -> {
                    // logger.debug("[PiiPciDetectionService] Scanning file: {}", filePath); // Can be too verbose
                    try {
                        // Using Files.lines() for potentially better memory usage with large files,
                        // but readAllLines is fine for moderately sized source code files.
                        // For this implementation, we need line numbers, so readAllLines is easier.
                        List<String> lines = Files.readAllLines(filePath); // Assuming UTF-8, might need Charset specification for robustness
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            // Simple check for very long lines to avoid performance issues with regex on massive single lines (e.g. minified files not caught by extension)
                            if (line.length() > 20000) { 
                                // logger.warn("[PiiPciDetectionService] Skipping very long line ({}) in file: {}", line.length(), filePath);
                                continue;
                            }
                            for (Map.Entry<String, Pattern> entry : effectivePatterns.entrySet()) {
                                String findingType = entry.getKey();
                                Pattern pattern = entry.getValue();
                                Matcher matcher = pattern.matcher(line);
                                while (matcher.find()) {
                                    PiiPciFinding finding = new PiiPciFinding(
                                            repoPath.relativize(filePath).toString().replace("\\", "/"), // Normalize path separators
                                            i + 1,             // Line number (1-indexed)
                                            matcher.start() +1,  // Column number (1-indexed)
                                            findingType,
                                            matcher.group()
                                    );
                                    allFindings.add(finding);
                                    logger.info("[PiiPciDetectionService] Found PII/PCI: Type={}, File={}, Line={}, Col={}, Match={}", findingType, finding.getFilePath(), finding.getLineNumber(), finding.getColumnNumber(),finding.getMatchedText());
                                }
                            }
                        }
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("malformedinputexception")) {
                            logger.warn("[PiiPciDetectionService] Skipping file due to charset issue (MalformedInputException): {}", filePath);
                        } else {
                            logger.error("[PiiPciDetectionService] Error reading file {}: {}", filePath, e.getMessage());
                        }
                    } catch (StackOverflowError సో) { // Renamed to avoid syntax issue if original char is problematic
                        logger.error("[PiiPciDetectionService] StackOverflowError while processing file (likely very complex regex or line structure): {}", filePath);
                    }
                     catch (Exception e) {
                        logger.error("[PiiPciDetectionService] Unexpected error processing file {}: {}", filePath, e.getMessage(), e);
                    }
                });
        } catch (IOException e) {
            logger.error("[PiiPciDetectionService] Error walking file tree for path {}: {}", repoPath, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("[PiiPciDetectionService] Unexpected error during repository scan for {}: {}", repoPath, e.getMessage(), e);
        }

        logger.info("[PiiPciDetectionService] Scan completed. Found {} PII/PCI items.", allFindings.size());
        return allFindings;
    }
} 