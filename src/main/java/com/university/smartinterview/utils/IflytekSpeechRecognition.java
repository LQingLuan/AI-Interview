package com.university.smartinterview.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class IflytekSpeechRecognition {

    @Value("${iflytek.speech.ws-url}")
    private String speechWsUrl;

    @Value("${iflytek.spark.api-key}")
    private String apiKey;

    @Value("${iflytek.spark.api-secret}")
    private String apiSecret;

    @Value("${iflytek.spark.appid}")
    private String appId;

    private final ConcurrentHashMap<String, RecognitionResult> resultCache = new ConcurrentHashMap<>();

    public static class RecognitionResult {
        private String partialText = "";
        private String finalText = "";
        private boolean isFinal = false;
        private int progress = 0;
        private int status = 1; // 0:成功, 1:处理中, 2:失败

        public String getPartialText() { return partialText; }
        public void setPartialText(String partialText) { this.partialText = partialText; }
        public String getFinalText() { return finalText; }
        public void setFinalText(String finalText) { this.finalText = finalText; }
        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
    }

    /**
     * 流式语音识别
     */
    public RecognitionResult streamingRecognize(byte[] audioData, String sessionId,
                                                String format, String domain, String accent) {
        RecognitionResult result = new RecognitionResult();
        resultCache.put(sessionId, result);

        int maxRetries = 3;
        int attempt = 0;
        final boolean[] success = {false};
        final Exception[] lastError = {null};

        while (attempt < maxRetries && !success[0]) {
            attempt++;
            System.out.println("流式识别尝试: " + attempt + "/" + maxRetries);

            try {
                // 生成鉴权URL
                String authUrl = generateAuthUrl(sessionId, domain, accent);

                // 创建WebSocket客户端
                WebSocketClient client = new WebSocketClient(new URI(authUrl)) {
                    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    private final AtomicBoolean finalResultReceived = new AtomicBoolean(false);

                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                        System.out.println("WebSocket connection opened successfully!");

                        // 发送开始帧
                        JSONObject frame = new JSONObject();
                        frame.put("common", new JSONObject()
                                .put("app_id", appId));
                        frame.put("business", new JSONObject()
                                .put("domain", domain)
                                .put("accent", accent)
                                .put("dwa", "wpgs")); // 启用中间结果
                        frame.put("data", new JSONObject()
                                .put("status", 0)
                                .put("format", format)
                                .put("encoding", "raw")
                                .put("audio", ""));

                        this.send(frame.toString());

                        // 发送音频数据
                        sendAudioData(audioData);
                    }

                    @Override
                    public void onMessage(String message) {
                        try {
                            JSONObject response = new JSONObject(message);
                            System.out.println("Received message from iFlyTek: " + response.toString(2));

                            // 检查是否存在 data 字段
                            if (!response.has("data")) {
                                handleErrorResponse(response, sessionId);
                                return;
                            }

                            JSONObject data = response.getJSONObject("data");
                            int status = data.getInt("status");

                            // 处理包含识别结果的消息（状态1和2）
                            if (status == 1 || status == 2) {
                                // 获取结果对象
                                JSONObject resultObj = data.getJSONObject("result");

                                // 提取识别文本
                                String text = extractTextFromResult(resultObj);

                                if (status == 1) {
                                    // 中间结果
                                    result.setPartialText(text);
                                    resultCache.put(sessionId, result);
                                    System.out.println("Partial result: " + text);
                                } else if (status == 2) {
                                    // 最终结果
                                    result.setFinalText(text);
                                    result.setFinal(true);
                                    result.setProgress(100);
                                    result.setStatus(0);
                                    resultCache.put(sessionId, result);
                                    System.out.println("Final result: " + text);
                                    finalResultReceived.set(true);
                                    success[0] = true; // 标记成功
                                }
                            }
                            // 处理其他状态（如状态0等）
                            else if (data.has("result")) {
                                // 可能有不同的数据结构
                                try {
                                    JSONObject resultObj = data.getJSONObject("result");
                                    String text = extractTextFromResult(resultObj);
                                    result.setPartialText(text);
                                    resultCache.put(sessionId, result);
                                } catch (JSONException e) {
                                    System.err.println("Alternative result parse error: " + e.getMessage());
                                }
                            }
                            // 处理错误状态
                            else {
                                handleUnknownStatus(status, data, sessionId);
                            }
                        } catch (JSONException e) {
                            System.err.println("JSON parsing error: " + e.getMessage());
                            System.err.println("Original message: " + message);
                            e.printStackTrace();

                            result.setStatus(2);
                            result.setFinalText("JSON解析错误: " + e.getMessage());
                            resultCache.put(sessionId, result);
                        }
                    }

                    /**
                     * 从讯飞API的复杂结果结构中提取文本 - 优化版
                     */
                    private String extractTextFromResult(JSONObject resultObj) throws JSONException {
                        // 1. 优先检查文本字段
                        if (resultObj.has("text")) {
                            return resultObj.getString("text");
                        }

                        // 2. 检查是否有完整句子结果
                        if (resultObj.has("cn") && resultObj.getJSONObject("cn").has("st")) {
                            JSONArray rtArray = resultObj.getJSONObject("cn").getJSONObject("st").getJSONArray("rt");
                            StringBuilder fullText = new StringBuilder();
                            for (int i = 0; i < rtArray.length(); i++) {
                                JSONObject rt = rtArray.getJSONObject(i);
                                if (rt.has("ws")) {
                                    JSONArray wsArray = rt.getJSONArray("ws");
                                    for (int j = 0; j < wsArray.length(); j++) {
                                        JSONObject ws = wsArray.getJSONObject(j);
                                        if (ws.has("cw")) {
                                            JSONArray cwArray = ws.getJSONArray("cw");
                                            for (int k = 0; k < cwArray.length(); k++) {
                                                JSONObject cw = cwArray.getJSONObject(k);
                                                if (cw.has("w")) {
                                                    fullText.append(cw.getString("w"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            return fullText.toString();
                        }

                        // 3. 回退到原始解析逻辑
                        if (resultObj.has("ws")) {
                            JSONArray wsArray = resultObj.getJSONArray("ws");
                            StringBuilder textBuilder = new StringBuilder();
                            for (int i = 0; i < wsArray.length(); i++) {
                                JSONObject wsItem = wsArray.getJSONObject(i);
                                if (wsItem.has("cw")) {
                                    JSONArray cwArray = wsItem.getJSONArray("cw");
                                    for (int j = 0; j < cwArray.length(); j++) {
                                        JSONObject cwItem = cwArray.getJSONObject(j);
                                        String word = cwItem.optString("w", "");
                                        textBuilder.append(word);
                                    }
                                }
                            }
                            return textBuilder.toString();
                        }

                        // 4. 返回空字符串作为后备
                        return "";
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        System.out.println("WebSocket closed. Code: " + code + ", Reason: " + reason);
                        if (code != 1000) {
                            result.setStatus(2);
                            result.setFinalText("连接关闭: " + reason);
                            resultCache.put(sessionId, result);
                        }

                        // 如果没有收到最终结果，尝试标记为失败
                        if (!finalResultReceived.get()) {
                            result.setStatus(2);
                            result.setFinalText("未收到最终结果: " + reason);
                            resultCache.put(sessionId, result);
                        }
                    }

                    @Override
                    public void onError(Exception ex) {
                        System.err.println("WebSocket error: " + ex.getMessage());
                        ex.printStackTrace();
                        result.setStatus(2);
                        result.setFinalText("连接错误: " + ex.getMessage());
                        resultCache.put(sessionId, result);
                        lastError[0] = ex;
                    }

                    private void sendAudioData(byte[] audioData) {
                        int chunkSize = 1280; // 讯飞推荐的块大小
                        System.out.println("开始发送音频数据，总大小: " + audioData.length + " 字节");

                        for (int i = 0; i < audioData.length; i += chunkSize) {
                            int end = Math.min(audioData.length, i + chunkSize);
                            byte[] chunk = new byte[end - i];
                            System.arraycopy(audioData, i, chunk, 0, chunk.length);

                            JSONObject frame = new JSONObject();
                            frame.put("data", new JSONObject()
                                    .put("status", 1)
                                    .put("format", format)
                                    .put("encoding", "raw")
                                    .put("audio", Base64.getEncoder().encodeToString(chunk)));

                            this.send(frame.toString());
                            System.out.println("发送音频分片: " + chunk.length + " 字节, 进度: " +
                                    ((i + chunk.length) * 100 / audioData.length) + "%");

                            // 更新进度
                            result.setProgress((int) ((i + chunk.length) * 100.0 / audioData.length));
                            resultCache.put(sessionId, result);

                            // 添加短暂延迟避免发送过快
                            try { Thread.sleep(10); } catch (InterruptedException e) {}
                        }

                        // 发送结束帧
                        JSONObject endFrame = new JSONObject();
                        endFrame.put("data", new JSONObject()
                                .put("status", 2)
                                .put("format", format)
                                .put("encoding", "raw")
                                .put("audio", ""));

                        this.send(endFrame.toString());
                        System.out.println("发送结束帧");
                    }
                };

                client.connectBlocking(5, TimeUnit.SECONDS);

                // 等待最终结果或超时
                long startTime = System.currentTimeMillis();
                while (!result.isFinal() &&
                        (System.currentTimeMillis() - startTime) < 15000) { // 15秒超时
                    Thread.sleep(500);
                }

                if (result.isFinal()) {
                    success[0] = true;
                } else {
                    System.out.println("识别超时，准备重试...");
                }
            } catch (Exception e) {
                System.err.println("流式识别异常: " + e.getMessage());
                e.printStackTrace();
                lastError[0] = e;
                // 等待后重试
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }

        if (!success[0]) {
            result.setStatus(2);
            String errorMsg = "识别失败";
            if (lastError[0] != null) {
                errorMsg += ": " + lastError[0].getMessage();
            } else if (result.getFinalText() != null && !result.getFinalText().isEmpty()) {
                errorMsg = result.getFinalText();
            }
            result.setFinalText(errorMsg);
            resultCache.put(sessionId, result);
        }

        return result;
    }

    private void handleErrorResponse(JSONObject response, String sessionId) {
        String errorMsg = "未知错误";

        try {
            if (response.has("message")) {
                errorMsg = response.getString("message");
            } else if (response.has("desc")) {
                errorMsg = response.getString("desc");
            } else if (response.has("code")) {
                int code = response.getInt("code");
                errorMsg = "错误代码: " + code;
            }
        } catch (JSONException e) {
            errorMsg = "解析错误消息失败: " + e.getMessage();
        }

        System.err.println("iFlyTek API error: " + errorMsg);

        RecognitionResult result = resultCache.getOrDefault(sessionId, new RecognitionResult());
        result.setStatus(2);
        result.setFinalText("识别失败: " + errorMsg);
        resultCache.put(sessionId, result);
    }

    private void handleUnknownStatus(int status, JSONObject data, String sessionId) {
        String errorMsg = "未知状态码: " + status;

        try {
            if (data.has("message")) {
                errorMsg += ", 消息: " + data.getString("message");
            }
        } catch (JSONException e) {
            errorMsg += ", 解析消息失败: " + e.getMessage();
        }

        System.err.println("Unknown status code: " + errorMsg);

        RecognitionResult result = resultCache.getOrDefault(sessionId, new RecognitionResult());
        result.setStatus(2);
        result.setFinalText("识别错误: " + errorMsg);
        resultCache.put(sessionId, result);
    }

    /**
     * 文件语音识别
     */
    public RecognitionResult fileRecognize(byte[] audioData, String sessionId,
                                           String format, String domain, String accent) {
        RecognitionResult result = new RecognitionResult();
        try {
            // 生成鉴权URL
            String authUrl = generateAuthUrl(sessionId, domain, accent);

            CountDownLatch latch = new CountDownLatch(1);

            // 创建WebSocket客户端
            WebSocketClient client = new WebSocketClient(new URI(authUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    // 发送开始帧
                    JSONObject frame = new JSONObject();
                    frame.put("common", new JSONObject()
                            .put("app_id", appId)); // 使用注入的appId
                    frame.put("business", new JSONObject()
                            .put("domain", domain)
                            .put("accent", accent));
                    frame.put("data", new JSONObject()
                            .put("status", 0)
                            .put("format", format)
                            .put("encoding", "raw")
                            .put("audio", ""));

                    this.send(frame.toString());

                    // 发送整个音频数据
                    JSONObject dataFrame = new JSONObject();
                    dataFrame.put("data", new JSONObject()
                            .put("status", 1)
                            .put("format", format)
                            .put("encoding", "raw")
                            .put("audio", Base64.getEncoder().encodeToString(audioData)));

                    this.send(dataFrame.toString());

                    // 发送结束帧
                    JSONObject endFrame = new JSONObject();
                    endFrame.put("data", new JSONObject()
                            .put("status", 2)
                            .put("format", format)
                            .put("encoding", "raw")
                            .put("audio", ""));

                    this.send(endFrame.toString());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject response = new JSONObject(message);
                        System.out.println("文件识别响应: " + response.toString(2));

                        if (response.has("data")) {
                            JSONObject data = response.getJSONObject("data");
                            int status = data.getInt("status");

                            if (status == 2) {
                                // 最终结果
                                JSONObject resultObj = data.getJSONObject("result");
                                String finalText = extractTextFromResult(resultObj);
                                result.setFinalText(finalText);
                                result.setFinal(true);
                                result.setProgress(100);
                                result.setStatus(0);
                                latch.countDown();
                            }
                        }
                    } catch (JSONException e) {
                        System.err.println("文件识别JSON解析错误: " + e.getMessage());
                        result.setStatus(2);
                        result.setFinalText("解析错误: " + e.getMessage());
                        latch.countDown();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (code != 1000) {
                        result.setStatus(2);
                        result.setFinalText("连接关闭: " + reason);
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    result.setStatus(2);
                    result.setFinalText("连接错误: " + ex.getMessage());
                    latch.countDown();
                }

                private String extractTextFromResult(JSONObject resultObj) throws JSONException {
                    // 复用流式识别中的文本提取方法
                    if (resultObj.has("text")) {
                        return resultObj.getString("text");
                    }

                    if (resultObj.has("ws")) {
                        JSONArray wsArray = resultObj.getJSONArray("ws");
                        StringBuilder textBuilder = new StringBuilder();

                        for (int i = 0; i < wsArray.length(); i++) {
                            JSONObject wsItem = wsArray.getJSONObject(i);
                            if (wsItem.has("cw")) {
                                JSONArray cwArray = wsItem.getJSONArray("cw");
                                if (cwArray.length() > 0) {
                                    JSONObject cwItem = cwArray.getJSONObject(0);
                                    String word = cwItem.optString("w", "");
                                    textBuilder.append(word);
                                }
                            }
                        }
                        return textBuilder.toString();
                    }
                    return "";
                }
            };

            client.connectBlocking(5, TimeUnit.SECONDS);
            latch.await(30, TimeUnit.SECONDS); // 等待识别完成
            return result;
        } catch (Exception e) {
            result.setStatus(2);
            result.setFinalText("识别异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 获取最终识别结果
     */
    public String getFinalResult(String sessionId) {
        RecognitionResult result = resultCache.get(sessionId);
        if (result != null && result.isFinal()) {
            return result.getFinalText();
        }
        return "未获取到最终结果";
    }

    /**
     * 生成讯飞鉴权URL
     */
    private String generateAuthUrl(String sessionId, String domain, String accent) throws Exception {
        String hostUrl = speechWsUrl;

        // 生成RFC1123格式的时间戳
        String date = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));

        // 生成签名原始数据
        String signatureOrigin = String.format(
                "host: %s\n" +
                        "date: %s\n" +
                        "GET /v2/iat HTTP/1.1",
                "iat-api.xfyun.cn", date
        );

        // 计算签名
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(keySpec);
        byte[] signatureBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signatureBytes);

        // 构建鉴权参数
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signature
        );

        // URL编码
        String auth = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        // 构建最终URL
        String finalUrl = String.format(
                "%s?authorization=%s&date=%s&host=%s",
                hostUrl,
                URLEncoder.encode(auth, "UTF-8"),
                URLEncoder.encode(date, "UTF-8"),
                URLEncoder.encode("iat-api.xfyun.cn", "UTF-8")
        );

        System.out.println("Generated auth URL: " + finalUrl);
        return finalUrl;
    }
}