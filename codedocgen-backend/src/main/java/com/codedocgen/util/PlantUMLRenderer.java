package com.codedocgen.util;

import com.codedocgen.model.DiagramType;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PlantUMLRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PlantUMLRenderer.class);
    
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