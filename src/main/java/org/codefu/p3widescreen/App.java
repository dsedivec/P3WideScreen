package org.codefu.p3widescreen;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
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

public class App
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static java.util.logging.Logger packageLogger;
    private static JTextField executableTextField;
    private static JTextField widthTextField;
    private static JTextField heightTextField;
    private static JButton patchButton;

    private static void createGUI() {
        // TODO: Set order that tab moves through controls?
        JPanel contentArea = new JPanel();
        contentArea.setLayout(new MigLayout());
        contentArea.add(new JLabel("Patrician 3 executable:"));
        executableTextField = new JTextField(20);
        contentArea.add(executableTextField, "pushx, grow");
        JButton selectExecutableButton = new JButton("Browse");
        // TODO: Browse needs to start at the file, if any, already entered
        // in the text box.
        selectExecutableButton.addActionListener(App::browseForExecutable);
        contentArea.add(selectExecutableButton, "wrap");
        contentArea.add(new JLabel("Width:"), "align right");
        widthTextField = new JTextField(4);
        contentArea.add(widthTextField, "split");
        contentArea.add(new JLabel("Height:"));
        heightTextField = new JTextField(4);
        contentArea.add(heightTextField, "wrap");
        // TODO: Don't enable patch button until all necessary fields are
        // filled in.
        patchButton = new JButton("Patch");
        patchButton.addActionListener(App::handlePatchButton);
        contentArea.add(patchButton, "span, align center, wrap");
        JTextArea logArea = new JTextArea(24, 60);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        contentArea.add(scrollPane, "span, pushy, grow, wrap");
        JButton clearButton = new JButton("Clear log");
        clearButton.addActionListener(actionEvent -> logArea.setText(null));
        contentArea.add(clearButton, "span, align center, wrap");
        // TODO: Set Mac app name?
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
            new PatchWorker(executableTextField.getText(), width, height);
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
                // TODO: This isn't always scrolling to the bottom.  Try
                // running the app twice.
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

    private static void browseForExecutable(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select your Patrician 3 executable");
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().compareToIgnoreCase("patrician3.exe") == 0;
            }

            @Override
            public String getDescription() {
                return "patrician3.exe";
            }
        });
        int returnValue = fc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File executable = fc.getSelectedFile();
            try {
                executableTextField.setText(executable.getCanonicalPath());
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
        // TODO: Reduce log level?
        packageLogger.setLevel(Level.FINEST);
        // Hand it over to Swing.
        SwingUtilities.invokeLater(App::createGUI);
    }

}
