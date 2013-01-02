package org.jenkinsci.plugins.remote_terminal_access;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.remoting.Callable;
import org.kohsuke.ajaxterm.PtyProcessBuilder;
import org.kohsuke.ajaxterm.Session;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
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
        return "monitor.png";
    }

    public String getDisplayName() {
        return "Interactive Terminal";
    }

    public String getUrlName() {
        return "interactive-terminal";
    }

    public synchronized boolean hasSession() {
        return session!=null && session.isAlive();
    }

    /**
     * Starts a new terminal session if non exists.
     */
    @RequirePOST
    public synchronized HttpResponse doStartSession() throws IOException, InterruptedException {
        if (hasSession())
            return HttpResponses.redirectToDot();
        return doRestartSession();
    }

    /**
     * Relaunches a terminal session, even if one is live.
     */
    @RequirePOST
    public synchronized HttpResponse doRestartSession() throws IOException, InterruptedException {
        if (session!=null)
            session.interrupt();

        IProcess proc = launcher.getChannel().call(
                new SessionFactoryTask(build.getEnvironment(listener),build.getWorkspace()));
        session = new Session(80,25,new ProcessAdapter(proc));

        return HttpResponses.redirectToDot();
    }

    /**
     * Handles ajaxterm update.
     */
    public void doU(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException {
        if (session!=null)
            session.handleUpdate(req, rsp);
        else
            rsp.setStatus(404);
    }

    private static class SessionFactoryTask implements Callable<IProcess, IOException> {
        private final EnvVars envs;
        private final FilePath pwd;
        public SessionFactoryTask(EnvVars envs, FilePath pwd) {
            this.envs = envs;
            this.pwd = pwd;
        }

        public IProcess call() throws IOException {
            PtyProcessBuilder pb = new PtyProcessBuilder();
            pb.commands("/bin/bash","-i");
            pb.envs(envs);
            pb.pwd(new File(pwd.getRemote()));
            return new RemotableProcess(pb.forkWithHelper());
        }
        private static final long serialVersionUID = 1L;
    }
}
