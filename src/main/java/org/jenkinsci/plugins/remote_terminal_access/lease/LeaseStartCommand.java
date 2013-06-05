package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.Extension;
import hudson.Util;
import hudson.cli.CLICommand;
import hudson.model.Label;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import java.util.ArrayList;
import java.util.List;

/**
 * Lease executors.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class LeaseStartCommand extends CLICommand {

    @Option(name="-n",usage="Human readable name that describe this command. Used in Jenkins' UI.")
    public String name;

    @Option(name="-d",usage="Estimated duration of this task in milliseconds, or -1 if unknown")
    public long duration = -1;

    @Argument
    public List<String> leases = new ArrayList<String>();

    @Option(name="-add",usage="Adds to existing lease, instead of creating a new one")
    public boolean add;

    @Override
    public String getShortDescription() {
        return "Lease a computer from Jenkins slave";
    }

    @Override
    protected int run() throws Exception {
        LeaseContext context = new LeaseFile(channel).get();
        if (context==null) {
            if (add) {
                stderr.println("There's no existing lease");
                return 1;
            }
            context = new LeaseContext();   // new lease
        } else {
            if (!add) {
                stderr.println("A lease already exists");
                return 1;
            }
        }

        for (String lease : leases) {
            Label l;
            String alias;

            String[] tokens = lease.split("=");
            switch (tokens.length) {
            case 1:
                alias = tokens[0];
                l = null;
                break;
            case 2:
                alias = tokens[0];
                l = Label.parseExpression(tokens[1]);
                break;
            default:
                throw new IllegalArgumentException("Invalid lease specifier: "+lease);
            }

            context.add(alias, l, name, duration);
        }



        // wait for everyone to start scheduling
        try {
            stderr.println("Waiting for all the executors to start running");
            context.waitForStart();
            new LeaseFile(channel).set(context);
            stderr.println("Ready. Current aliases: " + Util.join(context.getAliases(), ","));
            return 0;
        } catch (InterruptedException e) {
            context.end();
            throw e;
        }
    }
}
