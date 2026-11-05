package com.vku.chat.client;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class AudioRecorderSimulator {

    public static byte[] generateSimulatedAudio(int durationSec) {
        // Generate standard 8000Hz 8-bit mono sine wave PCM audio with harmonic modulation
        int sampleRate = 8000;
        int totalSamples = sampleRate * durationSec;
        byte[] buffer = new byte[totalSamples];
        for (int i = 0; i < totalSamples; i++) {
            double angle = 2.0 * Math.PI * i / (sampleRate / 440.0);
            double modulation = Math.sin(2.0 * Math.PI * i / (sampleRate / 2.0));
            buffer[i] = (byte) (Math.sin(angle) * 100 * (0.5 + 0.5 * modulation));
        }
        return buffer;
    }

    public static void playAudio(byte[] audioData) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(8000f, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Thiết bị âm thanh không hỗ trợ phát Audio");
                    return;
                }
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(audioData, 0, audioData.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                System.err.println("Không thể phát âm thanh: " + e.getMessage());
            }
        }).start();
    }
}
