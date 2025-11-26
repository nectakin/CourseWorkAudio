package org.example.audiotrack;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;


public class AudioWaveformPanel extends JPanel {
    private byte[] audioBytes;           
    private AudioFormat format;          
    private int channels;                

    public AudioWaveformPanel(File audioFile) {
        loadAudioFile(audioFile);
    }

    private void loadAudioFile(File audioFile) {
        try (AudioInputStream src = AudioIO.open(audioFile)) {
            AudioFormat srcFmt = src.getFormat();

            
            AudioFormat pcm16 = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFmt.getSampleRate(),
                    16,
                    srcFmt.getChannels(),
                    srcFmt.getChannels() * 2,      
                    srcFmt.getSampleRate(),
                    false                           
            );

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcm16, src)) {
                this.audioBytes = pcm.readAllBytes();
                this.format = pcm16;
                this.channels = pcm16.getChannels();
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            this.audioBytes = null;
            this.format = null;
            this.channels = 0;
        } catch (Exception e) {
            
            e.printStackTrace();
            this.audioBytes = null;
            this.format = null;
            this.channels = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (audioBytes == null || format == null) {
            g.drawString("No audio loaded", getWidth() / 2 - 50, getHeight() / 2);
            return;
        }

        int midY = getHeight() / 2;
        int frames = audioBytes.length / (2 * Math.max(1, channels)); 
        int step = Math.max(frames / Math.max(1, getWidth()), 1);

        g.setColor(Color.BLUE);

        for (int x = 0; x < getWidth(); x++) {
            int frameIndex = x * step;
            int sample = sampleAtFrame(frameIndex); 
            int y = midY - (int) (sample * (midY - 10) / 32768.0); 
            g.drawLine(x, midY, x, y);
        }
    }

    
    private int sampleAtFrame(int frameIndex) {
        int frameSize = 2 * channels; 
        int base = frameIndex * frameSize;

        if (base + frameSize > audioBytes.length) return 0;

        if (channels == 1) {
            return readLe16(audioBytes, base);
        } else {
            long sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                sum += readLe16(audioBytes, base + ch * 2);
            }
            return (int) (sum / channels);
        }
    }

    
    private static int readLe16(byte[] arr, int off) {
        int lo = arr[off] & 0xFF;
        int hi = arr[off + 1]; 
        return (hi << 8) | lo;
    }
}
