package org.codefu.p3widescreen;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;

class PatchWorker extends SwingWorker<Void, Void> {
    private static final DWordBinaryPatch[] executablePatches = {
        // The ?s in these first five are all width first, then height.
        new DWordBinaryPatch(0x23bf0, "c7 44 24 4c ? c7 44 24 50 ?"),
        new DWordBinaryPatch(0x2d168, "c7 44 24 18 ? c7 44 24 1c ?"),
        new DWordBinaryPatch(0x32d36, "c7 44 24 3c ? c7 44 24 40 ?"),
        new DWordBinaryPatch(0x5fe68, "c7 44 24 48 ? c7 44 24 4c ?"),
        new DWordBinaryPatch(0x63fed, "c7 44 24 24 ? c7 44 24 28 ?"),
        // Width is the only parameter here.
        new DWordBinaryPatch(0x29ad0, "3d 00 04 00 00 0f 84 af 00 00 00 3d ?"),
        new DWordBinaryPatch(0x29ff5, "3d 00 04 00 00 74 1e 3d ?"),
    };

    private static final INIPatcher accelMapPatcher;
    private static final INIPatcher screenGamePatcher;
    private static final INIPatcher texturesPatcher;

    static {
        accelMapPatcher = new INIPatcher();
        accelMapPatcher.addPatch("SCREEN2", "Size", "1280 1024",
                                 "$width $height");
        accelMapPatcher.addPatch("ANIM2", "Frame0", "30024 0 0 0 0 1280 1024 0",
                                 "30024 0 0 0 0 $width $height 0");
        screenGamePatcher = new INIPatcher();
        screenGamePatcher.addPatch("ANIM42", "Frame0", "11 0 0 0 0 996 42 0",
                                   "11 0 0 0 0 $width284 42 0");
        screenGamePatcher.addPatch("ANIM44", "Frame0", "9 0 0 0 0 284 424 0",
                                   "9 0 0 0 0 284 $height600 0");
        screenGamePatcher.addPatch("ANIM44", "Pos", "996 600",
                                   "$width284 600");
        texturesPatcher = new INIPatcher();
        texturesPatcher.addPatch("TEX30024", "OffsetNSize0", "0 0 1280 1024",
                                 "0 0 $width $height");
    }

    private static final String WORLD_MAP_IMAGE_NAME =
        "Vollansichtskarte1280.bmp";
    // Google translates "Hauptscreen" as "main screen" and I don't have
    // a better name for this constant.
    private static final String MAIN_SCREEN_IMAGE_NAME =
        "HauptscreenE1280.bmp";

    private static final Logger logger = LoggerFactory.getLogger(PatchWorker.class);

    private final File executableFile;
    private final File dataArchiveFile;
    private final File imagesDirectory;
    private final File scriptsDirectory;
    private final int width;
    private final int height;

    public PatchWorker(String gameDirectoryPath, int width, int height) {
        File gameDirectory = new File(gameDirectoryPath);
        executableFile = new File(gameDirectory, "Patrician3.exe");
        dataArchiveFile = new File(gameDirectory, "p2arch0_eng.cpr");
        imagesDirectory = new File(gameDirectory, "images");
        scriptsDirectory = new File(gameDirectory, "scripts");
        this.width = width;
        this.height = height;
    }

    private void backUpFile(File file) throws IOException {
        File directory = file.getParentFile();
        String baseName = file.getName();
        File backupFile = new File(directory, baseName + ".bak");
        int suffix = 0;
        while (backupFile.exists()) {
            // Arbitrarily chosen limit of 100.
            if (++suffix > 100) {
                throw new RuntimeException(String.format(
                    "can't find an available backup file name for %s", file));
            }
            backupFile =
                new File(directory,
                         String.format("%s.%02d.bak", baseName, suffix));
        }
        logger.info("backing up {} to {}", file, backupFile);
        Files.copy(file.toPath(), backupFile.toPath());
    }

    private boolean prePatchChecks() throws IOException {
        boolean checksPassed = true;
        // Look at the calculations done on width and height in
        // method createINIFiles.
        if (width < 284) {
            logger.error("width must be 284 or greater");
            checksPassed = false;
        }
        if (height < 600) {
            logger.error("height must be 600 or greater");
            checksPassed = false;
        }
        if (!executableFile.canRead()) {
            logger.error("can't read executable file: {}", executableFile);
            checksPassed = false;
        } else {
            RandomAccessFile executable =
                new RandomAccessFile(executableFile, "r");
            for (DWordBinaryPatch patch : executablePatches) {
                String patchError = patch.testPatch(executable);
                if (patchError != null) {
                    logger.error("error patching {}: {}", executableFile,
                                 patchError);
                    checksPassed = false;
                }
            }
        }
        if (!dataArchiveFile.canRead()) {
            logger.error("can't find data archive {}, should be in same"
                         + " directory as executable", dataArchiveFile);
            checksPassed = false;
        }
        for (File directory : new File[] {imagesDirectory, scriptsDirectory}) {
            if (directory.exists() && !directory.isDirectory()) {
                logger.error("{} exists but is not a directory", directory);
                checksPassed = false;
            }
        }
        return checksPassed;
    }

    @Override
    protected Void doInBackground() throws Exception {
        logger.info("running pre-patch checks");
        if (!prePatchChecks()) {
            return null;
        }
        CPRFile cprFile = new CPRFile(dataArchiveFile);
        createResizedImages(cprFile);
        createINIFiles(cprFile);
        patchExecutable();
        logger.info("patching complete");
        return null;
    }

    private void patchExecutable() throws IOException {
        backUpFile(executableFile);
        logger.info("patching {}", executableFile);
        try (RandomAccessFile executable =
                 new RandomAccessFile(executableFile, "rw")) {
            for (DWordBinaryPatch patch : executablePatches) {
                patch.patch(executable, width, height);
            }
        }
    }

    private void createINIFiles(CPRFile cprFile) throws IOException {
        HashMap<String, Object> substitutions = new HashMap<>();
        substitutions.put("width", width);
        substitutions.put("height", height);
        substitutions.put("width284", width - 284);
        // Original calculation for this was 424 + (height - 1024), which
        // might better help you figure out the significance of this value.
        // I believe the 1024 came from 1280x1024.
        substitutions.put("height600", height - 600);
        if (!scriptsDirectory.isDirectory()) {
            if (!scriptsDirectory.mkdir()) {
                throw new RuntimeException(String.format(
                    "failed to created directory %s", scriptsDirectory));
            }
        }
        applyINIPatch(cprFile, "accelMap.ini", accelMapPatcher, substitutions);
        applyINIPatch(cprFile, "screenGame.ini", screenGamePatcher,
                      substitutions);
        applyINIPatch(cprFile, "textures.ini", texturesPatcher, substitutions);
    }

    private void applyINIPatch(CPRFile cprFile, String iniFileName,
                               INIPatcher patcher,
                               HashMap<String, Object> substitutions)
            throws IOException {
        File outputFile = new File(scriptsDirectory, iniFileName);
        if (outputFile.exists()) {
            backUpFile(outputFile);
        }
        logger.info("producing patched {}", iniFileName);
        try (Reader origAccelMap = cprFile.getReader("scripts\\" + iniFileName);
             Writer patchedAccelMap =
                 new OutputStreamWriter(new FileOutputStream(outputFile),
                                        CPRFile.CHARSET)) {
            patcher.patch(origAccelMap, patchedAccelMap, substitutions);
        }
    }

    private void createResizedImages(CPRFile cprFile) throws IOException {
        if (!imagesDirectory.isDirectory()) {
            if (!imagesDirectory.mkdir()) {
                throw new RuntimeException(String.format(
                    "failed to create directory %s", imagesDirectory));
            }
        }
        writeResizedImage(cprFile, WORLD_MAP_IMAGE_NAME, width, height);
        // Note the relationships between these numbers and the INI
        // changes.  I assume this is not a coincidence.
        writeResizedImage(cprFile, MAIN_SCREEN_IMAGE_NAME, 284, height - 600);
    }

    private void writeResizedImage(CPRFile cprFile, String fileName,
                                   int width, int height)
            throws IOException {
        File outputBMP = new File(imagesDirectory, fileName);
        if (outputBMP.exists()) {
            backUpFile(outputBMP);
        }
        logger.info("resizing {} to {}x{}", fileName, width, height);
        InputStream originalImageStream =
            cprFile.getInputStream("images\\" + fileName);
        BufferedImage original = ImageIO.read(originalImageStream);
        ResampleOp resampleOp = new ResampleOp(width, height);
        resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
        BufferedImage resized = resampleOp.filter(original, null);
        ImageIO.write(resized, "bmp", outputBMP);
    }

}
