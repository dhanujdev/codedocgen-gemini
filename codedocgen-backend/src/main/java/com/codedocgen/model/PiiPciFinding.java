package com.codedocgen.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a finding of PII (Personally Identifiable Information) or PCI (Payment Card Industry) data
 */
@Data
@NoArgsConstructor
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
    
    /**
     * Gets the file path
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Sets the file path
     * @param filePath the file path to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Gets the line number
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Sets the line number
     * @param lineNumber the line number to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    /**
     * Gets the column number
     * @return the column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }
    
    /**
     * Sets the column number
     * @param columnNumber the column number to set
     */
    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }
    
    /**
     * Gets the finding type
     * @return the finding type
     */
    public String getFindingType() {
        return findingType;
    }
    
    /**
     * Sets the finding type
     * @param findingType the finding type to set
     */
    public void setFindingType(String findingType) {
        this.findingType = findingType;
    }
    
    /**
     * Gets the matched text
     * @return the matched text
     */
    public String getMatchedText() {
        return matchedText;
    }
    
    /**
     * Sets the matched text
     * @param matchedText the matched text to set
     */
    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }
} 