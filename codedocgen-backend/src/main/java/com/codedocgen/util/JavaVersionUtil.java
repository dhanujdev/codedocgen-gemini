package com.codedocgen.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaVersionUtil {

    private static final Logger LOGGER = Logger.getLogger(JavaVersionUtil.class.getName());

    public static String detectJavaVersionFromPom(File pomFile) {
        if (pomFile == null || !pomFile.exists() || !pomFile.isFile()) {
            LOGGER.log(Level.WARNING, "pom.xml file not found or is not a valid file: " + (pomFile != null ? pomFile.getAbsolutePath() : "null"));
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable DTD validation to prevent XXE vulnerabilities and network access
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            // 1. Check <properties><maven.compiler.source/target/release></properties>
            NodeList propertiesList = doc.getElementsByTagName("properties");
            if (propertiesList.getLength() > 0 && propertiesList.item(0) instanceof Element) {
                Element propertiesElement = (Element) propertiesList.item(0);
                String sourceVersion = getTagValue("maven.compiler.source", propertiesElement);
                if (sourceVersion != null) return normalizeJavaVersion(sourceVersion);
                String targetVersion = getTagValue("maven.compiler.target", propertiesElement);
                if (targetVersion != null) return normalizeJavaVersion(targetVersion);
                String releaseVersion = getTagValue("maven.compiler.release", propertiesElement);
                if (releaseVersion != null) return normalizeJavaVersion(releaseVersion);
            }

            // 2. Check maven-compiler-plugin configuration
            NodeList plugins = doc.getElementsByTagName("plugin");
            for (int i = 0; i < plugins.getLength(); i++) {
                if (plugins.item(i) instanceof Element) {
                    Element plugin = (Element) plugins.item(i);
                    String artifactId = getTagValue("artifactId", plugin);
                    if ("maven-compiler-plugin".equals(artifactId)) {
                        Element configuration = (Element) plugin.getElementsByTagName("configuration").item(0);
                        if (configuration != null) {
                            String sourceVersion = getTagValue("source", configuration);
                            if (sourceVersion != null) return normalizeJavaVersion(sourceVersion);
                            String targetVersion = getTagValue("target", configuration);
                            if (targetVersion != null) return normalizeJavaVersion(targetVersion);
                            String releaseVersion = getTagValue("release", configuration);
                            if (releaseVersion != null) return normalizeJavaVersion(releaseVersion);
                        }
                    }
                }
            }

            LOGGER.log(Level.INFO, "Java version not explicitly specified in pom.xml: " + pomFile.getAbsolutePath());
            return null; // Or a default version
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing pom.xml to detect Java version: " + pomFile.getAbsolutePath(), e);
            return null;
        }
    }

    private static String getTagValue(String tagName, Element element) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0 && nodeList.item(0) != null) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private static String normalizeJavaVersion(String version) {
        if (version == null) return null;
        version = version.trim();
        if (version.startsWith("1.")) { // e.g., 1.8 -> 8
            return version.substring(2);
        }
        // Could add more normalization for things like "JavaSE-11" -> "11"
        return version;
    }

    public static void main(String[] args) {
        // Create a dummy pom.xml for testing
        File testPom = new File("pom.xml");
        try {
            if (!testPom.exists()) {
                java.nio.file.Files.write(testPom.toPath(),
                        ("<project><properties><maven.compiler.source>1.8</maven.compiler.source></properties></project>").getBytes());
                System.out.println("Created dummy pom.xml for testing with Java 1.8");
                String version = detectJavaVersionFromPom(testPom);
                System.out.println("Detected Java version: " + version);
                testPom.delete();

                java.nio.file.Files.write(testPom.toPath(),
                        ("<project><build><plugins><plugin><artifactId>maven-compiler-plugin</artifactId>" +
                                "<configuration><release>11</release></configuration></plugin></plugins></build></project>").getBytes());
                System.out.println("Created dummy pom.xml for testing with Java 11 (release)");
                version = detectJavaVersionFromPom(testPom);
                System.out.println("Detected Java version: " + version);
                testPom.delete();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating test pom.xml", e);
        }
    }
} 