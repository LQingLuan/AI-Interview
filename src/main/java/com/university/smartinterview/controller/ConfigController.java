package com.university.smartinterview.controller;

import com.university.smartinterview.config.IflytekConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final IflytekConfig iflytekConfig;

    @Autowired
    public ConfigController(IflytekConfig iflytekConfig) {
        this.iflytekConfig = iflytekConfig;
    }

    @GetMapping("/iflytek")
    public Map<String, String> getIflytekConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("appId", iflytekConfig.getAppId());
        config.put("apiKey", iflytekConfig.getApiKey());
        config.put("apiSecret", iflytekConfig.getApiSecret());
        config.put("apiUrl", iflytekConfig.getApiUrl());
        return config;
    }
}