package com.university.smartinterview.service;

import com.university.smartinterview.dto.response.SpeechRecognitionRes;
import org.springframework.web.multipart.MultipartFile;

public interface SpeechService {

    /**
     * 流式语音识别（同步处理）
     * @param sessionId 会话ID
     * @param audioStream 音频流
     * @return 语音识别响应（包含部分结果和最终结果）
     */
    SpeechRecognitionRes streamingRecognition(String sessionId, byte[] audioStream);

    /**
     * 文件语音识别
     * @param file 音频文件
     * @return 语音识别响应
     */
    SpeechRecognitionRes fileRecognition(MultipartFile file);

    /**
     * 获取最终识别结果
     * @param sessionId 会话ID
     * @return 最终识别文本
     */
    String getFinalRecognitionResult(String sessionId);
}