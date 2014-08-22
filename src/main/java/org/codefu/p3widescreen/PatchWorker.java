package org.codefu.p3widescreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;

class PatchWorker extends SwingWorker<Void, Void> {
    private final Logger logger = LoggerFactory.getLogger(PatchWorker.class);
    private final String executablePath;

    public PatchWorker(String executablePath) {
        this.executablePath = executablePath;
    }

    @Override
    protected Void doInBackground() throws Exception {
        logger.info("patch start");
        File executableFile = new File(executablePath);
        File dataArchiveFile = new File(executableFile.getParentFile(),
                                        "p2arch0_eng.cpr");
        for (File f : new File[] {executableFile, dataArchiveFile}) {
            if (!f.isFile()) {
                logger.error("\"{}\" doesn't exist or isn't a regular file", f);
                return null;
            }
        }
        /*
        try {
            RandomAccessFile executable =
                new RandomAccessFile(executableFile, "rw");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE,
                       String.format("error opening %s", executableFile), e);
            return null;
        }
        */

        CPRFile cprFile = new CPRFile(dataArchiveFile);

        return null;
    }
}
