package com.exchange.util;

import com.exchange.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class HttpUtils {


    public static String httpPost(String url, String body, Map<String, String> headers) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type","application/json");
        request.setHeader("clienttype","web");
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (StringUtils.isNotBlank(value)) {
                    request.setHeader(key, value);
                }
            });
        }
        StringEntity stringEntity = new StringEntity(body, "UTF-8");
        request.setEntity(stringEntity);
        RequestConfig.custom().setConnectTimeout(Constants.CUSTOM_TIMEOUT).setSocketTimeout(Constants.SOCKET_TIMEOUT).build();

        CloseableHttpResponse response = client.execute(request);

        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return EntityUtils.toString(response.getEntity());
        } else {
            String errorResponse = EntityUtils.toString(response.getEntity());
            log.error(errorResponse);
            throw new IOException(errorResponse);
        }
    }

    public static String httpGet(String url,Map<String, String> headers) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.setHeader("Content-Type","application/json");
        request.setHeader("Clienttype","web");
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (StringUtils.isNotBlank(value)) {
                    request.setHeader(key, value);
                }
            });
        }
        RequestConfig.custom().setConnectTimeout(Constants.CUSTOM_TIMEOUT).setSocketTimeout(Constants.SOCKET_TIMEOUT).build();

        CloseableHttpResponse response = client.execute(request);

        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return EntityUtils.toString(response.getEntity());
        } else {
            String errorResponse = EntityUtils.toString(response.getEntity());
            log.error(errorResponse);
            throw new IOException(errorResponse);
        }
    }


}
