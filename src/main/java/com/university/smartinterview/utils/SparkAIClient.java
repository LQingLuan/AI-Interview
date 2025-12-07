// src/main/java/com/university/smartinterview/utils/SparkAIClient.java
package com.university.smartinterview.utils;

import com.university.smartinterview.config.IflytekConfig;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火API客户端封装 (使用WebSocket协议)
 */
@Component
public class SparkAIClient {

    // 使用官方示例中的domain值
    private static final String DOMAIN = "lite";
    private static final int RESPONSE_TIMEOUT_SECONDS = 60;

    public String callSparkAI(String question, String context, IflytekConfig.SparkAIConfig config) {
        System.out.println("Using SparkAI Config: " +
                config.getApiUrl() + ", AppID=" + config.getAppId());

        try {
            // 生成鉴权URL (使用URI代替URL)
            String authUrl = generateAuthUrl(config);
            System.out.println("WebSocket URL: " + authUrl);

            // 构建请求体
            JSONObject requestJson = buildRequestJson(config, question, context);

            // 打印请求体用于调试
            System.out.println("Sending request to Spark AI:");
            System.out.println("Body: " + requestJson.toString(2));

            // 使用WebSocket连接
            return connectViaWebSocket(authUrl, requestJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用讯飞星火API失败: " + e.getMessage(), e);
        }
    }

    private JSONObject buildRequestJson(IflytekConfig.SparkAIConfig config, String question, String context) {
        JSONObject requestJson = new JSONObject();

        // 1. 构建header
        JSONObject header = new JSONObject();
        header.put("app_id", config.getAppId());
        header.put("uid", UUID.randomUUID().toString().substring(0, 10));

        // 2. 构建parameter
        JSONObject parameter = new JSONObject();
        JSONObject chat = new JSONObject();
        chat.put("domain", DOMAIN);
        chat.put("temperature", 0.5);
        chat.put("max_tokens", 4096);
        parameter.put("chat", chat);

        // 3. 构建payload
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        JSONArray text = new JSONArray();

        // 添加上下文（如果有）
        if (context != null && !context.isEmpty()) {
            try {
                JSONArray contextArray = new JSONArray(context);
                for (int i = 0; i < contextArray.length(); i++) {
                    text.put(contextArray.getJSONObject(i));
                }
            } catch (Exception e) {
                text.put(new JSONObject()
                        .put("role", "user")
                        .put("content", context));
            }
        }

        // 添加当前问题
        text.put(new JSONObject()
                .put("role", "user")
                .put("content", question));

        message.put("text", text);
        payload.put("message", message);

        // 组装完整请求
        requestJson.put("header", header);
        requestJson.put("parameter", parameter);
        requestJson.put("payload", payload);

        return requestJson;
    }

    private String connectViaWebSocket(String url, String requestBody) throws Exception {
        final StringBuilder fullResponse = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = {null};

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        // 直接使用字符串URL创建Request
        Request request = new Request.Builder()
                .url(url)
                .build();

        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket connection opened");
                // 连接成功后发送请求
                webSocket.send(requestBody);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    System.out.println("Received message: " + text);
                    JSONObject responseJson = new JSONObject(text);

                    // 检查错误
                    if (responseJson.has("header")) {
                        JSONObject header = responseJson.getJSONObject("header");
                        if (header.getInt("code") != 0) {
                            throw new RuntimeException("API error: " + header.getString("message"));
                        }
                    }

                    // 提取有效负载
                    if (responseJson.has("payload")) {
                        JSONObject payload = responseJson.getJSONObject("payload");
                        if (payload.has("choices")) {
                            JSONObject choices = payload.getJSONObject("choices");
                            JSONArray textArray = choices.getJSONArray("text");

                            for (int i = 0; i < textArray.length(); i++) {
                                JSONObject textObj = textArray.getJSONObject(i);
                                String content = textObj.getString("content");
                                fullResponse.append(content);
                            }

                            // 检查是否结束
                            if (choices.has("status") && choices.getInt("status") == 2) {
                                System.out.println("Received final response");
                                webSocket.close(1000, "Normal closure");
                                latch.countDown();
                            }
                        }
                    }
                } catch (Exception e) {
                    error[0] = e;
                    latch.countDown();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket connection closed: " + code + " - " + reason);
                latch.countDown();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("WebSocket error: " + t.getMessage());
                error[0] = new RuntimeException("WebSocket failure", t);
                latch.countDown();
            }
        });

        // 等待响应完成或超时
        boolean completed = latch.await(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!completed) {
            throw new RuntimeException("API call timed out after " + RESPONSE_TIMEOUT_SECONDS + " seconds");
        }

        if (error[0] != null) {
            throw error[0];
        }

        return fullResponse.toString();
    }

    private String generateAuthUrl(IflytekConfig.SparkAIConfig config) throws Exception {
        String apiUrl = config.getApiUrl();
        String apiKey = config.getApiKey();
        String apiSecret = config.getApiSecret();

        // 解析原始URL
        URI uri = URI.create(apiUrl);
        String host = uri.getHost();
        String path = uri.getPath();

        // 生成日期 - 使用RFC 1123格式
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        // 构建签名字符串 - 修正格式
        String signatureOrigin = String.format(
                "host: %s\n" +
                        "date: %s\n" +
                        "GET %s HTTP/1.1",  // 注意这里没有多余的空格
                host, date, path
        );

        // 计算签名
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(keySpec);
        byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);

        // 构建授权字符串 - 修正格式
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, "hmac-sha256", signature
        );

        // 对授权字符串进行Base64编码
        String encodedAuth = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        // 构建最终URL - 修正参数格式
        return String.format("%s?authorization=%s&date=%s&host=%s",
                apiUrl,
                URLEncoder.encode(encodedAuth, "UTF-8"),
                URLEncoder.encode(date, "UTF-8"),
                URLEncoder.encode(host, "UTF-8")
        );
    }
}