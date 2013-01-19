package org.jenkinsci.plugins.remote_terminal_access;

import hudson.EnvVars;
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
    private Map<String,String> envs = Collections.emptyMap();

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
        this.envs = new HashMap<String, String>(map);
        return this;
    }

    /**
     * @param terminal
     *      if null, a process will be launched without a terminal.
     */
    public ProcessWithPty launch(AbstractBuild build, TaskListener listener, String terminal) throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(listener);
        if (terminal!=null)
            env.put("TERM", terminal);
        FilePath ws = build.getWorkspace();
        if (ws==null)
            throw new IOException("No workspace accessible: "+build.getFullDisplayName());
        IProcess proc;
        final String[] cmds = commands.toArray(new String[commands.size()]); // list might not be serializable
        if (terminal!=null) {
            proc = ws.act(new FileCallable<IProcess>() {
                public IProcess invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
                    PtyProcessBuilder pb = new PtyProcessBuilder();
                    pb.commands(cmds);
                    pb.envs(envs);
                    pb.envs(env);
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
                    pb.environment().putAll(env);
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
