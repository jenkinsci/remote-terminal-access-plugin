package org.jenkinsci.plugins.remote_terminal_access.ssh;

import hudson.util.StreamCopyThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link StreamCopyThread} variant that flushes the output stream.
 *
 * MINA's OutputStream does buffering, and {@link Process#getOutputStream()} talks about buffering, too,
 * so in both directions we use this to ensure bytes are delivered in a timely fashion.
 *
 * @author Kohsuke Kawaguchi
 */
public class FlushStreamCopyThread extends Thread {
    private final InputStream in;
    private final OutputStream out;
    private final boolean closeOut;

    public FlushStreamCopyThread(String threadName, InputStream in, OutputStream out, boolean closeOut) {
        super(threadName);
        this.in = in;
        if (out == null) {
            throw new NullPointerException("out is null");
        }
        this.out = out;
        this.closeOut = closeOut;
    }

    public FlushStreamCopyThread(String threadName, InputStream in, OutputStream out) {
        this(threadName,in,out,false);
    }

    @Override
    public void run() {
        try {
            try {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    if (in.available()==0)
                        out.flush();
                }
            } finally {
                // it doesn't make sense not to close InputStream that's already EOF-ed,
                // so there's no 'closeIn' flag.
                in.close();
                if(closeOut)
                    out.close();
            }
        } catch (IOException e) {
            // TODO: what to do?
        }
    }
}

