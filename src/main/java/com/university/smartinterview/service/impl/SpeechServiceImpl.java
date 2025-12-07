package com.university.smartinterview.service.impl;

import com.university.smartinterview.config.IflytekConfig;
import com.university.smartinterview.dto.response.SpeechRecognitionRes;
import com.university.smartinterview.service.SpeechService;
import com.university.smartinterview.utils.IflytekSpeechRecognition;
import io.jsonwebtoken.io.IOException;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

@Service
public class SpeechServiceImpl implements SpeechService {

    private final IflytekSpeechRecognition speechRecognition;
    private final IflytekConfig.SpeechRecognitionConfig speechConfig;

    @Autowired
    public SpeechServiceImpl(IflytekSpeechRecognition speechRecognition,
                             IflytekConfig.SpeechRecognitionConfig speechConfig) {
        this.speechRecognition = speechRecognition;
        this.speechConfig = speechConfig;
    }

    @Override
    public SpeechRecognitionRes streamingRecognition(String sessionId, byte[] audioStream) {
        try {
            // 添加音频格式验证
            System.out.println("原始音频大小: " + audioStream.length + " bytes");
            if (audioStream.length < 500) {
                throw new IOException("音频数据过小，可能未捕获到有效声音");
            }
            byte[] pcmAudio = convertWebmToPcm(audioStream);
            // 调用语音识别工具类进行流式识别
            IflytekSpeechRecognition.RecognitionResult result =
                    speechRecognition.streamingRecognize(
                            pcmAudio, // 使用转换后的音频
                            sessionId,
                            speechConfig.getAudioFormat(),
                            speechConfig.getDomain(),
                            speechConfig.getAccent()
                    );

            // 创建响应对象
            SpeechRecognitionRes response = new SpeechRecognitionRes();
            response.setSessionId(sessionId);
            response.setPartialResult(result.getPartialText());
            response.setFinalResult(result.getFinalText());
            response.setIsFinal(result.isFinal());
            response.setProgress(result.getProgress());
            response.setStatus(result.getStatus());

            return response;
        } catch (Exception e) {
            // 添加详细错误日志
            System.err.println("流式识别错误: " + e.getMessage());
            e.printStackTrace();
            // 创建错误响应
            SpeechRecognitionRes errorResponse = new SpeechRecognitionRes();
            errorResponse.setStatus(2);
            errorResponse.setFinalResult("流式识别错误: " + e.getMessage());
            return errorResponse;
        }
    }

    private byte[] convertWebmToPcm(byte[] webmData) throws Exception {
        // 实际项目中应使用FFmpeg或音频库进行转换
        // 这里是简化示例 - 实际需要完整实现
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000, 16, 1, 2, 16000, false);

        ByteArrayInputStream bais = new ByteArrayInputStream(webmData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 实际转换逻辑应使用AudioSystem或FFmpeg
        // 这里简化返回原始数据（仅示例）
        return webmData;
    }

/*文件识别方法*/
    @Override
    public SpeechRecognitionRes fileRecognition(MultipartFile file) {
        try {

            // 转换文件为字节数组
            byte[] audioData = file.getBytes();

            // 生成会话ID
            String sessionId = "FILE_" + System.currentTimeMillis();

            // 调用语音识别
            IflytekSpeechRecognition.RecognitionResult result =
                    speechRecognition.fileRecognize(
                            audioData,
                            sessionId,
                            speechConfig.getAudioFormat(),
                            speechConfig.getDomain(),
                            speechConfig.getAccent()
                    );

            // 创建响应对象
            SpeechRecognitionRes response = new SpeechRecognitionRes();
            response.setSessionId(sessionId);
            response.setPartialResult(""); // 文件识别没有部分结果
            response.setFinalResult(result.getFinalText());
            response.setIsFinal(true);
            response.setProgress(100);
            response.setStatus(result.getStatus());

            return response;
        } catch (Exception e) {
            // 创建错误响应
            SpeechRecognitionRes errorResponse = new SpeechRecognitionRes();
            errorResponse.setStatus(2);
            errorResponse.setFinalResult("文件识别错误: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    public String getFinalRecognitionResult(String sessionId) {
        // 从语音识别工具类获取最终结果
        return speechRecognition.getFinalResult(sessionId);
    }
}