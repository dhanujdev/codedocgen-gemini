package com.codedocgen.model;

import lombok.Data;
import java.util.List;

@Data
public class FieldMetadata {
    private String name;
    private String type;
    private List<String> annotations;
    private String visibility; // Optional: public, private, protected, default
    private boolean isStatic;   // Optional
    private boolean isFinal;    // Optional
    // Add other relevant details if needed, like full declaration string
} 