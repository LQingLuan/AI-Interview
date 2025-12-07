// src/main/java/com/university/smartinterview/dto/request/SpeechToTextReq.java
package com.university.smartinterview.dto.request;

import org.springframework.web.multipart.MultipartFile;

public class SpeechToTextReq {

    private String sessionId;
    private MultipartFile audioFile;
    private byte[] audioStream;

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }

    public byte[] getAudioStream() {
        return audioStream;
    }

    public void setAudioStream(byte[] audioStream) {
        this.audioStream = audioStream;
    }
}