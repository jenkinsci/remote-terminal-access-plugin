package org.jenkinsci.plugins.remote_terminal_access;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Makes {@link IProcess} look and feel like {@link Process} (or another {@link IProcess}).
 *
 * @author Kohsuke Kawaguchi
 * @see DelegatingProcess
 */
class ProcessAdapter extends Process implements Serializable, IProcess {
    private final IProcess delegate;

    ProcessAdapter(IProcess delegate) {
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

    private static final long serialVersionUID = 1L;
}
