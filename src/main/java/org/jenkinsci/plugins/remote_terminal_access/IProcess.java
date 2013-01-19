package org.jenkinsci.plugins.remote_terminal_access;

import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Remoting interface for {@link ProcessWithPty}
 *
 * @author Kohsuke Kawaguchi
 */
public interface IProcess {
    OutputStream getOutputStream();
    InputStream getInputStream();
    InputStream getErrorStream();
    int waitFor() throws InterruptedException;
    int exitValue();
    void destroy();
    void setWindowSize(int x, int y) throws IOException;
    void kill(int signal) throws IOException;
}
