package org.jenkinsci.plugins.remote_terminal_access.ssh;

import hudson.Extension;
import org.apache.sshd.server.Command;
import org.jenkinsci.main.modules.sshd.SshCommandFactory;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class CommandFactoryImpl extends SshCommandFactory {
    @Override
    public Command create(CommandLine commandLine) {
        String cmd = commandLine.get(0);
        // TODO: command name
        if (cmd.equals("diagnose"))
            return new DiagnoseCommand(commandLine);
        if (cmd.equals("diagnose-tunnel"))
            return new DiagnoseTunnelCommand(commandLine);
        return null;
    }
}
