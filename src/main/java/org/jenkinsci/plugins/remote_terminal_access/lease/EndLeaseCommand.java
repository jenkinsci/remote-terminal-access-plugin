package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.Extension;
import hudson.cli.CLICommand;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class EndLeaseCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return "Finish the lease in progress";
    }

    @Override
    protected int run() throws Exception {
        LeaseFile lf = new LeaseFile(channel);
        LeaseContext c = lf.get();
        if (c!=null) {
            stderr.println("Finishing the lease "+c.id);
            c.end();
            lf.clear();
        }
        return 0;
    }
}
