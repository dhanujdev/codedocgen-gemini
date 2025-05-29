package com.codedocgen.service;

import com.codedocgen.model.LogStatement;
import java.util.List;

public interface LoggerInsightsService {
    List<LogStatement> getLogInsights(String projectPath);
} 