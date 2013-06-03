package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 * Remembers the in-scope session in the current directory.
 *
 * @author Kohsuke Kawaguchi
 */
public class LeaseFile implements Serializable {
    private transient final Channel channel;

    public LeaseFile(Channel channel) {
        this.channel = channel;
    }

    /**
     * Deletes the existing lease file.
     */
    public void clear() throws InterruptedException, IOException {
        channel.call(new Callable<Void,IOException>() {
            public Void call() throws IOException {
                File d = new File(".").getAbsoluteFile();
                while (d!=null) {
                    File lease = new File(d,".jenkins.lease");
                    if (lease.exists()) {
                        lease.delete();
                        break;
                    }
                    d = d.getParentFile();
                }
                return null;
            }
        });
    }

    /**
     * Gets the current lease context if one is available.
     */
    public LeaseContext get() throws InterruptedException, IOException {
        Properties props = channel.call(new Callable<Properties,IOException>() {
            public Properties call() throws IOException {
                File d = new File(".").getAbsoluteFile();
                while (d!=null) {
                    File lease = new File(d,".jenkins.lease");
                    if (lease.exists()) {
                        Properties props = new Properties();
                        FileInputStream in = new FileInputStream(lease);
                        try {
                            props.load(in);
                        } finally {
                            in.close();
                        }
                        return props;
                    }
                    d = d.getParentFile();
                }
                return null;
            }
        });

        if (props!=null && Jenkins.SESSION_HASH.equals(props.getProperty("session")))
            return LeaseContext.getById(props.getProperty("lease"));

        return null;
    }

    public void set(LeaseContext context) throws InterruptedException, IOException {
        final String id = context.id;
        final String session = Jenkins.SESSION_HASH;

        channel.call(new Callable<Void, IOException>() {
            public Void call() throws IOException {
                File lease = new File(".jenkins.lease");
                Properties props = new Properties();
                props.setProperty("session", session);
                props.setProperty("lease", id);

                FileOutputStream out = new FileOutputStream(lease);
                try {
                    props.store(out, null);
                } finally {
                    out.close();
                }

                return null;
            }
        });
    }

    private static final long serialVersionUID = 1L;
}
