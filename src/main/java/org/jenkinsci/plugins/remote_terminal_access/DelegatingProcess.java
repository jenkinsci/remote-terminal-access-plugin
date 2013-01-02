package org.jenkinsci.plugins.remote_terminal_access;

import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Makes {@link Process} look and feel like {@link Process} (or another {@link IProcess}).
 *
 * @author Kohsuke Kawaguchi
 * @see ProcessAdapter
 */
class DelegatingProcess extends ProcessWithPty implements IProcess {
    protected final ProcessWithPty delegate;

    public DelegatingProcess(ProcessWithPty delegate) {
        this.delegate = delegate;
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
    public InputStream getErrorStream() {
        return delegate.getErrorStream();
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

    @Override
    public void setWindowSize(int x, int y) throws IOException {
        delegate.setWindowSize(x,y);
    }

    @Override
    public void kill(int signal) throws IOException {
        delegate.kill(signal);
    }
}
