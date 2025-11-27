package org.example.converter;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import org.example.audiotrack.Audiotrack;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class AudioMp3Converter implements Converter<File> {
    private static AudioMp3Converter INSTANCE;
    private final EncodingAttributes encodingAttributes;
    private final Encoder encoder;

    private static final int DEFAULT_RATE = 44_100;
    private static final int DEFAULT_CHANNELS = 2;
    private static final int DEFAULT_BITRATE = 192_000;

    private AudioMp3Converter() {
        encodingAttributes = new EncodingAttributes();
        encodingAttributes.setOutputFormat("mp3");
        encoder = new Encoder();
    }

    public static AudioMp3Converter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AudioMp3Converter();
        }
        return INSTANCE;
    }

    @Override
    public File convertTo(Audiotrack audiotrack) {
        AudioAttributes src = audiotrack.getAudioAttributes();
        AudioAttributes aa = new AudioAttributes();

        
        aa.setCodec("libmp3lame");
        int sRate   = (src != null ? src.getSamplingRate().orElse(DEFAULT_RATE)  : DEFAULT_RATE);
        int ch      = (src != null ? src.getChannels().orElse(DEFAULT_CHANNELS)  : DEFAULT_CHANNELS);
        aa.setSamplingRate(sRate);
        aa.setChannels(ch);
        aa.setBitRate(DEFAULT_BITRATE);

        encodingAttributes.setAudioAttributes(aa);

        try {
            File in = audiotrack.getFileLink();
            String base = stripExt(in.getName());
            Path outPath = in.toPath().resolveSibling(base + " (converted to mp3).mp3");
            File out = uniqueFile(outPath.toFile());

            MultimediaObject input = new MultimediaObject(in);
            encoder.encode(input, out, encodingAttributes);

            System.out.println("[SAVE] -> " + out.getAbsolutePath());
            return out;
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        }
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
        
    }

    
    private static File uniqueFile(File target) {
        if (!target.exists()) return target;
        String name = target.getName();
        String parent = target.getParent();
        String base = stripExt(name);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        int i = 2;
        while (true) {
            File alt = new File(parent, base + " (" + i + ")" + ext);
            if (!alt.exists()) return alt;
            i++;
        }
    }
}
