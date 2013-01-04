package org.jenkinsci.plugins.remote_terminal_access.ssh;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.jenkinsci.main.modules.sshd.AsynchronousCommand;
import org.jenkinsci.main.modules.sshd.SshCommandFactory.CommandLine;
import org.jenkinsci.plugins.remote_terminal_access.ProcessWithPtyLauncher;
import org.kohsuke.ajaxterm.ProcessWithPty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class DiagnoseCommand extends AsynchronousCommand {
    private ProcessWithPty proc;

    public DiagnoseCommand(CommandLine cmdLine) {
        super(cmdLine);
    }

    public void start(final Environment env) throws IOException {
        env.addSignalListener(new SignalListener() {
            public void signal(Signal signal) {
                try {
                    if (signal==Signal.WINCH) {
                        int w = Integer.parseInt(env.getEnv().get(Environment.ENV_COLUMNS));
                        int h = Integer.parseInt(env.getEnv().get(Environment.ENV_LINES));
                        proc.setWindowSize(w,h);
                    } else
                        proc.kill(signal.getNumeric());
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to send signal to " + proc, e);
                }
            }
        });
        super.start(env);
    }

    @Override
    protected int run() {
        try {
            List<String> args = getCmdLine().subList(1, getCmdLine().size());
            if (args.size()<1)
                return die("No job name specified");

            String jobname = args.get(0);
            args = args.subList(1,args.size());

            AbstractProject<?,?> p = Jenkins.getInstance().getItemByFullName(jobname,AbstractProject.class);
            if (p==null)
                return die("No such job found: "+jobname);

            // TODO: support the build number or permalink to be specified as an option
            AbstractBuild<?,?> build = p.getLastBuild();
            if (build==null)
                return die("No build found for job: "+jobname);

            Environment env = getEnvironment();
            String w = env.getEnv().get(Environment.ENV_COLUMNS);
            String h = env.getEnv().get(Environment.ENV_LINES);
            String term = env.getEnv().get(Environment.ENV_TERM);
            if (w == null || h == null || term == null)
                return die("No tty. Please run ssh with the -t option");

            // probably pty was requested. this is a somewhat weaker way of testing this
            // TODO: patch mina to remember handlePty call

            ProcessWithPtyLauncher pb = new ProcessWithPtyLauncher();
            if (args.isEmpty())
                pb.shell();
            else
                pb.commands(args);
            pb.envs(env.getEnv());

            proc = pb.launch(build, new StreamTaskListener(getOutputStream()), term);
            proc.setWindowSize(Integer.parseInt(w),Integer.parseInt(h));

            new FlushStreamCopyThread(getCmdLine()+" stdout pump",proc.getInputStream(),getOutputStream(),true).start();
            new FlushStreamCopyThread(getCmdLine()+" stdin pump", getInputStream(),proc.getOutputStream(),true).start();

            try {
                int exit = proc.waitFor();
                proc = null;
                return exit;
            } finally {
                try {// on abnormal termination, kill the process
                    if (proc!=null) {
                        proc.kill(9);
                        proc.destroy();
                    }
                    proc = null;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to send signal to " + proc, e);
                }
            }
        } catch (Exception e) {// this catch block becomes redundant with sshd-module 1.5
            PrintStream out = new PrintStream(getErrorStream());
            e.printStackTrace(out);
            out.flush();
            return 1;
        }
    }

    private int die(String msg) throws IOException {
        OutputStream err = getErrorStream();
        err.write(msg.getBytes());
        return 1;
    }

    private static final Logger LOGGER = Logger.getLogger(DiagnoseCommand.class.getName());
}
