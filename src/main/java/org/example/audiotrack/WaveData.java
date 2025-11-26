package org.example.audiotrack;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WaveData {
    private byte[] audioBytes;
    private int[]  audioData;
    private AudioFormat format;
    private double durationSec;
    private double durationMSec;

    public WaveData() {}

    
    public int[] extractAmplitudeFromFile(File file) {
        try {
            return extract(Path.of(file.toURI()));
        } catch (Exception e) {
            
            return new int[0];
        }
    }

    

    private int[] extract(Path path) throws Exception {
        
        try (AudioInputStream src = AudioIO.open(path)) {
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
                this.format = pcm16;

                byte[] bytes = readAllBytes(pcm);
                this.audioBytes = bytes;

                int channels  = pcm16.getChannels();
                int frameSize = 2 * channels;              
                int frames    = bytes.length / frameSize;

                if (frames <= 0) {
                    this.audioData   = new int[0];
                    this.durationSec = 0;
                    this.durationMSec= 0;
                    return this.audioData;
                }

                
                this.durationSec  = frames / pcm16.getSampleRate();
                this.durationMSec = durationSec * 1000.0;

                
                int target = Math.min(frames, 2000);
                int step   = Math.max(frames / target, 1);

                int[] out = new int[frames / step];
                int oi = 0;

                for (int f = 0; f + step <= frames; f += step) {
                    long sum = 0;
                    int  n   = 0;

                    for (int i = 0; i < step; i++) {
                        int base = (f + i) * frameSize;

                        if (channels == 1) {
                            sum += readLe16(bytes, base);
                            n++;
                        } else {
                            long chSum = 0;
                            for (int ch = 0; ch < channels; ch++) {
                                chSum += readLe16(bytes, base + ch * 2);
                            }
                            sum += (int)(chSum / channels);
                            n++;
                        }
                    }
                    out[oi++] = (int)(sum / Math.max(1, n));
                }

                if (oi != out.length) {
                    int[] trimmed = new int[oi];
                    System.arraycopy(out, 0, trimmed, 0, oi);
                    out = trimmed;
                }

                this.audioData = out;
                return out;
            }
        }
    }

    private static int readLe16(byte[] a, int off) {
        int lo = a[off] & 0xFF;
        int hi = a[off + 1];        
        return (hi << 8) | lo;      
    }

    private static byte[] readAllBytes(AudioInputStream ais) throws IOException {
        int frameSize = Math.max(1, ais.getFormat().getFrameSize());
        long framesLen = ais.getFrameLength();

        if (framesLen > 0) {
            int total = Math.toIntExact(framesLen * frameSize);
            byte[] buf = new byte[total];
            int pos = 0;
            while (pos < total) {
                int r = ais.read(buf, pos, total - pos);
                if (r < 0) break;
                pos += r;
            }
            if (pos == total) return buf;
            byte[] trimmed = new byte[pos];
            System.arraycopy(buf, 0, trimmed, 0, pos);
            return trimmed;
        } else {
            byte[] tmp = new byte[64 * 1024];
            int r;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            while ((r = ais.read(tmp)) >= 0) out.write(tmp, 0, r);
            return out.toByteArray();
        }
    }

    

    public byte[] getAudioBytes()        { return audioBytes; }
    public double getDurationSec()       { return durationSec; }
    public double getDurationMiliSec()   { return durationMSec; }
    public int[]  getAudioData()         { return audioData; }
    public AudioFormat getFormat()       { return format; }
}
