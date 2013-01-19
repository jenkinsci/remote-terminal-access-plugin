package org.jenkinsci.plugins.remote_terminal_access;

import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Makes {@link IProcess} look and feel like {@link Process} (or another {@link IProcess}).
 *
 * @author Kohsuke Kawaguchi
 * @see DelegatingProcess
 */
class ProcessAdapter extends ProcessWithPty implements Serializable, IProcess {
    private final IProcess delegate;

    ProcessAdapter(IProcess delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream getErrorStream() {
        return delegate.getErrorStream();
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
        delegate.setWindowSize(x, y);
    }

    public void kill(int signal) throws IOException {
        delegate.kill(signal);
    }

    private static final long serialVersionUID = 1L;
}
