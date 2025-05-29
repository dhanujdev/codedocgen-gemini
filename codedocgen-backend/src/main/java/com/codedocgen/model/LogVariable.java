package com.codedocgen.model;

public class LogVariable {
    private String name;
    private String type;
    private boolean isPii;
    private boolean isPci;

    public LogVariable() {
    }

    public LogVariable(String name, String type, boolean isPii, boolean isPci) {
        this.name = name;
        this.type = type;
        this.isPii = isPii;
        this.isPci = isPci;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPii() {
        return isPii;
    }

    public void setPii(boolean pii) {
        isPii = pii;
    }

    public boolean isPci() {
        return isPci;
    }

    public void setPci(boolean pci) {
        isPci = pci;
    }
} 