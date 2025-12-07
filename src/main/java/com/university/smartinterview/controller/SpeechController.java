package com.university.smartinterview.controller;

import com.university.smartinterview.dto.response.ResponseWrapper;
import com.university.smartinterview.dto.response.SpeechRecognitionRes;
import com.university.smartinterview.service.SpeechService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/")
public class SpeechController {

    private final SpeechService speechService;

    @Autowired
    public SpeechController(SpeechService speechService) {
        this.speechService = speechService;
    }

    /*
     * 实时语音识别（流式传输）
     * @param sessionId 会话ID
     * @param audio 音频片段
     * @return 实时识别结果

     */

    @PostMapping("/stream-recognize")
    public ResponseEntity<ResponseWrapper<SpeechRecognitionRes>> streamRecognition(
            @RequestParam String sessionId,
            @RequestParam("audio") MultipartFile audioFile) {

        try {
            byte[] audioStream = audioFile.getBytes();
            SpeechRecognitionRes result = speechService.streamingRecognition(sessionId, audioStream);

            return ResponseEntity.ok(ResponseWrapper.success(result));
        } catch (IOException e) {
            // 处理文件读取异常
            SpeechRecognitionRes error = new SpeechRecognitionRes();
            error.setStatus(2);
            error.setFinalResult("音频处理错误: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseWrapper.error(error));
        } catch (Exception e) {
            // 处理其他异常
            SpeechRecognitionRes error = new SpeechRecognitionRes();
            error.setStatus(2);
            error.setFinalResult("识别错误: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error(error));
        }
    }


    /**
     * 音频文件识别
     * @param file 上传的音频文件
     * @return 识别结果
     *
     */

    @PostMapping("/file-recognize")
    public ResponseEntity<ResponseWrapper<SpeechRecognitionRes>> fileRecognition(
            @RequestParam("file") MultipartFile file) {
        SpeechRecognitionRes result = speechService.fileRecognition(file);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    /**
     * 获取实时语音识别的最终结果
     * @param sessionId 会话ID
     * @return 最终识别文本

     */

    @GetMapping("/final-result")
    public ResponseEntity<ResponseWrapper<String>> getFinalRecognitionResult(
            @RequestParam String sessionId) {
        String result = speechService.getFinalRecognitionResult(sessionId);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }
}

