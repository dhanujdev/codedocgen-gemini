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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ParserConfiguration;
import com.codedocgen.parser.DaoAnalyzer;
import com.codedocgen.model.DaoOperationDetail;

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
            if (pomFileForCompile.exists() && pomFileForCompile.isFile()) {
                logger.info("Found pom.xml, attempting to compile the project via MavenBuildService.");
                try {
                    MavenExecutionResult compileResult = mavenBuildService.runMavenCommand(projectDir, "compile", "-DskipTests", "-q");
                    logger.info("Maven 'compile' command finished with exit code: {}", compileResult.getExitCode());

                    if (!compileResult.isSuccess()) {
                        logger.warn("Maven compile command failed with exit code {}. Generated sources might be missing or incomplete.", compileResult.getExitCode());
                        String output = compileResult.getOutput();
                        
                        // Check for specific dependency errors and try to fix them
                        if (output.contains("javax.activation.MimeTypeParseException") || 
                            output.contains("Unable to find artifact") ||
                            output.contains("Dependency resolution failed")) {
                            
                            logger.info("Specific error pattern found in Maven output. Attempting to fix missing dependencies with Maven dependency resolution...");
                            try {
                                MavenExecutionResult resolveResult = mavenBuildService.runMavenCommand(projectDir, "dependency:resolve", "-U", "-DskipTests", "-q");
                                logger.info("Maven 'dependency:resolve' command finished with exit code: {}", resolveResult.getExitCode());

                                if (resolveResult.isSuccess()) {
                                    logger.info("Dependency resolution successful, retrying compilation with -offline...");
                                    MavenExecutionResult retryCompileResult = mavenBuildService.runMavenCommand(projectDir,
                                            "compile",
                                            "-DskipTests",
                                            "-q",
                                            "-Djavax.activation.debug=true",
                                            "-offline"
                                    );
                                    logger.info("Retry Maven 'compile -offline' command finished with exit code: {}", retryCompileResult.getExitCode());
                                    if (!retryCompileResult.isSuccess()) {
                                        logger.warn("Retry compile command also failed with exit code {}.", retryCompileResult.getExitCode());
                                    }
                                } else {
                                    logger.warn("Dependency resolution failed with exit code {}. Skipping retry compile.", resolveResult.getExitCode());
                                }
                            } catch (Exception e) {
                                logger.warn("Error during Maven dependency resolution/retry: {}", e.getMessage());
                                if (e instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        } else {
                            logger.info("No specific known error pattern found in Maven compile output for automatic retry. Output was:\n{}", output);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error("Error while running Maven commands for Symbol Solver pre-step: {}", e.getMessage(), e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                logger.info("No pom.xml found in {}, skipping Maven pre-compile step.", projectDir.getAbsolutePath());
            }

            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            
            // Add ReflectionTypeSolver for JDK classes - prefer classloader
            logger.info("Attempting to add ReflectionTypeSolver (preferring context classloader)...");
            combinedTypeSolver.add(new ReflectionTypeSolver(true)); // Pass true to prefer classloader
            logger.info("ReflectionTypeSolver added to CombinedTypeSolver.");

            // Add main source directories FIRST
            File srcMainJava = new File(projectDir, "src/main/java");
            if (srcMainJava.exists()) {
                logger.info("Adding JavaParserTypeSolver for source root: {}", srcMainJava.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(srcMainJava));
            }
            File testJava = new File(projectDir, "src/test/java");
            if (testJava.exists()) {
                logger.info("Adding JavaParserTypeSolver for source root: {}", testJava.getAbsolutePath());
                combinedTypeSolver.add(new JavaParserTypeSolver(testJava));
            }
            
            // Add common generated source directories
            String[] generatedSourceDirs = {
                "target/generated-sources/jaxb",
                "target/generated-sources/annotations", // Common for annotation processors
                "target/generated-sources/apt",     // Another common for annotation processors
                "build/generated/sources/annotationProcessor/java/main" // Gradle default
            };

            for (String genDir : generatedSourceDirs) {
                File generatedSrcDir = new File(projectDir, genDir);
                if (generatedSrcDir.exists() && generatedSrcDir.isDirectory()) {
                    logger.info("Adding JavaParserTypeSolver for generated source root: {}", generatedSrcDir.getAbsolutePath());
                    combinedTypeSolver.add(new JavaParserTypeSolver(generatedSrcDir));
                }
            }

            // Fallback if no standard source roots found (AFTER attempting specific ones)
            if (!srcMainJava.exists() && !testJava.exists()) {
                boolean generatedSrcFound = false;
                for (String genDir : generatedSourceDirs) {
                    if (new File(projectDir, genDir).exists()) {
                        generatedSrcFound = true;
                        break;
                    }
                }
                if (!generatedSrcFound) { // Only add project root if no other source/gen-source was added
                     logger.warn("No standard or generated source roots found. Adding project root as JavaParserTypeSolver: {}", projectDir.getAbsolutePath());
                     combinedTypeSolver.add(new JavaParserTypeSolver(projectDir));
                }
            }

            // Attempt to add JarTypeSolvers for Maven project dependencies
            File pomFile = new File(projectDir, "pom.xml");
            if (pomFile.exists() && pomFile.isFile()) {
                logger.info("Found pom.xml, attempting to resolve Maven dependencies for Symbol Solver.");
                try {
                    String osName = System.getProperty("os.name").toLowerCase();
                    String mvnCommand = osName.contains("win") ? "mvn.cmd" : "mvn";
                    
                    ProcessBuilder pb = new ProcessBuilder(
                        mvnCommand,
                        "dependency:build-classpath",
                        "-Dmdep.outputFile=" + CLASSPATH_OUTPUT_FILE,
                        "-Dmdep.pathSeparator=;",
                        "-DincludeScope=compile",
                        "-q"
                    );
                    pb.directory(projectDir);
                    pb.redirectErrorStream(true);

                    logger.info("Executing Maven command: {}", String.join(" ", pb.command()));
                    Process process = pb.start();
                    
                    StringBuilder mavenOutput = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            mavenOutput.append(line).append(System.lineSeparator());
                        }
                    }

                    int exitCode = process.waitFor();
                    // Log Maven output regardless of exit code for better diagnostics
                    logger.info("Maven command 'dependency:build-classpath' finished with exit code: {}. Output:\n{}", exitCode, mavenOutput.toString());

                    File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE);
                    if (exitCode == 0 && classpathFile.exists() && classpathFile.isFile()) {
                        String classpath = new String(Files.readAllBytes(classpathFile.toPath()), StandardCharsets.UTF_8).trim();
                        logger.info("Raw classpath from {}: '{}'", CLASSPATH_OUTPUT_FILE, classpath);

                        if (classpath != null && !classpath.isEmpty()) {
                            String[] jarPaths = classpath.split(";");
                            logger.info("Found {} potential JAR paths in classpath.", jarPaths.length);
                            for (String jarPath : jarPaths) {
                                String trimmedJarPath = jarPath != null ? jarPath.trim() : "";
                                if (!trimmedJarPath.isEmpty()) {
                                    logger.debug("Processing classpath entry: '{}'", trimmedJarPath);
                                    File jarFile = new File(trimmedJarPath);
                                    if (jarFile.exists() && jarFile.isFile()) {
                                        try {
                                            logger.info("Adding JarTypeSolver for dependency: {}", jarFile.getAbsolutePath());
                                            combinedTypeSolver.add(new JarTypeSolver(jarFile));
                                        } catch (Exception e) { // Catching generic Exception to see any JarTypeSolver init errors
                                            logger.warn("Failed to add JarTypeSolver for {}: {} - {}", jarFile.getAbsolutePath(), e.getClass().getName(), e.getMessage());
                                        }
                                    } else {
                                        logger.warn("Dependency JAR path does not exist or is not a file: {}", trimmedJarPath);
                                    }
                                } else {
                                    logger.debug("Skipping empty or null classpath entry.");
                                }
                            }
                        } else {
                            logger.warn("Maven generated an empty or null classpath string in file: {}", classpathFile.getAbsolutePath());
                        }
                        if (!classpathFile.delete()) {
                            logger.warn("Failed to delete temporary classpath file: {}", classpathFile.getAbsolutePath());
                        }
                    } else {
                        logger.warn("Maven dependency:build-classpath command failed (exit code: {}) or classpath file {} not found or is not a file.", exitCode, classpathFile.getAbsolutePath());
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error("Error while resolving Maven dependencies for Symbol Solver: {}", e.getMessage(), e);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE);
                    if (classpathFile.exists()) {
                        classpathFile.delete();
                    }
                }
            } else {
                logger.info("No pom.xml found, skipping Maven dependency resolution for Symbol Solver.");
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
        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            ClassMetadataVisitor visitor = new ClassMetadataVisitor(javaFile.getAbsolutePath());
            visitor.visit(cu, null);
            return visitor.getClassMetadata();
        } catch (IOException e) {
            logger.error("IOException parsing file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("General error parsing file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
        }
        return null;
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
        private ClassMetadata classMetadata;
        private final String filePath;

        public ClassMetadataVisitor(String filePath) {
            this.filePath = filePath;
            this.classMetadata = new ClassMetadata();
            this.classMetadata.setFilePath(filePath);
        }

        private void processMethods(List<MethodDeclaration> methods, String currentPackageName, String currentClassName) {
            if (methods == null) return;
            List<MethodMetadata> methodMetadataList = new ArrayList<>();
            for (MethodDeclaration md : methods) {
                String methodName = md.getNameAsString();
                String returnType = md.getType().toString();
                List<String> parameters = md.getParameters().stream()
                        .map(p -> p.getType().toString() + " " + p.getNameAsString())
                        .collect(Collectors.toList());
                List<String> annotations = md.getAnnotations().stream()
                        .map(AnnotationExpr::toString)
                        .collect(Collectors.toList());
                List<String> exceptionsThrown = md.getThrownExceptions().stream()
                        .map(type -> type.asString())
                        .collect(Collectors.toList());
                String visibility = getVisibility(md);
                boolean isStatic = md.isStatic();
                boolean isAbstract = md.isAbstract();

                // --- Call Flow & Local Variable Extraction ---
                List<String> calledMethodsList = new ArrayList<>();
                List<String> localVariablesList = new ArrayList<>();
                List<List<String>> parameterAnnotationsList = new ArrayList<>();

                if (md.getBody().isPresent()) {
                    // Extract local variable declarations
                    md.getBody().get().findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class).forEach(varDecl -> {
                        varDecl.getVariables().forEach(var -> {
                            String type = varDecl.getElementType().asString();
                            String name = var.getNameAsString();
                            localVariablesList.add(type + " " + name);
                        });
                    });
                    // Extract called methods
                    md.getBody().get().findAll(MethodCallExpr.class).forEach(call -> {
                        try {
                            ResolvedMethodDeclaration resolved = call.resolve();
                            calledMethodsList.add(resolved.getQualifiedSignature());
                        } catch (Exception ex) { // Catch broader exceptions like UnsolvedSymbolException or UnsupportedOperationException
                            String calledMethodName = call.getNameAsString();
                            String fallbackCalledName = calledMethodName;
                            if (call.getScope().isPresent()) {
                                com.github.javaparser.ast.expr.Expression scope = call.getScope().get();
                                try {
                                    // Attempt to resolve the type of the scope
                                    com.github.javaparser.resolution.types.ResolvedType scopeType = scope.calculateResolvedType();
                                    if (scopeType.isReferenceType()) {
                                        fallbackCalledName = scopeType.asReferenceType().getQualifiedName() + "." + calledMethodName;
                                    } else {
                                        fallbackCalledName = scope.toString() + "." + calledMethodName; // Fallback for non-reference types
                                    }
                                } catch (Exception e) { // Scope resolution failed
                                    logger.debug("Fallback scope resolution failed for scope '{}' of call '{}' in {}.{}: {}", 
                                        scope.toString(), call.toString(), currentClassName, methodName, e.getMessage());
                                    fallbackCalledName = scope.toString() + "." + calledMethodName;
                                }
                            } else {
                                // No scope, attempt to use current class FQN
                                if (currentPackageName != null && !currentPackageName.isEmpty()) {
                                    fallbackCalledName = currentPackageName + "." + currentClassName + "." + calledMethodName;
                                } else {
                                    fallbackCalledName = currentClassName + "." + calledMethodName;
                                }
                            }
                            calledMethodsList.add(fallbackCalledName);
                            logger.warn("SymbolSolver failed for call '{}' in method '{}.{}' (Class: {}). Exception: {}. Using fallback: {}",
                                    call.toString(), methodName, md.getBegin().map(p -> "Line " + p.line).orElse("N/A"),
                                    currentClassName, ex.getClass().getSimpleName() + ": " + ex.getMessage(), fallbackCalledName);
                        }
                    });
                }
                // --- Parameter Annotations Extraction ---
                for (Parameter param : md.getParameters()) {
                    List<String> paramAnns = param.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList());
                    parameterAnnotationsList.add(paramAnns);
                }
                // --- End Call Flow & Local Variable Extraction ---

                DaoAnalyzer.DaoAnalysisResult daoResult = daoAnalyzer.analyze(md);
                List<DaoOperationDetail> daoOperations = daoResult.getOperations();

                MethodMetadata methodMeta = new MethodMetadata();
                methodMeta.setName(methodName);
                methodMeta.setReturnType(returnType);
                methodMeta.setParameters(parameters);
                methodMeta.setAnnotations(annotations);
                methodMeta.setExceptionsThrown(exceptionsThrown);
                methodMeta.setVisibility(visibility);
                methodMeta.setStatic(isStatic);
                methodMeta.setAbstract(isAbstract);
                methodMeta.setPackageName(currentPackageName); // Use passed currentPackageName
                methodMeta.setClassName(currentClassName);     // Use passed currentClassName
                
                methodMeta.setCalledMethods(calledMethodsList);
                methodMeta.setLocalVariables(localVariablesList);
                methodMeta.setParameterAnnotations(parameterAnnotationsList);
                // methodMeta.setExternalCalls(externalCalls); // externalCalls logic not shown here, assuming placeholder or future task

                methodMeta.setDaoOperations(daoOperations);
                // Set deprecated fields (ideally to empty lists or null if they are truly deprecated)
                methodMeta.setSqlQueries(new ArrayList<>()); // Deprecated
                methodMeta.setSqlTables(new ArrayList<>());  // Deprecated
                methodMeta.setSqlOperations(new ArrayList<>()); // Deprecated
                
                methodMetadataList.add(methodMeta);
            }
            classMetadata.setMethods(methodMetadataList);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            super.visit(n, arg);
            if (classMetadata == null) classMetadata = new ClassMetadata();
            classMetadata.setFilePath(this.filePath);
            String packageName = n.findCompilationUnit().flatMap(CompilationUnit::getPackageDeclaration).map(pd -> pd.getName().asString()).orElse("");
            classMetadata.setPackageName(packageName);
            classMetadata.setName(n.getNameAsString());
            classMetadata.setType(determineClassType(n));
            classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
            classMetadata.setAbstract(n.isAbstract());
            classMetadata.setInterface(n.isInterface());

            List<FieldMetadata> fieldList = new ArrayList<>();
            for (FieldDeclaration field : n.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    FieldMetadata fm = new FieldMetadata();
                    fm.setName(var.getNameAsString());
                    fm.setType(var.getType().toString());
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

            processMethods(n.getMethods(), packageName, n.getNameAsString());
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            super.visit(n, arg);
            if (classMetadata == null) classMetadata = new ClassMetadata();
            classMetadata.setFilePath(this.filePath);
            String packageName = n.findCompilationUnit().flatMap(CompilationUnit::getPackageDeclaration).map(pd -> pd.getName().asString()).orElse("");
            classMetadata.setPackageName(packageName);
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

            processMethods(n.getMethods(), packageName, n.getNameAsString());
        }

        @Override
        public void visit(AnnotationDeclaration n, Void arg) {
            super.visit(n, arg);
            if (classMetadata == null) {
                classMetadata = new ClassMetadata();
                classMetadata.setName(n.getNameAsString());
                 n.getFullyQualifiedName().ifPresent(fqn -> {
                    int lastDot = fqn.lastIndexOf('.');
                    if (lastDot > 0) {
                        classMetadata.setPackageName(fqn.substring(0, lastDot));
                    } else {
                         classMetadata.setPackageName("");
                    }
                });
                classMetadata.setType("annotation"); // Or "interface" if it's an annotation interface
                classMetadata.setAnnotations(n.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList()));
                classMetadata.setFilePath(this.filePath);
            }
        }

        private String determineClassType(ClassOrInterfaceDeclaration n) {
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
            if (n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().endsWith("Entity"))) return "entity";

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