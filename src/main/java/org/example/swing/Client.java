package org.example.swing;

import org.example.service.RecentActionsService;
import org.example.slider.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends JFrame {

    /* ---------- мережеве з'єднання ---------- */
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream  objectInputStream;

    /* ---------- UI-компоненти ---------- */
    private JPanel      content;
    private JPanel      waveContainer;
    private WavePanel   wavePanel;
    private RangeSlider rangeSlider;

    private JButton btnSelect;
    private JButton btnCopy;
    private JButton btnCut;
    private JButton btnPaste;
    private JButton btnConvertTo;
    private JButton btnSave;

    // введення діапазону в секундах + apply
    private JSpinner startSecSpinner;
    private JSpinner endSecSpinner;
    private JButton  applyBtn;

    // деформувати
    private JSpinner factorSpinner;
    private JButton  deformBtn;

    // paste по секундах
    private JSpinner pasteAtSecSpinner;
    private JButton  pasteAtBtn;

    // статус/тривалість
    private JLabel durationLabel;

    // останні дії
    private final RecentActionsService recent = new RecentActionsService();
    private LastActionsPanel lastPanel;

    // поточна кількість точок у поточній хвилі (для слайдера)
    private int currentDataLength = 0;

    // метадані завантаженого треку
    private double sampleRate   = 0.0;
    private int    channels     = 0;
    private int    frameSize    = 0;
    private long   totalFrames  = 0;
    private double durationSec  = 0.0;

    public Client() {
        super("Audio Editor (Client)");
        connectToServer();
        buildUi();
        bindActions();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 650);
        setLocationRelativeTo(null);
        updateButtonsState();
    }

    /* ---------- з'єднання з сервером ---------- */
    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 55555);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream  = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Сервер не запущений (localhost:55555). Запустіть Server.",
                    "Client",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }

    /* ---------- побудова UI ---------- */
    private void buildUi() {
        content = new JPanel(new BorderLayout());
        setContentPane(content);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnSelect    = new JButton("Select...");
        btnCopy      = new JButton("Copy");
        btnCut       = new JButton("Cut");
        btnPaste     = new JButton("Paste");
        btnConvertTo = new JButton("Convert to ▼");
        btnSave      = new JButton("Save WAV…");

        top.add(btnSelect);
        top.add(btnCopy);
        top.add(btnCut);
        top.add(btnPaste);
        top.add(btnConvertTo);
        top.add(btnSave);

        startSecSpinner = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 0.00, 0.01));
        endSecSpinner   = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 0.00, 0.01));
        applyBtn        = new JButton("Apply");

        startSecSpinner.setEditor(new JSpinner.NumberEditor(startSecSpinner, "#0.00"));
        endSecSpinner.setEditor(new JSpinner.NumberEditor(endSecSpinner,   "#0.00"));

        Dimension spinSize = new Dimension(80, startSecSpinner.getPreferredSize().height);
        startSecSpinner.setPreferredSize(spinSize);
        endSecSpinner.setPreferredSize(spinSize);

        top.add(Box.createHorizontalStrut(12));
        top.add(new JLabel("Start(s):"));
        top.add(startSecSpinner);
        top.add(new JLabel("End(s):"));
        top.add(endSecSpinner);
        top.add(applyBtn);

        factorSpinner = new JSpinner(new SpinnerNumberModel(1.25, 0.10, 4.00, 0.05)); // >1 быстрее, <1 медленнее
        factorSpinner.setEditor(new JSpinner.NumberEditor(factorSpinner, "#0.00"));
        factorSpinner.setPreferredSize(spinSize);
        deformBtn = new JButton("Deform");

        top.add(Box.createHorizontalStrut(12));
        top.add(new JLabel("Factor:"));
        top.add(factorSpinner);
        top.add(deformBtn);

        pasteAtSecSpinner = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 0.00, 0.01));
        pasteAtSecSpinner.setEditor(new JSpinner.NumberEditor(pasteAtSecSpinner, "#0.00"));
        pasteAtSecSpinner.setPreferredSize(spinSize);
        pasteAtBtn = new JButton("Paste at (s)");

        top.add(Box.createHorizontalStrut(12));
        top.add(new JLabel("Paste at(s):"));
        top.add(pasteAtSecSpinner);
        top.add(pasteAtBtn);

        durationLabel = new JLabel("—");
        durationLabel.setToolTipText("Тривалість / Частота дискретизації / Канали");
        top.add(Box.createHorizontalStrut(16));
        top.add(durationLabel);

        content.add(top, BorderLayout.NORTH);

        wavePanel = new WavePanel();
        wavePanel.setToolTipText("Клацніть на хвилі, щоб вказати позицію для Paste. " +
                "Для точного введення використовуйте 'Paste at(s)'.");
        waveContainer = new JPanel(new BorderLayout());
        waveContainer.add(new JScrollPane(wavePanel), BorderLayout.CENTER);
        content.add(waveContainer, BorderLayout.CENTER);

        lastPanel = new LastActionsPanel();
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.add(lastPanel, BorderLayout.CENTER);
        rightWrap.setPreferredSize(new Dimension(280, 10));
        content.add(rightWrap, BorderLayout.EAST);

        rangeSlider = new RangeSlider();
        rangeSlider.setVisible(false);
        rangeSlider.setToolTipText("Синя смуга знизу — виділення (Selection). " +
                "Її межі синхронізуються з полями Start/End при Apply.");
        content.add(rangeSlider, BorderLayout.SOUTH);
    }

    private void bindActions() {
        btnSelect.addActionListener(this::onSelect);
        btnCopy.addActionListener(e -> copy());
        btnCut.addActionListener(e -> cut());
        btnPaste.addActionListener(e -> paste());
        btnSave.addActionListener(e -> onSave());
        applyBtn.addActionListener(e -> onApplySelection());
        pasteAtBtn.addActionListener(e -> onPasteAtSeconds());
        deformBtn.addActionListener(e -> onDeform());

        JPopupMenu popup = new JPopupMenu();
        JMenuItem mp3  = new JMenuItem("mp3");
        JMenuItem ogg  = new JMenuItem("ogg");
        JMenuItem flac = new JMenuItem("flac");
        mp3.addActionListener(e -> convertTo("Mp3"));
        ogg.addActionListener(e -> convertTo("Ogg"));
        flac.addActionListener(e -> convertTo("Flac"));
        popup.add(mp3); popup.add(ogg); popup.add(flac);

        btnConvertTo.addActionListener(e -> popup.show(btnConvertTo, 0, btnConvertTo.getHeight()));

        startSecSpinner.addChangeListener(this::onRangeSpinnerChanged);
        endSecSpinner.addChangeListener(this::onRangeSpinnerChanged);
    }

    private void onRangeSpinnerChanged(ChangeEvent e) {
        updateButtonsState();
    }

    private void setUiEnabled(boolean enabled) {
        btnSelect.setEnabled(enabled);
        btnCopy.setEnabled(enabled);
        btnCut.setEnabled(enabled);
        btnPaste.setEnabled(enabled);
        btnConvertTo.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        applyBtn.setEnabled(enabled);
        deformBtn.setEnabled(enabled);
        pasteAtBtn.setEnabled(enabled);

        startSecSpinner.setEnabled(enabled);
        endSecSpinner.setEnabled(enabled);
        factorSpinner.setEnabled(enabled);
        pasteAtSecSpinner.setEnabled(enabled);
    }

    private void updateButtonsState() {
        boolean hasAudio = durationSec > 0.0 && currentDataLength > 0;
        double s = ((Number) startSecSpinner.getValue()).doubleValue();
        double e = ((Number) endSecSpinner.getValue()).doubleValue();
        boolean segValid = hasAudio && (0.0 <= s) && (s <= e) && (e <= durationSec + 1e-9);

        btnCopy.setEnabled(hasAudio && segValid);
        btnCut.setEnabled(hasAudio && segValid);
        deformBtn.setEnabled(hasAudio && segValid);

        btnPaste.setEnabled(hasAudio);
        pasteAtBtn.setEnabled(hasAudio);

        btnConvertTo.setEnabled(hasAudio);
        btnSave.setEnabled(hasAudio);
        applyBtn.setEnabled(hasAudio);
    }

    /* ---------- логіка apply (секунди -> індекси) ---------- */
    private void onApplySelection() {
        double startS = ((Number) startSecSpinner.getValue()).doubleValue();
        double endS   = ((Number) endSecSpinner.getValue()).doubleValue();

        String err = validateSeconds(startS, endS, durationSec);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Invalid segment", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int startIdx = secToIndex(startS);
        int endIdx   = secToIndex(endS);

        rangeSlider.setMinimum(0);
        rangeSlider.setMaximum(currentDataLength);
        rangeSlider.setValue(startIdx);
        rangeSlider.setUpperValue(endIdx);
        rangeSlider.setVisible(true);

        recent.addSegment(startS, endS);
        lastPanel.refreshFrom(recent);

        updateButtonsState();
        JOptionPane.showMessageDialog(this, "Selection applied.", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    /* ---------- deform ---------- */
    private void onDeform() {
        if (durationSec <= 0) {
            JOptionPane.showMessageDialog(this, "Спочатку завантажте файл.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double startS = ((Number) startSecSpinner.getValue()).doubleValue();
        double endS   = ((Number) endSecSpinner.getValue()).doubleValue();
        String err = validateSeconds(startS, endS, durationSec);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Invalid segment", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double factor = ((Number) factorSpinner.getValue()).doubleValue();
        if (factor <= 0.0 || Math.abs(factor - 1.0) < 1e-9) {
            JOptionPane.showMessageDialog(this, "Factor має бути > 0 і ≠ 1.00.", "Invalid factor", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            sendDeformCommand(startS, endS, factor);

            Object obj = objectInputStream.readObject();
            if (obj instanceof int[] data) {
                wavePanel.setData(data);
                currentDataLength = data.length;
                SwingUtilities.updateComponentTreeUI(content);

                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);
                rangeSlider.setVisible(true);

                requestStatsAndUpdateSecondsUI();

                recent.addSegment(startS, endS);
                lastPanel.refreshFrom(recent);

                JOptionPane.showMessageDialog(this, "Deform done (x" + String.format("%.2f", factor) + ").", "OK", JOptionPane.INFORMATION_MESSAGE);
            } else if (obj instanceof String s) {
                JOptionPane.showMessageDialog(this, s, "Deform failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Невідома відповідь від сервера.", "Deform failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Deform failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateButtonsState();
        }
    }

    /* ---------- вставити в секундах ---------- */
    private void onPasteAtSeconds() {
        if (durationSec <= 0) {
            JOptionPane.showMessageDialog(this, "Спочатку завантажте файл.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double sec = ((Number) pasteAtSecSpinner.getValue()).doubleValue();
        if (Double.isNaN(sec) || sec < 0 || sec > durationSec + 1e-9) {
            JOptionPane.showMessageDialog(this, "Невірна позиція для вставки (секунди).", "Invalid", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double nx = Math.max(0.0, Math.min(1.0, sec / durationSec));
        try {
            sendPasteCommand(nx);

            Object obj = objectInputStream.readObject();
            if (obj instanceof int[] data) {
                wavePanel.setData(data);
                currentDataLength = data.length;
                SwingUtilities.updateComponentTreeUI(content);

                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);
                rangeSlider.setVisible(true);

                requestStatsAndUpdateSecondsUI();

                recent.addSegment(sec, Math.min(durationSec, sec + 0.01));
                lastPanel.refreshFrom(recent);

                JOptionPane.showMessageDialog(this, "Paste done.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } else if (obj instanceof String s) {
                JOptionPane.showMessageDialog(this, s, "Paste failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Невідома відповідь від сервера.", "Paste failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Paste failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateButtonsState();
        }
    }

    /* ---------- валідація діапазону в секундах ---------- */
    private String validateSeconds(double start, double end, double maxSec) {
        if (currentDataLength <= 0 || maxSec <= 0) return "Файл не завантажено або тривалість невідома.";
        if (Double.isNaN(start) || Double.isNaN(end)) return "Start/End повинні бути числами.";
        if (start < 0) return "Start не може бути менше 0.";
        if (end <= start + 1e-3) return "Діапазон порожній: End має бути > Start.";
        if (end > maxSec + 1e-9) return "End не може перевищувати тривалість доріжки.";
        return null;
    }

    /* ---------- перетворення секунд <-> індекси точок ---------- */
    private int secToIndex(double sec) {
        if (durationSec <= 0 || currentDataLength <= 0) return 0;
        double norm = Math.min(1.0, Math.max(0.0, sec / durationSec));
        return (int) Math.round(norm * currentDataLength);
    }
    private double indexToSec(int idx) {
        if (currentDataLength <= 0) return 0.0;
        double norm = Math.min(1.0, Math.max(0.0, idx / (double) currentDataLength));
        return norm * durationSec;
    }

    /* ---------- вибір файлу ---------- */
    private void onSelect(ActionEvent e) {
        JFileChooser fc = new JFileChooser(new File("testsongs"));

        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Audio (wav, mp3, ogg, flac)", "wav", "mp3", "ogg", "flac"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("WAV",  "wav"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("MP3",  "mp3"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("OGG",  "ogg"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("FLAC", "flac"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            try {
                sendSelectCommand(selectedFile);

                Object obj = objectInputStream.readObject();
                if (obj instanceof int[] data) {
                    if (data.length == 0) {
                        JOptionPane.showMessageDialog(this, "Сервер повернув порожні дані",
                                "Open", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    wavePanel.setData(data);
                    currentDataLength = data.length;
                    SwingUtilities.updateComponentTreeUI(content);

                    rangeSlider.setMinimum(0);
                    rangeSlider.setMaximum(data.length);
                    rangeSlider.setValue((int) (rangeSlider.getMaximum() * 0.5));
                    rangeSlider.setUpperValue((int) (rangeSlider.getMaximum() * 0.75));
                    rangeSlider.setVisible(true);

                    requestStatsAndUpdateSecondsUI();

                    recent.addFile(selectedFile.toPath());
                    recent.addProject(selectedFile.getName());
                    lastPanel.refreshFrom(recent);

                } else if (obj instanceof String s) {
                    JOptionPane.showMessageDialog(this, s, "Open failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Невідома відповідь від сервера.",
                            "Open failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Open failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                updateButtonsState();
            }
        }
    }

    /* ---------- копіювати / вирізати / вставити ---------- */
    private void copy() {
        if (durationSec <= 0) {
            JOptionPane.showMessageDialog(this, "Спочатку завантажте файл.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double startS = ((Number) startSecSpinner.getValue()).doubleValue();
        double endS   = ((Number) endSecSpinner.getValue()).doubleValue();
        String err = validateSeconds(startS, endS, durationSec);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Invalid segment", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            sendCopyCommand(startS, endS); 
            Object obj = objectInputStream.readObject();
            if (obj instanceof int[] data) {
                wavePanel.setData(data);
                currentDataLength = data.length;
                SwingUtilities.updateComponentTreeUI(content);
                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);
                rangeSlider.setVisible(true);

                requestStatsAndUpdateSecondsUI();

                recent.addSegment(startS, endS);
                lastPanel.refreshFrom(recent);

                JOptionPane.showMessageDialog(this, "Copy done.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } else if (obj instanceof String s) {
                JOptionPane.showMessageDialog(this, s, "Copy failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Copy failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateButtonsState();
        }
    }

    private void cut() {
        if (durationSec <= 0) {
            JOptionPane.showMessageDialog(this, "Спочатку завантажте файл.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double startS = ((Number) startSecSpinner.getValue()).doubleValue();
        double endS   = ((Number) endSecSpinner.getValue()).doubleValue();
        String err = validateSeconds(startS, endS, durationSec);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Invalid segment", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            sendCutCommand(startS, endS); 
            Object obj = objectInputStream.readObject();
            if (obj instanceof int[] data) {
                wavePanel.setData(data);
                currentDataLength = data.length;
                SwingUtilities.updateComponentTreeUI(content);
                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);
                rangeSlider.setVisible(true);

                requestStatsAndUpdateSecondsUI();

                recent.addSegment(startS, endS);
                lastPanel.refreshFrom(recent);

                JOptionPane.showMessageDialog(this, "Cut done.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } else if (obj instanceof String s) {
                JOptionPane.showMessageDialog(this, s, "Cut failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cut failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateButtonsState();
        }
    }

    /* ---------- відправка команд на сервер (seconds) ---------- */
    private void sendCopyCommand(double startSec, double endSec) throws IOException {
        objectOutputStream.writeObject("copy");
        objectOutputStream.writeObject(startSec);
        objectOutputStream.writeObject(endSec);
        objectOutputStream.flush();
    }
    private void sendCutCommand(double startSec, double endSec) throws IOException {
        objectOutputStream.writeObject("cut");
        objectOutputStream.writeObject(startSec);
        objectOutputStream.writeObject(endSec);
        objectOutputStream.flush();
    }

    private void paste() {
        try {
            Double nx = wavePanel.getLastClickNormX();
            if (nx == null) {
                JOptionPane.showMessageDialog(this,
                        "Клацніть на хвилі, щоб вибрати точку вставки.\n" +
                                "Або використайте точний варіант: 'Paste at(s)'.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            sendPasteCommand(nx);

            Object obj = objectInputStream.readObject();
            if (obj instanceof int[] data) {
                wavePanel.setData(data);
                currentDataLength = data.length;
                SwingUtilities.updateComponentTreeUI(content);

                rangeSlider.setMinimum(0);
                rangeSlider.setMaximum(data.length);
                rangeSlider.setValue((int) (rangeSlider.getMaximum() * 0.5));
                rangeSlider.setUpperValue((int) (rangeSlider.getMaximum() * 0.75));

                requestStatsAndUpdateSecondsUI();

                double pos = nx * durationSec;
                recent.addSegment(pos, Math.min(durationSec, pos + 0.01));
                lastPanel.refreshFrom(recent);

                JOptionPane.showMessageDialog(this, "Paste done.", "OK", JOptionPane.INFORMATION_MESSAGE);
            } else if (obj instanceof String s) {
                JOptionPane.showMessageDialog(this, s, "Paste failed", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Невідома відповідь від сервера.",
                        "Paste failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Помилка вставки: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateButtonsState();
        }
    }

    /* ---------- зберегти робочий WAV ---------- */
    private void onSave() {
        if (durationSec <= 0) {
            JOptionPane.showMessageDialog(this, "Нічого зберігати — спочатку оберіть файл.", "Save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser(new File("testsongs"));
        fc.setDialogTitle("Save working WAV");
        fc.setSelectedFile(new File("edited.wav"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();

            if (!out.getName().toLowerCase().endsWith(".wav")) {
                out = new File(out.getParentFile(), out.getName() + ".wav");
            }
            try {
                sendSaveWavCommand(out);
                Object ack = objectInputStream.readObject();
                String msg = (ack == null) ? "unknown" : ack.toString();
                JOptionPane.showMessageDialog(this, msg, "Save", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Save WAV поки що недоступний (потрібно оновити Server.java). Деталі: " + ex.getMessage(),
                        "Save", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /* ---------- неблокуюча конверсія (SwingWorker) ---------- */
    private void convertTo(String format) {
        JDialog progress = new JDialog(this, "Encoding…", false);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        progress.add(bar);
        progress.setSize(240, 70);
        progress.setLocationRelativeTo(this);

        setUiEnabled(false);
        progress.setVisible(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                sendConvertToCommand(format);
                Object ack = objectInputStream.readObject();
                return (ack == null) ? "unknown" : ack.toString();
            }
            @Override protected void done() {
                try {
                    String msg = get();
                    JOptionPane.showMessageDialog(Client.this, msg, "Convert", JOptionPane.INFORMATION_MESSAGE);
                    requestStatsAndUpdateSecondsUI();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Client.this, "Convert failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    progress.dispose();
                    setUiEnabled(true);
                    updateButtonsState();
                }
            }
        };
        worker.execute();
    }

    /* ---------- запит статистики ---------- */
    private void requestStatsAndUpdateSecondsUI() throws IOException, ClassNotFoundException {
        double prevStart = ((Number) startSecSpinner.getValue()).doubleValue();
        double prevEnd   = ((Number) endSecSpinner.getValue()).doubleValue();
        double prevPaste = ((Number) pasteAtSecSpinner.getValue()).doubleValue();

        sendGetStats();
        Object statsObj = objectInputStream.readObject();
        if (statsObj instanceof double[] arr && arr.length >= 5) {
            sampleRate  = arr[0];
            channels    = (int) Math.round(arr[1]);
            frameSize   = (int) Math.round(arr[2]);
            totalFrames = (long) Math.round(arr[3]);
            durationSec = arr[4];

            SpinnerNumberModel startModel = new SpinnerNumberModel(
                    Math.max(0.00, Math.min(prevStart, durationSec)), 0.00, durationSec, 0.01);
            SpinnerNumberModel endModel = new SpinnerNumberModel(
                    Math.max(0.00, Math.min(prevEnd, durationSec)), 0.00, durationSec, 0.01);
            SpinnerNumberModel pasteModel = new SpinnerNumberModel(
                    Math.max(0.00, Math.min(prevPaste, durationSec)), 0.00, durationSec, 0.01);

            startSecSpinner.setModel(startModel);
            endSecSpinner.setModel(endModel);
            pasteAtSecSpinner.setModel(pasteModel);

            startSecSpinner.setEditor(new JSpinner.NumberEditor(startSecSpinner, "#0.00"));
            endSecSpinner.setEditor(new JSpinner.NumberEditor(endSecSpinner,   "#0.00"));
            pasteAtSecSpinner.setEditor(new JSpinner.NumberEditor(pasteAtSecSpinner, "#0.00"));

            durationLabel.setText(String.format("Total: %.2f s  |  SR: %.0f Hz  |  Ch: %d",
                    durationSec, sampleRate, channels));
        } else if (statsObj instanceof String s) {
            System.out.println("[getStats] " + s);
        }

        updateButtonsState();
    }

    /* ---------- відправка команд на сервер ---------- */
    private void sendSelectCommand(File file) throws IOException {
        objectOutputStream.writeObject("select");
        objectOutputStream.writeObject(file);
        objectOutputStream.flush();
    }
    private void sendPasteCommand(double x) throws IOException {
        objectOutputStream.writeObject("paste");
        objectOutputStream.writeObject(x);
        objectOutputStream.flush();
    }
    private void sendDeformCommand(double startSec, double endSec, double factor) throws IOException {
        objectOutputStream.writeObject("deform");
        objectOutputStream.writeObject(startSec);
        objectOutputStream.writeObject(endSec);
        objectOutputStream.writeObject(factor);
        objectOutputStream.flush();
    }
    private void sendConvertToCommand(String format) throws IOException {
        String cmd = "convertTo" + format;
        objectOutputStream.writeObject(cmd);
        objectOutputStream.flush();
    }
    private void sendSaveWavCommand(File outFile) throws IOException {
        objectOutputStream.writeObject("saveWav");
        objectOutputStream.writeObject(outFile);
        objectOutputStream.flush();
    }
    private void sendGetStats() throws IOException {
        objectOutputStream.writeObject("getStats");
        objectOutputStream.flush();
    }

    /* ---------- точка входу ---------- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client ui = new Client();
            ui.setVisible(true);
        });
    }
}
