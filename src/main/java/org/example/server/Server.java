package org.example.server;

import org.example.audiotrack.WaveData;

import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

public class Server {

    private static File sourceFile;
    private static File workingFile;
    private static File clipboardFile;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(55555)) {
            System.out.println("Server started on port 55555");

            try (Socket clientSocket = serverSocket.accept();
                 ObjectInputStream  ois = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

                System.out.println("Client connected: " + clientSocket.getInetAddress());

                while (true) {
                    String cmd = (String) ois.readObject();
                    if (cmd == null) continue;

                    if (Objects.equals(cmd, "select")) {
                        sourceFile = (File) ois.readObject();
                        try {
                            workingFile = prepareWorkingFile(sourceFile);

                            if (workingFile == null || !workingFile.exists() || workingFile.length() < 1024) {
                                oos.writeObject("Помилка відкриття: FFmpeg/JavaSound створили порожній WAV. " +
                                        "Спробуйте інший файл або перезапишіть його іншим енкодером.");
                            } else {
                                int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                                if (data == null || data.length == 0) {
                                    oos.writeObject("Не вдалося прочитати WAV (0 семплів). " +
                                            "Можливо, нестандартні параметри/заголовки.");
                                } else {
                                    RevisionManager.startSession(sourceFile, workingFile);
                                    oos.writeObject(data);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            oos.writeObject("Не вдалося відкрити файл: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "copy")) {
                        
                        double startSec = (double) ois.readObject();
                        double endSec   = (double) ois.readObject();
                        try {
                            long[] actual = copyAudioSecSafe(startSec, endSec);
                            int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                            RevisionManager.saveRevision("copy", actual[0], actual[1], workingFile);
                            oos.writeObject(data);
                        } catch (Exception ex) {
                            System.out.println("copyAudio error: " + ex);
                            oos.writeObject("Помилка copy: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "cut")) {
                        
                        double startSec = (double) ois.readObject();
                        double endSec   = (double) ois.readObject();
                        try {
                            long[] actual = cutAudioSecSafe(startSec, endSec);
                            int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                            RevisionManager.saveRevision("cut", actual[0], actual[1], workingFile);
                            oos.writeObject(data);
                        } catch (Exception ex) {
                            System.out.println("cutAudio error: " + ex);
                            oos.writeObject("Помилка cut: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "paste")) {
                        double x = (double) ois.readObject();
                        try {
                            long[] actual = pasteAudioSafe(x);
                            int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                            RevisionManager.saveRevision("paste", actual[0], actual[1], workingFile);
                            oos.writeObject(data);
                        } catch (Exception ex) {
                            System.out.println("pasteAudio error: " + ex);
                            oos.writeObject("Помилка paste: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "pasteAtSeconds")) {
                        double sec = (double) ois.readObject();
                        try {
                            File ref = (workingFile != null && workingFile.exists()) ? workingFile : sourceFile;
                            double[] stats = computeStatsDTO(ref);
                            if (stats == null || stats.length < 5) {
                                oos.writeObject("Статистика недоступна: спочатку оберіть файл (Select…).");
                                oos.flush();
                                continue;
                            }
                            double durationSec = stats[4];
                            if (durationSec <= 0) {
                                oos.writeObject("Невідома тривалість файлу.");
                                oos.flush();
                                continue;
                            }
                            double nx = Math.max(0.0, Math.min(1.0, sec / durationSec));

                            long[] actual = pasteAudioSafe(nx);
                            int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                            RevisionManager.saveRevision("paste@sec=" + sec, actual[0], actual[1], workingFile);
                            oos.writeObject(data);
                        } catch (Exception ex) {
                            System.out.println("pasteAtSeconds error: " + ex);
                            oos.writeObject("Помилка pasteAtSeconds: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "deform")) {
                        double startSec = (double) ois.readObject();
                        double endSec   = (double) ois.readObject();
                        double factor   = (double) ois.readObject();
                        try {
                            long[] actual = deformAudioSafe(startSec, endSec, factor);
                            int[] data = new WaveData().extractAmplitudeFromFile(workingFile);
                            RevisionManager.saveRevision("deform x" + factor, actual[0], actual[1], workingFile);
                            oos.writeObject(data);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            oos.writeObject("Помилка deform: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "convertToMp3")) {
                        handleConvertWithJave(oos, "mp3",  "libmp3lame", ".mp3");
                        RevisionManager.saveRevision("convert:mp3", -1, -1, workingFile);

                    } else if (Objects.equals(cmd, "convertToOgg")) {
                        handleConvertWithJave(oos, "ogg",  "libvorbis",  ".ogg");
                        RevisionManager.saveRevision("convert:ogg", -1, -1, workingFile);

                    } else if (Objects.equals(cmd, "convertToFlac")) {
                        handleConvertWithJave(oos, "flac", "flac",       ".flac");
                        RevisionManager.saveRevision("convert:flac", -1, -1, workingFile);

                    } else if (Objects.equals(cmd, "getStats")) {
                        try {
                            double[] stats = computeStatsDTO(workingFile != null ? workingFile : sourceFile);
                            if (stats == null) {
                                oos.writeObject("Статистика недоступна: спочатку оберіть файл (Select…).");
                            } else {
                                oos.writeObject(stats);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            oos.writeObject("Помилка отримання статистики: " + ex.getMessage());
                        }
                        oos.flush();

                    } else if (Objects.equals(cmd, "saveWav")) {
                        try {
                            File outFile = (File) ois.readObject();
                            String msg = handleSaveWav(outFile);
                            oos.writeObject(msg);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            oos.writeObject("Помилка збереження: " + ex.getMessage());
                        }
                        oos.flush();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    // -------------------------- підготовка робочого WAV --------------------------

    private static File prepareWorkingFile(File src) throws Exception {
        if (src == null || !src.exists()) return null;

        String name = src.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".wav")) return src; 

        File out = new File(ensureTmpDir(), stripExt(src.getName()) + " (working).wav");
        if (out.exists() && !out.delete()) {
            out = File.createTempFile(stripExt(src.getName()) + " (working) - ", ".wav", ensureTmpDir());
        }

        try {
            transcodeToWav(src, out, true);
        } catch (Exception ignore) {}

        if (!out.exists() || out.length() < 4096) {
            try { out.delete(); } catch (Exception ignored) {}
            out = new File(ensureTmpDir(), stripExt(src.getName()) + " (working).wav");
            transcodeToWav(src, out, false);
        }

        if (!out.exists() || out.length() < 4096) {
            try { out.delete(); } catch (Exception ignore) {}
            throw new IOException("FFmpeg створив надто малий WAV.");
        }

        System.out.println("[WORKING WAV] " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
        return out;
    }

    private static void transcodeToWav(File src, File out, boolean strict) throws EncoderException {
        EncodingAttributes ea = new EncodingAttributes();
        ea.setOutputFormat("wav");

        if (strict) {
            AudioAttributes aa = new AudioAttributes();
            aa.setCodec("pcm_s16le");
            aa.setChannels(2);
            aa.setSamplingRate(44_100);
            ea.setAudioAttributes(aa);
        } else {
            ea.setAudioAttributes(null);
        }

        new Encoder().encode(new MultimediaObject(src), out, ea);
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return (i > 0) ? n.substring(0, i) : n;
    }

    private static File ensureTmpDir() {
        File dir = new File("target/tmp");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // -------------------------- операції редагування --------------------------

    private static long[] copyAudioSafe(int startFrame, int endFrame) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");
        try (AudioInputStream in = AudioSystem.getAudioInputStream(workingFile)) {
            AudioFormat fmt = in.getFormat();
            int frameSize   = Math.max(1, fmt.getFrameSize());
            long total      = totalFrames(in, frameSize);

            long s = clamp(startFrame, 0, total);
            long e = clamp(endFrame,   0, total);
            if (e <= s) throw new IllegalArgumentException("Порожній діапазон copy.");

            skipFrames(in, s, frameSize);
            long framesToCopy = e - s;
            AudioInputStream segment = new AudioInputStream(in, fmt, framesToCopy);

            clipboardFile = new File(ensureTmpDir(), "temp.wav");
            AudioSystem.write(segment, AudioFileFormat.Type.WAVE, clipboardFile);
            return new long[]{ s, e };
        }
    }

    private static long[] cutAudioSafe(int startFrame, int endFrame) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");

        File tmp = new File(ensureTmpDir(), "cut_result.wav");

        try (AudioInputStream in1 = AudioSystem.getAudioInputStream(workingFile);
             AudioInputStream in2 = AudioSystem.getAudioInputStream(workingFile)) {

            AudioFormat fmt = in1.getFormat();
            int frameSize   = Math.max(1, fmt.getFrameSize());
            long total      = totalFrames(in1, frameSize);

            long s = clamp(startFrame, 0, total);
            long e = clamp(endFrame,   0, total);
            if (e <= s) return new long[]{ s, e };

            AudioInputStream first = new AudioInputStream(in1, fmt, s);
            skipFrames(in2, e, frameSize);
            long tailFrames = Math.max(0, total - e);
            AudioInputStream second = new AudioInputStream(in2, fmt, tailFrames);

            SequenceInputStream seq = new SequenceInputStream(first, second);
            AudioInputStream out = new AudioInputStream(seq, fmt, s + tailFrames);

            AudioSystem.write(out, AudioFileFormat.Type.WAVE, tmp);
        }

        replaceWorkingWith(tmp);
        return new long[]{ startFrame, endFrame };
    }

    private static long[] pasteAudioSafe(double x) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");
        if (clipboardFile == null || !clipboardFile.exists())
            throw new IllegalStateException("Фрагмент для копіювання відсутній.");

        File tmp = new File(ensureTmpDir(), "paste_result.wav");

        try (AudioInputStream in1  = AudioSystem.getAudioInputStream(workingFile);
             AudioInputStream in2  = AudioSystem.getAudioInputStream(workingFile);
             AudioInputStream clip = AudioSystem.getAudioInputStream(clipboardFile)) {

            AudioFormat fmt = in1.getFormat();
            int frameSize   = Math.max(1, fmt.getFrameSize());
            long total      = totalFrames(in1, frameSize);

            double nx = Math.min(1.0, Math.max(0.0, x));
            long insertAt = clamp(Math.round(total * nx), 0, total);

            AudioInputStream first = new AudioInputStream(in1, fmt, insertAt);

            skipFrames(in2, insertAt, frameSize);
            long tailFrames = Math.max(0, total - insertAt);
            AudioInputStream second = new AudioInputStream(in2, fmt, tailFrames);

            long clipFrames = totalFrames(clip, Math.max(1, clip.getFormat().getFrameSize()));
            AudioInputStream mid = new AudioInputStream(clip, fmt, clipFrames);

            SequenceInputStream seq  = new SequenceInputStream(first, mid);
            SequenceInputStream seq2 = new SequenceInputStream(seq, second);
            AudioInputStream out = new AudioInputStream(seq2, fmt, insertAt + clipFrames + tailFrames);

            AudioSystem.write(out, AudioFileFormat.Type.WAVE, tmp);
        }

        replaceWorkingWith(tmp);
        try (AudioInputStream in = AudioSystem.getAudioInputStream(workingFile)) {
            long total = totalFrames(in, Math.max(1, in.getFormat().getFrameSize()));
            long insertAt = (long)Math.round(total * Math.min(1.0, Math.max(0.0, x)));
            return new long[]{ insertAt, insertAt };
        }
    }

    private static long[] deformAudioSafe(double startSec, double endSec, double factor) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");
        if (factor <= 0.0 || Math.abs(factor - 1.0) < 1e-9)
            throw new IllegalArgumentException("Невалідний коефіцієнт.");
    
        File tmp = new File(ensureTmpDir(), "deform_result.wav");
    
        
        final AudioFormat fmt;
        final int frameSize;
        final float sr;
        final long totalFrames;
        try (AudioInputStream in0 = ensurePcm16le(AudioSystem.getAudioInputStream(workingFile))) {
            fmt        = in0.getFormat();
            frameSize  = Math.max(1, fmt.getFrameSize());
            sr         = fmt.getSampleRate();
            totalFrames= totalFrames(in0, frameSize);
        }
    
        long s = clamp(Math.round(startSec * sr), 0, totalFrames);
        long e = clamp(Math.round(endSec   * sr), 0, totalFrames);
        if (e <= s) throw new IllegalArgumentException("Порожній діапазон deform.");
    
        
        try (AudioInputStream inFirst0 = ensurePcm16le(AudioSystem.getAudioInputStream(workingFile));
             AudioInputStream inMid0   = ensurePcm16le(AudioSystem.getAudioInputStream(workingFile));
             AudioInputStream inTail0  = ensurePcm16le(AudioSystem.getAudioInputStream(workingFile))) {
    
            
            AudioInputStream first = new AudioInputStream(inFirst0, fmt, s);
    
            
            skipFrames(inMid0, s, frameSize);
            long midFrames = e - s;
            byte[] midBytes    = readExactFramesToBytes(inMid0, midFrames, frameSize);
            byte[] midScaled   = timeScalePcm16le(midBytes, fmt.getChannels(), factor, frameSize);
            long   outMidFrames= Math.max(1, midScaled.length / frameSize);
            ByteArrayInputStream midBAIS = new ByteArrayInputStream(midScaled);
            AudioInputStream midAIS = new AudioInputStream(midBAIS, fmt, outMidFrames);
    
            
            skipFrames(inTail0, e, frameSize);
            long tailFrames = Math.max(0, totalFrames - e);
            AudioInputStream tail = new AudioInputStream(inTail0, fmt, tailFrames);
    
            
            SequenceInputStream seq1 = new SequenceInputStream(first, midAIS);
            SequenceInputStream seq2 = new SequenceInputStream(seq1, tail);
            long totalOutFrames = s + outMidFrames + tailFrames;
            try (AudioInputStream outAIS = new AudioInputStream(seq2, fmt, totalOutFrames)) {
                AudioSystem.write(outAIS, AudioFileFormat.Type.WAVE, tmp);
            }
        }
    
        replaceWorkingWith(tmp);
        return new long[] { s, e };
    }
    
    
    
    

    // ---------- секунди -> кадри обгортки ----------
    private static long[] copyAudioSecSafe(double startSec, double endSec) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");
        try (AudioInputStream in = AudioSystem.getAudioInputStream(workingFile)) {
            float sr = in.getFormat().getSampleRate();
            long s = Math.max(0, Math.round(startSec * sr));
            long e = Math.max(0, Math.round(endSec   * sr));
            if (e <= s) throw new IllegalArgumentException("Порожній діапазон copy.");
            return copyAudioSafe((int) s, (int) e);
        }
    }
    private static long[] cutAudioSecSafe(double startSec, double endSec) throws Exception {
        if (workingFile == null) throw new IllegalStateException("Файл не обраний.");
        try (AudioInputStream in = AudioSystem.getAudioInputStream(workingFile)) {
            float sr = in.getFormat().getSampleRate();
            long s = Math.max(0, Math.round(startSec * sr));
            long e = Math.max(0, Math.round(endSec   * sr));
            if (e <= s) return new long[]{ s, e };
            return cutAudioSafe((int) s, (int) e);
        }
    }

    // -------------------------- конвертація --------------------------

    private static void handleConvertWithJave(ObjectOutputStream out,
                                              String container, String audioCodec, String ext) throws IOException {

        File srcForEncode = (workingFile != null && workingFile.exists()) ? workingFile : sourceFile;
        File baseForOutput = (sourceFile != null && sourceFile.exists()) ? sourceFile : srcForEncode;

        if (srcForEncode == null || !srcForEncode.exists()) {
            out.writeObject("Помилка: спочатку оберіть файл (Select…).");
            out.flush();
            return;
        }

        try {
            String baseName = stripExt(baseForOutput.getName());
            File outFile = new File(baseForOutput.getParentFile(),
                    baseName + " (converted to " + container + ")" + ext);

            AudioAttributes aa = new AudioAttributes();
            aa.setCodec(audioCodec);
            aa.setChannels(2);
            aa.setSamplingRate(44_100);
            if (!"flac".equals(container)) {
                aa.setBitRate(192_000);
            }

            EncodingAttributes ea = new EncodingAttributes();
            ea.setOutputFormat(container);
            ea.setAudioAttributes(aa);

            new Encoder().encode(new MultimediaObject(srcForEncode), outFile, ea);

            System.out.println("[SERVER SAVE] -> " + outFile.getAbsolutePath());
            out.writeObject("OK: " + outFile.getAbsolutePath());
        } catch (EncoderException ex) {
            ex.printStackTrace();
            out.writeObject("Помилка конвертації: " + ex.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            out.writeObject("Невідома помилка: " + t.getMessage());
        } finally {
            out.flush();
        }
    }

    // -------------------------- зберегти WAV --------------------------

    private static String handleSaveWav(File outFile) {
        try {
            File src = (workingFile != null && workingFile.exists()) ? workingFile : sourceFile;
            if (src == null || !src.exists()) {
                return "Помилка: немає активного файлу для збереження.";
            }
            if (outFile == null) {
                return "Помилка: невірний шлях для збереження.";
            }
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return "Помилка: не вдалося створити директорію: " + parent;
            }
            copyFileUsingStream(src, outFile);
            return "OK: " + outFile.getAbsolutePath();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Помилка збереження: " + ex.getMessage();
        }
    }

    // -------------------------- утиліти --------------------------

    private static void replaceWorkingWith(File tmp) throws IOException {
        if (tmp == null || !tmp.exists())
            throw new FileNotFoundException("Відсутній тимчасовий файл результату.");
        if (workingFile == null)
            throw new IllegalStateException("Немає робочого файлу.");

        try {
            Files.move(tmp.toPath(), workingFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            copyFileUsingStream(tmp, workingFile);
            if (!tmp.delete()) tmp.deleteOnExit();
        }
    }

    private static void skipFrames(AudioInputStream ais, long framesToSkip, int frameSize) throws IOException {
        long bytesToSkip = framesToSkip * (long) frameSize;
        byte[] buf = new byte[(int) Math.min(64L * 1024, Math.max(4096, frameSize * 1024))];
        while (bytesToSkip > 0) {
            int n = ais.read(buf, 0, (int) Math.min(buf.length, bytesToSkip));
            if (n < 0) break;
            bytesToSkip -= n;
        }
    }

    private static long totalFrames(AudioInputStream ais, int frameSize) throws IOException {
        long fl = ais.getFrameLength();
        if (fl > 0) return fl;
        int available = ais.available();
        return (available > 0 && frameSize > 0) ? (available / frameSize) : 0;
    }

    private static long clamp(long v, long lo, long hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
    }

    // -------------------------- метадані --------------------------

    private static double[] computeStatsDTO(File f) {
        if (f == null || !f.exists()) return null;
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            AudioFormat fmt = in.getFormat();
            int frameSize   = Math.max(1, fmt.getFrameSize());
            long total      = totalFrames(in, frameSize);
            double sr = fmt.getSampleRate();
            double ch = fmt.getChannels();
            double fs = frameSize;
            double dur = (sr > 0 ? total / sr : 0.0);
            return new double[]{ sr, ch, fs, total, dur };
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    

    private static AudioInputStream ensurePcm16le(AudioInputStream input) throws UnsupportedAudioFileException {
        AudioFormat sourceFormat = input.getFormat();
        boolean isPcm16Le =
                AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding()) &&
                sourceFormat.getSampleSizeInBits() == 16 &&
                !sourceFormat.isBigEndian();

        if (isPcm16Le) {
            return input;
        }

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false 
        );
        return AudioSystem.getAudioInputStream(targetFormat, input);
    }

    private static byte[] readExactFramesToBytes(AudioInputStream input,
                                                 long framesToRead,
                                                 int frameSize) throws IOException {
        long bytesToReadLong = Math.max(0, framesToRead) * (long) frameSize;
        int bytesToRead = (int) Math.min(Integer.MAX_VALUE, bytesToReadLong);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytesToRead);
        byte[] chunk = new byte[Math.min(64 * 1024, Math.max(4096, frameSize * 1024))];
        int remaining = bytesToRead;
        while (remaining > 0) {
            int n = input.read(chunk, 0, Math.min(chunk.length, remaining));
            if (n < 0) break; 
            buffer.write(chunk, 0, n);
            remaining -= n;
        }
        return buffer.toByteArray();
    }

    private static byte[] timeScalePcm16le(byte[] sourceBytes,
                                            int numChannels,
                                            double timeScaleFactor,
                                            int frameSize) {
        if (sourceBytes == null || sourceBytes.length == 0) return new byte[0];
        if (numChannels <= 0 || frameSize <= 0 || timeScaleFactor <= 0.0) return sourceBytes;

        int sourceFrames = sourceBytes.length / frameSize;
        if (sourceFrames <= 1) return sourceBytes.clone();

        int targetFrames = Math.max(1, (int) Math.round(sourceFrames * timeScaleFactor));
        byte[] targetBytes = new byte[targetFrames * frameSize];

        for (int outFrameIndex = 0; outFrameIndex < targetFrames; outFrameIndex++) {
            double position;
            if (targetFrames == 1) {
                position = 0.0;
            } else {
                position = (outFrameIndex / (double) (targetFrames - 1)) * (sourceFrames - 1);
            }

            int leftIndex = (int) Math.floor(position);
            int rightIndex = Math.min(sourceFrames - 1, leftIndex + 1);
            double t = position - leftIndex;

            int outBase = outFrameIndex * frameSize;
            int leftBase = leftIndex * frameSize;
            int rightBase = rightIndex * frameSize;

            for (int ch = 0; ch < numChannels; ch++) {
                int lo = leftBase + ch * 2;
                int ro = rightBase + ch * 2;

                short s0 = (short) (((sourceBytes[lo + 1] & 0xFF) << 8) | (sourceBytes[lo] & 0xFF));
                short s1 = (short) (((sourceBytes[ro + 1] & 0xFF) << 8) | (sourceBytes[ro] & 0xFF));

                double v = s0 * (1.0 - t) + s1 * t;
                int vi = clampToShort(Math.round((float) v));

                int oo = outBase + ch * 2;
                targetBytes[oo]     = (byte) (vi & 0xFF);
                targetBytes[oo + 1] = (byte) ((vi >>> 8) & 0xFF);
            }
        }

        return targetBytes;
    }

    private static int clampToShort(int value) {
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return value;
    }
}
