package org.kohsuke.idea.fastopen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class FastOpener extends AnAction {
    private File getProjectFromClipboard() {
        File f = getFileFromClipboard();
        if(f==null) return null;

        String n = f.getName();
        if(n.endsWith(".ipr") || n.equals("pom.xml"))
            return f;   // project file name is selected

        if(f.isDirectory()) {
            // directory is selected. Find the project file
            File[] children = f.listFiles();
            for( File child : children ) {
                if(child.getName().endsWith(".ipr"))
                    return child;
            }
            // next attempt is to find POM
            for( File child : children ) {
                if(child.getName().equals("pom.xml"))
                    return child;
            }
        }

        // not a project file
        return null;
    }

    private File getFileFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            if(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String data = clipboard.getData(DataFlavor.stringFlavor).toString();
                File f =  new File(data.trim());
                if(f.exists())  return f;
            }

            if(clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                List<File> data = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                if(!data.isEmpty()) return data.get(0);
            }
        } catch (UnsupportedFlavorException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    public void update(AnActionEvent event) {
        Presentation p = event.getPresentation();

        File f = getProjectFromClipboard();
        if(f!=null) {
            p.setEnabled(true);
            p.setText("Open Project _Fast [.../"+f.getParentFile().getName()+'/'+f.getName()+"]");
        } else {
            p.setEnabled(false);
            p.setText("Open Project _Fast");
        }
    }

    public void actionPerformed(AnActionEvent e) {
        try {
            File f = getProjectFromClipboard();
            if(f!=null)
                ProjectManager.getInstance().loadAndOpenProject(f.getAbsolutePath());
        } catch (Exception x) {
            Messages.showErrorDialog(x.getMessage(),"Error!");
        }
    }
}
