package org.jenkinsci.plugins.remote_terminal_access.ssh;

/**
 * @author Kohsuke Kawaguchi
 */
public interface ChannelAware {
    void setChannel(OurChannelSession channel);
}
