package org.jenkinsci.plugins.remote_terminal_access.ssh;

import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.main.modules.sshd.SshCommandFactory.CommandLine;
import org.jenkinsci.plugins.remote_terminal_access.ProcessWithPtyLauncher;
import org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * SSH command that connects to the workspace of the job.
 *
 * @author Kohsuke Kawaguchi
 */
public class DiagnoseCommand extends AbstractRemoteSshCommand {
    private Thread thread;
    private AbstractBuild<?,?> build;

    public DiagnoseCommand(CommandLine cmdLine) {
        super(cmdLine);
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    @Override
    protected int main(List<String> args) throws IOException, InterruptedException {
        thread = Thread.currentThread();

        if (args.size()<1)
            throw new AbortException("No job name specified");

        String jobname = args.get(0);
        args = args.subList(1,args.size());

        build = resolveBuild(jobname);

        build.checkPermission(TerminalSessionAction.ACCESS);

        final TerminalSessionAction tsa = TerminalSessionAction.getFor(build);
        if (tsa!=null)  tsa.associate(this);
        try {
            return run(args);
        } finally {
            if (tsa!=null)  tsa.unassociate(this);
        }
    }

    @Override
    protected void configure(ProcessWithPtyLauncher pb) throws IOException, InterruptedException {
        pb.configure(build, new StreamTaskListener(getOutputStream()));
    }

    private AbstractBuild<?, ?> resolveBuild(String jobname) throws AbortException {
        String buildName = null;
        int idx = jobname.indexOf('#');
        if (idx>0) {
            buildName = jobname.substring(0,idx);
            jobname = jobname.substring(idx+1);
        }

        AbstractProject<?,?> p = Jenkins.getInstance().getItemByFullName(jobname,AbstractProject.class);
        if (p==null)
            throw new AbortException("No such job found: "+jobname);

        AbstractBuild<?,?> build = null;

        if (buildName==null) {
            build = p.getLastBuild();
            buildName = "lastBuild";
        } else {
            try {// number?
                int n = Integer.parseInt(buildName);
                build = p.getBuildByNumber(n);
            } catch (NumberFormatException _) {
                // permalink?
                Permalink link = p.getPermalinks().get(buildName);
                if (link!=null)
                    build = (AbstractBuild<?,?>)link.resolve(p);
            }
        }

        if (build==null)
            throw new AbortException("No such build found: "+jobname+" "+buildName);
        return build;
    }

    private static final Logger LOGGER = Logger.getLogger(DiagnoseCommand.class.getName());
}
