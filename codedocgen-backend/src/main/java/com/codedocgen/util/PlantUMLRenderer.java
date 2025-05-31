package com.codedocgen.util;

import com.codedocgen.model.DiagramType;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import java.util.HashMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;

@Component
public class PlantUMLRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PlantUMLRenderer.class);
    
    @Value("${app.graphviz.dot.executable.path:dot}")
    private String dotExecutablePath;
    
    @PostConstruct
    public void init() {
        configurePlantUML();
    }
    
    /**
     * Configures PlantUML to use the specified dot executable path
     */
    private void configurePlantUML() {
        String resolvedDotPath = SystemInfoUtil.getExecutableCommand(dotExecutablePath, "dot");
        
        if (resolvedDotPath != null && !resolvedDotPath.equals("dot")) {
            // Set the GRAPHVIZ_DOT environment variable via system property
            System.setProperty("GRAPHVIZ_DOT", resolvedDotPath);
            logger.info("Set GRAPHVIZ_DOT system property to: {}", resolvedDotPath);
        } else {
            logger.info("Using default 'dot' executable from PATH for PlantUML");
        }
        
        // Test if dot is available
        if (SystemInfoUtil.isExecutableOnPath(resolvedDotPath)) {
            logger.info("GraphViz dot executable is available at: {}", resolvedDotPath);
        } else {
            logger.warn("GraphViz dot executable may not be available. This could affect diagram generation. Path: {}", resolvedDotPath);
            
            // Additional fallback for Windows - check common installation locations
            if (SystemInfoUtil.isWindows()) {
                Map<String, String> commonPaths = new HashMap<>();
                commonPaths.put("C:\\Program Files\\Graphviz\\bin\\dot.exe", "C:\\Program Files\\Graphviz\\bin\\dot.exe");
                commonPaths.put("C:\\Program Files (x86)\\Graphviz\\bin\\dot.exe", "C:\\Program Files (x86)\\Graphviz\\bin\\dot.exe");
                
                for (Map.Entry<String, String> entry : commonPaths.entrySet()) {
                    File dotFile = new File(entry.getValue());
                    if (dotFile.exists() && dotFile.canExecute()) {
                        logger.info("Found GraphViz dot executable at common Windows location: {}", entry.getValue());
                        System.setProperty("GRAPHVIZ_DOT", entry.getValue());
                        logger.info("Set GRAPHVIZ_DOT system property to: {}", entry.getValue());
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Renders a PlantUML diagram to a file
     * 
     * @param plantUmlSource The PlantUML source string
     * @param outputFilePath The path to save the diagram to
     * @param diagramType The type of diagram being rendered
     * @return The path to the rendered diagram or null if rendering failed
     */
    public String renderDiagram(String plantUmlSource, String outputFilePath, DiagramType diagramType) {
        try {
            // Create directories if they don't exist
            File outputFile = new File(outputFilePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                logger.error("Failed to create parent directories for {}", outputFilePath);
                return null;
            }
            
            // Generate the diagram
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            
            // We use SVG format for all diagrams as it's vector-based and scales well
            FileFormatOption formatOption = new FileFormatOption(FileFormat.SVG);
            String desc = reader.generateImage(os, formatOption);
            
            if (desc == null) {
                logger.error("PlantUML failed to generate {} diagram", diagramType);
                return null;
            }
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                os.writeTo(fos);
            }
            
            logger.info("{} diagram generated: {}", diagramType, outputFilePath);
            return outputFilePath;
            
        } catch (IOException e) {
            logger.error("Error rendering {} diagram: {}", diagramType, e.getMessage(), e);
            return null;
        }
    }
} 