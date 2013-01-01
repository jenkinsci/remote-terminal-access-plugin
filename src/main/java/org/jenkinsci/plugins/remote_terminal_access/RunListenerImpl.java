package org.jenkinsci.plugins.remote_terminal_access;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.listeners.RunListener;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class RunListenerImpl extends RunListener<AbstractBuild> {
    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new TerminalSessionAction(build,launcher,listener);
    }
}
