package org.jenkinsci.plugins.remote_terminal_access;

import hudson.AbortException;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.kohsuke.ajaxterm.ProcessWithPty;
import org.kohsuke.ajaxterm.PtyProcessBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.*;

/**
 * Launches a {@link ProcessWithPty} on a machine &amp; directory that's running a {@link AbstractBuild}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ProcessWithPtyLauncher implements Serializable {
    private List<String> commands;
    private final Map<String,String> envs = new HashMap<String,String>();
    /**
     * Specifies the current directory to launch the process in
     * (and it also determines the slave it will be run on.)
     */
    private FilePath ws;

    public ProcessWithPtyLauncher shell() {
        return commands(asList("/bin/bash", "-i"));
    }

    public ProcessWithPtyLauncher commands(List<String> commands) {
        this.commands = new ArrayList<String>(commands);
        return this;
    }

    /**
     * Overrides on top of what the build gets.
     */
    public ProcessWithPtyLauncher envs(Map<String,String> map) {
        this.envs.putAll(map);
        return this;
    }

    public ProcessWithPtyLauncher ws(FilePath ws) {
        this.ws = ws;
        return this;
    }

    /**
     * Configures the launcher to launch the process in the workspace of an in-progress build with the same
     * set of environment variables.
     */
    public ProcessWithPtyLauncher configure(AbstractBuild build, TaskListener listener) throws IOException, InterruptedException {
        envs.putAll(build.getEnvironment(listener));
        ws = build.getWorkspace();
        if (ws ==null)
            throw new AbortException("No workspace accessible: "+build.getFullDisplayName());
        if (!ws.isDirectory())
            throw new AbortException("Workspace doesn't exist: "+ ws);
        return this;
    }

    /**
     * @param terminal
     *      if null, a process will be launched without a terminal.
     */
    public ProcessWithPty launch(String terminal) throws IOException, InterruptedException {
        IProcess proc;
        final String[] cmds = commands.toArray(new String[commands.size()]); // list might not be serializable
        if (terminal!=null) {
            envs.put("TERM", terminal);
            proc = ws.act(new FileCallable<IProcess>() {
                public IProcess invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
                    PtyProcessBuilder pb = new PtyProcessBuilder();
                    pb.commands(cmds);
                    pb.envs(envs);
                    pb.pwd(dir);
                    return new RemotableProcess(pb.forkWithHelper());
                }

                private static final long serialVersionUID = 1L;
            });
        } else {
            proc = ws.act(new FileCallable<IProcess>() {
                public IProcess invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
                    ProcessBuilder pb = new ProcessBuilder();
                    pb.command(cmds);
                    pb.environment().putAll(envs);
                    pb.environment().remove("TERM");    // no terminal
                    pb.directory(dir);
                    return new RemotableProcess(new ProcessWithFakePty(pb.start()));
                }

                private static final long serialVersionUID = 1L;
            });
        }
        return new ProcessAdapter(proc);
    }

    private static final long serialVersionUID = 1L;
}
