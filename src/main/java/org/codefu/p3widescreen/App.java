package org.codefu.p3widescreen;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class App
{

    private static JTextField executablePath;

    private static void createGUI() {
        JPanel contentArea = new JPanel();
        contentArea.setLayout(new MigLayout());
        contentArea.add(new JLabel("Patrician 3 executable:"));
        executablePath = new JTextField(20);
        contentArea.add(executablePath, "pushx, grow");
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
        contentArea.add(patchButton, "span, align center, wrap");
        JTextArea logArea = new JTextArea(24, 40);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        contentArea.add(scrollPane, "span, pushy, grow");
        JFrame logWindow = new JFrame("Patrician 3 resolution patcher");
        //XXX
        //logWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        logWindow.setContentPane(contentArea);
        logWindow.pack();
        contentArea.setMinimumSize(contentArea.getPreferredSize());
        logWindow.setMinimumSize(logWindow.getSize());
        logWindow.setVisible(true);
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
                executablePath.setText(executable.getCanonicalPath());
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
