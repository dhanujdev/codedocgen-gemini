package com.codedocgen.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for log statement information
 */
@Data
@NoArgsConstructor
public class LogStatement {
    private String className;
    private String methodName;
    private int lineNumber;
    private String logLevel;
    private String message;
    private String filePath;
    private boolean containsSensitiveData;
    private String sensitiveDataType;
} 