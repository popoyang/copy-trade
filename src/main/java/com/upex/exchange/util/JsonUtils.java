package com.upex.exchange.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.upex.exchange.model.ApiResponse;

public class JsonUtils {
    public static <T> T parseResponse(String json, TypeReference<ApiResponse<T>> typeRef) {
        ApiResponse<T> response = JSON.parseObject(json, typeRef);
        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        throw new RuntimeException("Invalid API response: " + json);
    }
}

