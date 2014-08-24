package org.codefu.p3widescreen;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;

class PatchWorker extends SwingWorker<Void, Void> {
    private static final DWordBinaryPatch[] patches = {
        // The ?s in these first five are all width first, then height.
        new DWordBinaryPatch(0x23bf0, "c7 44 24 4c ? c7 44 24 50 ?"),
        new DWordBinaryPatch(0x2d168, "c7 44 24 18 ? c7 44 24 1c ?"),
        new DWordBinaryPatch(0x32d36, "c7 44 24 3c ? c7 44 24 40 ?"),
        new DWordBinaryPatch(0x5fe68, "c7 44 24 48 ? c7 44 24 4c ?"),
        new DWordBinaryPatch(0x63fed, "c7 44 24 24 ? c7 44 24 28 ?"),
        new DWordBinaryPatch(0x29ad0, "3d 00 04 00 00 0f 84 af 00 00 00 3d ?"),
        new DWordBinaryPatch(0x29ff5, "3d 00 04 00 00 74 1e 3d ?"),
    };
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
        RandomAccessFile executable =
            new RandomAccessFile(executableFile, "r");
        for (DWordBinaryPatch patch : patches) {
            String patchError = patch.testPatch(executable);
            if (patchError != null) {
                logger.error("error patching {}: {}", executableFile,
                             patchError);
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
        InputStream worldMap =
            cprFile.getFile("images\\Vollansichtskarte1280.bmp");
        BufferedImage original = ImageIO.read(worldMap);
        ResampleOp resampleOp = new ResampleOp(1440, 900);
        resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
        BufferedImage resized = resampleOp.filter(original, null);
        ImageIO.write(resized, "bmp", new File("/tmp/resized.bmp"));

        return null;
    }
}
