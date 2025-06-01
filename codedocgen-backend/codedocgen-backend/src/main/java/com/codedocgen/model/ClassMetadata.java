package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;


/**
 * Metadata for a Java class
 */
@Data
@NoArgsConstructor
public class ClassMetadata {
    private String name;
    private String packageName;
    private String type;
    private List<String> annotations = new ArrayList<>();
    private List<MethodMetadata> methods = new ArrayList<>();
    private List<FieldMetadata> fields = new ArrayList<>();
    private String parentClass;
    private List<String> interfaces = new ArrayList<>();
    private String filePath;
    private boolean isAbstract;
    private boolean isInterface;
}
