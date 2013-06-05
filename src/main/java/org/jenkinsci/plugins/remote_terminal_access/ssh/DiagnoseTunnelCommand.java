package org.jenkinsci.plugins.remote_terminal_access.ssh;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;
import org.jenkinsci.main.modules.sshd.AsynchronousCommand;
import org.jenkinsci.main.modules.sshd.SshCommandFactory.CommandLine;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hudson.Util.*;

/**
 * {@link Command} implementation that tunnels another SSH session in its stdin/stdout.
 *
 * The tunneled SSH session runs the "diagnose JOBNAME" command implicitly, thereby
 * making the nested SSH session behave like a real SSH connection to a real server.
 *
 * @author Kohsuke Kawaguchi
 */
public class DiagnoseTunnelCommand extends AbstractTunnelCommand {

    @Argument(metaVar="JOBNAME",index=0,required=true)
    String jobName;

    @Option(name="-suffix")
    String jobNameSuffix="";

    public DiagnoseTunnelCommand(CommandLine command) {
        super(command);
    }

    @Override
    protected int run() throws Exception {
        List<String> args = getCmdLine().subList(1, getCmdLine().size());
        new CmdLineParser(this).parseArgument(args);

        // trim off the suffix if it's there
        if (jobName.endsWith(jobNameSuffix))
            jobName = jobName.substring(0,jobName.length()-jobNameSuffix.length());

        return super.run();
    }

    @Override
    protected CommandFactory getCommandFactory() {
        return new CommandFactory() {
            public Command createCommand(String command) {
                return new DiagnoseCommand(new CommandLine(String.format("diagnose '%s' %s",jobName,command)));
            }
        };
    }

    @Override
    protected Factory<Command> getShellFactory() {
        return new Factory<Command>() {
            public Command create() {
                return new DiagnoseCommand(new CommandLine(String.format("diagnose '%s'",jobName)));
            }
        };
    }
}