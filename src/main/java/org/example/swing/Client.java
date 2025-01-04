package org.example.swing;

import org.example.slider.RangeSlider;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends JDialog {
    {
        Socket socket = null;
        try {
            socket = new Socket("localhost", 55555);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());

        } catch (IOException e) {
            System.out.println("Server is not running.");
            System.exit(1);
        }
    }

    private JPanel contentPane;
    private JButton button;
    private JPanel wave;
    private WavePanel wavePanel;
    private JButton copyButton;
    private JButton convertToButton;
    private JButton pasteButton;
    private JButton cutButton;
    private RangeSlider rangeSlider;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public Client() {
        setContentPane(contentPane);
        setModal(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                select();
            }
        });
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });
        cutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cut();
            }
        });
        pasteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paste();
            }
        });
    }

//    private void select() {
//        JFileChooser fileChooser = new JFileChooser();
//        int state = fileChooser.showOpenDialog(null);
//        if (state == JFileChooser.APPROVE_OPTION) {
//            File selectedFile = fileChooser.getSelectedFile();
//            fileChooser.setVisible(false);
//
//            WaveData waveData = new WaveData();
//            int[] data = waveData.extractAmplitudeFromFile(selectedFile);
//
//            wavePanel.setData(data);
//            SwingUtilities.updateComponentTreeUI(contentPane);
//        }
//    }

    private void select() {
        JFileChooser fileChooser = new JFileChooser();
        int state = fileChooser.showOpenDialog(null);
        if (state == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileChooser.setVisible(false);
            try {
                sendSelectCommand(selectedFile);
                int[] data = (int[]) objectInputStream.readObject();
                wavePanel.setData(data);
                SwingUtilities.updateComponentTreeUI(contentPane);

                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);

                rangeSlider.setValue(((int) (rangeSlider.getMaximum() * 0.5)));
                rangeSlider.setUpperValue(((int) (rangeSlider.getMaximum() * 0.75)));
                rangeSlider.setVisible(true);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void copy() {
        try {
            sendCopyCommand(rangeSlider.getValue(), rangeSlider.getUpperValue());

            int[] data = (int[]) objectInputStream.readObject();
            wavePanel.setData(data);
            SwingUtilities.updateComponentTreeUI(contentPane);

            rangeSlider.setMinimum(0);
            rangeSlider.setMaximum(data.length);

            rangeSlider.setVisible(true);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void cut() {
        try {
            sendCutCommand(rangeSlider.getValue(), rangeSlider.getUpperValue());

            int[] data = (int[]) objectInputStream.readObject();
            wavePanel.setData(data);
            SwingUtilities.updateComponentTreeUI(contentPane);

            rangeSlider.setMinimum(0);
            rangeSlider.setMaximum(data.length);

            rangeSlider.setValue(((int) (rangeSlider.getMaximum() * 0.5)));
            rangeSlider.setUpperValue(((int) (rangeSlider.getMaximum() * 0.75)));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void paste() {
        try {
            double x = wave.getMousePosition().getX() / wave.getWidth();
            System.out.println(x);
            sendPasteCommand(x);

            int[] data = (int[]) objectInputStream.readObject();
            wavePanel.setData(data);
            SwingUtilities.updateComponentTreeUI(contentPane);

            rangeSlider.setMinimum(0);
            rangeSlider.setMaximum(data.length);

            rangeSlider.setValue(((int) (rangeSlider.getMaximum() * 0.5)));
            rangeSlider.setUpperValue(((int) (rangeSlider.getMaximum() * 0.75)));
        } catch (Exception e) {
            System.out.println("Мишка знаходиться у неправильному місці. Будь ласка, оберіть місце на звуковій доріжці.");
        }
    }

    private void convertTo(String format) {
        try {
            sendConvertToCommand(format);
            JOptionPane.showMessageDialog(null, "Файл було успішно форматовано у " + format);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }


    private void sendCopyCommand(int l, int u) throws IOException {
        objectOutputStream.writeObject("copy");
        objectOutputStream.writeObject(l);
        objectOutputStream.writeObject(u);
    }

    private void sendCutCommand(int l, int u) throws IOException {
        objectOutputStream.writeObject("cut");
        objectOutputStream.writeObject(l);
        objectOutputStream.writeObject(u);
    }

    private void sendSelectCommand(File file) throws IOException {
        objectOutputStream.writeObject("select");
        objectOutputStream.writeObject(file);
    }

    private void sendPasteCommand(double x) throws IOException {
        objectOutputStream.writeObject("paste");
        objectOutputStream.writeObject(x);
    }

    private void sendConvertToCommand(String format) throws IOException {
        objectOutputStream.writeObject("convertTo" + format);
    }

    private void createUIComponents() {
        wave = new JPanel();
        wavePanel = new WavePanel();
        rangeSlider = new RangeSlider();
        rangeSlider.setVisible(false);

        convertToButton = new JButton();

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem mp3 = new JMenuItem("mp3");
        JMenuItem ogg = new JMenuItem("ogg");
        JMenuItem flac = new JMenuItem("flac");

        mp3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertTo("Mp3");
            }
        });

        ogg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertTo("Ogg");
            }
        });

        flac.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convertTo("Flac");
            }
        });

        popupMenu.add(mp3);
        popupMenu.add(ogg);
        popupMenu.add(flac);

        convertToButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {;
                popupMenu.show(convertToButton, 0, convertToButton.getHeight());
            }
        });


    }

    public static void main(String[] args) {
        Client dialog = new Client();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

}
