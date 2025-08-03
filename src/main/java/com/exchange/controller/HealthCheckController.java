package com.exchange.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存活探针,准备探针
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {


    /**
     * 存活探针,准备探针
     */
    @GetMapping({"/live", "/readiness"})
    public boolean healthCheck() {
        return true;
    }


}
