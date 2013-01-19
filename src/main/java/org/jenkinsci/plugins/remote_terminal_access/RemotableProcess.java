package org.jenkinsci.plugins.remote_terminal_access;

import hudson.remoting.Channel;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.RemoteOutputStream;
import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * {@link IProcess} implementation that acts accordingly when remoted via {@link Channel}
 *
 * <p>
 * When not remoted, this object simply acts as a delegation to another {@link Process} object
 * (hence extending {@link DelegatingProcess}.)
 *
 * @author Kohsuke Kawaguchi
 */
public class RemotableProcess extends DelegatingProcess implements Serializable {
    public RemotableProcess(ProcessWithPty delegate) {
        super(delegate);
    }

    private Object writeReplace() {
        // if remoted, the remote side first goes through the cache layer to reduce calls,
        // then remoting, finally to RemotedProcess so that InputStream/OutputStream are properly wrapped.
        return new CachingProcessAdapter(Channel.current().export(IProcess.class,new RemotedProcess(delegate)));
    }

    private static final long serialVersionUID = 1L;
}

final class RemotedProcess extends DelegatingProcess implements Serializable {
    RemotedProcess(ProcessWithPty delegate) {
        super(delegate);
    }

    @Override
    public OutputStream getOutputStream() {
        return new RemoteOutputStream(super.getOutputStream());
    }

    @Override
    public InputStream getInputStream() {
        return new RemoteInputStream(super.getInputStream());
    }

    @Override
    public InputStream getErrorStream() {
        return new RemoteInputStream(super.getErrorStream());
    }

    private static final long serialVersionUID = 1L;
}
