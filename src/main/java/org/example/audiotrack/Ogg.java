package org.example.audiotrack;

import ws.schild.jave.encode.AudioAttributes;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Ogg extends Audiotrack {
    private AudioAttributes audioAttributes;
    private File fileLink;

    public Ogg(String filePath) {
        checkFileFormat(filePath);
        File audioFile = new File(filePath);
        fileLink = audioFile;
        audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("vorbis");
        audioAttributes.setBitRate(128000);
        audioAttributes.setChannels(2);
        audioAttributes.setSamplingRate(44100);
    }


    private void checkFileFormat(String path) {
        try {
            if (!Files.probeContentType(Path.of(path)).equals("audio/ogg")) {
                throw new IllegalArgumentException("Wrong audio format");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }


    public File getFileLink() {
        return fileLink;
    }

    @Override
    public AudioInputStream getAudioInputStream() {
        return null;
    }

    @Override
    public Audiotrack copy() {
        return null;
    }
}
