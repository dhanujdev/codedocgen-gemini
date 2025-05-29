package com.codedocgen.service.impl;

import com.codedocgen.model.LogStatement;
import com.codedocgen.model.LogVariable;
import com.codedocgen.service.LoggerInsightsService;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LoggerInsightsServiceImpl implements LoggerInsightsService {

    private static final Logger logger = LoggerFactory.getLogger(LoggerInsightsServiceImpl.class);
    private static final Set<String> LOGGER_METHOD_NAMES = Set.of("trace", "debug", "info", "warn", "error");

    private final Pattern piiSpecificKeywordsPattern;
    private final Pattern pciKeywordsPattern;
    private final Pattern generalSensitiveKeywordsPattern;

    public LoggerInsightsServiceImpl(
            @Value("${app.pii-keywords-regex}") String piiKeywordsRegex,
            @Value("${app.pci-keywords-regex}") String pciKeywordsRegex,
            @Value("${app.general-sensitive-keywords-regex}") String generalKeywordsRegex) {
        this.piiSpecificKeywordsPattern = Pattern.compile(piiKeywordsRegex.trim(), Pattern.CASE_INSENSITIVE);
        this.pciKeywordsPattern = Pattern.compile(pciKeywordsRegex.trim(), Pattern.CASE_INSENSITIVE);
        this.generalSensitiveKeywordsPattern = Pattern.compile(generalKeywordsRegex.trim(), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public List<LogStatement> getLogInsights(String projectPath) {
        List<LogStatement> logStatements = new ArrayList<>();
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(projectPath))); 
        // Add more source directories if necessary, e.g. for dependencies for more accurate type solving
        // combinedTypeSolver.add(new JavaParserTypeSolver(new File(projectPath + "/src/main/java")));


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        try (Stream<Path> paths = Files.walk(Paths.get(projectPath))) {
            List<File> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File javaFile : javaFiles) {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(javaFile);
                    new LogVisitor(combinedTypeSolver).visit(cu, logStatements);
                } catch (IOException e) {
                    logger.error("Failed to parse Java file: {}", javaFile.getAbsolutePath(), e);
                } catch (ParseProblemException e) {
                    logger.error("Parsing problem in Java file: {}. Details: {}", javaFile.getAbsolutePath(), e.getMessage());
                } catch (Exception e) { // Catch other runtime exceptions from symbol solving
                     logger.error("Error processing file {}: {}", javaFile.getAbsolutePath(), e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Error walking through project path: {}", projectPath, e);
        }
        return logStatements;
    }

    private class LogVisitor extends VoidVisitorAdapter<List<LogStatement>> {
        private final CombinedTypeSolver typeSolver;
        private String currentClassName = null;

        public LogVisitor(CombinedTypeSolver typeSolver) {
            this.typeSolver = typeSolver;
        }

        private void updateClassNameFromType(TypeDeclaration<?> typeDeclaration) {
            if (typeDeclaration != null && typeDeclaration.getFullyQualifiedName().isPresent()) {
                currentClassName = typeDeclaration.getFullyQualifiedName().get();
            } else if (typeDeclaration != null && typeDeclaration.getNameAsString() != null) {
                 // Fallback for local classes or types where FQN might not be straightforward
                currentClassName = typeDeclaration.getNameAsString();
            }
        }

        @Override
        public void visit(CompilationUnit cu, List<LogStatement> logStatements) {
            String initialClassName = "UnknownClass";
            if (cu.getPrimaryType().isPresent()) {
                TypeDeclaration<?> primaryType = cu.getPrimaryType().get();
                if (primaryType.getFullyQualifiedName().isPresent()) {
                    initialClassName = primaryType.getFullyQualifiedName().get();
                }
            } else if (cu.getPackageDeclaration().isPresent() && !cu.getTypes().isEmpty()){
                 // Fallback if primary type is not set but types exist (e.g. multiple top-level classes, though rare)
                 TypeDeclaration<?> firstType = cu.getType(0);
                 if(firstType.getFullyQualifiedName().isPresent()){
                     initialClassName = firstType.getFullyQualifiedName().get();
                 } else {
                    initialClassName = cu.getPackageDeclaration().get().getNameAsString() + "." + firstType.getNameAsString();
                 }
            }
            currentClassName = initialClassName;
            super.visit(cu, logStatements);
        }
        
        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<LogStatement> arg) {
            String outerClassName = currentClassName;
            updateClassNameFromType(n);
            super.visit(n, arg);
            currentClassName = outerClassName; // Restore after visiting children
        }

        @Override
        public void visit(EnumDeclaration n, List<LogStatement> arg) {
            String outerClassName = currentClassName;
            updateClassNameFromType(n);
            super.visit(n, arg);
            currentClassName = outerClassName; // Restore after visiting children
        }

        @Override
        public void visit(AnnotationDeclaration n, List<LogStatement> arg) {
            String outerClassName = currentClassName;
            updateClassNameFromType(n);
            super.visit(n, arg);
            currentClassName = outerClassName; // Restore after visiting children
        }
        
        @Override
        public void visit(RecordDeclaration n, List<LogStatement> arg) {
            String outerClassName = currentClassName;
            updateClassNameFromType(n);
            super.visit(n, arg);
            currentClassName = outerClassName; // Restore after visiting children
        }

        @Override
        public void visit(MethodCallExpr n, List<LogStatement> logStatements) {
            // Ensure currentClassName is set. If null, it might be a static block or initializer outside a type.
            // For simplicity, we require a class context for logs.
            if (currentClassName == null || currentClassName.equals("UnknownClass")) {
                 // Try to find the enclosing type declaration if not already set
                 Optional<Node> ancestor = n.getParentNode();
                 while(ancestor.isPresent() && !(ancestor.get() instanceof TypeDeclaration)) {
                     ancestor = ancestor.get().getParentNode();
                 }
                 ancestor.ifPresent(node -> {
                     if (node instanceof TypeDeclaration) {
                         updateClassNameFromType((TypeDeclaration<?>) node);
                     }
                 });
                 if (currentClassName == null || currentClassName.equals("UnknownClass")) {
                    logger.debug("Skipping MethodCallExpr in {} because currentClassName is not resolved.", n.getRange().map(r -> r.begin.toString()).orElse("unknown location"));
                    return; // Skip if class name can't be determined
                 }
            }

            super.visit(n, logStatements); // Call super after currentClassName logic if it might affect children

            String methodName = n.getNameAsString();

            if (LOGGER_METHOD_NAMES.contains(methodName.toLowerCase())) {
                // Check if the scope is a logger (e.g., log.info, LOGGER.error)
                 Optional<Expression> scopeOpt = n.getScope();
                 if (scopeOpt.isPresent()) {
                    // Basic check: if scope is NameExpr, could be 'log' or 'LOGGER'
                    // Advanced check would involve resolving type of scopeOpt.get()
                    // and checking if it's a known logger type (e.g. SLF4J Logger, Log4j Logger)
                 } else {
                     // Not a typical logger call like log.info(), might be a static import or other form.
                     // For simplicity, we'll skip these for now or require scope.
                     return;
                 }


                String logLevel = methodName.toLowerCase();
                String message = "";
                List<LogVariable> variables = new ArrayList<>();
                boolean piiRiskInMessage = false;
                boolean pciRiskInMessage = false;
                boolean generalRiskInMessage = false;

                if (!n.getArguments().isEmpty()) {
                    Expression firstArg = n.getArgument(0);
                    if (firstArg.isStringLiteralExpr()) {
                        message = firstArg.asStringLiteralExpr().getValue();
                        if (piiSpecificKeywordsPattern.matcher(message).find()) {
                            piiRiskInMessage = true;
                        }
                        if (pciKeywordsPattern.matcher(message).find()) {
                            pciRiskInMessage = true;
                        }
                        if (generalSensitiveKeywordsPattern.matcher(message).find()) {
                            generalRiskInMessage = true;
                        }
                    } else {
                        // Handle cases where the first argument is not a string literal (e.g. a variable)
                        // For now, we'll just represent it as a dynamic message part.
                        message = "{DYNAMIC_MESSAGE}";
                        // And add this first non-string argument as a variable itself
                        addVariable(firstArg, variables);
                    }

                    for (int i = 1; i < n.getArguments().size(); i++) {
                        addVariable(n.getArgument(i), variables);
                    }
                }
                
                boolean overallPiiRisk = piiRiskInMessage || variables.stream().anyMatch(LogVariable::isPii);
                boolean overallPciRisk = pciRiskInMessage || variables.stream().anyMatch(LogVariable::isPci);
                // If generalRiskInMessage is true, we might flag both PII and PCI as a precaution,
                // or have a separate "general_sensitive" flag if needed.
                // For now, let's assume general sensitive terms contribute to both PII and PCI risk for broader coverage.
                if (generalRiskInMessage) {
                    overallPiiRisk = true;
                    overallPciRisk = true;
                }


                logStatements.add(new LogStatement(
                        UUID.randomUUID().toString(),
                        currentClassName,
                        n.getRange().map(r -> r.begin.line).orElse(-1),
                        logLevel,
                        message,
                        variables,
                        overallPiiRisk,
                        overallPciRisk
                ));
            } else if (methodName.equals("println") || methodName.equals("print")) {
                 // Handle System.out.println and System.err.println
                 Optional<Expression> scopeOpt = n.getScope();
                 if (scopeOpt.isPresent() && scopeOpt.get().isFieldAccessExpr()) {
                     Expression systemExpr = scopeOpt.get().asFieldAccessExpr().getScope();
                     String fieldName = scopeOpt.get().asFieldAccessExpr().getNameAsString();
                     if (systemExpr.isNameExpr() && systemExpr.asNameExpr().getNameAsString().equals("System") && (fieldName.equals("out") || fieldName.equals("err"))) {
                        String sysOutMessage = "";
                        List<LogVariable> sysOutVariables = new ArrayList<>();
                        boolean sysOutPiiRisk = false;
                        boolean sysOutPciRisk = false;
                        boolean sysOutGeneralRisk = false;

                        if (!n.getArguments().isEmpty()) {
                            Expression arg = n.getArgument(0); // System.out.println usually takes one main argument
                             if (arg.isStringLiteralExpr()) {
                                 sysOutMessage = arg.asStringLiteralExpr().getValue();
                                 if (piiSpecificKeywordsPattern.matcher(sysOutMessage).find()) {
                                     sysOutPiiRisk = true;
                                 }
                                 if (pciKeywordsPattern.matcher(sysOutMessage).find()) {
                                     sysOutPciRisk = true;
                                 }
                                 if (generalSensitiveKeywordsPattern.matcher(sysOutMessage).find()) {
                                     sysOutGeneralRisk = true;
                                 }
                             } else {
                                 sysOutMessage = "{DYNAMIC_CONTENT}";
                                 addVariable(arg, sysOutVariables); // Treat the whole arg as a variable
                             }
                             // If there are more arguments (e.g. in a custom print method that we misinterpret),
                             // this basic logic might not capture them all as separate variables easily.
                        }
                        
                        boolean overallSysOutPiiRisk = sysOutPiiRisk || sysOutVariables.stream().anyMatch(LogVariable::isPii);
                        boolean overallSysOutPciRisk = sysOutPciRisk || sysOutVariables.stream().anyMatch(LogVariable::isPci);
                        if (sysOutGeneralRisk) {
                            overallSysOutPiiRisk = true;
                            overallSysOutPciRisk = true;
                        }

                        logStatements.add(new LogStatement(
                            UUID.randomUUID().toString(),
                            currentClassName,
                            n.getRange().map(r -> r.begin.line).orElse(-1),
                            "debug", // Treat System.out as debug level
                            sysOutMessage,
                            sysOutVariables,
                            overallSysOutPiiRisk,
                            overallSysOutPciRisk
                        ));
                     }
                 }
            }
        }

        private void addVariable(Expression argExpr, List<LogVariable> variables) {
            String varName = "unknown_var";
            String varType = "unknown_type";
            boolean isPii = false;
            boolean isPci = false;
            boolean isGeneralSensitive = false;

            try {
                // Attempt to resolve the expression to get its name and type
                if (argExpr.isNameExpr()) {
                    varName = argExpr.asNameExpr().getNameAsString();
                     ResolvedValueDeclaration resolved = argExpr.asNameExpr().resolve();
                     varType = resolved.getType().describe();
                } else if (argExpr.isFieldAccessExpr()) {
                    varName = argExpr.asFieldAccessExpr().getNameAsString();
                    varType = argExpr.asFieldAccessExpr().calculateResolvedType().describe();
                } else if (argExpr.isMethodCallExpr()){
                     varName = argExpr.asMethodCallExpr().getNameAsString()+"()";
                     varType = argExpr.asMethodCallExpr().calculateResolvedType().describe();
                }
                 else { // For other expression types (literals, binary ops, etc.)
                    varName = argExpr.toString(); // Use the expression itself as a "name"
                    try {
                       varType = argExpr.calculateResolvedType().describe();
                    } catch (Exception e) {
                        // If type cannot be resolved (e.g. for literals without context or complex expressions)
                        varType = argExpr.getClass().getSimpleName(); // Fallback to expression class name
                        logger.debug("Could not resolve type for expression: {}. Falling back to class name. Error: {}", argExpr, e.getMessage());
                    }
                }
            } catch (Exception e) { // Catch UnsolvedSymbolException or other resolution errors
                logger.warn("Could not fully resolve variable: {} in class {}. Error: {}. Using fallback name/type.", argExpr, currentClassName, e.getMessage());
                 varName = argExpr.toString(); // fallback name
                 // varType remains "unknown_type" or try a simpler type inference
                 if (argExpr.isStringLiteralExpr()) varType = "String";
                 else if (argExpr.isIntegerLiteralExpr()) varType = "int";
                 else if (argExpr.isDoubleLiteralExpr()) varType = "double";
                 else if (argExpr.isBooleanLiteralExpr()) varType = "boolean";
                 else if (argExpr.isNullLiteralExpr()) varType = "null";
                 else varType = "complex_object"; // General fallback for unresolved complex objects
            }
            
            // Sensitivity check for variable name
            if (piiSpecificKeywordsPattern.matcher(varName).find()) {
                isPii = true;
            }
            if (pciKeywordsPattern.matcher(varName).find()) {
                isPci = true;
            }
            if (generalSensitiveKeywordsPattern.matcher(varName).find()) {
                isGeneralSensitive = true; // Flag general sensitivity
            }

            // Sensitivity check for type (very basic)
            // These type checks are broad; specific keyword matching on names is often more reliable.
            String lowerVarType = varType.toLowerCase();
            if (lowerVarType.contains("user") || lowerVarType.contains("customer") || lowerVarType.contains("account") || lowerVarType.contains("person")) {
                 if (!lowerVarType.contains("id") && !lowerVarType.contains("count") && !lowerVarType.contains("type")) { 
                     isPii = true; // Likely PII if it's a complex object representing these entities
                 }
            }
            if (lowerVarType.contains("card") || lowerVarType.contains("payment") || lowerVarType.contains("transactiondetail")) {
                if (!lowerVarType.contains("id") && !lowerVarType.contains("status") && !lowerVarType.contains("type")) {
                    isPci = true; // Likely PCI if it's a complex object related to these
                }
            }

            // If general sensitive keyword was found in name, and no specific PII/PCI was found yet,
            // flag both as a precaution.
            if (isGeneralSensitive) {
                if (!isPii) isPii = true;
                if (!isPci) isPci = true;
            }

            variables.add(new LogVariable(varName, varType, isPii, isPci));
        }
    }
} 