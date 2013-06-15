import hudson.cli.CLI;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.FastPipedInputStream;
import hudson.remoting.FastPipedOutputStream;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) throws Exception {
        CLI cli = new CLI(new URL("http://localhost:8080/jenkins"));

        final FastPipedInputStream p1i = new FastPipedInputStream();
        final FastPipedOutputStream p2o = new FastPipedOutputStream();

        FastPipedInputStream p2i = new FastPipedInputStream(p2o);
        FastPipedOutputStream p1o = new FastPipedOutputStream(p1i);

        new Thread() {
            @Override
            public void run() {
                try {
                    Channel ch = new Channel("cli", Executors.newCachedThreadPool(), p1i, p2o);
                    // TODO: use the equivalent of StandardOutputSwapper in core to prevent System.out.println() in the target VM from destroying the channel
                    ch.call(new TestCallable());
                    ch.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        int r = cli.execute(Arrays.asList("channel-process", "master"), p2i, p1o, System.err);
        System.exit(r);
    }

    private static class TestCallable implements Callable<Void, IOException> {
        public Void call() throws IOException {
            System.err.println("Hello!");
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private static final long serialVersionUID = 1L;
    }
}
