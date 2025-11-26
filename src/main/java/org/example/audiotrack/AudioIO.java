package org.example.audiotrack;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;


public final class AudioIO {
    private AudioIO() {}

    
    public static AudioInputStream open(Path path) throws Exception {
        try {
            return AudioSystem.getAudioInputStream(
                new BufferedInputStream(Files.newInputStream(path)));
        } catch (UnsupportedAudioFileException ex) {
            Path tmp = toWavWithJAVE(path);
            return AudioSystem.getAudioInputStream(tmp.toFile());
        }
    }

    
    public static AudioInputStream open(File file) throws Exception {
        return open(file.toPath());
    }

    
    public static String probeCodec(Path path) {
        try {
            MultimediaObject mo = new MultimediaObject(path.toFile());
            MultimediaInfo info = mo.getInfo();
            return (info != null && info.getAudio() != null) ? info.getAudio().getDecoder() : "n/a";
        } catch (Exception e) {
            return "n/a";
        }
    }

    
    private static Path toWavWithJAVE(Path in) throws IOException, EncoderException {
        Path targetDir = Paths.get("target");
        Files.createDirectories(targetDir);

        Path out = targetDir.resolve("tmp_" + in.getFileName() + ".wav");

        AudioAttributes a = new AudioAttributes();
        a.setCodec("pcm_s16le");
        a.setChannels(2);
        a.setSamplingRate(44100);

        EncodingAttributes enc = new EncodingAttributes();
        enc.setOutputFormat("wav");
        enc.setAudioAttributes(a);

        new Encoder().encode(new MultimediaObject(in.toFile()), out.toFile(), enc);
        return out;
    }
}
