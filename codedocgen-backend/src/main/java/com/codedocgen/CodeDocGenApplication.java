package com.codedocgen;

import com.codedocgen.util.SystemInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CodeDocGenApplication {

    private static final Logger logger = LoggerFactory.getLogger(CodeDocGenApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CodeDocGenApplication.class, args);
    }

    @Bean
    public CommandLineRunner logSystemInfo() {
        return args -> {
            logger.info("-----------------------------------------------------------");
            logger.info("Operating System: {}", SystemInfoUtil.getOperatingSystem());
            logger.info("Detected Maven Version: {}", SystemInfoUtil.getMavenVersion());
            logger.info("-----------------------------------------------------------");
        };
    }
} 