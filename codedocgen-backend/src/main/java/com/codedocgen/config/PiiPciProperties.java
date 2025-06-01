package com.codedocgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component; // Or @Configuration

import java.util.HashMap;
import java.util.Map;

@Component // Make it a Spring bean
@ConfigurationProperties(prefix = "app")
public class PiiPciProperties {

    private Pii pii = new Pii();
    private Pci pci = new Pci();

    public Pii getPii() {
        return pii;
    }

    public void setPii(Pii pii) {
        this.pii = pii;
    }

    public Pci getPci() {
        return pci;
    }

    public void setPci(Pci pci) {
        this.pci = pci;
    }

    public static class Pii {
        private Map<String, String> patterns = new HashMap<>();

        public Map<String, String> getPatterns() {
            return patterns;
        }

        public void setPatterns(Map<String, String> patterns) {
            this.patterns = patterns;
        }
    }

    public static class Pci {
        private Map<String, String> patterns = new HashMap<>();

        public Map<String, String> getPatterns() {
            return patterns;
        }

        public void setPatterns(Map<String, String> patterns) {
            this.patterns = patterns;
        }
    }
} 