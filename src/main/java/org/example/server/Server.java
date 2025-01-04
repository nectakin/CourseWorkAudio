package org.example.server;

import org.example.audiotrack.AudioWaveformPanel;
import org.example.audiotrack.Wav;
import org.example.audiotrack.WaveData;
import org.example.converter.AudioFlacConverter;
import org.example.converter.AudioMp3Converter;
import org.example.converter.AudioOggConverter;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class Server {

    private static File file;
    private static File temp;

    public static void main(String[] args) {

        try {


            ServerSocket serverSocket = new ServerSocket(55555);
            System.out.println("Server started on port 55555");

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());


            ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            AudioWaveformPanel waveData = new AudioWaveformPanel ();




            while (true) {
                String command = (String) objectInputStream.readObject();

                if (Objects.equals(command, "select")) {
                    file = (File) objectInputStream.readObject();
                    objectOutputStream.writeObject(waveData.extractAmplitudeFromFile(file));
                } else if (Objects.equals(command, "copy")) {
                    int l = (int) objectInputStream.readObject();
                    int u = (int) objectInputStream.readObject();
                    copyAudio(l, u);
                    objectOutputStream.writeObject(waveData.extractAmplitudeFromFile(file));
                } else if (Objects.equals(command, "cut")) {
                    int l = (int) objectInputStream.readObject();
                    int u = (int) objectInputStream.readObject();
                    cutAudio(l, u);
                    objectOutputStream.writeObject(waveData.extractAmplitudeFromFile(file));
                } else if (Objects.equals(command, "paste")) {
                    double x = (double) objectInputStream.readObject();
                    pasteAudio(x);
                    objectOutputStream.writeObject(waveData.extractAmplitudeFromFile(file));
                } else if (Objects.equals(command, "convertToMp3")) {
                    AudioMp3Converter converter = AudioMp3Converter.getInstance();
                    Wav wav = new Wav(file.getPath());
                    File mp3 = converter.convertTo(wav);
                    mp3.createNewFile();
                } else if (Objects.equals(command, "convertToOgg")) {
                    AudioOggConverter converter = AudioOggConverter.getInstance();
                    Wav wav = new Wav(file.getPath());
                    File ogg = converter.convertTo(wav);
                    ogg.createNewFile();
                } else if (Objects.equals(command, "convertToFlac")) {
                    AudioFlacConverter converter = AudioFlacConverter.getInstance();
                    Wav wav = new Wav(file.getPath());
                    File flac = converter.convertTo(wav);
                    flac.createNewFile();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }


    public static void copyAudio(int startByte, int endByte) {
        AudioInputStream inputStream = null;
        AudioInputStream shortenedStream = null;
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            AudioFormat format = fileFormat.getFormat();
            inputStream = AudioSystem.getAudioInputStream(file);
            inputStream.skip(startByte * 2L);
            shortenedStream = new AudioInputStream(inputStream, format, (endByte - startByte));


            temp = new File("temp.wav");
            temp.deleteOnExit();
            AudioSystem.write(shortenedStream, fileFormat.getType(), temp);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (shortenedStream != null) try {
                shortenedStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void cutAudio(int startByte, int endByte) {
        AudioInputStream inputStream = null;
        AudioInputStream inputStream2 = null;

        AudioInputStream firstPart = null;
        AudioInputStream secondPart = null;
        AudioInputStream ais = null;
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            AudioFormat format = fileFormat.getFormat();
            inputStream = AudioSystem.getAudioInputStream(file);
            inputStream2 = AudioSystem.getAudioInputStream(file);

            firstPart = new AudioInputStream(inputStream, fileFormat.getFormat(), startByte);

            inputStream2.skip((endByte - startByte) * 2L);

            secondPart = new AudioInputStream(inputStream2, fileFormat.getFormat(), inputStream2.available() / 2);

            SequenceInputStream sequenceInputStream = new SequenceInputStream(firstPart, secondPart);
            ais = new AudioInputStream(sequenceInputStream, format, secondPart.getFrameLength());

            File temp = new File("temp.wav");
            AudioSystem.write(ais, fileFormat.getType(), temp);
            copyFileUsingStream(temp, file);
            temp.delete();


        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (firstPart != null) try {
                firstPart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (secondPart != null) try {
                secondPart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (inputStream2 != null) try {
                inputStream2.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (ais != null) try {
                ais.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void pasteAudio(double x) {
        if (temp == null) {
            throw new RuntimeException("Фрагмент для копіювання відсутній.");
        }
        AudioInputStream inputStream = null;
        AudioInputStream inputStream2 = null;
        AudioInputStream inputStream3 = null;

        AudioInputStream firstPart = null;
        AudioInputStream secondPart = null;
        AudioInputStream middlePart = null;
        AudioInputStream ais = null;

        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            AudioFormat format = fileFormat.getFormat();
            inputStream = AudioSystem.getAudioInputStream(file);
            inputStream2 = AudioSystem.getAudioInputStream(file);
            inputStream3 = AudioSystem.getAudioInputStream(temp);

            int size = inputStream.available() / 2;
            int startByte = (int) (size * x);

            firstPart = new AudioInputStream(inputStream, fileFormat.getFormat(), startByte);

            inputStream2.skip(startByte * 2L);
            secondPart = new AudioInputStream(inputStream2, fileFormat.getFormat(), inputStream2.available() / 2);


            middlePart = new AudioInputStream(inputStream3, fileFormat.getFormat(), inputStream3.available() / 2);

            SequenceInputStream sequenceInputStream = new SequenceInputStream(firstPart, middlePart);
            SequenceInputStream sequenceInputStream2 = new SequenceInputStream(sequenceInputStream, secondPart);

            ais = new AudioInputStream(sequenceInputStream2, format, firstPart.getFrameLength() + middlePart.getFrameLength() + secondPart.getFrameLength());

            File temp = new File("temp.wav");
            AudioSystem.write(ais, fileFormat.getType(), temp);
            copyFileUsingStream(temp, file);
            temp.delete();
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (firstPart != null) try {
                firstPart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (secondPart != null) try {
                secondPart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (inputStream2 != null) try {
                inputStream2.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (inputStream3 != null) try {
                inputStream3.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (ais != null) try {
                secondPart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
            if (middlePart != null) try {
                middlePart.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}