package com.csye6225.webapp.controller;

import com.csye6225.webapp.service.HealthCheckService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthz")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    @GetMapping
    public ResponseEntity<Void> healthCheck(HttpServletRequest request) {

        //check if request has payload
        if (request.getContentLength() > 0 || request.getQueryString() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        boolean isDbConnected = healthCheckService.isDatabaseConnected();

        if (isDbConnected) {
            return ResponseEntity.ok().header("Cache-Control", "no-cache").build();
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Cache-Control", "no-cache")
                    .build();
        }
    }
}
