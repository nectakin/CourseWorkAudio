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

public class AudioFlacConverter implements Converter<File> {
    private static AudioFlacConverter INSTANCE;
    private final EncodingAttributes encodingAttributes;
    private final Encoder encoder;

    private static final int DEFAULT_RATE = 44_100;
    private static final int DEFAULT_CHANNELS = 2;

    private AudioFlacConverter() {
        encodingAttributes = new EncodingAttributes();
        encodingAttributes.setOutputFormat("flac");
        encoder = new Encoder();
    }

    public static AudioFlacConverter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AudioFlacConverter();
        }
        return INSTANCE;
    }

    @Override
    public File convertTo(Audiotrack audiotrack) {
        AudioAttributes src = audiotrack.getAudioAttributes();
        AudioAttributes aa = new AudioAttributes();

        
        aa.setCodec("flac");
        int sRate   = (src != null ? src.getSamplingRate().orElse(DEFAULT_RATE)  : DEFAULT_RATE);
        int ch      = (src != null ? src.getChannels().orElse(DEFAULT_CHANNELS)  : DEFAULT_CHANNELS);
        aa.setSamplingRate(sRate);
        aa.setChannels(ch);

        encodingAttributes.setAudioAttributes(aa);

        try {
            File in = audiotrack.getFileLink();
            String base = stripExt(in.getName());
            Path outPath = in.toPath().resolveSibling(base + " (converted to flac).flac");
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
