package com.exchange.util;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisHelper {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, long ttl, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public boolean addToSet(String key, String value) {
        return stringRedisTemplate.opsForSet().add(key, value) > 0;
    }

    public boolean isMemberOfSet(String key, String value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    public void removeFromSet(String key, String value) {
        stringRedisTemplate.opsForSet().remove(key, value);
    }

    public void setJson(String key, Object obj, long ttl, TimeUnit unit) {
        String json = JSON.toJSONString(obj);
        set(key, json, ttl, unit);
    }
    public void setJson(String key, Object obj) {
        String json = JSON.toJSONString(obj);
        set(key, json);
    }

    public <T> T getJson(String key, Class<T> clazz) {
        String json = get(key);
        return json == null ? null : JSON.parseObject(json, clazz);
    }
}

