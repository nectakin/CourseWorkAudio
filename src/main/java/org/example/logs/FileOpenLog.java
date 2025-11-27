package org.example.logs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

public class FileOpenLog implements Subscriber {
    private File logFile;

    public FileOpenLog(File logFile) {
       this.logFile = logFile;
    }

    @Override
    public void update() {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("File was opened at " + LocalTime.now() + " " + LocalDate.now() + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
/*FileOpenLog*/