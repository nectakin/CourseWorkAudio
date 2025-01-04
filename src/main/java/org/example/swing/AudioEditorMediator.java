package org.example.swing;
import org.example.converter.AudioFlacConverter;
import org.example.converter.AudioMp3Converter;
import org.example.converter.AudioOggConverter;
import org.example.audiotrack.*;
import org.example.logs.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class AudioEditorMediator implements UIMediator {
    private JButton loadFileButton;
    private JButton convertToMp3Button;
    private JButton convertToOggButton;
    private JButton convertToFlacButton;
    private JLabel fileLabel;
    private JPanel waveformPanel;
    private Logger logger;
    private File selectedFile;

    public AudioEditorMediator(JButton loadFileButton, JButton convertToMp3Button, JButton convertToOggButton, JButton convertToFlacButton,
                               JLabel fileLabel, JPanel waveformPanel, Logger logger) {
        this.loadFileButton = loadFileButton;
        this.convertToMp3Button = convertToMp3Button;
        this.convertToOggButton = convertToOggButton;
        this.convertToFlacButton = convertToFlacButton;
        this.fileLabel = fileLabel;
        this.waveformPanel = waveformPanel;
        this.logger = logger;

        initialize();
    }

    private void initialize() {
        loadFileButton.addActionListener(e -> notify(loadFileButton, "loadFile"));
        convertToMp3Button.addActionListener(e -> notify(convertToMp3Button, "convertToMp3"));
        convertToOggButton.addActionListener(e -> notify(convertToOggButton, "convertToOgg"));
        convertToFlacButton.addActionListener(e -> notify(convertToFlacButton, "convertToFlac"));
    }

    @Override
    public void notify(Component sender, String event) {
        switch (event) {
            case "loadFile":
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    fileLabel.setText("Selected: " + selectedFile.getName());
                    displayWaveform(selectedFile);
                    logger.fileOpen();
                }
                break;

            case "convertToMp3":
                convertAudio("mp3");
                break;

            case "convertToOgg":
                convertAudio("ogg");
                break;

            case "convertToFlac":
                convertAudio("flac");
                break;

            default:
                throw new IllegalArgumentException("Unknown event: " + event);
        }
    }

    private void convertAudio(String format) {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(null, "Please load a file first.");
            return;
        }

        AudioAdapter adapter = createAudioAdapter(selectedFile);
        File convertedFile;
        switch (format) {
            case "mp3":
                convertedFile = AudioMp3Converter.getInstance().convertTo(new Mp3(adapter.adaptFile().getAbsolutePath()));
                break;
            case "ogg":
                convertedFile = AudioOggConverter.getInstance().convertTo(new Ogg(adapter.adaptFile().getAbsolutePath()));
                break;
            case "flac":
                convertedFile = AudioFlacConverter.getInstance().convertTo(new Flac(adapter.adaptFile().getAbsolutePath()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
        JOptionPane.showMessageDialog(null, "File converted to " + format.toUpperCase() + " successfully!");
    }

    private void displayWaveform(File audioFile) {
        waveformPanel.removeAll();
        waveformPanel.setLayout(new BorderLayout());
        waveformPanel.add(new AudioWaveformPanel(audioFile), BorderLayout.CENTER);
        waveformPanel.revalidate();
        waveformPanel.repaint();
    }

    private AudioAdapter createAudioAdapter(File file) {
        Audiotrack audiotrack = createAudiotrack(file);
        return new AudioAdapter(audiotrack);
    }

    private Audiotrack createAudiotrack(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".mp3")) {
            return new Mp3(file.getAbsolutePath());
        } else if (fileName.endsWith(".ogg")) {
            return new Ogg(file.getAbsolutePath());
        } else if (fileName.endsWith(".flac")) {
            return new Flac(file.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + fileName);
        }
    }
}
