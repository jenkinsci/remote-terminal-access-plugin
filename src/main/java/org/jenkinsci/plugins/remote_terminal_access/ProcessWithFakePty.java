package org.jenkinsci.plugins.remote_terminal_access;

import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProcessWithFakePty extends ProcessWithPty {
    private final Process delegate;

    ProcessWithFakePty(Process delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public OutputStream getOutputStream() {
        return delegate.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return delegate.getInputStream();
    }

    @Override
    public int waitFor() throws InterruptedException {
        return delegate.waitFor();
    }

    @Override
    public int exitValue() {
        return delegate.exitValue();
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    public void setWindowSize(int x, int y) throws IOException {
        // ignore
    }

    public void kill(int signal) throws IOException {
        delegate.destroy();
    }

    private static final long serialVersionUID = 1L;
}
