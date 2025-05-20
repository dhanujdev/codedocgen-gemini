package com.codedocgen.parser;

import org.springframework.stereotype.Component;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.MethodMetadata;
import java.util.*;
import org.slf4j.Logger; // Uncomment if logger is used
import org.slf4j.LoggerFactory; // Uncomment if logger is used

@Component
public class CallFlowAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CallFlowAnalyzer.class); // Uncomment if logger is used
    
    // Set to track methods we've already warned about to reduce log noise
    private Set<String> warnedMethods = new HashSet<>();

    // Method to initiate call flow generation from a single entrypoint (can be used for specific tests or kept for compatibility)
    public List<String> getCallFlow(String entryMethodFQN, List<ClassMetadata> classes) {
        Map<String, MethodMetadata> methodMap = new HashMap<>();
        Map<String, ClassMetadata> classMap = new HashMap<>();
        buildMaps(classes, methodMap, classMap);

        List<String> flow = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfs(entryMethodFQN, methodMap, flow, visited, classMap, classes);
        return flow;
    }

    // Main method to get all call flows for relevant entrypoints (controllers, SOAP services)
    public Map<String, List<String>> getEntrypointCallFlows(List<ClassMetadata> classes) {
        logger.info("CallFlowAnalyzer received {} classes for analysis.", classes != null ? classes.size() : 0);
        Map<String, MethodMetadata> methodMap = new HashMap<>();
        Map<String, ClassMetadata> classMap = new HashMap<>();
        buildMaps(classes, methodMap, classMap);
        logger.info("Built methodMap with {} entries and classMap with {} entries.", methodMap.size(), classMap.size());

        // Print all method map keys for debugging
        logger.debug("Method map keys:" + methodMap.keySet());
        // for (String key : methodMap.keySet()) {
        //     logger.debug("  methodMap key: {}", key);
        // }

        Map<String, List<String>> flows = new HashMap<>();
        int entryPointClassesFound = 0;
        int totalEntryPointsAttempted = 0;
        try {
            if (classes == null) {
                logger.warn("Input class list is null. Cannot generate call flows.");
                return flows;
            }
            for (ClassMetadata cls : classes) {
                if (("controller".equalsIgnoreCase(cls.getType()) || "soap".equalsIgnoreCase(cls.getType())) && cls.getMethods() != null) {
                    entryPointClassesFound++;
                    logger.debug("Processing entry point class: {} (Type: {}) with {} methods.", cls.getName(), cls.getType(), cls.getMethods().size());
                    String classFQN = (cls.getPackageName() != null && !cls.getPackageName().isEmpty()) ?
                        cls.getPackageName() + "." + cls.getName() : cls.getName();
                    for (MethodMetadata m : cls.getMethods()) {
                        totalEntryPointsAttempted++;
                        String entryPointBaseFQN = classFQN + "." + m.getName();
                        String entryPointDisplayFQN = entryPointBaseFQN + "(" +
                                                    (m.getParameters() != null ? String.join(", ", m.getParameters()) : "") +
                                                    ")";
                        logger.debug("Attempting DFS for entry point: {}", entryPointDisplayFQN);
                        logger.debug("  Entry point FQN: {}", entryPointBaseFQN);
                        logger.debug("  Called methods for this entry point:");
                        if (m.getCalledMethods() != null) {
                            for (String called : m.getCalledMethods()) {
                                logger.debug("    calledMethod: {}", called);
                            }
                        }

                        List<String> flow = new ArrayList<>();
                        Set<String> visited = new HashSet<>();
                        dfs(entryPointBaseFQN, methodMap, flow, visited, classMap, classes);

                        if (!flow.isEmpty()) {
                            logger.debug("Successfully generated flow for entry point: {} ({} steps). First step: {}", entryPointDisplayFQN, flow.size(), flow.get(0));
                            flows.put(entryPointDisplayFQN, flow);
                        } else {
                            logger.warn("Flow for entry point {} was empty after DFS. This should not happen if the entry point itself was processed.", entryPointDisplayFQN);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error during call flow generation: {}", e.getMessage(), e);
            // Optionally, you might want to return the partially generated flows or an error indicator
        }
        logger.info("Found {} entry point class(es). Attempted DFS for {} total entry point methods. Generated {} call flows.", entryPointClassesFound, totalEntryPointsAttempted, flows.size());
        return flows;
    }

    private void buildMaps(List<ClassMetadata> classes, Map<String, MethodMetadata> methodMap, Map<String, ClassMetadata> classMap) {
        if (classes == null) {
            logger.warn("Input class list to buildMaps is null.");
            return;
        }
        for (ClassMetadata cls : classes) {
            String classFQN = (cls.getPackageName() != null && !cls.getPackageName().isEmpty()) ?
                cls.getPackageName() + "." + cls.getName() : cls.getName();
            classMap.put(classFQN, cls);

            if (cls.getMethods() != null) {
                for (MethodMetadata m : cls.getMethods()) {
                    // Store reference to parent class and package in method metadata if not already there
                    // This should ideally be done during parsing (JavaParserServiceImpl)
                    if (m.getClassName() == null) m.setClassName(cls.getName());
                    if (m.getPackageName() == null) m.setPackageName(cls.getPackageName());

                    String baseMethodFQN = classFQN + "." + m.getName();
                    methodMap.put(baseMethodFQN, m);
                }
            }
        }
    }

    private String stripParams(String fqnWithParams) {
        if (fqnWithParams == null) return null;
        int paramIndex = fqnWithParams.indexOf('(');
        if (paramIndex != -1) {
            return fqnWithParams.substring(0, paramIndex);
        }
        return fqnWithParams;
    }

    private void dfs(String methodLookupKey,
                     Map<String, MethodMetadata> methodMap,
                     List<String> flow,
                     Set<String> visited,
                     Map<String, ClassMetadata> classMap,
                     List<ClassMetadata> allProjectClasses) {

        String baseMethodFQN = stripParams(methodLookupKey);
        logger.debug("DFS: Processing key '{}', base FQN '{}'. Visited size: {}. Current flow size: {}", methodLookupKey, baseMethodFQN, visited.size(), flow.size());

        if (baseMethodFQN == null) {
            logger.warn("DFS: baseMethodFQN is null for lookupKey '{}'. Cannot proceed.", methodLookupKey);
            flow.add("ERROR: Null baseMethodFQN for " + methodLookupKey);
            return;
        }

        if (!visited.add(baseMethodFQN)) {
            logger.debug("DFS: Method {} already visited. Skipping.", baseMethodFQN);
            return;
        }

        MethodMetadata currentMethodMeta = methodMap.get(baseMethodFQN);
        if (currentMethodMeta == null) {
            // Improved fallback mechanism for method resolution
            boolean found = false;
            
            // 1. First attempt: Try to match by method name across all classes with sophisticated matching
            String methodNameOnly = baseMethodFQN.contains(".") ? baseMethodFQN.substring(baseMethodFQN.lastIndexOf('.') + 1) : baseMethodFQN;
            String expectedClassName = baseMethodFQN.contains(".") ? 
                baseMethodFQN.substring(0, baseMethodFQN.lastIndexOf('.')) : "";
            
            if (!expectedClassName.isEmpty() && expectedClassName.contains(".")) {
                expectedClassName = expectedClassName.substring(expectedClassName.lastIndexOf('.') + 1);
            }
            
            // Try to find by class name + method name
            if (!expectedClassName.isEmpty()) {
                for (Map.Entry<String, MethodMetadata> entry : methodMap.entrySet()) {
                    MethodMetadata m = entry.getValue();
                    if (m.getName().equals(methodNameOnly) && 
                        m.getClassName() != null && 
                        m.getClassName().equals(expectedClassName)) {
                        currentMethodMeta = m;
                        logger.debug("DFS: Found method by class+method name matching: {}.{}", expectedClassName, methodNameOnly);
                        found = true;
                        break;
                    }
                }
            }
            
            // 2. Second attempt: Try exact method name match
            if (!found) {
                for (Map.Entry<String, MethodMetadata> entry : methodMap.entrySet()) {
                    if (entry.getValue().getName().equals(methodNameOnly)) {
                        currentMethodMeta = entry.getValue();
                        logger.debug("DFS: Found method by exact name match: {}", methodNameOnly);
                        found = true;
                        break;
                    }
                }
            }
            
            // 3. Third attempt: Try to handle common framework methods with special handling
            if (!found && (baseMethodFQN.contains("Repository.") || 
                           baseMethodFQN.contains("CrudRepository.") ||
                           baseMethodFQN.contains("JpaRepository.") ||
                           baseMethodFQN.contains("Optional.") ||
                           baseMethodFQN.contains("List."))) {
                
                // Special handling for common framework methods - create a synthetic node
                // but don't add warning to logs as these are expected
                String simpleMethodName = methodNameOnly;
                String frameworkClass = "";
                
                if (baseMethodFQN.contains("Repository")) frameworkClass = "Repository";
                else if (baseMethodFQN.contains("Optional")) frameworkClass = "Optional";
                else if (baseMethodFQN.contains("List")) frameworkClass = "List";
                
                flow.add("Framework method: " + frameworkClass + "." + simpleMethodName + "()");
                logger.debug("DFS: Adding framework method placeholder for: {}", baseMethodFQN);
                return;
            }
            
            // If still not found after all attempts
            if (!found) {
                if (!baseMethodFQN.equals(methodLookupKey)) {
                    logger.debug("DFS: Method {} not found by base FQN. Original key {} was different (had params).", baseMethodFQN, methodLookupKey);
                }
                
                // Only warn once per method FQN to reduce log noise
                if (!warnedMethods.contains(baseMethodFQN)) {
                    logger.warn("DFS: All attempts failed to resolve path for '{}' called from '{}'. Skipping unresolved path.", 
                        baseMethodFQN, 
                        flow.isEmpty() ? "Entry Point" : flow.get(flow.size() - 1));
                    warnedMethods.add(baseMethodFQN);
                }
                
                // Add node to flow anyway to maintain graph integrity
                if (flow.isEmpty()) {
                    flow.add(methodLookupKey + " (Entry Point)");
                } else {
                    // Add as unresolved but don't break the flow
                    flow.add("UNRESOLVED: " + methodLookupKey);
                }
                return;
            }
        }

        // --- Parameter and local variable type tracking ---
        Map<String, String> paramTypeMap = new HashMap<>();
        if (currentMethodMeta.getParameters() != null) {
            for (String param : currentMethodMeta.getParameters()) {
                String[] parts = param.trim().split(" ");
                if (parts.length == 2) {
                    paramTypeMap.put(parts[1], parts[0]);
                }
            }
        }
        // Local variable extraction (if available in MethodMetadata)
        if (currentMethodMeta.getLocalVariables() != null) {
            for (String localVar : currentMethodMeta.getLocalVariables()) {
                String[] parts = localVar.trim().split(" ");
                if (parts.length == 2) {
                    paramTypeMap.put(parts[1], parts[0]);
                    logger.debug("DFS: Found local variable '{}' of type '{}' in method '{}'", parts[1], parts[0], methodLookupKey);
                }
            }
        }
        // --- End parameter and local variable type tracking ---

        String fqnForDisplay = (currentMethodMeta.getPackageName() != null && !currentMethodMeta.getPackageName().isEmpty() ? currentMethodMeta.getPackageName() + "." : "") +
                                currentMethodMeta.getClassName() + "." +
                                currentMethodMeta.getName() +
                                "(" + (currentMethodMeta.getParameters() != null ? String.join(", ", currentMethodMeta.getParameters()) : "") + ")";
        logger.debug("DFS: Current method display FQN: {}", fqnForDisplay);

        if (flow.isEmpty()) {
            logger.debug("DFS: Adding entry point {} to flow.", fqnForDisplay);
            flow.add(fqnForDisplay);
        }

        if (currentMethodMeta.getCalledMethods() != null && !currentMethodMeta.getCalledMethods().isEmpty()) {
            logger.debug("DFS: Method {} has {} called methods to process.", fqnForDisplay, currentMethodMeta.getCalledMethods().size());
            String currentMethodClassFQN = (currentMethodMeta.getPackageName() != null && !currentMethodMeta.getPackageName().isEmpty() ? currentMethodMeta.getPackageName() + "." : "") +
                                         currentMethodMeta.getClassName();
            ClassMetadata currentClass = classMap.get(currentMethodClassFQN);
            if (currentClass == null) {
                logger.warn("DFS: Could not find current class {} in classMap for method {}.", currentMethodClassFQN, fqnForDisplay);
            }

            for (String calledMethodSignatureFromParser : currentMethodMeta.getCalledMethods()) {
                logger.debug("DFS: Processing called method signature from parser: '{}'", calledMethodSignatureFromParser);
                String strippedCalledMethod = stripParams(calledMethodSignatureFromParser);
                if (strippedCalledMethod == null) {
                    logger.warn("DFS: Stripped called method is null for signature: '{}'", calledMethodSignatureFromParser);
                    flow.add("ERROR: Null stripped method for " + calledMethodSignatureFromParser);
                    continue;
                }
                boolean resolvedAndTraversed = false;

                // --- Graceful fallback for framework-injected fields ---
                if (strippedCalledMethod.startsWith("log.")) {
                    logger.debug("DFS: Skipping framework-injected field 'log' for '{}'.", strippedCalledMethod);
                    continue;
                }
                // --- End fallback ---

                // --- Parameter type and chained call resolution ---
                if (!resolvedAndTraversed && strippedCalledMethod.contains(".")) {
                    String[] parts = strippedCalledMethod.split("\\.");
                    if (parts.length > 1) {
                        String currentTypeFQN = null;
                        String currentName = parts[0];
                        // Step 1: Resolve the type of the first segment (parameter or field)
                        if (paramTypeMap.containsKey(currentName)) {
                            String paramType = paramTypeMap.get(currentName);
                            for (ClassMetadata searchCls : allProjectClasses) {
                                if (searchCls.getName().equals(paramType)) {
                                    currentTypeFQN = (searchCls.getPackageName() != null && !searchCls.getPackageName().isEmpty() ? searchCls.getPackageName() + "." : "") + searchCls.getName();
                                    break;
                                }
                            }
                            if (currentTypeFQN == null) {
                                currentTypeFQN = paramType;
                            }
                        } else if (currentClass != null && currentClass.getFields() != null) {
                            for (com.codedocgen.model.FieldMetadata field : currentClass.getFields()) {
                                String fieldNameInDecl = field.getName();
                                if (fieldNameInDecl != null && fieldNameInDecl.equals(currentName)) {
                                    String fieldTypeNameSimple = field.getType();
                                    if (fieldTypeNameSimple != null) {
                                        fieldTypeNameSimple = fieldTypeNameSimple.replaceAll("<.*?>", "");
                                        for (ClassMetadata searchCls : allProjectClasses) {
                                            if (searchCls.getName().equals(fieldTypeNameSimple)) {
                                                currentTypeFQN = (searchCls.getPackageName() != null && !searchCls.getPackageName().isEmpty() ? searchCls.getPackageName() + "." : "") + searchCls.getName();
                                                break;
                                            }
                                        }
                                        if (currentTypeFQN == null) {
                                            currentTypeFQN = fieldTypeNameSimple;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        // Step 2: Recursively resolve each method in the chain
                        boolean chainResolved = true;
                        for (int i = 1; i < parts.length; i++) {
                            String methodName = parts[i];
                            if (currentTypeFQN == null) {
                                chainResolved = false;
                                break;
                            }
                            String candidateFQN = currentTypeFQN + "." + methodName;
                            MethodMetadata methodMeta = methodMap.get(candidateFQN);
                            if (methodMeta != null) {
                                // If this is the last segment, add to flow and recurse
                                if (i == parts.length - 1) {
                                    String resolvedTargetDisplayFQN = (methodMeta.getPackageName() != null && !methodMeta.getPackageName().isEmpty() ? methodMeta.getPackageName() + "." : "") +
                                                                      methodMeta.getClassName() + "." +
                                                                      methodMeta.getName() +
                                                                      "(" + (methodMeta.getParameters() != null ? String.join(", ", methodMeta.getParameters()) : "") + ")";
                                    logger.debug("DFS: Chained call resolution success. Adding '{}' to flow and recursing.", resolvedTargetDisplayFQN);
                                    flow.add(" -> " + resolvedTargetDisplayFQN);
                                    dfs(candidateFQN, methodMap, flow, visited, classMap, allProjectClasses);
                                    resolvedAndTraversed = true;
                                } else {
                                    // Update currentTypeFQN to the return type of this method
                                    String returnType = methodMeta.getReturnType();
                                    // Try to find FQN in allProjectClasses
                                    String foundFQN = null;
                                    for (ClassMetadata searchCls : allProjectClasses) {
                                        if (searchCls.getName().equals(returnType)) {
                                            foundFQN = (searchCls.getPackageName() != null && !searchCls.getPackageName().isEmpty() ? searchCls.getPackageName() + "." : "") + searchCls.getName();
                                            break;
                                        }
                                    }
                                    if (foundFQN != null) {
                                        currentTypeFQN = foundFQN;
                                    } else {
                                        currentTypeFQN = returnType;
                                    }
                                }
                            } else {
                                chainResolved = false;
                                break;
                            }
                        }
                        if (chainResolved && resolvedAndTraversed) {
                            // Already handled in the loop
                        }
                    }
                }
                // --- End parameter type and chained call resolution ---

                // Attempt 1: Direct lookup of stripped signature in methodMap (covers FQNs from SymbolSolver)
                if (!resolvedAndTraversed) {
                    logger.debug("DFS: Attempt 1 - Direct lookup for '{}'", strippedCalledMethod);
                    MethodMetadata targetMethodMeta = methodMap.get(strippedCalledMethod);
                    if (targetMethodMeta != null) {
                        String targetDisplayFQN = (targetMethodMeta.getPackageName() != null && !targetMethodMeta.getPackageName().isEmpty() ? targetMethodMeta.getPackageName() + "." : "") +
                                                  targetMethodMeta.getClassName() + "." +
                                                  targetMethodMeta.getName() +
                                                  "(" + (targetMethodMeta.getParameters() != null ? String.join(", ", targetMethodMeta.getParameters()) : "") + ")";
                        logger.debug("DFS: Attempt 1 Success. Adding '{}' to flow and recursing.", targetDisplayFQN);
                        flow.add(" -> " + targetDisplayFQN);
                        dfs(strippedCalledMethod, methodMap, flow, visited, classMap, allProjectClasses);
                        resolvedAndTraversed = true;
                    }
                }

                // Attempt 3: Simple method name (likely in current class or an import that wasn't fully resolved by parser)
                if (!resolvedAndTraversed && !strippedCalledMethod.contains(".")) {
                    logger.debug("DFS: Attempt 3 - Simple name resolution for '{}' in current class context '{}'", strippedCalledMethod, currentMethodClassFQN);
                    String fqnInCurrentClass = currentMethodClassFQN + "." + strippedCalledMethod;
                    MethodMetadata targetInCurrentClass = methodMap.get(fqnInCurrentClass);
                    if (targetInCurrentClass != null) {
                         String targetDisplayFQN = (targetInCurrentClass.getPackageName() != null && !targetInCurrentClass.getPackageName().isEmpty() ? targetInCurrentClass.getPackageName() + "." : "") +
                                                   targetInCurrentClass.getClassName() + "." +
                                                   targetInCurrentClass.getName() +
                                                   "(" + (targetInCurrentClass.getParameters() != null ? String.join(", ", targetInCurrentClass.getParameters()) : "") + ")";
                        logger.debug("DFS: Attempt 3 Success. Adding '{}' to flow and recursing.", targetDisplayFQN);
                        flow.add(" -> " + targetDisplayFQN);
                        dfs(fqnInCurrentClass, methodMap, flow, visited, classMap, allProjectClasses);
                        resolvedAndTraversed = true;
                    }
                }

                if(!resolvedAndTraversed) {
                    logger.warn("DFS: All attempts failed to resolve path for '{}' called from '{}'. Skipping unresolved path.", calledMethodSignatureFromParser, fqnForDisplay);
                    // Do not add UNRESOLVED_PATH to the flow; just skip
                }
            }
        } else {
            logger.debug("DFS: Method {} has no called methods or list is empty.", fqnForDisplay);
        }
        logger.debug("DFS: Finished processing for {}. Current flow size: {}. Flow: {}", fqnForDisplay, flow.size(), String.join("\n", flow));
    }
} 