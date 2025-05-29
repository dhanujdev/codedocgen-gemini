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

@Service
public class JavaParserServiceImpl implements JavaParserService {

    private static final Logger logger = LoggerFactory.getLogger(JavaParserServiceImpl.class);
    private static final String CLASSPATH_OUTPUT_FILE = "codedocgen_cp.txt";

    private static final DaoAnalyzer daoAnalyzer = new DaoAnalyzer();

    private final MavenBuildService mavenBuildService;

    private File currentProjectDir;
    private JavaSymbolSolver symbolResolver;

    @Autowired
    public JavaParserServiceImpl(MavenBuildService mavenBuildService) {
        this.mavenBuildService = mavenBuildService;
    }

    private void ensureSymbolSolverInitialized(File projectDir) {
        if (this.symbolResolver == null || !projectDir.equals(this.currentProjectDir)) {
            logger.info("Initializing JavaParser Symbol Solver for project: {}", projectDir.getAbsolutePath());
            this.currentProjectDir = projectDir;

            File pomFileForCompile = new File(projectDir, "pom.xml");
            // Attempt to compile and resolve dependencies first
            if (pomFileForCompile.exists() && pomFileForCompile.isFile()) {
                logger.info("Found pom.xml, attempting to build classpath and compile the project for robust symbol resolution.");
                try {
                    // 1. Build classpath first to make all dependencies available
                    String osName = System.getProperty("os.name").toLowerCase();
                    String mvnCommand = osName.contains("win") ? "mvn.cmd" : "mvn";
                    ProcessBuilder pbClasspath = new ProcessBuilder(
                        mvnCommand,
                        "dependency:build-classpath",
                        "-Dmdep.outputFile=" + CLASSPATH_OUTPUT_FILE,
                        "-Dmdep.pathSeparator=" + File.pathSeparator,
                        "-q"
                    );
                    pbClasspath.directory(projectDir);
                    pbClasspath.redirectErrorStream(true);
                    logger.info("Executing Maven command: {}", String.join(" ", pbClasspath.command()));
                    Process processClasspath = pbClasspath.start();
                    StringBuilder mavenClasspathOutput = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(processClasspath.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            mavenClasspathOutput.append(line).append(System.lineSeparator());
                        }
                    }
                    int classpathExitCode = processClasspath.waitFor();
                    logger.info("Maven 'dependency:build-classpath' finished with exit code: {}. Output:\n{}", classpathExitCode, mavenClasspathOutput.toString());

                    // 2. Attempt to compile the project
                    logger.info("Attempting to compile the project via MavenBuildService.");
                    MavenExecutionResult compileResult = mavenBuildService.runMavenCommandWithExplicitVersion(projectDir, (String) null, "compile", "-DskipTests", "-q", "-Dmaven.compiler.failOnError=false", "-Dmaven.compiler.failOnWarning=false");
                    logger.info("Maven 'compile' command finished with exit code: {}. Output:\n{}", compileResult.getExitCode(), compileResult.getOutput());

                    if (!compileResult.isSuccess()) {
                        logger.warn("Initial Maven compile command failed with exit code {}. Generated sources might be missing or incomplete. Errors might affect symbol resolution.", compileResult.getExitCode());
                        // Further attempts to resolve dependencies if compile fails can be added here if needed,
                        // but build-classpath should have handled most dependency availability.
                    }

                } catch (IOException | InterruptedException e) {
                    logger.error("Error while running Maven commands for Symbol Solver pre-step: {}", e.getMessage(), e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                logger.info("No pom.xml found in {}, skipping Maven pre-compile and classpath build steps.", projectDir.getAbsolutePath());
            }

            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            
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
            File srcMainJava = new File(projectDir, "src/main/java");
            if (srcMainJava.exists() && srcMainJava.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for main source root: {}", srcMainJava.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(srcMainJava));
            } else {
                logger.warn("Main source directory {} does not exist.", srcMainJava.getAbsolutePath());
            }

            File srcTestJava = new File(projectDir, "src/test/java");
            if (srcTestJava.exists() && srcTestJava.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for test source root: {}", srcTestJava.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(srcTestJava));
            } else {
                logger.warn("Test source directory {} does not exist.", srcTestJava.getAbsolutePath());
            }
            
            // Add common generated source directories
            String[] generatedSourceDirs = {
                "target/generated-sources/annotations", // Common for annotation processors
                "target/generated-sources/apt",     // Another common for annotation processors
                "target/generated-sources/jaxb",    // For JAXB if used
                "build/generated/sources/annotationProcessor/java/main" // Gradle default
            };
            for (String genDir : generatedSourceDirs) {
                File generatedSrcDir = new File(projectDir, genDir);
                if (generatedSrcDir.exists() && generatedSrcDir.isDirectory()) {
                    logger.info("Adding JavaParserTypeSolver for generated source root: {}", generatedSrcDir.getAbsolutePath());
                    combinedTypeSolver.add(new JavaParserTypeSolver(generatedSrcDir));
                }
            }

            // Add compiled output directories of the project being analyzed
            // These are important for resolving symbols from compiled code, especially after annotation processing (Lombok)
            File targetClasses = new File(projectDir, "target/classes");
            if (targetClasses.exists() && targetClasses.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for project's compiled classes: {}", targetClasses.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(targetClasses));
            } else {
                logger.warn("Project compiled classes directory {} does not exist. This might affect resolution of generated code (e.g., Lombok).", targetClasses.getAbsolutePath());
            }
            File targetTestClasses = new File(projectDir, "target/test-classes");
            if (targetTestClasses.exists() && targetTestClasses.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for project's compiled test classes: {}", targetTestClasses.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(targetTestClasses));
            } else {
                logger.warn("Project compiled test classes directory {} does not exist.", targetTestClasses.getAbsolutePath());
            }

            // Add JarTypeSolvers for Maven project dependencies (using the pre-built classpath file)
            File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE);
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
                                        combinedTypeSolver.add(new JarTypeSolver(jarFile));
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
                 logger.warn("Classpath file {} not found after 'dependency:build-classpath'. Dependencies might not be resolved.", CLASSPATH_OUTPUT_FILE);
            }
            
            // Fallback if no standard source roots found (AFTER attempting specific ones)
            boolean primarySourceFound = (srcMainJava.exists() && srcMainJava.isDirectory()) ||
                                       (srcTestJava.exists() && srcTestJava.isDirectory()) ||
                                       (targetClasses.exists() && targetClasses.isDirectory());
            boolean generatedSrcFound = false;
            for (String genDir : generatedSourceDirs) {
                if (new File(projectDir, genDir).exists()) {
                    generatedSrcFound = true;
                    break;
                }
            }
            if (!primarySourceFound && !generatedSrcFound) {
                 logger.warn("No standard, generated, or target/classes source roots found. Adding project root as a last resort JavaParserTypeSolver: {}", projectDir.getAbsolutePath());
                 combinedTypeSolver.add(new JavaParserTypeSolver(projectDir)); // Least preferred, broad scope
            }

            this.symbolResolver = new JavaSymbolSolver(combinedTypeSolver);
            ParserConfiguration config = new ParserConfiguration().setSymbolResolver(this.symbolResolver);
            StaticJavaParser.setConfiguration(config);
            logger.info("JavaParser Symbol Solver initialized and configuration set for project: {}", projectDir.getAbsolutePath());
        }
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
        ensureSymbolSolverInitialized(currentProjectDir != null ? currentProjectDir : javaFile.getParentFile()); // Initialize if not already, use project dir or parent
        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");
            ClassMetadataVisitor visitor = new ClassMetadataVisitor(javaFile.getAbsolutePath(), packageName);
            visitor.visit(cu, null);
            return visitor.getClassMetadata();
        } catch (Exception e) {
            logger.error("Failed to parse Java file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
            return null; // Or throw a custom exception
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
                    ClassMetadata classMetadata = parseFile(javaFilePath.toFile());
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

    private static class ClassMetadataVisitor extends VoidVisitorAdapter<Void> {
        // Change to a simpler logger name
        private static final Logger visitorLogger = LoggerFactory.getLogger("com.codedocgen.parser.ClassMetadataVisitorLogger");

        private ClassMetadata classMetadata;
        private final String filePath;
        private final String packageName;
        private MethodMetadata currentMethodMetadata;
        private String currentClassName;

        public ClassMetadataVisitor(String filePath, String packageName) {
            this.filePath = filePath;
            this.packageName = packageName;
            this.classMetadata = new ClassMetadata();
            this.classMetadata.setFilePath(filePath);
            this.classMetadata.setPackageName(packageName);
        }

        private void processMethods(List<MethodDeclaration> methods, String currentPackageName, String currentClassName) {
            if (methods == null) return;
            List<MethodMetadata> methodMetadataList = new ArrayList<>();
            for (MethodDeclaration md : methods) {
                MethodMetadata methodMeta = new MethodMetadata();
                String methodName = md.getNameAsString();
                methodMeta.setName(methodName); // Set name on the instance first

                // Set currentMethodMetadata for the visitor to use when traversing this method's body
                this.currentMethodMetadata = methodMeta; 

                visitorLogger.trace("[CMV] Processing method: {}.{} (Package: {})", currentClassName, methodName, currentPackageName);

                String returnTypeStr = "void"; // Default
                try {
                    ResolvedType resolvedReturnType = md.getType().resolve();
                    returnTypeStr = resolvedReturnType.describe();
                    visitorLogger.trace("[CMV] Method {}.{} - Resolved return type: {}", currentClassName, methodName, returnTypeStr);
                } catch (Exception e) {
                    visitorLogger.warn("[CMV] Could not resolve return type for method {} in class {}. Using declared type: {}. Error: {}", methodName, currentClassName, md.getType().toString(), e.getMessage());
                    returnTypeStr = md.getType().toString();
                }

                final String finalCurrentClassNameForLambda = currentClassName; // For use in lambda
                List<String> parameters = new ArrayList<>();
                for (com.github.javaparser.ast.body.Parameter p : md.getParameters()) {
                    try {
                        ResolvedType resolvedType = p.getType().resolve();
                        parameters.add(resolvedType.describe() + " " + p.getNameAsString());
                    } catch (Exception e) {
                        visitorLogger.warn("[CMV] Could not resolve parameter type for {} in method {} in class {}. Using declared type: {}. Error: {}", p.getNameAsString(), methodName, finalCurrentClassNameForLambda, p.getType().toString(), e.getMessage());
                        parameters.add(p.getType().toString() + " " + p.getNameAsString());
                    }
                }
                visitorLogger.trace("[CMV] Method {}.{} - Parameters: {}", currentClassName, methodName, parameters);


                List<String> annotations = md.getAnnotations().stream()
                        .map(AnnotationExpr::toString)
                        .collect(Collectors.toList());
                List<String> exceptionsThrown = md.getThrownExceptions().stream()
                        .map(type -> type.asString())
                        .collect(Collectors.toList());
                String visibility = getVisibility(md);
                boolean isStatic = md.isStatic();
                boolean isAbstract = md.isAbstract();

                List<String> localVariablesList = new ArrayList<>();
                List<List<String>> parameterAnnotationsList = new ArrayList<>();
                 // Initialize calledMethods list, it will be populated by visit(MethodCallExpr)
                methodMeta.setCalledMethods(new ArrayList<>());


                if (md.getBody().isPresent()) {
                    // Extract local variable declarations
                    md.getBody().get().findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class).forEach(varDecl -> {
                        varDecl.getVariables().forEach(var -> {
                            String typeStr;
                            try {
                                ResolvedType resolvedType = var.getType().resolve();
                                typeStr = resolvedType.describe();
                            } catch (Exception e) {
                                visitorLogger.warn("[CMV] Could not resolve type for local variable {} in method {} in class {}. Using declared type: {}. Error: {}", var.getNameAsString(), methodName, finalCurrentClassNameForLambda, var.getType().toString(), e.getMessage());
                                typeStr = var.getType().toString();
                            }
                            String name = var.getNameAsString();
                            localVariablesList.add(typeStr + " " + name);
                        });
                    });
                    visitorLogger.trace("[CMV] Method {}.{} - Local variables: {}", currentClassName, methodName, localVariablesList.size());

                    // Visit the method body to find MethodCallExpr instances
                    // This will trigger visit(MethodCallExpr n, Void arg) for calls within this method
                    visitorLogger.trace("[CMV] Method {}.{} - Visiting method body.", currentClassName, methodName);
                    md.getBody().get().accept(this, null); 
                    visitorLogger.trace("[CMV] Method {}.{} - Finished visiting method body. Called methods found by visitor: {}", currentClassName, methodName, methodMeta.getCalledMethods() != null ? methodMeta.getCalledMethods().size() : 0);

                } else {
                    visitorLogger.trace("[CMV] Method {}.{} has no body (e.g., abstract or interface method).", currentClassName, methodName);
                }
                // --- Parameter Annotations Extraction ---
                for (Parameter param : md.getParameters()) {
                    List<String> paramAnns = param.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList());
                    parameterAnnotationsList.add(paramAnns);
                }
                // --- End Call Flow & Local Variable Extraction ---

                DaoAnalyzer.DaoAnalysisResult daoResult = daoAnalyzer.analyze(md);
                List<DaoOperationDetail> daoOperations = daoResult.getOperations();

                methodMeta.setReturnType(returnTypeStr);
                methodMeta.setParameters(parameters);
                methodMeta.setAnnotations(annotations);
                methodMeta.setExceptionsThrown(exceptionsThrown);
                methodMeta.setVisibility(visibility);
                methodMeta.setStatic(isStatic);
                methodMeta.setAbstract(isAbstract);
                methodMeta.setPackageName(currentPackageName);
                methodMeta.setClassName(currentClassName);
                
                // calledMethods is now populated by the visitor's visit(MethodCallExpr)
                methodMeta.setLocalVariables(localVariablesList);
                methodMeta.setParameterAnnotations(parameterAnnotationsList);

                methodMeta.setDaoOperations(daoOperations);
                methodMeta.setSqlQueries(new ArrayList<>()); 
                methodMeta.setSqlTables(new ArrayList<>());  
                methodMeta.setSqlOperations(new ArrayList<>()); 
                
                methodMetadataList.add(methodMeta);
            }
            classMetadata.setMethods(methodMetadataList);
            this.currentMethodMetadata = null; // Clear after processing all methods of a class
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            super.visit(n, arg);
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
                        ResolvedType resolvedType = var.getType().resolve();
                        fieldTypeStr = resolvedType.describe();
                    } catch (Exception e) {
                        logger.warn("Could not resolve type for field {} in class {}. Using declared type: {}. Error: {}", var.getNameAsString(), classMetadata.getName(), var.getTypeAsString(), e.getMessage());
                        fieldTypeStr = var.getTypeAsString();
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
            classMetadata.setName(n.getNameAsString());
            classMetadata.setType("annotation");
            classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
            
            List<FieldMetadata> members = new ArrayList<>();
            // Iterate over AnnotationMemberDeclaration for annotation types
            n.getMembers().forEach(memberDeclaration -> {
                if (memberDeclaration instanceof AnnotationMemberDeclaration) {
                    AnnotationMemberDeclaration amd = (AnnotationMemberDeclaration) memberDeclaration;
                    FieldMetadata fieldMeta = new FieldMetadata();
                    fieldMeta.setName(amd.getNameAsString());
                    // Use asString() for AST Type, resolve if symbol solver is robust enough in the future
                    fieldMeta.setType(amd.getType().asString()); 
                    fieldMeta.setAnnotations(amd.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
                    // Annotation members are implicitly public and static final (for values)
                    fieldMeta.setVisibility("public"); 
                    // Default value if present
                    amd.getDefaultValue().ifPresent(val -> fieldMeta.setInitializer(val.toString()));
                    // Annotation members don't have traditional static/final keywords in their declaration 
                    // but their nature is somewhat constant-like. Not setting isStatic/isFinal here for now.
                    members.add(fieldMeta);
                }
            });
            classMetadata.setFields(members); // Store annotation members as fields
            // Clear methods if any were added by a super visit or previous logic, annotations don't have methods in this context.
            classMetadata.setMethods(new ArrayList<>()); 
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);
            if (currentMethodMetadata == null) {
                visitorLogger.trace("[CMV] MethodCallExpr visited outside of a current method context: {} in class {}", n.toString(), classMetadata != null ? classMetadata.getName() : "UNKNOWN_CLASS");
                return;
            }

            visitorLogger.trace("[CMV] Visiting MethodCallExpr: '{}' in method: '{}' in class: '{}'", 
                n.toString(), 
                currentMethodMetadata.getName(),
                (classMetadata != null && classMetadata.getName() != null) ? classMetadata.getName() : (currentClassName != null ? currentClassName : "UNKNOWN_CLASS"));

            // Ensure calledMethods list is initialized
            if (currentMethodMetadata.getCalledMethods() == null) {
                currentMethodMetadata.setCalledMethods(new ArrayList<>());
            }

            // Attempt to resolve the method call
            try {
                ResolvedMethodDeclaration resolvedMethod = n.resolve();
                String resolvedSignature = resolvedMethod.getQualifiedSignature();
                visitorLogger.trace("[CMV] Successfully resolved method call via n.resolve(). Signature: '{}'", resolvedSignature);
                currentMethodMetadata.getCalledMethods().add(resolvedSignature);

                visitorLogger.trace("[CMV] Resolved method '{}' has {} parameters.", resolvedMethod.getName(), resolvedMethod.getNumberOfParams());
                for (int i = 0; i < resolvedMethod.getNumberOfParams(); i++) {
                    ResolvedParameterDeclaration param = resolvedMethod.getParam(i);
                    visitorLogger.trace("[CMV]   Param {}: Name='{}', Type='{}', Variadic={}", 
                        i, 
                        param.getName(), 
                        param.getType().describe(), 
                        param.isVariadic());
                }

            } catch (Exception e) {
                visitorLogger.trace("[CMV] Failed to resolve method call '{}' using n.resolve(). Attempting fallback. Exception: {} - {}", n.getNameAsString(), e.getClass().getName(), e.getMessage());
                String fallbackSignature = tryToConstructUnresolvedSignature(n, this.packageName, this.currentClassName != null ? this.currentClassName : (classMetadata != null ? classMetadata.getName() : "UNKNOWN_CLASS"));
                visitorLogger.trace("[CMV] Fallback signature constructed: '{}'", fallbackSignature);
                currentMethodMetadata.getCalledMethods().add(fallbackSignature);
            }
        }

        private String tryToConstructUnresolvedSignature(MethodCallExpr n, String currentPackageName, String currentClassName) {
            visitorLogger.trace("[CMV-Fallback] Attempting to construct unresolved signature for: '{}' (Class: {}, Package: {})", n.toString(), currentClassName, currentPackageName);

            String methodName = n.getNameAsString();
            String scopeName = null; // Initialize to null

            if (n.getScope().isPresent()) {
                Expression scopeExpr = n.getScope().get();
                scopeName = scopeExpr.toString(); // Initial raw scope
                visitorLogger.trace("[CMV-Fallback] Method call has explicit scope: '{}'", scopeName);
                try {
                    ResolvedType resolvedType = scopeExpr.calculateResolvedType();
                    scopeName = resolvedType.describe(); // Attempt to get fully qualified name
                    visitorLogger.trace("[CMV-Fallback] Scope expression resolved to type: '{}'", scopeName);
                } catch (Exception e) {
                    visitorLogger.trace("[CMV-Fallback] Could not resolve type of scope expression '{}'. Using raw expression. Error: {} - {}", scopeExpr.toString(), e.getClass().getName(), e.getMessage());
                    // scopeName remains the raw scopeExpr.toString()
                }
            } else {
                visitorLogger.trace("[CMV-Fallback] No explicit scope. Assuming current class or import. Current class for context: '{}'", currentClassName);
                scopeName = currentClassName; // Default to current class name if no explicit scope
            }

            visitorLogger.trace("[CMV-Fallback] Tentative scope for signature: '{}'", scopeName);

            List<String> argTypes = new ArrayList<>();
            for (Expression argExpr : n.getArguments()) {
                try {
                    ResolvedType argType = argExpr.calculateResolvedType();
                    argTypes.add(argType.describe());
                    visitorLogger.trace("[CMV-Fallback] Resolved argument type: '{}' for expr '{}'", argType.describe(), argExpr.toString());
                } catch (Exception e) {
                    visitorLogger.trace("[CMV-Fallback] Could not resolve type of argument '{}'. Using UNKNOWN_PARAM_TYPE. Error: {} - {}", argExpr.toString(), e.getClass().getName(), e.getMessage());
                    argTypes.add("UNKNOWN_PARAM_TYPE"); // Or use argExpr.toString() if preferred for more detail
                }
            }

            String params = String.join(", ", argTypes);
            String finalSignature = "UNRESOLVED_CALL: " + (scopeName != null && !scopeName.trim().isEmpty() ? scopeName + "." : "") + methodName + "(" + params + ")";
            visitorLogger.trace("[CMV-Fallback] Final constructed signature: '{}'", finalSignature);
            return finalSignature;
        }

        private String determineClassType(ClassOrInterfaceDeclaration n, String currentPackageName) {
            // Prioritize Spring stereotype annotations
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Controller") || a.getNameAsString().endsWith("RestController"))) return "controller";
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Service"))) return "service";
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Repository"))) return "repository";

            // Heuristic: Repository by name or by interface extension
            if (n.getNameAsString().endsWith("Repository")) return "repository";
            if (n.getExtendedTypes().stream().anyMatch(t -> t.getNameAsString().matches("(JpaRepository|CrudRepository|PagingAndSortingRepository)"))) return "repository";
            if (n.getImplementedTypes().stream().anyMatch(t -> t.getNameAsString().matches("(JpaRepository|CrudRepository|PagingAndSortingRepository)"))) return "repository";

            // Check for SOAP Endpoint annotation (e.g., Spring WS @Endpoint)
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("Endpoint"))) return "soap";
            // Add check for JAX-WS @WebService if needed
            // if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("WebService"))) return "soap";

            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Component"))) {
                 if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Configuration"))) return "config";
                 return "component"; // Generic component if not a more specific stereotype like Repository
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
            // If in a .model, .domain, or .dto package and not identified as anything else, it's a model/dto.
            boolean isInModelPackage = currentPackageName != null && 
                (currentPackageName.contains(".model") || 
                 currentPackageName.contains(".domain") || 
                 currentPackageName.contains(".dto"));

            if (isInModelPackage) {
                return "model";
            }

            // If no specific stereotype, then determine by structure
            if (n.isInterface()) return "interface";
            // Abstract class check removed as it might overlap with stereotypes; type will be stereotype if applicable.
            // if (n.isAbstract()) return "abstract"; // Can be an abstract service, controller, etc.
            
            if (n.getNameAsString().endsWith("Test") || n.getNameAsString().startsWith("Test")) return "test";
            
            return "class"; // Default to class if no other type matches
        }

        private String getVisibility(MethodDeclaration md) {
            if (md.isPublic()) return "public";
            if (md.isProtected()) return "protected";
            if (md.isPrivate()) return "private";
            return "default"; // package-private
        }

        // Helper method to get visibility string from Modifiers
        private String getVisibilityFromModifiers(NodeList<Modifier> modifiers) {
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC)) return "public";
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PROTECTED)) return "protected";
            if (modifiers.stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE)) return "private";
            return "default"; // package-private
        }

        public ClassMetadata getClassMetadata() {
            return classMetadata;
        }
    }
} 