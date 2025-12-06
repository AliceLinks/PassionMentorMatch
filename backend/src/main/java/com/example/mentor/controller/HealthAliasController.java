package com.example.mentor.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthAliasController {

    private final HealthEndpoint healthEndpoint;

    public HealthAliasController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    // Alias for platforms probing "/health" instead of "/actuator/health"
    @GetMapping({"/health", "/healthz"})
    public ResponseEntity<Map<String, Object>> health() {
        HealthComponent component = healthEndpoint.health();
        Status status = component.getStatus();
        HttpStatus http = Status.UP.equals(status) || "UP".equalsIgnoreCase(status.getCode())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        Map<String, Object> body = new HashMap<>();
        body.put("status", status.getCode());
        return new ResponseEntity<>(body, http);
    }

    // Avoid noisy 404 logs for browsers/proxies requesting favicon
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
