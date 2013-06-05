package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.cli.CLICommand;
import hudson.model.Label;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.IOException;
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

    @Override
    public String getShortDescription() {
        return "Lease a computer from Jenkins slave";
    }

    @Override
    protected int run() throws Exception {
        LeaseContext context = attachContext();

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

    protected LeaseContext attachContext() throws InterruptedException, IOException {
        LeaseContext context = new LeaseFile(channel).get();
        if (context==null) {
            return new LeaseContext();   // new lease
        } else {
            throw new AbortException("A lease already exists");
        }
    }
}
