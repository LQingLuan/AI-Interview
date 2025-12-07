// src/main/java/com/university/smartinterview/utils/AudioConverter.java
package com.university.smartinterview.utils;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 音频格式转换工具类
 * 支持常见音频格式转换、采样率调整、声道转换等操作
 */
public class AudioConverter {

    // 目标音频格式（讯飞语音识别要求）
    private static final AudioFormat TARGET_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, // 编码格式
            16000,               // 采样率 (Hz)
            16,                  // 采样位数
            1,                   // 声道数 (单声道)
            2,                   // 帧大小 (字节)
            16000,               // 帧速率 (Hz)
            false                // 是否大端序
    );

    /**
     * 将任意音频格式转换为讯飞要求的PCM格式
     * @param audioData 原始音频数据
     * @param originalFormat 原始音频格式
     * @return 转换后的PCM音频数据
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public static byte[] convertToPcm(byte[] audioData, AudioFormat originalFormat)
            throws UnsupportedAudioFileException, IOException {

        // 如果已经是目标格式，直接返回
        if (isTargetFormat(originalFormat)) {
            return audioData;
        }

        // 创建音频输入流
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream sourceStream = new AudioInputStream(bais, originalFormat, audioData.length / originalFormat.getFrameSize())) {

            // 转换为目标格式
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(TARGET_FORMAT, sourceStream);

            // 读取转换后的数据
            return readAllBytes(convertedStream);
        }
    }

    /**
     * 将MP3音频转换为PCM格式
     * @param mp3Data MP3音频数据
     * @return PCM格式音频数据
     */
    public static byte[] convertMp3ToPcm(byte[] mp3Data) throws UnsupportedAudioFileException, IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(mp3Data);
             AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(bais)) {

            // 获取MP3格式信息
            AudioFormat mp3Format = mp3Stream.getFormat();

            // 转换为目标格式
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(TARGET_FORMAT, mp3Stream);

            return readAllBytes(pcmStream);
        }
    }

    /**
     * 将WAV音频转换为PCM格式
     * @param wavData WAV音频数据
     * @return PCM格式音频数据
     */
    public static byte[] convertWavToPcm(byte[] wavData) throws UnsupportedAudioFileException, IOException {
        // WAV文件可能包含头部信息，需要提取纯音频数据
        return extractPcmFromWav(wavData);
    }

    /**
     * 从WAV文件中提取PCM数据
     * @param wavData 完整的WAV文件数据
     * @return 纯PCM音频数据
     */
    private static byte[] extractPcmFromWav(byte[] wavData) {
        // WAV文件头通常是44字节
        final int WAV_HEADER_SIZE = 44;

        if (wavData.length <= WAV_HEADER_SIZE) {
            return wavData;
        }

        // 返回去掉头部的纯PCM数据
        return Arrays.copyOfRange(wavData, WAV_HEADER_SIZE, wavData.length);
    }

    /**
     * 调整音频采样率
     * @param audioData PCM音频数据
     * @param originalFormat 原始音频格式
     * @param targetSampleRate 目标采样率
     * @return 调整后的音频数据
     */
    public static byte[] resampleAudio(byte[] audioData, AudioFormat originalFormat, float targetSampleRate)
            throws UnsupportedAudioFileException, IOException {

        // 创建目标格式
        AudioFormat targetFormat = new AudioFormat(
                originalFormat.getEncoding(),
                targetSampleRate,
                originalFormat.getSampleSizeInBits(),
                originalFormat.getChannels(),
                originalFormat.getFrameSize(),
                targetSampleRate,
                originalFormat.isBigEndian()
        );

        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream sourceStream = new AudioInputStream(bais, originalFormat, audioData.length / originalFormat.getFrameSize())) {

            // 转换采样率
            AudioInputStream resampledStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);

            return readAllBytes(resampledStream);
        }
    }

    /**
     * 将立体声转换为单声道
     * @param stereoData 立体声PCM数据
     * @param format 原始音频格式
     * @return 单声道PCM数据
     */
    public static byte[] convertStereoToMono(byte[] stereoData, AudioFormat format) {
        if (format.getChannels() == 1) {
            return stereoData;
        }

        int sampleSize = format.getSampleSizeInBits() / 8;
        int frameSize = format.getFrameSize();
        int numFrames = stereoData.length / frameSize;

        // 单声道帧大小是立体声的一半
        byte[] monoData = new byte[numFrames * sampleSize];

        for (int i = 0; i < numFrames; i++) {
            int stereoIndex = i * frameSize;
            int monoIndex = i * sampleSize;

            // 简单平均法合并左右声道
            for (int j = 0; j < sampleSize; j++) {
                int left = stereoData[stereoIndex + j] & 0xFF;
                int right = stereoData[stereoIndex + sampleSize + j] & 0xFF;
                int avg = (left + right) / 2;
                monoData[monoIndex + j] = (byte) avg;
            }
        }

        return monoData;
    }

    /**
     * 音频降噪处理（简单实现）
     * @param pcmData PCM音频数据
     * @param threshold 降噪阈值 (0-128)
     * @return 降噪后的音频数据
     */
    public static byte[] reduceNoise(byte[] pcmData, int threshold) {
        byte[] cleanedData = new byte[pcmData.length];

        for (int i = 0; i < pcmData.length; i++) {
            int sample = pcmData[i] & 0xFF;

            // 简单阈值降噪
            if (Math.abs(sample - 128) < threshold) {
                cleanedData[i] = (byte) 128; // 静音
            } else {
                cleanedData[i] = pcmData[i];
            }
        }

        return cleanedData;
    }

    /**
     * 标准化音频音量
     * @param pcmData PCM音频数据
     * @param targetPeak 目标峰值 (0-127)
     * @return 标准化后的音频数据
     */
    public static byte[] normalizeVolume(byte[] pcmData, int targetPeak) {
        // 1. 找到当前最大峰值
        int currentPeak = 0;
        for (byte b : pcmData) {
            int sample = Math.abs(b - 128);
            if (sample > currentPeak) {
                currentPeak = sample;
            }
        }

        // 如果已经是静音或峰值太小，直接返回
        if (currentPeak == 0 || currentPeak >= targetPeak) {
            return pcmData;
        }

        // 2. 计算放大系数
        double ratio = (double) targetPeak / currentPeak;

        // 3. 应用放大
        byte[] normalizedData = new byte[pcmData.length];
        for (int i = 0; i < pcmData.length; i++) {
            int sample = pcmData[i] & 0xFF;
            int centered = sample - 128;
            int amplified = (int) (centered * ratio);

            // 限制在有效范围内
            if (amplified > 127) amplified = 127;
            if (amplified < -128) amplified = -128;

            normalizedData[i] = (byte) (amplified + 128);
        }

        return normalizedData;
    }

    /**
     * 检查音频格式是否符合目标要求
     * @param format 音频格式
     * @return 是否符合要求
     */
    private static boolean isTargetFormat(AudioFormat format) {
        return format.getEncoding().equals(TARGET_FORMAT.getEncoding()) &&
                format.getSampleRate() == TARGET_FORMAT.getSampleRate() &&
                format.getSampleSizeInBits() == TARGET_FORMAT.getSampleSizeInBits() &&
                format.getChannels() == TARGET_FORMAT.getChannels() &&
                format.getFrameSize() == TARGET_FORMAT.getFrameSize() &&
                format.getFrameRate() == TARGET_FORMAT.getFrameRate() &&
                format.isBigEndian() == TARGET_FORMAT.isBigEndian();
    }

    /**
     * 从AudioInputStream读取所有字节
     * @param stream 音频输入流
     * @return 音频字节数组
     * @throws IOException
     */
    private static byte[] readAllBytes(AudioInputStream stream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = stream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    /**
     * 获取目标音频格式
     * @return 目标音频格式对象
     */
    public static AudioFormat getTargetFormat() {
        return TARGET_FORMAT;
    }

    /**
     * 获取目标音频格式参数
     * @return 格式参数描述
     */
    public static String getTargetFormatDescription() {
        return String.format("PCM_SIGNED, 16000 Hz, 16 bit, mono, %d bytes/frame", TARGET_FORMAT.getFrameSize());
    }

    /**
     * 获取音频的基本信息
     * @param audioData 音频数据
     * @param format 音频格式
     * @return 音频信息字符串
     */
    public static String getAudioInfo(byte[] audioData, AudioFormat format) {
        int duration = (int) ((audioData.length / format.getFrameSize()) / format.getFrameRate() * 1000);

        return String.format(
                "格式: %s, 采样率: %.1f kHz, 位深: %d bit, 声道: %d, 时长: %d ms, 大小: %d bytes",
                format.getEncoding(),
                format.getSampleRate() / 1000.0,
                format.getSampleSizeInBits(),
                format.getChannels(),
                duration,
                audioData.length
        );
    }
}