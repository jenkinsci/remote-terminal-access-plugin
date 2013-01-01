package org.jenkinsci.plugins.remote_terminal_access;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.EnvironmentList;
import hudson.model.Run;
import hudson.model.TransientBuildActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class TransientBuildActionFactoryImpl extends TransientBuildActionFactory {
    @Override
    public Collection<? extends Action> createFor(Run target) {
        if (target instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) target;
            EnvironmentList e = build.getEnvironments();
            if (e!=null) {
                TerminalSessionAction o = e.get(TerminalSessionAction.class);
                if (o!=null)
                    return Collections.singleton(o);
            }
        }
        return Collections.emptyList();
    }
}
