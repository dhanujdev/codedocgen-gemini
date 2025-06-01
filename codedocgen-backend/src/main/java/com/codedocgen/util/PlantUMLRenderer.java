package com.codedocgen.util;

import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.codedocgen.model.DiagramType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for rendering PlantUML diagrams
 */
@Component
public class PlantUMLRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PlantUMLRenderer.class);
    
    /**
     * Renders a PlantUML diagram and saves it to a file with the specified diagram type
     * @param plantUmlSource PlantUML source code
     * @param outputPath Output file path
     * @param diagramType Type of diagram
     * @return Path to the saved file, or null if there was an error
     */
    public String renderDiagram(String plantUmlSource, String outputPath, DiagramType diagramType) {
        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            File outputFile = new File(outputPath);
            
            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                reader.outputImage(outputStream, new FileFormatOption(FileFormat.SVG));
            }
            
            logger.info("Successfully rendered {} diagram to: {}", diagramType, outputPath);
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Error rendering {} diagram to file: {}", diagramType, outputPath, e);
            return null;
        }
    }
    
    /**
     * Renders a PlantUML diagram as PNG and returns it as a Base64 string
     * @param plantUmlSource PlantUML source code
     * @return Base64 encoded PNG image
     */
    public String renderAsPngBase64(String plantUmlSource) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            reader.outputImage(outputStream, new FileFormatOption(FileFormat.PNG));
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            logger.error("Error rendering PlantUML diagram as PNG", e);
            return "";
        }
    }
    
    /**
     * Renders a PlantUML diagram as SVG
     * @param plantUmlSource PlantUML source code
     * @return SVG as string
     */
    public String renderAsSvg(String plantUmlSource) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            reader.outputImage(outputStream, new FileFormatOption(FileFormat.SVG));
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error rendering PlantUML diagram as SVG", e);
            return "";
        }
    }
    
    /**
     * Saves a PlantUML diagram as a file
     * @param plantUmlSource PlantUML source code
     * @param outputPath Output file path
     * @param format Output file format
     * @return true if successful, false otherwise
     */
    public boolean saveToFile(String plantUmlSource, String outputPath, FileFormat format) {
        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            File outputFile = new File(outputPath);
            
            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                reader.outputImage(outputStream, new FileFormatOption(format));
            }
            
            return true;
        } catch (IOException e) {
            logger.error("Error saving PlantUML diagram to file: {}", outputPath, e);
            return false;
        }
    }
    
    /**
     * Gets the PlantUML source code with proper start and end tags
     * @param content PlantUML diagram content
     * @return PlantUML source with @startuml and @enduml tags
     */
    public String getPlantUmlSource(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "@startuml\n@enduml";
        }
        
        String trimmedContent = content.trim();
        
        // Check if the content already has the start/end tags
        boolean hasStartTag = trimmedContent.startsWith("@startuml");
        boolean hasEndTag = trimmedContent.endsWith("@enduml");
        
        StringBuilder builder = new StringBuilder();
        
        if (!hasStartTag) {
            builder.append("@startuml\n");
        }
        
        builder.append(trimmedContent);
        
        if (!hasEndTag) {
            if (!trimmedContent.endsWith("\n")) {
                builder.append("\n");
            }
            builder.append("@enduml");
        }
        
        return builder.toString();
    }
} 