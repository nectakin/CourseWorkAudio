package org.example.server;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

final class RevisionManager {

    private static File sessionDir = null;
    private static int  counter    = 0;

    private RevisionManager() {}

    
    public static void startSession(File baseFile, File workingWav) {
        try {
            String base = (baseFile != null ? stripExt(baseFile.getName()) : "session");
            String ts   = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            sessionDir  = new File("target/revisions/" + base + "-" + ts);
            if (!sessionDir.exists()) sessionDir.mkdirs();
            counter = 0;

            
            saveRevision("select", -1, -1, workingWav);
        } catch (Exception ignore) {
            
        }
    }

    
    public static void saveRevision(String action, long startFrame, long endFrame, File workingWav) {
        if (sessionDir == null) return;      
        if (workingWav == null || !workingWav.exists()) return;

        counter++;
        String idx = String.format("%04d", counter);
        File snap  = new File(sessionDir, "rev-" + idx + ".wav");

        try {
            
            Files.copy(workingWav.toPath(), snap.toPath(), StandardCopyOption.REPLACE_EXISTING);

            
            double sr = 0.0;
            long frames = 0L;
            double dur  = 0.0;
            try (AudioInputStream in = AudioSystem.getAudioInputStream(snap)) {
                AudioFormat fmt = in.getFormat();
                int frameSize   = Math.max(1, fmt.getFrameSize());
                frames = totalFrames(in, frameSize);
                sr     = fmt.getSampleRate();
                dur    = (sr > 0 ? frames / sr : 0.0);
            } catch (Exception ignored) {}

            
            File log = new File(sessionDir, "log.txt");
            try (PrintWriter pw = new PrintWriter(new FileWriter(log, true))) {
                String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                pw.printf(
                        "%s | %s | start=%d | end=%d | frames=%d | sr=%.1f | dur=%.3f | file=%s%n",
                        ts, action, startFrame, endFrame, frames, sr, dur, snap.getName()
                );
            }
        } catch (Exception ignore) {
            
        }
    }

    
    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return (i > 0) ? n.substring(0, i) : n;
    }

    private static long totalFrames(AudioInputStream ais, int frameSize) throws IOException {
        long fl = ais.getFrameLength();
        if (fl > 0) return fl;
        int available = ais.available();
        return (available > 0 && frameSize > 0) ? (available / frameSize) : 0;
    }
}
