package com.exchange.controller;

import com.exchange.sevice.LeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/headers")
@Slf4j
public class HeadersController {

    @Autowired
    private LeadService leadService;


    // 修改或新增一个 header
    @PostMapping("/update")
    public ResponseEntity<String> updateHeader(@RequestParam String key, @RequestParam String value) {
        leadService.updateHeader(key, value);
        return ResponseEntity.ok("Header updated");
    }
}

