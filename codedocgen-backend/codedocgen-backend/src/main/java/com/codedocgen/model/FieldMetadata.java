package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;


/**
 * Metadata for a Java field
 */
@Data
@NoArgsConstructor
public class FieldMetadata {
    private String name;
    private String type;
    private List<String> annotations = new ArrayList<>();
    private String visibility; // e.g., public, private, protected, default
    private boolean isStatic;
    private boolean isFinal;
    private String initializer; // Added for default values, e.g. in annotations
}
