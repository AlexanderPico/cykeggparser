package org.cytoscape.keggparser.dialogs;


import org.cytoscape.keggparser.KEGGParserPlugin;
import org.cytoscape.model.CyNetwork;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class KeggSaveDialog extends JFileChooser {
    private File recentDir;
    private static String suffix = "";

    public File getRecentDir() {
        return recentDir;
    }

    public KeggSaveDialog(String filterSuffix) {
        suffix = filterSuffix;
        recentDir = new File(KEGGParserPlugin.getKEGGParserDir(), "recentKeggDir.txt");
        if (!recentDir.exists())
            try {
                recentDir.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        try {
            Scanner recentKeggDir = new Scanner(recentDir);
            if (recentKeggDir.hasNextLine())
                setCurrentDirectory(new File(recentKeggDir.nextLine()));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(KeggSaveDialog.suffix);
            }

            @Override
            public String getDescription() {
                return suffix;
            }
        });

        setDialogTitle("Save network");

        CyNetwork currentNetwork = KEGGParserPlugin.cyApplicationManager.getCurrentNetwork();
        setSelectedFile(new File(getCurrentDirectory(),
                currentNetwork.getRow(currentNetwork).get(CyNetwork.NAME, String.class) + suffix));
    }

    @Override
    public void approveSelection() {
        File selectedFile = getSelectedFile();
        final int maxFileLength = 40;

        if (selectedFile.getName().length() == 0 || selectedFile.getName().length() > maxFileLength) {
            JOptionPane.showMessageDialog(this,
                    String.format("The file name's length should be less than %d symbols) ", maxFileLength));
            return;
        }


        if (selectedFile.exists()) {
            int response = JOptionPane.showConfirmDialog(this,
                    "The file " + selectedFile.getName() +
                            " already exists. Do you want to replace the existing file?",
                    "Ovewrite file", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response != JOptionPane.YES_OPTION)
                return;
        }

        super.approveSelection();
    }
}
