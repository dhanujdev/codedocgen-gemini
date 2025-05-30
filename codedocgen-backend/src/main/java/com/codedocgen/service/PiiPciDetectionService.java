package com.codedocgen.service;

import com.codedocgen.model.PiiPciFinding;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public interface PiiPciDetectionService {
    List<PiiPciFinding> scanRepository(Path repoPath, Map<String, Pattern> piiPciPatterns);
} 