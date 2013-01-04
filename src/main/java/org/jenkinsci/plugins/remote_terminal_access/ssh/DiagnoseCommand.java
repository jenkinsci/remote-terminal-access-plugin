package org.jenkinsci.plugins.remote_terminal_access.ssh;

import hudson.util.StreamCopyThread;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.jenkinsci.main.modules.sshd.AsynchronousCommand;
import org.jenkinsci.main.modules.sshd.SshCommandFactory.CommandLine;
import org.kohsuke.ajaxterm.ProcessWithPty;
import org.kohsuke.ajaxterm.PtyProcessBuilder;

import java.io.IOException;
import java.io.OutputStream;
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
    protected int run() throws Exception {
        Environment env = getEnvironment();
        String w = env.getEnv().get(Environment.ENV_COLUMNS);
        String h = env.getEnv().get(Environment.ENV_LINES);
        String term = env.getEnv().get(Environment.ENV_TERM);
        if (w!=null && h!=null && term!=null) {
            // probably pty was requested. this is a somewhat weaker way of testing this
            // TODO: patch mina to remember handlePty call
            PtyProcessBuilder pb = new PtyProcessBuilder();
            List<String> cmds = getCmdLine().subList(1, getCmdLine().size());
            if (cmds.isEmpty())
                pb.commands("/bin/bash","-i");
            else
                pb.commands(cmds);
            pb.envs(env.getEnv()); // this include terminal
//            pb.pwd(new File(pwd.getRemote()));
            proc = pb.forkWithHelper();

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
        } else {
            String msg = "No tty. Please run ssh with the -t option";
            OutputStream err = getErrorStream();
            err.write(msg.getBytes());
            return 1;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DiagnoseCommand.class.getName());
}
