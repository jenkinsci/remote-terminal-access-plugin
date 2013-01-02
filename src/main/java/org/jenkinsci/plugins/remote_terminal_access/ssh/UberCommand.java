package org.jenkinsci.plugins.remote_terminal_access.ssh;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class UberCommand implements Command, ChannelAware {
    public void setChannel(OurChannelSession channel) {
        channel.getEnvionment().addSignalListener(new SignalListener() {
            public void signal(Signal signal) {
                System.out.println("Sending a signal: "+signal);
            }
        });
    }

    public void setInputStream(InputStream in) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void setOutputStream(OutputStream out) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void setErrorStream(OutputStream err) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void setExitCallback(ExitCallback callback) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void start(Environment env) throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void destroy() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
