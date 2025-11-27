package org.example.audiotrack;

import ws.schild.jave.encode.AudioAttributes;

import java.io.File;

public class AudioAdapter{
    private final Audiotrack audiotrack;

    public AudioAdapter(Audiotrack audiotrack) {
        this.audiotrack = audiotrack;
    }

    public AudioAttributes adaptAttributes() {
        return audiotrack.getAudioAttributes();
    }

    public File adaptFile() {
        return audiotrack.getFileLink();
    }
}
