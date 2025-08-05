package com.exchange.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/admin/redis")
public class RedisAdminController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/pending-close")
    public Set<String> listPendingCloseSet() {
        return stringRedisTemplate.opsForSet().members("copy_trade:pending_close_set");
    }

    @DeleteMapping("/pending-close/{symbol}/{side}")
    public String removeFromPendingCloseSet(@PathVariable String symbol, @PathVariable String side) {
        String key = symbol + "_" + side;
        stringRedisTemplate.opsForSet().remove("copy_trade:pending_close_set", key);
        return "Removed: " + key;
    }

    @DeleteMapping("/pending-close")
    public String clearPendingCloseSet() {
        stringRedisTemplate.delete("copy_trade:pending_close_set");
        return "Pending close set cleared.";
    }
}

