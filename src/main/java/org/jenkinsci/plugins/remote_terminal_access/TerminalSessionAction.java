package org.jenkinsci.plugins.remote_terminal_access;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.remoting.Callable;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
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
 * {@link Action} that attach to {@link AbstractBuild} to show the "interactive terminal" option in the UI.
 *
 * Also an {@link Environment} to make the build hang while the terminal is active.
 *
 * @author Kohsuke Kawaguchi
 */
public class TerminalSessionAction extends Environment implements Action {
    private final AbstractBuild build;
    private final Launcher launcher;
    private final BuildListener listener;

    private volatile Session session;

    public TerminalSessionAction(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    @Override
    public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        if (hasSession() && build.getResult()!=Result.ABORTED) {// if the build is already aborted, don't hang any further
            listener.getLogger().println("Waiting for "+
                    HyperlinkNote.encodeTo("./"+getUrlName(),"the interactive terminal")+" to exit");
            do {
                Session s = session; // capture for consistency
                if (s!=null)
                    s.join();
            } while(hasSession());
        }
        return true;
    }

    public String getIconFileName() {
        return "monitor.png";
    }

    public String getDisplayName() {
        if (build.hasPermission(ACCESS))
            return "Interactive Terminal";
        else
            return null;    // hidden for users who don't have the permission
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
    public HttpResponse doStartSession() throws IOException, InterruptedException {
        if (hasSession())
            return HttpResponses.redirectToDot();
        return doRestartSession();
    }

    /**
     * Relaunches a terminal session, even if one is live.
     */
    @RequirePOST
    public synchronized HttpResponse doRestartSession() throws IOException, InterruptedException {
        build.checkPermission(ACCESS);

        if (session!=null)
            session.interrupt();

        EnvVars env = build.getEnvironment(listener);
        env.put("TERM",Session.getAjaxTerm());
        IProcess proc = launcher.getChannel().call(
                new SessionFactoryTask(env,build.getWorkspace()));
        session = new Session(80,25,new ProcessAdapter(proc));

        return HttpResponses.redirectToDot();
    }

    /**
     * Handles ajaxterm update.
     */
    public void doU(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException {
        build.checkPermission(ACCESS);

        Session s = session; // capture for consistency
        if (s!=null)
            s.handleUpdate(req, rsp);
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

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(Run.class, Messages._TerminalSessionAction_Permissions_Title());
    public static final Permission ACCESS = new Permission(PERMISSIONS,"Access",Messages._TerminalSessionAction_AccessPermission_Description(), Job.CONFIGURE, PermissionScope.RUN);

}
