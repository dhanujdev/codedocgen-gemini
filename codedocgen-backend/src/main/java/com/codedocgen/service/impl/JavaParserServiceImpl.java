package com.codedocgen.service.impl;

// ... existing imports ...
import com.codedocgen.dto.MavenExecutionResult;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.model.FieldMetadata;
import com.codedocgen.model.ParameterMetadata;
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
import com.codedocgen.util.SystemInfoUtil;

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
    private static final DaoAnalyzer daoAnalyzerInstance = new DaoAnalyzer();
    private static final String TEST_PATH_FRAGMENT = File.separator + "src" + File.separator + "test" + File.separator + "java";

    @Autowired
    private MavenBuildService mavenBuildService;

    private File currentProjectDir;
    private JavaSymbolSolver symbolResolver;

    private void ensureSymbolSolverInitialized(File projectDir) {
        if (this.symbolResolver == null || !projectDir.equals(this.currentProjectDir)) {
            logger.info("Initializing JavaParser Symbol Solver for project: {}", projectDir.getAbsolutePath());
            this.currentProjectDir = projectDir;

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

            // Check if it's a Maven project
            File pomFile = new File(projectDir, "pom.xml");
            boolean isMavenProject = pomFile.exists() && pomFile.isFile();
            
            // Check if it's a Gradle project
            File gradleBuildFile = new File(projectDir, "build.gradle");
            File gradleKtsBuildFile = new File(projectDir, "build.gradle.kts");
            boolean isGradleProject = gradleBuildFile.exists() || gradleKtsBuildFile.exists();
            
            if (isGradleProject) {
                logger.info("Detected Gradle project in {}. Handling source directories.", projectDir.getAbsolutePath());
                // Add standard Gradle source directories
                addGradleSourceDirectories(projectDir, combinedTypeSolver);
            } else if (isMavenProject) {
                logger.info("Detected Maven project in {}. Running lightweight Maven commands.", projectDir.getAbsolutePath());
                
                // Process Maven project source directories and dependencies
                processMavenProject(projectDir, combinedTypeSolver);
            } else {
                // Handle projects with no standard build system
                logger.info("No standard build system detected. Using default source directories.");
                addDefaultSourceDirectories(projectDir, combinedTypeSolver);
            }

            this.symbolResolver = new JavaSymbolSolver(combinedTypeSolver);
            ParserConfiguration config = new ParserConfiguration().setSymbolResolver(this.symbolResolver);
            StaticJavaParser.setConfiguration(config);
            logger.info("JavaParser Symbol Solver initialized and configuration set for project: {}", projectDir.getAbsolutePath());
        }
    }
    
    /**
     * Process a Maven project more efficiently, avoiding heavy operations
     */
    private void processMavenProject(File projectDir, CombinedTypeSolver combinedTypeSolver) {
        // First add main source directories (these exist regardless of build success)
        addMavenSourceDirectories(projectDir, combinedTypeSolver);
        
        try {
            // Only run dependency:build-classpath (much lighter than compile or install)
            logger.info("Building classpath using MavenBuildService (compile scope only).");
            MavenExecutionResult classpathResult = mavenBuildService.runMavenCommandWithExplicitVersion(projectDir, null, 
                "dependency:build-classpath", 
                "-Dmdep.outputFile=" + CLASSPATH_OUTPUT_FILE,
                "-Dmdep.pathSeparator=" + File.pathSeparator,
                "-Dmdep.includeScopes=compile", // Only include compile scope - reduce size
                "-q"
            );
            logger.info("Maven 'dependency:build-classpath' finished with exit code: {}.", classpathResult.getExitCode());
            
            // Process the classpath file
            File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE);
            if (classpathResult.getExitCode() == 0 && classpathFile.exists() && classpathFile.isFile()) {
                processClasspathFile(classpathFile, combinedTypeSolver);
            } else {
                logger.warn("Maven dependency:build-classpath command failed or classpath file not found.");
            }
        } catch (Exception e) {
            logger.error("Error while resolving Maven dependencies for Symbol Solver: {}", e.getMessage());
        } finally {
            // Clean up temporary file
            File classpathFile = new File(projectDir, CLASSPATH_OUTPUT_FILE);
            if (classpathFile.exists()) {
                if (!classpathFile.delete()) {
                    logger.warn("Failed to delete temporary classpath file: {}", classpathFile.getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * Add standard Maven source directories to the solver
     */
    private void addMavenSourceDirectories(File projectDir, CombinedTypeSolver combinedTypeSolver) {
        // Add main source directory first
        File srcMainJava = new File(projectDir, "src/main/java");
        if (srcMainJava.exists() && srcMainJava.isDirectory()) {
            logger.info("Adding JavaParserTypeSolver for source root: {}", srcMainJava.getAbsolutePath());
            try {
                combinedTypeSolver.add(new JavaParserTypeSolver(srcMainJava));
            } catch (Exception e) {
                logger.warn("Failed to add source directory {}: {}", srcMainJava.getAbsolutePath(), e.getMessage());
            }
        }
        
        // Add generated source directories
        String[] generatedSourceDirs = {
            "target/generated-sources/annotations",
            "target/generated-sources/jaxb",
            "target/generated-sources/apt"
        };
        
        for (String genDir : generatedSourceDirs) {
            File generatedSrcDir = new File(projectDir, genDir);
            if (generatedSrcDir.exists() && generatedSrcDir.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for generated source root: {}", generatedSrcDir.getAbsolutePath());
                try {
                    combinedTypeSolver.add(new JavaParserTypeSolver(generatedSrcDir));
                } catch (Exception e) {
                    logger.warn("Failed to add generated source directory {}: {}", generatedSrcDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Add standard Gradle source directories to the solver
     */
    private void addGradleSourceDirectories(File projectDir, CombinedTypeSolver combinedTypeSolver) {
        // Add standard Gradle source directories
        String[] gradleSrcDirs = {
            "src/main/java",
            "build/generated/sources/annotationProcessor/java/main",
            "build/generated/sources/jaxb/java/main"
        };
        
        for (String srcDir : gradleSrcDirs) {
            File dir = new File(projectDir, srcDir);
            if (dir.exists() && dir.isDirectory()) {
                logger.info("Adding JavaParserTypeSolver for Gradle source root: {}", dir.getAbsolutePath());
                try {
                    combinedTypeSolver.add(new JavaParserTypeSolver(dir));
                } catch (Exception e) {
                    logger.warn("Failed to add Gradle source directory {}: {}", dir.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Add default source directories for non-standard projects
     */
    private void addDefaultSourceDirectories(File projectDir, CombinedTypeSolver combinedTypeSolver) {
        // Last resort: try to identify source directories by convention
        File srcDir = new File(projectDir, "src");
        if (srcDir.exists() && srcDir.isDirectory()) {
            logger.info("Adding JavaParserTypeSolver for source root: {}", srcDir.getAbsolutePath());
            try {
                combinedTypeSolver.add(new JavaParserTypeSolver(srcDir));
            } catch (Exception e) {
                logger.warn("Failed to add source directory {}: {}", srcDir.getAbsolutePath(), e.getMessage());
            }
        } else {
            // If no src directory, use project root as a last resort
            logger.info("No standard source directories found. Using project root: {}", projectDir.getAbsolutePath());
            try {
                combinedTypeSolver.add(new JavaParserTypeSolver(projectDir));
            } catch (Exception e) {
                logger.warn("Failed to add project directory {}: {}", projectDir.getAbsolutePath(), e.getMessage());
            }
        }
    }
    
    /**
     * Process the classpath file and add jar dependencies to the solver
     */
    private void processClasspathFile(File classpathFile, CombinedTypeSolver combinedTypeSolver) throws IOException {
        String classpath = new String(Files.readAllBytes(classpathFile.toPath()), StandardCharsets.UTF_8).trim();
        logger.debug("Classpath from {}: '{}'", CLASSPATH_OUTPUT_FILE, classpath);
        
        if (classpath == null || classpath.isEmpty()) {
            logger.warn("Empty classpath file generated");
            return;
        }
        
        String[] jarPaths = classpath.split(File.pathSeparator);
        logger.info("Found {} potential JAR paths in classpath.", jarPaths.length);
        
        // Limit the number of JARs to process to avoid excessive memory usage
        int maxJars = Math.min(jarPaths.length, 100); // Limit to first 100 JARs
        int processedJars = 0;
        
        for (int i = 0; i < maxJars; i++) {
            String jarPath = jarPaths[i].trim();
            if (jarPath.isEmpty()) continue;
            
            File jarFile = new File(jarPath);
            if (jarFile.exists() && jarFile.isFile()) {
                try {
                    logger.debug("Adding JarTypeSolver for dependency: {}", jarFile.getAbsolutePath());
                    combinedTypeSolver.add(new JarTypeSolver(jarFile));
                    processedJars++;
                } catch (Exception e) {
                    logger.warn("Failed to add JarTypeSolver for {}: {}", jarFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
        
        logger.info("Successfully added {} JAR dependencies to the solver", processedJars);
        if (jarPaths.length > maxJars) {
            logger.info("Note: Limited processing to {} out of {} JARs to conserve resources", maxJars, jarPaths.length);
        }
    }

    @Override
    public List<ClassMetadata> parseProject(File projectDir) {
        return parseProject(projectDir, null);
    }

    @Override
    public ClassMetadata parseFile(File javaFile) {
        return parseFileWithResolver(javaFile, this.symbolResolver, daoAnalyzerInstance, javaFile.getAbsolutePath());
    }

    /**
     * Parse a file with a specific resolver, DAO analyzer, and file path
     */
    private ClassMetadata parseFileWithResolver(File javaFile, JavaSymbolSolver resolver, DaoAnalyzer daoAnalyzer, String filePath) {
        try (FileInputStream in = new FileInputStream(javaFile)) {
            CompilationUnit cu = StaticJavaParser.parse(in);
            ClassMetadataVisitor visitor = new ClassMetadataVisitor(filePath, daoAnalyzer);
            visitor.visit(cu, null);
            return visitor.getClassMetadata();
        } catch (IOException e) {
            logger.error("IOException parsing file {}: {}", javaFile.getAbsolutePath(), e.getMessage());
        } catch (Exception e) {
            logger.error("General error parsing file {}: {}", javaFile.getAbsolutePath(), e.getMessage());
        }
        return null;
    }

    // Overloaded method to collect parse warnings
    public List<ClassMetadata> parseProject(File projectDir, List<String> parseWarnings) {
        ensureSymbolSolverInitialized(projectDir);
        List<ClassMetadata> allClassMetadata = new ArrayList<>();
        Path projectPath = projectDir.toPath(); // Root path of the project

        try (Stream<Path> javaFiles = Files.walk(projectPath)
                .filter(path -> path.toString().endsWith(".java") && 
                               !path.toString().contains(TEST_PATH_FRAGMENT) && // Exclude test files
                               Files.isRegularFile(path))) {
            javaFiles.forEach(javaFilePath -> { // javaFilePath is an absolute Path
                try {
                    // Calculate relative path here and pass it to the visitor/parser
                    String relativePath = projectPath.relativize(javaFilePath).toString();
                    ClassMetadata classMetadata = parseFileWithResolver(javaFilePath.toFile(), this.symbolResolver, this.daoAnalyzerInstance, relativePath);
                    if (classMetadata != null) {
                        allClassMetadata.add(classMetadata);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing file {}: {}", javaFilePath, e.getMessage());
                    if (parseWarnings != null) {
                        parseWarnings.add(javaFilePath.toString() + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Error walking through project directory {}: {}", projectPath, e.getMessage());
            if (parseWarnings != null) {
                parseWarnings.add("Error walking project directory: " + e.getMessage());
            }
        }
        return allClassMetadata;
    }

    private static class ClassMetadataVisitor extends VoidVisitorAdapter<Void> {
        private ClassMetadata classMetadata;
        private final String filePath;
        private final DaoAnalyzer daoAnalyzer;

        public ClassMetadataVisitor(String filePath, DaoAnalyzer daoAnalyzer) {
            this.filePath = filePath;
            this.daoAnalyzer = daoAnalyzer;
            this.classMetadata = new ClassMetadata();
            this.classMetadata.setFilePath(filePath);
        }

        // ... rest of the ClassMetadataVisitor class remains the same
        // Include your existing implementation of processMethods, visit methods, etc.
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
                
                // Convert String parameters to ParameterMetadata objects
                List<ParameterMetadata> paramMetadataList = new ArrayList<>();
                if (parameters != null) {
                    for (String param : parameters) {
                        String[] parts = param.trim().split(" ");
                        if (parts.length >= 2) {
                            ParameterMetadata paramMeta = new ParameterMetadata();
                            paramMeta.setType(parts[0]);
                            paramMeta.setName(parts[1]);
                            paramMetadataList.add(paramMeta);
                        }
                    }
                }
                methodMeta.setParameters(paramMetadataList);
                
                methodMeta.setAnnotations(annotations);
                methodMeta.setExceptionsThrown(exceptionsThrown);
                methodMeta.setVisibility(visibility);
                methodMeta.setStatic(isStatic);
                methodMeta.setAbstract(isAbstract);
                methodMeta.setPackageName(currentPackageName); // Use passed currentPackageName
                methodMeta.setClassName(currentClassName);     // Use passed currentClassName
                
                methodMeta.setCalledMethods(calledMethodsList);
                
                // Convert String localVariables to VariableMetadata objects
                List<MethodMetadata.VariableMetadata> varMetadataList = new ArrayList<>();
                if (localVariablesList != null) {
                    for (String var : localVariablesList) {
                        String[] parts = var.trim().split(" ");
                        if (parts.length >= 2) {
                            MethodMetadata.VariableMetadata varMeta = 
                                new MethodMetadata.VariableMetadata(parts[0], parts[1]);
                            varMetadataList.add(varMeta);
                        }
                    }
                }
                methodMeta.setLocalVariables(varMetadataList);
                
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