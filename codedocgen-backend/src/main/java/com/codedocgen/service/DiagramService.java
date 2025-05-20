package com.codedocgen.service;

import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.DiagramType;
import java.util.List;
import java.util.Map;

public interface DiagramService {
    Map<DiagramType, String> generateDiagrams(List<ClassMetadata> classMetadata, String outputDir);
    String generateClassDiagram(List<ClassMetadata> classMetadata, String outputDir);
    String generateSequenceDiagram(List<String> callFlow, String outputDir, String diagramName);
    // Add other diagram generation methods (e.g., sequence, component)
} 