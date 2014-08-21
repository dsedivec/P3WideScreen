package org.codefu.p3widescreen;

import javax.swing.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

class PatchWorker extends SwingWorker<Void, Void> {
    private final String executablePath;
    private final Logger logger;

    public PatchWorker(String executablePath, Logger logger) {
        this.executablePath = executablePath;
        this.logger = logger;
    }

    @Override
    protected Void doInBackground() throws Exception {
        File executableFile = new File(executablePath);
        if (!executableFile.isFile()) {
            logger.log(Level.SEVERE,
                       "\"{0}\" doesn''t exist or isn't a regular file",
                       executableFile);
        }
        return null;
    }
}
