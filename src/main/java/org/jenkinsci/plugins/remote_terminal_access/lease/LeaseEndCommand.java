package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.Extension;
import hudson.cli.CLICommand;
import org.kohsuke.args4j.Argument;

import java.util.ArrayList;
import java.util.List;

/**
 * Finishes the lease in progress.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class LeaseEndCommand extends CLICommand {
    @Argument(metaVar="ALIAS",usage="Aliases to end. If empty, end the entire lease")
    public List<String> aliases = new ArrayList<String>();


    @Override
    public String getShortDescription() {
        return "Finish the lease in progress";
    }

    @Override
    protected int run() throws Exception {
        LeaseFile lf = new LeaseFile(channel);
        LeaseContext c = lf.get();
        if (c!=null) {
            c.checkOwner();
            if (aliases.isEmpty()) {
                stderr.println("Finishing the lease "+c.id);
                c.end();
                lf.clear();
            } else {
                for (String a : aliases) {
                    stderr.println("Ending the alias: "+a);
                    c.remove(a);
                }
                lf.set(c); // write back the updated information
            }
            return 0;
        } else {
            stderr.println("No lease found in the current directory");
            return 1;
        }
    }
}
