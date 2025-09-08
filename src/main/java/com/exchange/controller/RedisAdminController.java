package com.exchange.controller;

import com.exchange.common.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/admin/redis")
public class RedisAdminController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/snapshot/{symbol}/{side}/{accountType}")
    public String snapshot(@PathVariable String symbol, @PathVariable String side, @PathVariable String accountType) {
        log.info("Start to snapshot {}, {}, {}", symbol, side, accountType);
        String key = RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH
                + ":"
                + accountType + ":"
                + symbol + "_"
                + side;
        stringRedisTemplate.delete(key);
        return "Pending close set cleared.";
    }

    @GetMapping("/pending-close")
    public Set<String> listPendingCloseSet() {
        return stringRedisTemplate.opsForSet().members("copy_trade:pending_close_set");
    }

    @DeleteMapping("/pending-close/{symbol}/{side}")
    public String removeFromPendingCloseSet(@PathVariable String symbol, @PathVariable String side) {
        log.info("Start to remove {}, {}", symbol, side);
        String key = symbol + "_" + side;
        stringRedisTemplate.opsForSet().remove("copy_trade:pending_close_set", key);
        return "Removed: " + key;
    }
}

