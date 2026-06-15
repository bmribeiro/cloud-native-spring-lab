package com.cloudnativespringlab.productservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
public class ConfigInfoController {

    private final Environment environment;

    @Value("${app.message:Local fallback configuration}")
    private String message;

    public ConfigInfoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/config-info")
    public Map<String, Object> getConfigInfo() {
        return Map.of(
                "service", environment.getProperty("spring.application.name", "unknown"),
                "message", message,
                "activeProfiles", Arrays.asList(environment.getActiveProfiles())
        );
    }
}
