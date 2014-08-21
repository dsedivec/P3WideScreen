package org.codefu.p3widescreen;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.*;

import static javax.swing.SwingWorker.StateValue;

public class App
{
    private static JTextField executableTextField;
    private static Logger logger = Logger.getLogger("P3WideScreen");

    private static void createGUI() {
        JPanel contentArea = new JPanel();
        contentArea.setLayout(new MigLayout());
        contentArea.add(new JLabel("Patrician 3 executable:"));
        executableTextField = new JTextField(20);
        contentArea.add(executableTextField, "pushx, grow");
        JButton selectExecutableButton = new JButton("Browse");
        selectExecutableButton.addActionListener(App::browseForExecutable);
        contentArea.add(selectExecutableButton, "wrap");
        contentArea.add(new JLabel("Width:"), "align right");
        JTextField width = new JTextField(4);
        contentArea.add(width, "split");
        contentArea.add(new JLabel("Height:"));
        JTextField height = new JTextField(4);
        contentArea.add(height, "wrap");
        JButton patchButton = new JButton("Patch");
        patchButton.addActionListener(actionEvent -> {
            patchButton.setEnabled(false);
            PatchWorker patchButtonWorker = new PatchWorker(
                executableTextField.getText(), logger);
            patchButtonWorker.addPropertyChangeListener(changeEvent -> {
                if (changeEvent.getPropertyName().equals("state")
                    && changeEvent.getNewValue() == StateValue.DONE) {
                    patchButton.setEnabled(true);
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
            @Override
            public void publish(LogRecord record) {
                String message = getFormatter().format(record);
                if (SwingUtilities.isEventDispatchThread()) {
                    logArea.append(message);
                } else {
                    SwingUtilities.invokeLater(() -> logArea.append(message));
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
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
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
        SwingUtilities.invokeLater(App::createGUI);
    }

}
