package com.codedocgen.model;

public class PiiPciFinding {
    private String filePath;
    private int lineNumber;
    private int columnNumber; // Start position of the finding in the line
    private String findingType; // e.g., PII_SSN, PCI_CREDIT_CARD
    private String matchedText;

    public PiiPciFinding(String filePath, int lineNumber, int columnNumber, String findingType, String matchedText) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.findingType = findingType;
        this.matchedText = matchedText;
    }

    // Getters and setters
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getFindingType() {
        return findingType;
    }

    public void setFindingType(String findingType) {
        this.findingType = findingType;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }
} 