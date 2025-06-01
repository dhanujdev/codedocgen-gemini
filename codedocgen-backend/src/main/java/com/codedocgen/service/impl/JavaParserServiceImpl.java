package com.codedocgen.service.impl;

import com.codedocgen.dto.MavenExecutionResult;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.model.FieldMetadata;
import com.codedocgen.service.JavaParserService;
import com.codedocgen.service.MavenBuildService;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ParserConfiguration;
import com.codedocgen.parser.DaoAnalyzer;
import com.codedocgen.model.DaoOperationDetail;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.Expression;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Pattern;

// Imports for XML parsing
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource; // Added for parsing without DTD validation for security

@Service
public class JavaParserServiceImpl implements JavaParserService {

    private static final Logger logger = LoggerFactory.getLogger(JavaParserServiceImpl.class);
    private static final String CLASSPATH_OUTPUT_FILE = "codedocgen_cp.txt";

    private final DaoAnalyzer daoAnalyzerInstance;

    private final MavenBuildService mavenBuildService;

    private File currentProjectDir;
    private JavaSymbolSolver symbolResolver;

    @Autowired
    public JavaParserServiceImpl(MavenBuildService mavenBuildService) {
        this.mavenBuildService = mavenBuildService;
        this.daoAnalyzerInstance = new DaoAnalyzer();
    }

    private void ensureSymbolSolverInitialized(File projectDir) {
        if (this.symbolResolver == null || !projectDir.equals(this.currentProjectDir)) {
            logger.info("Initializing JavaParser Symbol Solver for project: {}", projectDir.getAbsolutePath());
            this.currentProjectDir = projectDir;

            File gradleBuildFile = new File(projectDir, "build.gradle");
            File gradleKtsBuildFile = new File(projectDir, "build.gradle.kts");
            File gradlewBatFile = new File(projectDir, "gradlew.bat");
            File gradlewFile = new File(projectDir, "gradlew");
            File pomFile = new File(projectDir, "pom.xml");

            boolean isGradleProject = gradleBuildFile.exists() || gradleKtsBuildFile.exists();
            boolean isMavenProject = pomFile.exists() && !isGradleProject; // Prefer Gradle if both somehow exist

            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            List<File> moduleBaseDirs = new ArrayList<>(); // For multi-module projects

            if (isGradleProject) {
                logger.info("Detected Gradle project in {}. Running Gradle commands.", projectDir.getAbsolutePath());
                String gradleExecutable;
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) {
                    gradleExecutable = gradlewBatFile.exists() ? gradlewBatFile.getAbsolutePath() : "gradle.bat";
                } else {
                    gradleExecutable = gradlewFile.exists() ? gradlewFile.getAbsolutePath() : "gradle";
                    if (gradlewFile.exists() && !gradlewFile.canExecute()) {
                        try {
                            if (gradlewFile.setExecutable(true)) {
                                logger.info("Made {} executable.", gradlewFile.getAbsolutePath());
                            } else {
                                logger.warn("Failed to make {} executable. Build might fail.", gradlewFile.getAbsolutePath());
                            }
                        } catch (SecurityException se) {
                            logger.warn("SecurityException while trying to make {} executable: {}. Build might fail.", gradlewFile.getAbsolutePath(), se.getMessage());
                        }
                    }
                }

                try {
                    logger.info("Attempting to compile Gradle project using: {} clean build classes -x test -q --console=plain", gradleExecutable);
                    ProcessBuilder pbGradleBuild = new ProcessBuilder(gradleExecutable, "clean", "build", "classes", "-x", "test", "-q", "--console=plain");
                    pbGradleBuild.directory(projectDir);
                    pbGradleBuild.redirectErrorStream(true);
                    Process processGradleBuild = pbGradleBuild.start();
                    StringBuilder gradleBuildOutput = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(processGradleBuild.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            gradleBuildOutput.append(line).append(System.lineSeparator());
                        }
                    }
                    int gradleBuildExitCode = processGradleBuild.waitFor();
                    logger.info("Gradle command finished with exit code: {}. Output: {}", gradleBuildExitCode, gradleBuildOutput.toString());

                    if (gradleBuildExitCode != 0) {
                        logger.warn("Gradle command failed with exit code {}. Symbol resolution might be incomplete.", gradleBuildExitCode);
                    } else {
                        logger.info("Gradle build successful. Verifying JAXB generated file paths for 'com.revinate.sample.datatype'...");
                        // Check for a known JAXB generated file in the expected package structure
                        String expectedJaxbPackagePath = "com" + File.separator + "revinate" + File.separator + "sample" + File.separator + "datatype";
                        String[] jaxbCheckFiles = { "FibonacciFault.java", "FactorialFault.java" };

                        for (String genDirPrefix : new String[]{"build/generated-sources/jaxb", "build/generated/sources/jaxb/main/java", "build/generated/jaxb"}) {
                            for (String checkFile : jaxbCheckFiles) {
                                File specificJaxbFile = new File(projectDir, genDirPrefix + File.separator + expectedJaxbPackagePath + File.separator + checkFile);
                                if (specificJaxbFile.exists()) {
                                    logger.info("Confirmed JAXB generated file found at: {}", specificJaxbFile.getAbsolutePath());
                                } else {
                                    // This is just a trace, as the directory itself will be added to the solver
                                    logger.trace("JAXB generated file NOT found at path: {}", specificJaxbFile.getAbsolutePath());
                                }
                            }
                        }
                    }
                    logger.info("For Gradle projects, JarTypeSolver setup for precise dependency resolution currently relies on system/IDE classpath or requires a custom task to output classpath. We will rely on ReflectionTypeSolver and JavaParserTypeSolver for build outputs.");
                    // Attempt to find JARs in common Gradle output locations
                    List<Path> gradleJarPaths = new ArrayList<>();
                    try {
                        Path buildLibs = projectDir.toPath().resolve("build").resolve("libs");
                        if (Files.exists(buildLibs) && Files.isDirectory(buildLibs)) {
                            try (Stream<Path> walk = Files.walk(buildLibs)) {
                                walk.filter(path -> path.toString().endsWith(".jar"))
                                    .forEach(gradleJarPaths::add);
                            }
                            logger.info("Found {} JARs in {}", gradleJarPaths.size(), buildLibs);
                        }

                        // Also check subprojects - common for multi-project builds
                        File[] subdirectories = projectDir.listFiles(File::isDirectory);
                        if (subdirectories != null) {
                            for (File subDir : subdirectories) {
                                Path subLibs = subDir.toPath().resolve("libs"); // Some older Gradle versions or custom configs might put them here
                                if (Files.exists(subLibs) && Files.isDirectory(subLibs)) {
                                    int currentSize = gradleJarPaths.size();
                                    try (Stream<Path> walk = Files.walk(subLibs)) {
                                        walk.filter(path -> path.toString().endsWith(".jar"))
                                            .forEach(gradleJarPaths::add);
                                    }
                                    if (gradleJarPaths.size() > currentSize) {
                                        logger.info("Found {} JARs in {}", gradleJarPaths.size() - currentSize, subLibs);
                                    }
                                }
                                Path subBuildLibs = subDir.toPath().resolve("build").resolve("libs");
                                if (Files.exists(subBuildLibs) && Files.isDirectory(subBuildLibs)) {
                                    int currentSize = gradleJarPaths.size();
                                    try (Stream<Path> walk = Files.walk(subBuildLibs)) {
                                        walk.filter(path -> path.toString().endsWith(".jar"))
                                            .forEach(gradleJarPaths::add);
                                    }
                                     if (gradleJarPaths.size() > currentSize) {
                                        logger.info("Found {} JARs in {}", gradleJarPaths.size() - currentSize, subBuildLibs);
                                    }
                                }
                            }
                        }
                        
                        for (Path jarPath : gradleJarPaths) {
                            try {
                                logger.info("Adding JarTypeSolver for Gradle discovered JAR: {}", jarPath.toString());
                                combinedTypeSolver.add(new JarTypeSolver(jarPath.toString()));
                            } catch (Exception e) {
                                logger.warn("Failed to add JarTypeSolver for Gradle JAR {}: {}", jarPath.toString(), e.getMessage());
                            }
                        }

                    } catch (IOException e) {
                        logger.warn("Error trying to discover Gradle JARs in build/libs: {}", e.getMessage());
                    }

                } catch (IOException | InterruptedException e) {
                    logger.error("Error while running Gradle command: {}", e.getMessage(), e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }

                // After Maven commands, try to find modules if it's a Maven project
                if (pomFile.exists()) {
                    moduleBaseDirs.addAll(getMavenModules(pomFile, projectDir));
                }
            } else if (isMavenProject) {
                logger.info("Detected Maven project in {}. Running Maven commands.", projectDir.getAbsolutePath());
                try {
                    // No need to construct mvnCommand or ProcessBuilder here directly for classpath
                    // Let MavenBuildService handle that with all configurations.
                    logger.info("Building classpath using MavenBuildService.");
                    MavenExecutionResult classpathResult = mavenBuildService.runMavenCommandWithExplicitVersion(projectDir, null, 
                        "dependency:build-classpath", 
                        "-Dmdep.outputFile=" + CLASSPATH_OUTPUT_FILE,
                        "-Dmdep.pathSeparator=" + File.pathSeparator,
                        "-DincludeScope=compile",
                        "-q"
                    );
                    logger.info("Maven 'dependency:build-classpath' finished with exit code: {}. Output: {}", classpathResult.getExitCode(), classpathResult.getOutput());

                    logger.info("Attempting to compile the project via MavenBuildService.");
                    MavenExecutionResult compileResult = mavenBuildService.runMavenCommandWithExplicitVersion(projectDir, (String) null, 
                        "compile", 
                        "-DskipTests", 
                        "-q", 
                        "-Dmaven.compiler.failOnError=false", 
                        "-Dmaven.compiler.failOnWarning=false"
                    );
                    logger.info("Maven 'compile' command finished with exit code: {}. Output: {}", compileResult.getExitCode(), compileResult.getOutput());

                    if (!compileResult.isSuccess()) {
                        logger.warn("Initial Maven compile command failed with exit code {}. Errors might affect symbol resolution.", compileResult.getExitCode());
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error("Error while running Maven commands for Symbol Solver pre-step: {}", e.getMessage(), e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }

                // After Maven commands, try to find modules if it's a Maven project
                if (pomFile.exists()) {
                    moduleBaseDirs.addAll(getMavenModules(pomFile, projectDir));
                }
            } else {
                logger.warn("No pom.xml or build.gradle/build.gradle.kts file found in {}. Skipping build system pre-compile and classpath build steps. Resolution will be limited.", projectDir.getAbsolutePath());
            }
            
            // Always add the root project directory itself as a base for sources
            if (!moduleBaseDirs.contains(projectDir)) {
                moduleBaseDirs.add(0, projectDir); // Add root project first
            }

            // Add ReflectionTypeSolver for JDK classes - prefer classloader
            logger.info("Attempting to add ReflectionTypeSolver (preferring context classloader).");
            try {
                combinedTypeSolver.add(new ReflectionTypeSolver(true)); // Prefer context classloader
                logger.info("ReflectionTypeSolver (with context classloader) added.");
            } catch (Exception e) {
                logger.warn("Failed to add ReflectionTypeSolver with context classloader: {}", e.getMessage());
                try {
                    logger.info("Attempting to add ReflectionTypeSolver (without context classloader).");
                    combinedTypeSolver.add(new ReflectionTypeSolver(false)); // Fallback to not using context classloader
                    logger.info("ReflectionTypeSolver (without context classloader) added.");
                } catch (Exception e2) {
                    logger.error("Failed to add any ReflectionTypeSolver: {}", e2.getMessage());
                }
            }

            // Add project's own source roots first
            // File srcMainJava = new File(projectDir, "src/main/java"); // Old way
            // if (srcMainJava.exists() && srcMainJava.isDirectory()) {
            //     logger.info("Adding JavaParserTypeSolver for source root: {}", srcMainJava.getAbsolutePath());
            //     combinedTypeSolver.add(new JavaParserTypeSolver(srcMainJava));
            // }

            // File srcTestJava = new File(projectDir, "src/test/java"); // Old way
            // if (srcTestJava.exists() && srcTestJava.isDirectory()) {
            //     logger.info("Adding JavaParserTypeSolver for test source root: {}", srcTestJava.getAbsolutePath());
            //     combinedTypeSolver.add(new JavaParserTypeSolver(srcTestJava));
            // }
            
            // Common source directory patterns to look for in each module
            String[] commonSrcDirs = {"src/main/java", "src/test/java"};
            String[] commonResourceDirs = {"src/main/resources", "src/test/resources"}; // Though not directly parsed, good for context
            String[] commonGeneratedAnnotationsDirs = {"target/generated-sources/annotations", "build/generated/sources/annotationProcessor/java/main"}; // Maven, Gradle
            String[] commonGeneratedSourcesDirs = {
                "target/generated-sources", // Maven general
                "build/generated-sources",  // Gradle general
                "build/generated/sources/jaxb/main/java", // Gradle JAXB example
                "build/generated/sources/xjc/main/java", // Common for XJC plugin
                "target/generated-sources/jaxb", // Maven JAXB
                "target/generated-sources/xjc", // Maven XJC
                "target/generated-sources/wsimport", // Maven wsimport
                // Add more known generated source locations as needed
            };


            for (File baseDir : moduleBaseDirs) {
                logger.info("Processing module/project directory for TypeSolvers: {}", baseDir.getAbsolutePath());
                for (String srcPath : commonSrcDirs) {
                    File srcDir = new File(baseDir, srcPath);
                    if (srcDir.exists() && srcDir.isDirectory()) {
                        logger.info("Adding JavaParserTypeSolver for source root: {}", srcDir.getAbsolutePath());
                        combinedTypeSolver.add(new JavaParserTypeSolver(srcDir));
                    }
                }
                for (String genPath : commonGeneratedAnnotationsDirs) {
                    File genDir = new File(baseDir, genPath);
                    if (genDir.exists() && genDir.isDirectory()) {
                        logger.info("Adding JavaParserTypeSolver for generated annotations root: {}", genDir.getAbsolutePath());
                        combinedTypeSolver.add(new JavaParserTypeSolver(genDir));
                    }
                }
                for (String genPath : commonGeneratedSourcesDirs) {
                    File genDir = new File(baseDir, genPath);
                     if (genDir.exists() && genDir.isDirectory()) {
                        // Check if it's a directory with .java files before adding
                        try (Stream<Path> walk = Files.walk(genDir.toPath(), 3)) { // Limit depth to avoid large scans
                            if (walk.anyMatch(p -> p.toString().endsWith(".java"))) {
                                logger.info("Adding JavaParserTypeSolver for general generated source root: {}", genDir.getAbsolutePath());
                                combinedTypeSolver.add(new JavaParserTypeSolver(genDir));
                            } else {
                                logger.debug("Skipping generated source directory {} as it contains no .java files (within depth 3).", genDir.getAbsolutePath());
                            }
                        } catch (IOException e) {
                            logger.warn("Could not walk directory {}: {}", genDir.getAbsolutePath(), e.getMessage());
                        }
                    }
                }
            }


            // Add compiled output directories of the project being analyzed
            // These are important for resolving symbols from compiled code, especially after annotation processing (Lombok)
            if (isGradleProject) {
                String[] buildClassesDirs = {
                    "build/classes/java/main",      // Gradle default for main sourceset
                    "build/classes/kotlin/main",    // Gradle default for Kotlin
                    "build/classes/groovy/main",    // Gradle default for Groovy
                    "build/classes/scala/main",     // Gradle default for Scala
                    "build/resources/main"          // Gradle resources (might contain .class files in some cases or provide context)
                };
                for (File baseDir : moduleBaseDirs) { // Iterate modules for Gradle too
                    for (String classesPath : buildClassesDirs) {
                        File dir = new File(baseDir, classesPath);
                        if (dir.exists() && dir.isDirectory()) {
                            logger.info("Adding JarTypeSolver for Gradle build output directory: {}", dir.getAbsolutePath());
                             try {
                                combinedTypeSolver.add(new JarTypeSolver(dir.toPath()));
                            } catch (Exception e) {
                                logger.warn("Failed to add JarTypeSolver for Gradle build output {}: {} - {}. This directory will be skipped.", dir.getAbsolutePath(), e.getClass().getName(), e.getMessage());
                            }
                        }
                    }
                }
            }
            
            // Maven specific or general fallback if dirs exist
            String[] targetClassesDirs = {"target/classes", "target/test-classes"};
            for (File baseDir : moduleBaseDirs) { // Iterate modules
                for (String classesPath : targetClassesDirs) {
                    File dir = new File(baseDir, classesPath);
                    if (dir.exists() && dir.isDirectory()) {
                        logger.info("Adding JarTypeSolver for project's compiled classes directory: {}", dir.getAbsolutePath());
                        try {
                            combinedTypeSolver.add(new JarTypeSolver(dir.toPath()));
                        } catch (Exception e) {
                            logger.warn("Failed to add JarTypeSolver for {}: {} - {}. This directory will be skipped.", dir.getAbsolutePath(), e.getClass().getName(), e.getMessage());
                        }
                    } else if (isMavenProject && classesPath.equals("target/classes")) { // Only warn if it's a Maven project and main classes are missing
                        logger.warn("Maven project compiled classes directory {} does not exist in module {}.", classesPath, baseDir.getName());
                    }
                }
            }

            // Add JarTypeSolvers for Maven project dependencies (using the pre-built classpath file)
            // This part is Maven-specific because it relies on CLASSPATH_OUTPUT_FILE from the root project
            if (isMavenProject) {
                File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE); // Classpath file is generated at root
                if (classpathFile.exists() && classpathFile.isFile()) {
                    try {
                        String classpath = new String(Files.readAllBytes(classpathFile.toPath()), StandardCharsets.UTF_8).trim();
                        logger.info("Raw classpath from {}: '{}'", CLASSPATH_OUTPUT_FILE, classpath);
                        if (classpath != null && !classpath.isEmpty()) {
                            String[] jarPaths = classpath.split(Pattern.quote(File.pathSeparator));
                            logger.info("Found {} potential JAR paths in classpath file.", jarPaths.length);
                            for (String jarPath : jarPaths) {
                                String trimmedJarPath = jarPath != null ? jarPath.trim() : "";
                                if (!trimmedJarPath.isEmpty()) {
                                    File jarFile = new File(trimmedJarPath);
                                    if (jarFile.exists() && jarFile.isFile()) {
                                        try {
                                            logger.info("Adding JarTypeSolver for dependency: {}", jarFile.getAbsolutePath());
                                            combinedTypeSolver.add(new JarTypeSolver(jarFile.toPath()));
                                        } catch (Exception e) {
                                            logger.warn("Failed to add JarTypeSolver for {}: {} - {}. This JAR will be skipped.", jarFile.getAbsolutePath(), e.getClass().getName(), e.getMessage());
                                        }
                                    } else {
                                        logger.warn("Dependency JAR path from classpath file does not exist or is not a file: {}", trimmedJarPath);
                                    }
                                }
                            }
                        } else {
                            logger.warn("Classpath file {} was empty or null.", CLASSPATH_OUTPUT_FILE);
                        }
                    } catch (IOException e) {
                        logger.error("Error reading classpath file {}: {}", CLASSPATH_OUTPUT_FILE, e.getMessage());
                    } finally {
                        if (!classpathFile.delete()) {
                            logger.warn("Failed to delete temporary classpath file: {}", classpathFile.getAbsolutePath());
                        }
                    }
                } else {
                     logger.warn("Maven classpath file {} not found after 'dependency:build-classpath'. Dependencies might not be resolved via JarTypeSolver.", CLASSPATH_OUTPUT_FILE);
                }
            }
            
            // Fallback if no standard source roots found (AFTER attempting specific ones)
            boolean primarySourceFound = false;
            for (String genDir : commonGeneratedSourcesDirs) {
                if (new File(projectDir, genDir).exists()) {
                    primarySourceFound = true;
                    break;
                }
            }
            if (!primarySourceFound) {
                 logger.warn("No standard, generated, or target/classes source roots found. Adding project root as a last resort JavaParserTypeSolver: {}", projectDir.getAbsolutePath());
                 combinedTypeSolver.add(new JavaParserTypeSolver(projectDir)); // Least preferred, broad scope
            }

            this.symbolResolver = new JavaSymbolSolver(combinedTypeSolver);
            ParserConfiguration config = new ParserConfiguration().setSymbolResolver(this.symbolResolver);
            StaticJavaParser.setConfiguration(config);
            logger.info("JavaParser Symbol Solver initialized and configuration set for project: {}", projectDir.getAbsolutePath());
        }
    }

    /**
     * Parses a pom.xml file to find declared modules.
     * @param pomFile The pom.xml file.
     * @param projectRoot The root directory of the main project.
     * @return A list of File objects representing the base directories of the modules.
     */
    private List<File> getMavenModules(File pomFile, File projectRoot) {
        List<File> moduleDirs = new ArrayList<>();
        if (!pomFile.exists()) {
            logger.warn("getMavenModules: pom.xml not found at {}", pomFile.getAbsolutePath());
            return moduleDirs;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Disable DTD validation and external entities for security
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            dbFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
            dbFactory.setExpandEntityReferences(false);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            
            // Use InputSource to parse to avoid issues with DTDs in some poms
            Document doc = dBuilder.parse(new InputSource(new FileInputStream(pomFile)));
            doc.getDocumentElement().normalize();

            org.w3c.dom.NodeList moduleNodes = doc.getElementsByTagName("module");
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                Node node = moduleNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String moduleName = node.getTextContent().trim();
                    if (!moduleName.isEmpty()) {
                        File moduleDir = new File(projectRoot, moduleName);
                        if (moduleDir.exists() && moduleDir.isDirectory()) {
                            logger.info("Discovered Maven module: {}", moduleDir.getAbsolutePath());
                            moduleDirs.add(moduleDir);
                        } else {
                            logger.warn("Declared Maven module directory does not exist: {}", moduleDir.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing pom.xml ({}) for modules: {}", pomFile.getAbsolutePath(), e.getMessage(), e);
        }
        return moduleDirs;
    }

    @Override
    public List<ClassMetadata> parseProject(File projectDir) {
        ensureSymbolSolverInitialized(projectDir);
        List<ClassMetadata> allClassMetadata = new ArrayList<>();
        Path projectPath = projectDir.toPath();

        try (Stream<Path> javaFiles = Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".java") && Files.isRegularFile(path))) {
            javaFiles.forEach(javaFilePath -> {
                try {
                    ClassMetadata classMetadata = parseFile(javaFilePath.toFile());
                    if (classMetadata != null) {
                        allClassMetadata.add(classMetadata);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing file {}: {}", javaFilePath, e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            logger.error("Error walking through project directory {}: {}", projectPath, e.getMessage(), e);
        }
        return allClassMetadata;
    }

    @Override
    public ClassMetadata parseFile(File javaFile) {
        ensureSymbolSolverInitialized(currentProjectDir != null ? currentProjectDir : javaFile.getParentFile());
        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");
            ClassMetadataVisitor visitor = new ClassMetadataVisitor(javaFile.getAbsolutePath(), packageName, this.symbolResolver, this.daoAnalyzerInstance);
            visitor.visit(cu, null);
            return visitor.getClassMetadata();
        } catch (Exception e) {
            logger.error("Failed to parse Java file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
            return null;
        }
    }

    // Overloaded method to collect parse warnings
    public List<ClassMetadata> parseProject(File projectDir, List<String> parseWarnings) {
        ensureSymbolSolverInitialized(projectDir);
        List<ClassMetadata> allClassMetadata = new ArrayList<>();
        Path projectPath = projectDir.toPath();

        try (Stream<Path> javaFiles = Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".java") && Files.isRegularFile(path))) {
            javaFiles.forEach(javaFilePath -> {
                try {
                    ClassMetadata classMetadata = parseFileWithResolver(javaFilePath.toFile(), this.symbolResolver, this.daoAnalyzerInstance);
                    if (classMetadata != null) {
                        allClassMetadata.add(classMetadata);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing file {}: {}", javaFilePath, e.getMessage(), e);
                    if (parseWarnings != null) {
                        parseWarnings.add(javaFilePath.toString() + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Error walking through project directory {}: {}", projectPath, e.getMessage(), e);
            if (parseWarnings != null) {
                parseWarnings.add("Error walking project directory: " + e.getMessage());
            }
        }
        return allClassMetadata;
    }

    // Helper for parseProject to use the already initialized resolver
    private ClassMetadata parseFileWithResolver(File javaFile, JavaSymbolSolver symResolver, DaoAnalyzer daoAnalyst) {
         try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");
            ClassMetadataVisitor visitor = new ClassMetadataVisitor(javaFile.getAbsolutePath(), packageName, symResolver, daoAnalyst);
            visitor.visit(cu, null);
            return visitor.getClassMetadata();
        } catch (Exception e) {
            logger.error("Failed to parse Java file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
            throw new RuntimeException("Failed to parse file with resolver: " + javaFile.getAbsolutePath(), e);
        }
    }

    private static class ClassMetadataVisitor extends VoidVisitorAdapter<Void> {
        private static final Logger visitorLogger = LoggerFactory.getLogger("com.codedocgen.parser.ClassMetadataVisitorLogger");

        private ClassMetadata classMetadata;
        private final String filePath;
        private final String packageName;
        private List<MethodMetadata> methodMetadataList;
        private MethodMetadata currentMethodMetadata;
        private String currentClassName;
        private final JavaSymbolSolver symbolResolver;
        private final DaoAnalyzer daoAnalyzer;

        public ClassMetadataVisitor(String filePath, String packageName, JavaSymbolSolver symbolResolver, DaoAnalyzer daoAnalyzer) {
            this.filePath = filePath;
            this.packageName = packageName;
            this.classMetadata = new ClassMetadata();
            this.classMetadata.setFilePath(filePath);
            this.classMetadata.setPackageName(packageName);
            this.symbolResolver = symbolResolver;
            this.daoAnalyzer = daoAnalyzer;
        }

        private void processMethods(List<MethodDeclaration> methods, String currentPackageName, String currentClassName) {
            if (this.methodMetadataList == null) {
                this.methodMetadataList = new ArrayList<>();
            }
            for (MethodDeclaration md : methods) {
                MethodMetadata method = new MethodMetadata();
                method.setName(md.getNameAsString());
                method.setReturnType(md.getType().toString());
                method.setParameters(md.getParameters().stream()
                        .map(p -> p.getType().toString() + " " + p.getNameAsString())
                        .collect(Collectors.toList()));
                method.setAnnotations(md.getAnnotations().stream()
                        .map(AnnotationExpr::toString)
                        .collect(Collectors.toList()));
                method.setExceptionsThrown(md.getThrownExceptions().stream()
                        .map(type -> type.toString())
                        .collect(Collectors.toList()));
                method.setVisibility(getVisibility(md));
                method.setStatic(md.isStatic());
                method.setAbstract(md.isAbstract());
                method.setPackageName(currentPackageName);
                method.setClassName(currentClassName);
                method.setResolvedMethodNode(md);

                List<List<String>> paramAnnotationsList = new ArrayList<>();
                for (Parameter param : md.getParameters()) {
                    List<String> currentParamAnnotations = param.getAnnotations().stream()
                            .map(AnnotationExpr::toString)
                            .collect(Collectors.toList());
                    paramAnnotationsList.add(currentParamAnnotations);
                }
                method.setParameterAnnotations(paramAnnotationsList);

                List<String> returnTypeAnns = new ArrayList<>();
                md.getType().getAnnotations().forEach(ann -> returnTypeAnns.add(ann.toString()));
                method.setReturnTypeAnnotations(returnTypeAnns);

                method.setCalledMethods(new ArrayList<>());
                method.setExternalCalls(new ArrayList<>());
                method.setDaoOperations(new ArrayList<>());
                method.setLocalVariables(new ArrayList<>());

                md.getBody().ifPresent(body -> {
                    body.findAll(VariableDeclarator.class).forEach(var -> {
                        method.getLocalVariables().add(var.getType().toString() + " " + var.getNameAsString());
                    });
                });

                this.currentMethodMetadata = method;
                MethodCallVisitor methodCallVisitor = new MethodCallVisitor(this.classMetadata, method, this.symbolResolver, currentPackageName, currentClassName);
                md.accept(methodCallVisitor, null);
                
                method.setDaoOperations(this.daoAnalyzer.analyze(md).getOperations());

                this.methodMetadataList.add(method);
            }
            if (this.classMetadata != null && this.methodMetadataList != null) {
                this.classMetadata.setMethods(this.methodMetadataList);
            }
            this.currentMethodMetadata = null;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            super.visit(n, arg);
            this.currentClassName = n.getNameAsString();
            classMetadata.setName(n.getNameAsString());
            classMetadata.setType(determineClassType(n, this.packageName));
            classMetadata.setInterface(n.isInterface());
            classMetadata.setAbstract(n.isAbstract());
            classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));

            List<FieldMetadata> fieldList = new ArrayList<>();
            for (FieldDeclaration field : n.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    FieldMetadata fm = new FieldMetadata();
                    fm.setName(var.getNameAsString());
                    String fieldTypeStr;
                    try {
                        ResolvedType resolvedType = field.getElementType().resolve();
                        fieldTypeStr = resolvedType.describe();
                    } catch (Exception e) {
                        try {
                            ResolvedType resolvedType = var.getType().resolve();
                            fieldTypeStr = resolvedType.describe();
                        } catch (Exception e2) {
                            visitorLogger.warn("Could not resolve type for field {} in class {}. Using declared type: {}. Error: {}", var.getNameAsString(), classMetadata.getName(), var.getTypeAsString(), e2.getMessage());
                            fieldTypeStr = var.getTypeAsString();
                        }
                    }
                    fm.setType(fieldTypeStr);
                    fm.setAnnotations(field.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
                    fm.setVisibility(getVisibilityFromModifiers(field.getModifiers()));
                    fm.setStatic(field.isStatic());
                    fm.setFinal(field.isFinal());
                    fieldList.add(fm);
                }
            }
            classMetadata.setFields(fieldList);

            n.getExtendedTypes().stream().findFirst().ifPresent(ct -> classMetadata.setParentClass(ct.getNameAsString()));
            List<String> interfaces = n.getImplementedTypes().stream().map(ct -> ct.getNameAsString()).collect(Collectors.toList());
            classMetadata.setInterfaces(interfaces);

            processMethods(n.getMethods(), this.packageName, n.getNameAsString());
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            super.visit(n, arg);
            this.currentClassName = n.getNameAsString();
            classMetadata.setName(n.getNameAsString());
            classMetadata.setType("enum");
            classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
            
            List<FieldMetadata> fields = n.getEntries().stream().map(entry -> {
                FieldMetadata fm = new FieldMetadata();
                fm.setName(entry.getNameAsString());
                fm.setType(n.getNameAsString());
                fm.setAnnotations(entry.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
                fm.setVisibility("public");
                fm.setStatic(true);
                fm.setFinal(true);
                return fm;
            }).collect(Collectors.toList());
            classMetadata.setFields(fields);

            List<String> interfaces = n.getImplementedTypes().stream().map(ct -> ct.getNameAsString()).collect(Collectors.toList());
            classMetadata.setInterfaces(interfaces);

            processMethods(n.getMethods(), this.packageName, n.getNameAsString());
        }

        @Override
        public void visit(AnnotationDeclaration n, Void arg) {
            super.visit(n, arg);
            this.currentClassName = n.getNameAsString();
            classMetadata.setName(n.getNameAsString());
            classMetadata.setType("annotation");
            classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
            
            List<FieldMetadata> members = new ArrayList<>();
            n.getMembers().forEach(memberDeclaration -> {
                if (memberDeclaration instanceof AnnotationMemberDeclaration) {
                    AnnotationMemberDeclaration amd = (AnnotationMemberDeclaration) memberDeclaration;
                    FieldMetadata fieldMeta = new FieldMetadata();
                    fieldMeta.setName(amd.getNameAsString());
                    fieldMeta.setType(amd.getType().asString()); 
                    fieldMeta.setAnnotations(amd.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
                    fieldMeta.setVisibility("public"); 
                    amd.getDefaultValue().ifPresent(val -> fieldMeta.setInitializer(val.toString()));
                    members.add(fieldMeta);
                }
            });
            classMetadata.setFields(members);
            classMetadata.setMethods(new ArrayList<>()); 
        }

        private String determineClassType(ClassOrInterfaceDeclaration n, String currentPackageName) {
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Controller") || a.getNameAsString().endsWith("RestController"))) return "controller";
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Service"))) return "service";
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Repository"))) return "repository";

            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("WebService"))) return "soap";
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("Endpoint"))) return "soap";

            if (n.getNameAsString().endsWith("Repository")) return "repository";
            if (n.getExtendedTypes().stream().anyMatch(t -> t.getNameAsString().matches("(JpaRepository|CrudRepository|PagingAndSortingRepository)"))) return "repository";
            if (n.getImplementedTypes().stream().anyMatch(t -> t.getNameAsString().matches("(JpaRepository|CrudRepository|PagingAndSortingRepository)"))) return "repository";

            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Component"))) {
                 if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Configuration"))) return "config";
                 return "component";
            }
            if (n.isAnnotationPresent("Entity") || n.isAnnotationPresent("jakarta.persistence.Entity") || n.isAnnotationPresent("javax.persistence.Entity") || n.isAnnotationPresent("Table") || n.isAnnotationPresent("jakarta.persistence.Table") || n.isAnnotationPresent("javax.persistence.Table")) {
                return "entity";
            }
            if (n.isAnnotationPresent("RestController") || n.isAnnotationPresent("Controller")) {
                return "controller";
            }
            if (n.isAnnotationPresent("Repository") || n.getNameAsString().endsWith("Repository") || n.getNameAsString().endsWith("Dao") || n.getNameAsString().endsWith("DAO")) {
                return "repository";
            }
            boolean isInModelPackage = currentPackageName != null && 
                (currentPackageName.contains(".model") || 
                 currentPackageName.contains(".domain") || 
                 currentPackageName.contains(".dto"));

            if (isInModelPackage) {
                return "model";
            }

            if (n.isInterface()) return "interface";
            
            if (n.getNameAsString().endsWith("Test") || n.getNameAsString().startsWith("Test")) return "test";
            
            return "class";
        }

        private String getVisibility(MethodDeclaration md) {
            if (md.isPublic()) return "public";
            if (md.isProtected()) return "protected";
            if (md.isPrivate()) return "private";
            return "default";
        }

        private String getVisibilityFromModifiers(NodeList<Modifier> modifiers) {
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC)) return "public";
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PROTECTED)) return "protected";
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE)) return "private";
            return "default";
        }

        public ClassMetadata getClassMetadata() {
            if (this.classMetadata != null && this.classMetadata.getMethods() == null && this.methodMetadataList != null) {
                this.classMetadata.setMethods(this.methodMetadataList);
            }
            return classMetadata;
        }

        private class MethodCallVisitor extends VoidVisitorAdapter<Void> {
            private final ClassMetadata classMetadataContext;
            private final MethodMetadata methodMetadataContext;
            private final JavaSymbolSolver specificResolver;
            private final String outerClassPackageName;
            private final String outerClassName;

            public MethodCallVisitor(ClassMetadata classCtx, MethodMetadata methodCtx, JavaSymbolSolver resolver, String pkgName, String clsName) {
                this.classMetadataContext = classCtx;
                this.methodMetadataContext = methodCtx;
                this.specificResolver = resolver;
                this.outerClassPackageName = pkgName;
                this.outerClassName = clsName;
            }

            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);

                if (methodMetadataContext == null) {
                    visitorLogger.trace("[MCV] MethodCallExpr visited but methodMetadataContext is null. Call: {}. Class context: {}", n.toString(), classMetadataContext != null ? classMetadataContext.getName() : "UNKNOWN_CLASS");
                    return;
                }

                visitorLogger.trace("[MCV] Visiting MethodCallExpr: '{}' in method: '{}' of class: '{}'", 
                    n.toString(), 
                    methodMetadataContext.getName(),
                    outerClassName != null ? outerClassName : "UNKNOWN_CLASS");

                if (methodMetadataContext.getCalledMethods() == null) {
                    methodMetadataContext.setCalledMethods(new ArrayList<>()); 
                }

                try {
                    ResolvedMethodDeclaration resolvedMethod = n.resolve(); 
                    String resolvedSignature = resolvedMethod.getQualifiedSignature();
                    visitorLogger.trace("[MCV] Successfully resolved method call '{}'. Signature: '{}'", n.getNameAsString(), resolvedSignature);
                    methodMetadataContext.getCalledMethods().add(resolvedSignature);
                } catch (Exception e) {
                    visitorLogger.warn("[MCV] Failed to resolve method call '{}' in {}.{}. Attempting fallback. Error: {} - {}", 
                        n.getNameAsString(), outerClassName, methodMetadataContext.getName(), e.getClass().getName(), e.getMessage(), e);
                    String fallbackSignature = tryToConstructUnresolvedSignature(n, this.outerClassPackageName, this.outerClassName);
                    visitorLogger.trace("[MCV] Fallback signature for '{}': '{}'", n.getNameAsString(), fallbackSignature);
                    methodMetadataContext.getCalledMethods().add(fallbackSignature);
                }
            }

            private String tryToConstructUnresolvedSignature(MethodCallExpr n, String currentPackageName, String currentClassName) {
                visitorLogger.trace("[MCV-Fallback] Constructing signature for unresolved call: '{}' in context class: {}, package: {}", n.toString(), currentClassName, currentPackageName);
                String methodName = n.getNameAsString();
                String scopeName = null; 

                if (n.getScope().isPresent()) {
                    Expression scopeExpr = n.getScope().get();
                    scopeName = scopeExpr.toString(); 
                    visitorLogger.trace("[MCV-Fallback] Explicit scope: '{}'", scopeName);
                    try {
                        ResolvedType resolvedType = scopeExpr.calculateResolvedType(); 
                        scopeName = resolvedType.describe(); 
                        visitorLogger.trace("[MCV-Fallback] Scope resolved to type: '{}'", scopeName);
                    } catch (Exception e) {
                        visitorLogger.warn("[MCV-Fallback] Could not resolve type of scope expression '{}' for method call '{}'. Using raw scope. Error: {} - {}", 
                            scopeExpr.toString(), methodName, e.getClass().getName(), e.getMessage());
                    }
                } else {
                    visitorLogger.trace("[MCV-Fallback] No explicit scope for call to '{}'. Assuming current class or import context: '{}'", methodName, currentClassName);
                    scopeName = currentClassName;
                }
                
                List<String> argTypes = new ArrayList<>();
                for (Expression argExpr : n.getArguments()) {
                    try {
                        ResolvedType argType = argExpr.calculateResolvedType(); 
                        argTypes.add(argType.describe());
                        visitorLogger.trace("[MCV-Fallback] Arg '{}' resolved to type: '{}'", argExpr.toString(), argType.describe());
                    } catch (Exception e) {
                        visitorLogger.warn("[MCV-Fallback] Could not resolve type of argument '{}' for method call '{}'. Using UNKNOWN_PARAM_TYPE. Error: {} - {}", 
                            argExpr.toString(), methodName, e.getClass().getName(), e.getMessage());
                        argTypes.add("UNKNOWN_PARAM_TYPE"); 
                    }
                }
                String params = String.join(", ", argTypes);
                String finalSignature = "UNRESOLVED_CALL: " + (scopeName != null && !scopeName.trim().isEmpty() ? scopeName + "." : "") + methodName + "(" + params + ")";
                visitorLogger.trace("[MCV-Fallback] Constructed signature for '{}': '{}'", methodName, finalSignature);
                return finalSignature;
            }
        }
    }
} 