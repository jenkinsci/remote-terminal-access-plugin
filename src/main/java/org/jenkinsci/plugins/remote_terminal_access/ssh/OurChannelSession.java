package org.jenkinsci.plugins.remote_terminal_access.ssh;

import org.apache.sshd.common.Channel;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.ChannelSession;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class OurChannelSession extends ChannelSession {
    public static final NamedFactory<Channel> FACTORY = new NamedFactory<Channel>() {
        public String getName() {
            return "session";
        }

        public Channel create() {
            return new OurChannelSession();
        }
    };

    public Environment getEnvionment() {
        return env;
    }

    @Override
    protected void prepareCommand() throws IOException {
        if (command instanceof ChannelAware)
            ((ChannelAware) command).setChannel(this);
        super.prepareCommand();
    }


}
