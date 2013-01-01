package org.jenkinsci.plugins.remote_terminal_access;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * {@link ProcessAdapter} that caches the calls.
 *
 * @author Kohsuke Kawaguchi
 */
class CachingProcessAdapter extends ProcessAdapter implements Serializable {
    private InputStream in;
    private OutputStream out;
    private Integer exitCode;

    CachingProcessAdapter(IProcess delegate) {
        super(delegate);
    }

    @Override
    public OutputStream getOutputStream() {
        if (out==null)
            out = super.getOutputStream();
        return out;
    }

    @Override
    public InputStream getInputStream() {
        if (in==null)
            in = super.getInputStream();
        return in;
    }

    @Override
    public int waitFor() throws InterruptedException {
        if (exitCode==null)
            exitCode = super.waitFor();
        return exitCode;
    }

    @Override
    public int exitValue() {
        if (exitCode!=null) return exitCode;
        return super.exitValue();
    }

    private static final long serialVersionUID = 1L;
}
