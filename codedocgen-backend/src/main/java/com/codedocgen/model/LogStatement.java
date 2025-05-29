package com.codedocgen.model;

import java.util.List;

public class LogStatement {
    private String id;
    private String className;
    private int line;
    private String level;
    private String message;
    private List<LogVariable> variables;
    private boolean isPiiRisk;
    private boolean isPciRisk;

    public LogStatement() {
    }

    public LogStatement(String id, String className, int line, String level, String message, List<LogVariable> variables, boolean isPiiRisk, boolean isPciRisk) {
        this.id = id;
        this.className = className;
        this.line = line;
        this.level = level;
        this.message = message;
        this.variables = variables;
        this.isPiiRisk = isPiiRisk;
        this.isPciRisk = isPciRisk;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<LogVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<LogVariable> variables) {
        this.variables = variables;
    }

    public boolean isPiiRisk() {
        return isPiiRisk;
    }

    public void setPiiRisk(boolean piiRisk) {
        isPiiRisk = piiRisk;
    }

    public boolean isPciRisk() {
        return isPciRisk;
    }

    public void setPciRisk(boolean pciRisk) {
        isPciRisk = pciRisk;
    }
} 