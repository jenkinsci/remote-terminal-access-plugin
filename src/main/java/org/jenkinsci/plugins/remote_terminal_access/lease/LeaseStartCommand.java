package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.cli.CLICommand;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Run;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
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
        Jenkins.getInstance().checkPermission(TerminalSessionAction.ACCESS);

        LeaseContext context = attachContext();
        context.checkOwner();

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

    protected LeaseContext attachContext() throws InterruptedException, IOException, CmdLineException {
        LeaseContext context = new LeaseFile(channel).get();
        if (context==null) {
            context = new LeaseContext();   // new lease

            Run x = optCurrentlyBuilding();
            if (x!=null) {
                // if this is inside a build, record it
                LeaseBuildAction lba = x.getAction(LeaseBuildAction.class);
                if (lba==null)
                    x.addAction(lba=new LeaseBuildAction());
                lba.addLease(context);
            }

            return context;
        } else {
            throw new AbortException("A lease already exists");
        }
    }

    /**
     * If the command is currently running inside a build, return it. Otherwise null.
     *
     * TODO: switch to the same method in core 1.519
     */
    protected Run optCurrentlyBuilding() throws CmdLineException {
        try {
            CLICommand c = CLICommand.getCurrent();
            if (c==null)    throw new IllegalStateException("Not executing a CLI command");
            String[] envs = c.checkChannel().call(new GetCharacteristicEnvironmentVariables());

            if (envs[0]==null || envs[1]==null)
                return null;

            Job j = Jenkins.getInstance().getItemByFullName(envs[0],Job.class);
            if (j==null)    throw new CmdLineException("No such job: "+envs[0]);

            try {
                Run r = j.getBuildByNumber(Integer.parseInt(envs[1]));
                if (r==null)    throw new CmdLineException("No such build #"+envs[1]+" in "+envs[0]);
                return r;
            } catch (NumberFormatException e) {
                throw new CmdLineException("Invalid build number: "+envs[1]);
            }
        } catch (IOException e) {
            throw new CmdLineException("Failed to identify the build being executed",e);
        } catch (InterruptedException e) {
            throw new CmdLineException("Failed to identify the build being executed",e);
        }
    }

    /**
     * Gets the environment variables that points to the build being executed.
     */
    private static final class GetCharacteristicEnvironmentVariables implements Callable<String[],IOException> {
        public String[] call() throws IOException {
            return new String[] {
                System.getenv("JOB_NAME"),
                System.getenv("BUILD_NUMBER")
            };
        }
    }
}
