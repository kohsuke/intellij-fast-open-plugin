package org.kohsuke.idea.fastopen;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.Messages;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class FastOpener extends AnAction {
    private File getProjectFromClipboard() {
        File f = getFileFromClipboard();
        if(f==null) return null;

        return adjust(f);
    }

    public static File adjust(File f) {
        String n = f.getName();
        if(n.endsWith(".ipr") || n.equals("pom.xml"))
            return f;   // project file name is selected

        if(f.isDirectory()) {
            // directory is selected. Find the project file
            File[] children = f.listFiles();
            for( File child : children ) {
                if(child.getName().endsWith(".ipr"))
                    return child;
                if(child.getName().equals(".idea"))
                    return f;
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
            // fall through
        } catch (IOException e) {
            // fall through
        }

        // if this is Unix, try xclip
        // one would expect that this is unnecessary, but somehow the above scheme
        // doesn't catch the contents xclip put into the clipboard.
        if(File.separatorChar=='/') {
            try {
                final File[] result = new File[1];
                Thread t = new Thread() {
                    public void run() {
                        Process p = null;
                        try {
                            ByteArrayOutputStream buf = new ByteArrayOutputStream();
                            ProcessBuilder b = new ProcessBuilder("xclip", "-o", "-selection", "clipboard");
                            b.redirectErrorStream(true);
                            p = b.start();
                            p.getOutputStream().close();
                            copyStream(p.getInputStream(), buf);
                            p.waitFor();
                            result[0] = new File(new String(buf.toByteArray()).trim());
                        } catch (IOException e) {
                            // ignore
                            if(p!=null) p.destroy();
                        } catch (InterruptedException e) {
                            // ignore
                            if(p!=null) p.destroy();
                        }
                    }
                };
                t.start();
                t.join(1000);
                t.interrupt(); // abandon the processing if it's taking too long
                if(result[0]!=null && result[0].exists())  return result[0];
            } catch (InterruptedException e) {
                // fall through
            }
        }

        return null;
    }

    private void copyStream(InputStream i, OutputStream o) throws IOException {
        byte[] buf = new byte[1024];
        int len;

        while((len=i.read(buf))>=0)
            o.write(buf,0,len);
        i.close();
        o.close();
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
                 ProjectUtil.openOrImport(f.getAbsolutePath(), null, true);
        } catch (Exception x) {
            Messages.showErrorDialog(x.getMessage(),"Error!");
        }
    }
}
