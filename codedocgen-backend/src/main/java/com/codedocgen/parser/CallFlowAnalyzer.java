package com.codedocgen.parser;

import org.springframework.stereotype.Component;
import com.codedocgen.model.ClassMetadata;
import com.codedocgen.model.MethodMetadata;
import com.codedocgen.model.ParameterMetadata;
import java.util.*;
import org.slf4j.Logger; // Uncomment if logger is used
import org.slf4j.LoggerFactory; // Uncomment if logger is used

@Component
public class CallFlowAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(CallFlowAnalyzer.class); // Uncomment if logger is used
    
    // Set to track methods we've already warned about to reduce log noise
    private Set<String> warnedSignatures = new HashSet<>();

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
        // logger.debug("Method map keys:" + methodMap.keySet());
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
                                                    (m.getParameters() != null ? 
                                                     m.getParameters().stream()
                                                     .map(param -> param.toString())
                                                     .collect(java.util.stream.Collectors.joining(", ")) : "") +
                                                    ")";
                        logger.debug("Attempting DFS for entry point: {}", entryPointDisplayFQN);
                        logger.debug("  Entry point FQN for methodMap lookup: {}", entryPointBaseFQN);
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
                    // Ensure className and packageName are set on methodMeta if not already
                    if (m.getClassName() == null) m.setClassName(cls.getName());
                    if (m.getPackageName() == null) m.setPackageName(cls.getPackageName());

                    String baseMethodFQN = classFQN + "." + m.getName(); // Key for methodMap is still base FQN
                    // If we wanted to support overloading directly in methodMap, the key would need to include param types.
                    // For now, we rely on calledMethodSignatures from parser being more specific.
                    methodMap.put(baseMethodFQN, m); 
                }
            }
        }
    }

    // Helper to extract base FQN (Class.method) from a signature like Class.method(paramType1,paramType2)
    private String getBaseFqn(String signature) {
        if (signature == null) return null;
        int paramIndex = signature.indexOf('(');
        if (paramIndex != -1) {
            return signature.substring(0, paramIndex);
        }
        return signature;
    }

    private void dfs(String methodLookupKey, // This is expected to be a base FQN (Class.method) for initial lookup
                     Map<String, MethodMetadata> methodMap,
                     List<String> flow,
                     Set<String> visited,
                     Map<String, ClassMetadata> classMap,
                     List<ClassMetadata> allProjectClasses) {

        logger.debug("DFS: Input methodLookupKey: '{}'", methodLookupKey);

        // If the lookup key already indicates a special type (e.g., from JavaParserServiceImpl fallback),
        // add it directly and don't recurse further down this path.
        if (methodLookupKey != null && 
            (methodLookupKey.startsWith("UNRESOLVED_CALL:") || 
             methodLookupKey.startsWith("FRAMEWORK_CALL:") || // Future-proofing if parser adds this
             methodLookupKey.startsWith("EXTERNAL_LIB:") || // Future-proofing
             methodLookupKey.startsWith("ERROR:") )) { // Error during parsing
            logger.debug("DFS: methodLookupKey '{}' is pre-classified. Adding to flow and returning.", methodLookupKey);
            // Ensure it's not a duplicate of the very last item to avoid redundant entries if parser and analyzer both add prefixes.
            // This simple check might need refinement based on how prefixes are layered.
            if (flow.isEmpty() || !flow.get(flow.size() - 1).equals(methodLookupKey)) {
                 flow.add(methodLookupKey);
            }
            // Do not remove from visited here, as this is a terminal node for this path.
            return;
        }
        
        String baseMethodFQN = getBaseFqn(methodLookupKey); 
        logger.debug("DFS: Processing key '{}', base FQN '{}'. Visited size: {}. Current flow size: {}", methodLookupKey, baseMethodFQN, visited.size(), flow.size());

        if (baseMethodFQN == null) {
            logger.warn("DFS: baseMethodFQN is null for lookupKey '{}'. Cannot proceed.", methodLookupKey);
            flow.add("ERROR: Null baseMethodFQN for " + methodLookupKey);
            return;
        }

        // Visited set should track the specific signature if possible, though currently uses baseFQN.
        // If methodLookupKey could carry parameter info, visited could be more precise.
        if (!visited.add(baseMethodFQN)) { // For now, cycle detection is on base FQN
            logger.debug("DFS: Method {} already visited (based on base FQN). Skipping.", baseMethodFQN);
            return;
        }

        MethodMetadata currentMethodMeta = methodMap.get(baseMethodFQN); 
        // If currentMethodMeta is null, it means the base FQN wasn't in our map.
        // This can happen if the called method is from an external lib not fully parsed, or if a class was missed.

        if (currentMethodMeta == null) {
            // Fallback: try to find a match if methodLookupKey had parameters and differs from baseMethodFQN
            // This part might be less relevant if methodLookupKey is always base.
            if (!baseMethodFQN.equals(methodLookupKey) && methodLookupKey.contains("(")) {
                 // Try a lookup with the original key if it was more specific, though methodMap uses base FQN keys.
                 // This path is unlikely to find anything new unless methodMap keys change.
            }

            // If still not found, apply previous fallback logic (though it might be less effective now)
            boolean foundViaFallback = false;
            String methodNameOnly = baseMethodFQN.contains(".") ? baseMethodFQN.substring(baseMethodFQN.lastIndexOf('.') + 1) : baseMethodFQN;
            String expectedClassNameFromFQN = baseMethodFQN.contains(".") ? baseMethodFQN.substring(0, baseMethodFQN.lastIndexOf('.')) : "";

            // Try to find by class name + method name (iterating all methods)
            if (!expectedClassNameFromFQN.isEmpty()) {
                for (Map.Entry<String, MethodMetadata> entry : methodMap.entrySet()) {
                    MethodMetadata m = entry.getValue();
                    String methodClassName = (m.getPackageName() != null && !m.getPackageName().isEmpty() ? m.getPackageName() + "." : "") + m.getClassName();
                    if (m.getName().equals(methodNameOnly) && methodClassName.equals(expectedClassNameFromFQN)) {
                        currentMethodMeta = m;
                        logger.debug("DFS: Found method by FQN class + method name matching: {}.{}", expectedClassNameFromFQN, methodNameOnly);
                        foundViaFallback = true;
                        break;
                    }
                }
            }

            if (!foundViaFallback) {
                 // Only warn once per method signature to reduce log noise
                String warningSignature = methodLookupKey; // Use the original lookup key for warning
                
                // Check for Spring Data Repository common methods
                String calledClassName = baseMethodFQN.contains(".") ? baseMethodFQN.substring(0, baseMethodFQN.lastIndexOf('.')) : "";
                String calledMethodName = baseMethodFQN.contains(".") ? baseMethodFQN.substring(baseMethodFQN.lastIndexOf('.') + 1) : baseMethodFQN;
                ClassMetadata calledClassMeta = classMap.get(calledClassName);
                boolean isKnownRepositoryCall = false;
                if (calledClassMeta != null && "repository".equalsIgnoreCase(calledClassMeta.getType())) {
                    if (calledMethodName.startsWith("save") || calledMethodName.startsWith("find") || 
                        calledMethodName.startsWith("delete") || calledMethodName.startsWith("exists") ||
                        calledMethodName.startsWith("count")) {
                        flow.add("FRAMEWORK_CALL (Spring Data): " + warningSignature);
                        isKnownRepositoryCall = true;
                    }
                }

                // Check for other common unresolved patterns (JDK, common libs)
                boolean isCommonJdkOrLib = false;
                if (!isKnownRepositoryCall) {
                    if (baseMethodFQN.startsWith("java.util.Optional") || baseMethodFQN.startsWith("java.util.regex") || 
                        baseMethodFQN.startsWith("java.lang.String") || baseMethodFQN.startsWith("org.slf4j.Logger") ||
                        baseMethodFQN.startsWith("java.time.LocalDateTime") || baseMethodFQN.startsWith("java.util.Objects") ||
                        baseMethodFQN.startsWith("org.springframework.validation") || baseMethodFQN.startsWith("org.springframework.context.support")) {
                        flow.add("FRAMEWORK_CALL (JDK/Lib): " + warningSignature);
                        isCommonJdkOrLib = true;
                    }
                }

                if (!isKnownRepositoryCall && !isCommonJdkOrLib) {
                    if (!warnedSignatures.contains(warningSignature)) {
                        logger.warn("DFS: MethodMetadata not found for '{}' (base FQN: '{}'). Called from '{}'. It might be an external library method or a parsing gap. Adding placeholder.", 
                            warningSignature, baseMethodFQN,
                            flow.isEmpty() ? "Entry Point" : flow.get(flow.size() - 1));
                        warnedSignatures.add(warningSignature);
                    }
                    flow.add("UNRESOLVED_OR_EXTERNAL: " + warningSignature);
                }
                visited.remove(baseMethodFQN); // Allow re-visiting if a different call path leads here through a resolved method
                return;
            }
        }

        // --- Parameter and local variable type tracking (uses FQNs from MethodMetadata) ---
        Map<String, String> paramTypeMap = new HashMap<>();
        if (currentMethodMeta.getParameters() != null) {
            for (ParameterMetadata param : currentMethodMeta.getParameters()) {
                paramTypeMap.put(param.getName(), param.getType()); // variable name -> type
            }
        }
        
        // Local variable extraction (if available in MethodMetadata)
        if (currentMethodMeta.getLocalVariables() != null) {
            for (MethodMetadata.VariableMetadata var : currentMethodMeta.getLocalVariables()) {
                paramTypeMap.put(var.getName(), var.getType());
                logger.debug("DFS: Found local variable '{}' of type '{}' in method '{}'", 
                             var.getName(), var.getType(), methodLookupKey);
            }
        }
        // --- End parameter and local variable type tracking ---

        String fqnForDisplay = (currentMethodMeta.getPackageName() != null && !currentMethodMeta.getPackageName().isEmpty() ? currentMethodMeta.getPackageName() + "." : "") +
                                currentMethodMeta.getClassName() + "." +
                                currentMethodMeta.getName() +
                                "(" + (currentMethodMeta.getParameters() != null ? 
                                      currentMethodMeta.getParameters().stream()
                                      .map(param -> param.toString())
                                      .collect(java.util.stream.Collectors.joining(", ")) : "") + ")";
        logger.debug("DFS: Current method display FQN: {}", fqnForDisplay);

        if (flow.isEmpty()) {
            logger.debug("DFS: Adding entry point {} to flow.", fqnForDisplay);
            flow.add(fqnForDisplay); 
        } else {
            // Avoid adding the same method signature back-to-back if it's already the last element
            // This can happen if the call was to itself (direct recursion handled by 'visited')
            // or if the parsing logic leads to it.
            if (!flow.get(flow.size()-1).equals(fqnForDisplay)) {
                 // Check if the last element was an UNRESOLVED_OR_EXTERNAL placeholder for the current method
                 // This might occur if a method call resolves to currentMethodMeta, but was initially added as unresolved.
                 String lastElement = flow.get(flow.size()-1);
                 if (lastElement.startsWith("UNRESOLVED_OR_EXTERNAL: ") && getBaseFqn(lastElement.substring("UNRESOLVED_OR_EXTERNAL: ".length())).equals(baseMethodFQN)) {
                    logger.debug("DFS: Replacing placeholder '{}' with resolved method '{}'", lastElement, fqnForDisplay);
                    flow.set(flow.size()-1, fqnForDisplay);
                 } else {
                    logger.debug("DFS: Adding {} to flow. Last element was {}.", fqnForDisplay, lastElement);
                    flow.add(fqnForDisplay);
                 }
            } else {
                 logger.debug("DFS: Method {} is already the last element in the flow. Not re-adding.", fqnForDisplay);
            }
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
                
                // The calledMethodSignatureFromParser should ideally be an FQN with parameter types if resolved by JavaParserServiceImpl
                String calledBaseFQN = getBaseFqn(calledMethodSignatureFromParser);
                if (calledBaseFQN == null) {
                    logger.warn("DFS: Stripped base FQN is null for signature: '{}'", calledMethodSignatureFromParser);
                    flow.add("ERROR: Null stripped method for " + calledMethodSignatureFromParser);
                    continue;
                }

                boolean resolvedAndTraversed = false;

                // Attempt 1: Direct lookup of the base FQN in methodMap.
                // This is the primary lookup. If calledMethodSignatureFromParser has params, they are for display/matching, not map key.
                logger.debug("DFS: Attempt 1 - Direct lookup for base '{}' (from '{}')", calledBaseFQN, calledMethodSignatureFromParser);
                MethodMetadata targetMethodMeta = methodMap.get(calledBaseFQN);

                if (targetMethodMeta != null) {
                    // Now we have a candidate class. If calledMethodSignatureFromParser included params, 
                    // we could try to find an overload in targetMethodMeta's class that matches.
                    // For now, we assume base FQN match is sufficient to proceed with recursion using that targetMethodMeta.
                    // The fqnForDisplay of this targetMethodMeta will show its actual parameters.

                    // Construct the display FQN for the target method based on its metadata
                    String targetDisplayFQN = (targetMethodMeta.getPackageName() != null && !targetMethodMeta.getPackageName().isEmpty() ? targetMethodMeta.getPackageName() + "." : "") +
                                              targetMethodMeta.getClassName() + "." +
                                              targetMethodMeta.getName() +
                                              "(" + (targetMethodMeta.getParameters() != null ? 
                                                    targetMethodMeta.getParameters().stream()
                                                    .map(param -> param.toString())
                                                    .collect(java.util.stream.Collectors.joining(", ")) : "") + ")";
                    
                    logger.debug("DFS: Attempt 1 Potential Match. Target display FQN: '{}'. Recursing with base key: '{}'", targetDisplayFQN, calledBaseFQN);
                    // flow.add(" -> " + targetDisplayFQN); // Added inside the recursive DFS call if new
                    dfs(calledBaseFQN, methodMap, flow, visited, classMap, allProjectClasses); // Recurse with base FQN
                    resolvedAndTraversed = true;
                } else {
                    logger.debug("DFS: Attempt 1 No direct match in methodMap for base '{}'", calledBaseFQN);
                }

                // Fallback for chained calls (e.g., obj.method1().method2())
                // This needs careful implementation if calledMethodSignatureFromParser is already specific.
                // The JavaParserServiceImpl should ideally resolve these chains into direct, fully-qualified calls.
                // If it still appears as a chain here, it means the parser couldn't fully resolve it.
                if (!resolvedAndTraversed && calledMethodSignatureFromParser.contains(".") && !calledMethodSignatureFromParser.startsWith(currentMethodMeta.getPackageName() !=null ? currentMethodMeta.getPackageName() : "")) {
                     // This logic might be too simplistic if the chained call isn't easily resolvable with current context
                     // Consider if this fallback is still needed given improved parser.
                     // flow.add(" -> CHAINED_CALL_PLACEHOLDER: " + calledMethodSignatureFromParser);
                     // resolvedAndTraversed = true; // Mark as handled to avoid unresolved warning for this specific case
                }


                // If not resolved by direct lookup or specific handlers:
                if (!resolvedAndTraversed) {
                    if (!warnedSignatures.contains(calledMethodSignatureFromParser)) {
                        logger.warn("DFS: Method '{}' called from '{}' could not be resolved in methodMap or by fallbacks. Adding as UNRESOLVED.", 
                            calledMethodSignatureFromParser, fqnForDisplay);
                        warnedSignatures.add(calledMethodSignatureFromParser);
                    }
                    flow.add("UNRESOLVED_CALL: " + calledMethodSignatureFromParser);
                    // Do not recurse for unresolved calls
                }
            }
        } else {
            logger.debug("DFS: Method {} has no called methods or list is empty.", fqnForDisplay);
        }
        logger.debug("DFS: Finished processing for {}. Current flow size: {}. Flow: {}", fqnForDisplay, flow.size(), String.join("\n", flow));
    }
} 