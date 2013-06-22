package org.jenkinsci.plugins.remote_terminal_access;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.cli.CLICommand;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Queue.Executable;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.remoting.Which;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.remote_terminal_access.lease.LeasedTask;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Launches a new JVM on a slave and connect it with the master with remoting channel.
 *
 * stdout/stdin of the client side of the CLI gets connected to the channel, and stderr
 * is forwarded straight to the stderr of the client.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ChannelProcessCommand extends CLICommand {
    /**
     * Label that specifies the slave you'll want.
     */
    @Argument(required=true)
    public String labelExpr;

    @Option(name="-J",usage="JVM option")
    public List<String> jvmOpts = new ArrayList<String>();

    @Override
    public String getShortDescription() {
        return "Launch a new JVM on a slave and connect it with the master with remoting";
    }

    private int exitCode;

    @Override
    protected int run() throws Exception {
        Label l = Label.parseExpression(labelExpr);

        LeasedTask t = new LeasedTask(l,"TODO",-1,new Runnable() {
            public void run() {
                try {
                    String javaExe = System.getProperty("java.home") + "/bin/java";
                    File slaveJar = Which.jarFile(hudson.remoting.Launcher.class);

                    ArgumentListBuilder args = new ArgumentListBuilder().add(javaExe);
                    for (String jvmOpt : jvmOpts)
                        args.add(jvmOpt);
                    if(slaveJar.isFile())
                        args.add("-jar").add(slaveJar);
                    else // in production code this never happens, but during debugging this is convenient
                        args.add("-cp").add(slaveJar).add(hudson.remoting.Launcher.class.getName());

                    TaskListener taskListener = TaskListener.NULL;  // TODO
                    Launcher l = Computer.currentComputer().getNode().createLauncher(taskListener);


                    Proc p = l.launch().cmds(args).stdout(stdout).stdin(stdin).stderr(stderr).start();

                    exitCode = p.join();

                    LOGGER.log(Level.INFO, "Ended");
                } catch (InterruptedException e) {
                    // this happens when we are done
                } catch (IOException e) {
                    e.printStackTrace(); // TODO
                }
            }
        });

        // wait for the stuff to start executing
        QueueTaskFuture<Executable> f = (QueueTaskFuture<Executable>) Jenkins.getInstance().getQueue().schedule(t, 0).getFuture();
        f.get();

        return exitCode;
    }

    private static final Logger LOGGER = Logger.getLogger(ChannelProcessCommand.class.getName());
}
