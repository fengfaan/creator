package com.aiwriter.controller;

import com.aiwriter.model.ApiResponse;
import com.aiwriter.model.ConfigItem;
import com.aiwriter.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResponse<List<ConfigItem>> list() {
        return ApiResponse.ok(configService.list());
    }

    @GetMapping("/{key}")
    public ApiResponse<ConfigItem> get(@PathVariable String key) {
        return ApiResponse.ok(configService.get(key));
    }

    @PostMapping
    public ApiResponse<Void> set(@RequestBody Map<String, String> body) {
        configService.set(body.get("key"), body.get("value"));
        return ApiResponse.ok();
    }
}
