package org.example;

import javax.sound.sampled.*;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.ServiceLoader;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

public class AudioDiag {

    public static void main(String[] args) {
        printFileTypes();
        printProviders();

        System.out.println("\nOpen attempts:");
        List.of(
            Paths.get("testsongs/mpthree.mp3"),
            Paths.get("testsongs/ogg.ogg"),
            Paths.get("testsongs/flac.flac")
        ).forEach(AudioDiag::tryOpenSmart);
    }

    
    private static void printFileTypes() {
        System.out.println("File types visible:");
        for (AudioFileFormat.Type t : AudioSystem.getAudioFileTypes()) {
            System.out.println(" * ." + t.getExtension());
        }
    }

    
    private static void printProviders() {
        System.out.println("\nAudioFileReaders loaded:");
        for (AudioFileReader r : ServiceLoader.load(AudioFileReader.class)) {
            System.out.println(" * " + r.getClass().getName());
        }
    }

    
    private static void tryOpenSmart(Path p) {
        String label = p.toString();
        try (AudioInputStream ais = openWithJavaSound(p)) {
            AudioFormat f = ais.getFormat();
            System.out.printf("OK   %-22s  %s, %.1f kHz, %d-bit, ch=%d%n",
                label, f.getEncoding(), f.getSampleRate() / 1000.0, f.getSampleSizeInBits(), f.getChannels());
            return;
        } catch (Throwable ex) {
            System.out.printf("SPI FAIL %-18s  %s: %s%n", label, ex.getClass().getSimpleName(), ex.getMessage());
        }

        
        try {
            File in = p.toFile();
            MultimediaObject mo = new MultimediaObject(in);
            MultimediaInfo info = mo.getInfo();
            String codec = (info != null && info.getAudio() != null) ? info.getAudio().getDecoder() : "n/a";
            System.out.println("  JAVE sees codec: " + codec + " → decoding to WAV and retrying via JavaSound...");

            Path tmp = Paths.get("target", "tmp_" + p.getFileName() + ".wav");
            decodeToWav(p, tmp);

            try (AudioInputStream ais2 = openWithJavaSound(tmp)) {
                AudioFormat f2 = ais2.getFormat();
                System.out.printf("OK   %-22s  %s, %.1f kHz, %d-bit, ch=%d  (via JAVE → %s)%n",
                    label, f2.getEncoding(), f2.getSampleRate() / 1000.0, f2.getSampleSizeInBits(), f2.getChannels(), tmp);
            }
        } catch (Throwable ex2) {
            System.out.printf("JAVE FAIL %-16s  %s: %s%n", label, ex2.getClass().getSimpleName(), ex2.getMessage());
        }
    }

    
    private static AudioInputStream openWithJavaSound(Path p) throws Exception {
        return AudioSystem.getAudioInputStream(
            new BufferedInputStream(Files.newInputStream(p)));
    }

    
    private static void decodeToWav(Path in, Path out) throws EncoderException, IOException {
        Files.createDirectories(out.getParent());

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("pcm_s16le");      
        audio.setChannels(2);
        audio.setSamplingRate(44100);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("wav");
        attrs.setAudioAttributes(audio);

        new Encoder().encode(new MultimediaObject(in.toFile()), out.toFile(), attrs);
    }
}
