package org.example.swing;

import org.example.database.DatabaseInitializer;
import org.example.logs.Logger;
import org.example.composite.*;

import javax.swing.*;
import java.awt.*;

public class AudioEditorUI {
    public static void main(String[] args) {
        DatabaseInitializer.initializeDatabase();

        JFrame frame = new JFrame("Audio Editor");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JButton loadFileButton = new JButton("Load Audio File");
        loadFileButton.setBounds(50, 50, 180, 30);

        JLabel fileLabel = new JLabel("No file selected");
        fileLabel.setBounds(250, 50, 300, 30);

        JButton convertToMp3Button = new JButton("Convert to MP3");
        convertToMp3Button.setBounds(50, 100, 180, 30);

        JButton convertToOggButton = new JButton("Convert to OGG");
        convertToOggButton.setBounds(50, 150, 180, 30);

        JButton convertToFlacButton = new JButton("Convert to FLAC");
        convertToFlacButton.setBounds(50, 200, 180, 30);

        JPanel waveformPanel = new JPanel();
        waveformPanel.setBounds(50, 250, 500, 100);
        waveformPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        Logger logger = new Logger();

        UIContainer mainContainer = new UIContainer();

        UIContainer buttonPanel = new UIContainer();
        buttonPanel.add(new UIElement(loadFileButton));
        buttonPanel.add(new UIElement(convertToMp3Button));
        buttonPanel.add(new UIElement(convertToOggButton));
        buttonPanel.add(new UIElement(convertToFlacButton));

        mainContainer.add(buttonPanel);
        mainContainer.add(new UIElement(fileLabel));
        mainContainer.add(new UIElement(waveformPanel));

        frame.add(loadFileButton);
        frame.add(fileLabel);
        frame.add(convertToMp3Button);
        frame.add(convertToOggButton);
        frame.add(convertToFlacButton);
        frame.add(waveformPanel);

        AudioEditorMediator mediator = new AudioEditorMediator(
                loadFileButton, convertToMp3Button, convertToOggButton, convertToFlacButton,
                fileLabel, waveformPanel, logger);

        mainContainer.operation();

        frame.setVisible(true);

    }
}
