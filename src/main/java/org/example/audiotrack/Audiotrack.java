package org.example.audiotrack;

import ws.schild.jave.encode.AudioAttributes;

import javax.sound.sampled.AudioInputStream;
import java.io.File;

public abstract class Audiotrack {
    public abstract AudioAttributes getAudioAttributes();
    public abstract File getFileLink();
    public abstract AudioInputStream getAudioInputStream();
    public abstract Audiotrack copy();
}
