package com.upex.exchange.controller;

import com.upex.exchange.sevice.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private LeadService leadService;

    // TODO 后续加权限校验、验签
    @PostMapping("/update")
    public String updateHeaders(@RequestBody Map<String,String>  map) {
        leadService.updateHeaders(map);
        return "SUCCESS";
    }
}

