package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Executable;
import hudson.model.Queue.Task;
import hudson.model.ResourceList;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.SubTask;
import hudson.security.ACL;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * TODO: share with distfork
 *
 * @author Kohsuke Kawaguchi
 */
public class LeasedTask implements Queue.TransientTask {
    private final Label label;
    private final String displayName;
    private final long estimatedDuration;
    private final Runnable runnable;

    public LeasedTask(Label label, String displayName, long estimatedDuration, Runnable runnable) {
        this.label = label;
        this.displayName = displayName;
        this.estimatedDuration = estimatedDuration;
        this.runnable = runnable;
    }

    public Label getAssignedLabel() {
        return label;
    }

    public Node getLastBuiltOn() {
        return null;
    }

    public boolean isBuildBlocked() {
        return false;
    }

    public String getWhyBlocked() {
        return null;
    }

    public String getName() {
        return getDisplayName();
    }

    public String getFullDisplayName() {
        return getDisplayName();
    }

    public long getEstimatedDuration() {
        return estimatedDuration;
    }

    public Executable createExecutable() throws IOException {
        return new Executable() {
            public SubTask getParent() {
                return LeasedTask.this;
            }

            public void run() {
                runnable.run();
            }

            public long getEstimatedDuration() {
                return -1;
            }

            @Override
            public String toString() {
                return displayName;
            }
        };
    }

    public void checkAbortPermission() {
        getACL().checkPermission(AbstractProject.ABORT);
    }

    public boolean hasAbortPermission() {
        return getACL().hasPermission(AbstractProject.ABORT);
    }

    private ACL getACL() {
        return Hudson.getInstance().getACL();
    }

    public String getUrl() {
        // TODO: what to show?
        return null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceList getResourceList() {
        return new ResourceList();
    }

    public CauseOfBlockage getCauseOfBlockage() {
        // not blocked at any time
        return null;
    }

    public boolean isConcurrentBuild() {
        // concurrently buildable
        return true;
    }

    public Collection<? extends SubTask> getSubTasks() {
        return Collections.singleton(this);
    }

    public Task getOwnerTask() {
        return this;
    }

    public Object getSameNodeConstraint() {
        return null;
    }
}