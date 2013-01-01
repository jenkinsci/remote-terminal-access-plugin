package org.jenkinsci.plugins.remote_terminal_access;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Remoting interface for {@link Process}
 *
 * @author Kohsuke Kawaguchi
 */
public interface IProcess {
    OutputStream getOutputStream();
    InputStream getInputStream();
    int waitFor() throws InterruptedException;
    int exitValue();
    void destroy();
}
