package org.example.audiotrack;

import ws.schild.jave.encode.AudioAttributes;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Wav extends Audiotrack {
    private AudioAttributes audioAttributes;
    private AudioInputStream audioInputStream;
    private File fileLink;

    public Wav(File audioFile) {
        checkFileFormat(audioFile.getPath());
        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
        fileLink = audioFile;
        audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("libmp3lame");
        audioAttributes.setBitRate(128000);
        audioAttributes.setChannels(2);
        audioAttributes.setSamplingRate(44100);
    }

    public Wav(String path) {
        checkFileFormat(path);
        File audioFile = new File(path);
        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
        fileLink = audioFile;
        audioAttributes = new AudioAttributes();
        
        audioAttributes.setBitRate(128000);
        audioAttributes.setChannels(2);
        audioAttributes.setSamplingRate(44100);
    }

    private Wav(AudioInputStream audioInputStream) {
        this.audioInputStream = audioInputStream;
        audioAttributes = new AudioAttributes();
        
        audioAttributes.setBitRate(128000);
        audioAttributes.setChannels(2);
        audioAttributes.setSamplingRate(44100);
    }

    private void checkFileFormat(String path) {
        try {
            if (!Files.probeContentType(Path.of(path)).equals("audio/wav")) {
                throw new IllegalArgumentException("Wrong audio format");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Wav copy() {
        return new Wav(this.audioInputStream);
    }

    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }

    public AudioInputStream getAudioInputStream() {
        return audioInputStream;
    }

    @Override
    public File getFileLink() {
        return fileLink;
    }
}
