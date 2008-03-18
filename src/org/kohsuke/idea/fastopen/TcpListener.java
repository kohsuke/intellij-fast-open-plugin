package org.kohsuke.idea.fastopen;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.ide.impl.ProjectUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

/**
 * Listens to TCP connection and opens the given project.
 * 
 * @author Kohsuke Kawaguchi
 */
public class TcpListener extends Thread implements ApplicationComponent {
    public TcpListener() {
        super("Fast-open plugin TCP listener");
    }

    @NonNls @NotNull
    public String getComponentName() {
        return getClass().getName();
    }

    public void initComponent() {
        start();
    }

    public void disposeComponent() {
        interrupt();
    }

    public void run() {
        System.out.println("fast-open thread started listening");
        try {
            ServerSocket ss = new ServerSocket(39524,0, InetAddress.getByAddress(new byte[]{127,0,0,1}));
            while(true) {
                Socket socket = ss.accept();
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final String fileName = r.readLine().trim();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            File f = FastOpener.adjust(new File(fileName));
                            if(f!=null)
                                ProjectUtil.openOrImport(f.getAbsolutePath(), null, true);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
