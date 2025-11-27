package org.example.logs;

import java.io.File;

public class Logger {
    EventManager eventManager;
    final File logFile = new File("logs/log.txt");
    FileOpenLog fileOpenLog;

    // ...
    public Logger() {
        eventManager = new EventManager("openFile");

        fileOpenLog = new FileOpenLog(logFile);
        eventManager.subscribe(fileOpenLog, "openFile");

        //...
    }

    public void fileOpen() {
        eventManager.notifySubscribers("openFile");
    }
}
/*Logger*/