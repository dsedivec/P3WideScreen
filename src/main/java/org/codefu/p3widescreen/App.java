package org.codefu.p3widescreen;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static javax.swing.SwingWorker.StateValue;

// TODO: Set Mac app name
// http://stackoverflow.com/q/3154638/2305480
// https://developer.apple.com/library/mac/documentation/java/conceptual/java14development/07-nativeplatformintegration/nativeplatformintegration.html
public class App
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static java.util.logging.Logger packageLogger;
    private static JTextField gameDirectoryTextField;
    private static JTextField widthTextField;
    private static JTextField heightTextField;
    private static JButton patchButton;

    private static void createGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException
                 | InstantiationException
                 | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            // Well, just ignore it, I guess?  Logging not set up at
            // this point, either.
            e.printStackTrace();
        }
        JPanel contentArea = new JPanel();
        contentArea.setLayout(new MigLayout());
        contentArea.add(new JLabel("Patrician 3 directory:"));
        gameDirectoryTextField = new JTextField(20);
        contentArea.add(gameDirectoryTextField, "pushx, grow");
        JButton selectExecutableButton = new JButton("Browse");
        selectExecutableButton.addActionListener(App::browseForGameDirectory);
        contentArea.add(selectExecutableButton, "wrap");
        contentArea.add(new JLabel("Width:"), "align right");
        widthTextField = new JTextField("1280", 4);
        contentArea.add(widthTextField, "split");
        contentArea.add(new JLabel("Height:"));
        heightTextField = new JTextField("1024", 4);
        contentArea.add(heightTextField, "wrap");
        patchButton = new JButton("Patch");
        patchButton.addActionListener(App::handlePatchButton);
        contentArea.add(patchButton, "span, align center, wrap");
        JTextArea logArea = new JTextArea(24, 60);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        // Always scroll to bottom on append.
        DefaultCaret caret = (DefaultCaret)logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(logArea);
        contentArea.add(scrollPane, "span, pushy, grow, wrap");
        JButton clearButton = new JButton("Clear log");
        clearButton.addActionListener(actionEvent -> logArea.setText(null));
        contentArea.add(clearButton, "span, align center, wrap");
        JFrame logWindow = new JFrame("Patrician 3 resolution patcher");
        logWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        logWindow.setContentPane(contentArea);
        logWindow.pack();
        logWindow.setMinimumSize(logWindow.getSize());
        logWindow.setVisible(true);
        setUpLogging(logArea);
    }

    private static int getIntFromTextField(JTextField textField,
                                            String title) {
        try {
            return Integer.parseInt(textField.getText());
        } catch (NumberFormatException e) {
            logger.error("invalid {}", title);
            throw e;
        }
    }

    private static void handlePatchButton(ActionEvent actionEvent) {
        int width, height;
        try {
            width = getIntFromTextField(widthTextField, "width");
            height = getIntFromTextField(heightTextField, "height");
        } catch (NumberFormatException e) {
            // getIntFromTextField logged for us.
            return;
        }
        PatchWorker patchButtonWorker =
            new PatchWorker(gameDirectoryTextField.getText(), width, height);
        patchButtonWorker.addPropertyChangeListener(changeEvent -> {
            if (changeEvent.getPropertyName().equals("state")
                && changeEvent.getNewValue() == StateValue.DONE) {
                logger.debug("patch control returned");
                patchButton.setEnabled(true);
                try {
                    patchButtonWorker.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("exception encountered during patching",
                                 e);
                }
            }
        });
        patchButton.setEnabled(false);
        patchButtonWorker.execute();
    }

    private static void setUpLogging(JTextArea logArea) {
        Handler handler = new Handler() {
            public void appendToLogArea(LogRecord record) {
                String message = getFormatter().format(record);
                logArea.append(message);
                //noinspection ThrowableResultOfMethodCallIgnored
                Throwable throwable = record.getThrown();
                if (throwable != null) {
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    throwable.printStackTrace(printWriter);
                    logArea.append(stringWriter.toString());
                }
            }

            @Override
            public void publish(LogRecord record) {
                if (SwingUtilities.isEventDispatchThread()) {
                    appendToLogArea(record);
                } else {
                    SwingUtilities.invokeLater(() -> appendToLogArea(record));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("%s: %s\n",
                                     record.getLevel().getLocalizedName(),
                                     formatMessage(record));
            }
        });
        packageLogger.addHandler(handler);
        packageLogger.setUseParentHandlers(false);
        Thread.setDefaultUncaughtExceptionHandler(
            (thread, exception) ->
                logger.error("uncaught exception on thread {}",
                             thread, exception));
    }

    private static void browseForGameDirectory(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select your Patrician 3 directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        File currentDirectory = new File(gameDirectoryTextField.getText());
        // This doesn't really work with directories, but at least it puts
        // you in the right parent directory.  I tried the scary solution
        // given at http://stackoverflow.com/a/9119414/2305480 but it
        // didn't work for me.
        fc.setSelectedFile(currentDirectory);
        // Hack to make JFileChooser show something in its stupid
        // "File format" combo box.  I got this idea from Stack Overflow.
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories";
            }
        });
        int returnValue = fc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File gameDirectory = fc.getSelectedFile();
            try {
                gameDirectoryTextField.setText(
                    gameDirectory.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(
                    "failed to get path from file chooser", e);
            }
        }
    }

    public static void main(String[] args) {
        // Set log level for our whole package.
        String packageName = App.class.getPackage().getName();
        packageLogger = java.util.logging.Logger.getLogger(packageName);
        //packageLogger.setLevel(Level.FINEST);
        // Hand it over to Swing.
        SwingUtilities.invokeLater(App::createGUI);
    }

}
