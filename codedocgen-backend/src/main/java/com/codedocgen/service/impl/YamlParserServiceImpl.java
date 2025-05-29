package com.codedocgen.service.impl;

import com.codedocgen.service.YamlParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

@Service
public class YamlParserServiceImpl implements YamlParserService {

    private static final Logger logger = LoggerFactory.getLogger(YamlParserServiceImpl.class);

    @Override
    public Map<String, Object> parseYamlFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("File path is null or empty, cannot parse YAML.");
            return null;
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(filePath)) {
            Map<String, Object> data = yaml.load(inputStream);
            logger.info("Successfully parsed YAML file: {}", filePath);
            return data;
        } catch (FileNotFoundException e) {
            logger.error("YAML file not found: {}", filePath, e);
        } catch (Exception e) { // Catches YAMLException and other potential issues
            logger.error("Failed to parse YAML file: {}. Error: {}", filePath, e.getMessage(), e);
        }
        return null;
    }
} 