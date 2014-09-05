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

    private static void createGUI() {
        // TODO: Set order that tab moves through controls?
        JPanel contentArea = new JPanel();
        contentArea.setLayout(new MigLayout());
        contentArea.add(new JLabel("Patrician 3 executable:"));
        executableTextField = new JTextField(20);
        contentArea.add(executableTextField, "pushx, grow");
        JButton selectExecutableButton = new JButton("Browse");
        // TODO: Really need a clear button.
        // TODO: Browse needs to start at the file, if any, already entered
        // in the text box.
        selectExecutableButton.addActionListener(App::browseForExecutable);
        contentArea.add(selectExecutableButton, "wrap");
        contentArea.add(new JLabel("Width:"), "align right");
        JTextField width = new JTextField(4);
        contentArea.add(width, "split");
        contentArea.add(new JLabel("Height:"));
        JTextField height = new JTextField(4);
        contentArea.add(height, "wrap");
        // TODO: Don't enable patch button until all necessary fields are
        // filled in.
        JButton patchButton = new JButton("Patch");
        patchButton.addActionListener(actionEvent -> {
            patchButton.setEnabled(false);
            PatchWorker patchButtonWorker = new PatchWorker(
                executableTextField.getText(),
                Integer.parseInt(width.getText()),
                Integer.parseInt(height.getText()));
            patchButtonWorker.addPropertyChangeListener(changeEvent -> {
                if (changeEvent.getPropertyName().equals("state")
                    && changeEvent.getNewValue() == StateValue.DONE) {
                    packageLogger.info("patch control returned");
                    patchButton.setEnabled(true);
                    // Want to make sure no errors escape us.  XXX should
                    // maybe do better here before release.
                    try {
                        patchButtonWorker.get();
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("exception encountered during patching",
                                     e);
                    }
                }
            });
            patchButtonWorker.execute();
        });
        contentArea.add(patchButton, "span, align center, wrap");
        JTextArea logArea = new JTextArea(24, 40);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        contentArea.add(scrollPane, "span, pushy, grow");
        JFrame logWindow = new JFrame("Patrician 3 resolution patcher");
        logWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        logWindow.setContentPane(contentArea);
        logWindow.pack();
        logWindow.setMinimumSize(logWindow.getSize());
        logWindow.setVisible(true);
        setUpLogging(logArea);
    }

    private static void setUpLogging(JTextArea logArea) {
        Handler handler = new Handler() {
            public void appendToLogArea(LogRecord record) {
                // TODO: This isn't always scrolling to the bottom.  Try
                // running the app twice.
                String message = getFormatter().format(record);
                logArea.append(message);
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
        packageLogger.setLevel(Level.FINEST);
        // Hand it over to Swing.
        SwingUtilities.invokeLater(App::createGUI);
    }

}
