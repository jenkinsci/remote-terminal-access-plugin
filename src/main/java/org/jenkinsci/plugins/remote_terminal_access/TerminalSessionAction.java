package org.jenkinsci.plugins.remote_terminal_access;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.remoting.Callable;
import org.kohsuke.ajaxterm.PtyProcessBuilder;
import org.kohsuke.ajaxterm.Session;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class TerminalSessionAction extends Environment implements Action {
    private final AbstractBuild build;
    private final Launcher launcher;
    private final BuildListener listener;

    private Session session;

    public TerminalSessionAction(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public String getIconFileName() {
        return "terminal.png";
    }

    public String getDisplayName() {
        return "Launch Interactive Terminal";
    }

    public String getUrlName() {
        return "interactive-terminal";
    }

    public synchronized Session getSession() throws IOException, InterruptedException {
        if (session==null) {
            IProcess proc = launcher.getChannel().call(new SessionFactoryTask());
            session = new Session(80,25,new ProcessAdapter(proc));
        }
        return session;
    }

    public void doU(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException {
        getSession().handleUpdate(req, rsp);
    }

    private static class SessionFactoryTask implements Callable<IProcess, IOException> {
        public IProcess call() throws IOException {
            PtyProcessBuilder pb = new PtyProcessBuilder();
            pb.commands("/bin/bash","-i");  // TODO: properly inherit the environment for the build
            return new RemotableProcess(pb.fork());
        }
        private static final long serialVersionUID = 1L;
    }
}
