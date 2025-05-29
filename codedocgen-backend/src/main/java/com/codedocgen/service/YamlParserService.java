package com.codedocgen.service;

import java.util.Map;

public interface YamlParserService {
    /**
     * Parses a YAML file into a Map structure.
     * @param filePath The absolute path to the YAML file.
     * @return A Map representing the YAML content, or null if parsing fails.
     */
    Map<String, Object> parseYamlFile(String filePath);
} 