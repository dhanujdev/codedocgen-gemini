package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents metadata for a method parameter
 */
@Data
@NoArgsConstructor
public class ParameterMetadata {
    private String name;
    private String type;
    private List<String> annotations = new ArrayList<>();
    private boolean isFinal;
    private String defaultValue;
    
    // For Spring MVC endpoint analysis
    private boolean isRequestParam;
    private boolean isPathVariable;
    private boolean isRequestBody;
    private boolean isModelAttribute;
    
    public void addAnnotation(String annotation) {
        annotations.add(annotation);
    }

    /**
     * Returns a string representation of this parameter
     * This is crucial for String.join() operations in the codebase
     */
    @Override
    public String toString() {
        return type + " " + name;
    }
}
