package com.example.ailogistics.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/incident")
public class SystemConfigController {

    @Value("${ai.model.name}")
    private String modelName;

    private final Environment environment;

    public SystemConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("applicationName", environment.getProperty("spring.application.name"));
        config.put("activeProfiles", environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles() : new String[]{"default"});
        config.put("activeModel", modelName);
        config.put("serverPort", environment.getProperty("local.server.port", environment.getProperty("server.port")));
        return config;
    }
}