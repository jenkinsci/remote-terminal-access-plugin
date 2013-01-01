package org.jenkinsci.plugins.remote_terminal_access;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Makes {@link Process} look and feel like {@link Process} (or another {@link IProcess}).
 *
 * @author Kohsuke Kawaguchi
 * @see ProcessAdapter
 */
class DelegatingProcess extends Process implements IProcess {
    protected final Process delegate;

    public DelegatingProcess(Process delegate) {
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
}
