package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.AbortException;
import hudson.Extension;

import java.io.IOException;

/**
 * Adds additional slaves to the current lease.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class LeaseAddCommand extends LeaseStartCommand {
    @Override
    public String getShortDescription() {
        return "Adds additional slaves to the current lease";
    }

    @Override
    protected LeaseContext attachContext() throws InterruptedException, IOException {
        LeaseContext context = new LeaseFile(channel).get();
        if (context==null) {
            throw new AbortException("There's no existing lease");
        }
        return context;
    }
}
