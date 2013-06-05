package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.AbortException;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Queue.Executable;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.IOException2;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * @author Kohsuke Kawaguchi
 */
public class LeaseContext {
    /**
     * Currently active leases.
     */
    private final ConcurrentMap<String,QueueTaskFuture<?>> tasks = new ConcurrentHashMap<String,QueueTaskFuture<?>>();

    public final String id = Util.getDigestOf(UUID.randomUUID().toString()).substring(0,8);

    public LeaseContext() {
        LeaseContextMap.get().add(this);
    }

    public static LeaseContext getById(String id) {
        return LeaseContextMap.get().get(id);
    }

    public void add(final String alias, Label label, String displayName, long duration) {
        if (displayName==null)
            displayName = id+":"+alias;
        else
            displayName += " "+id+":"+alias;

        LeasedTask t = new LeasedTask(label,displayName,duration,new Runnable() {
            public void run() {
                try {
                    Object lock = LeaseContext.this;
                    synchronized (lock) {
                        while (tasks.containsKey(alias))
                            lock.wait();
                    }
                } catch (InterruptedException e) {
                    // this happens when we are done
                }
            }
        });
        QueueTaskFuture<Executable> f = (QueueTaskFuture<Executable>)Jenkins.getInstance().getQueue().schedule(t, 0).getFuture();
        if (tasks.putIfAbsent(alias, f)!=null) {
            f.cancel(true);
            throw new IllegalArgumentException("Alias "+alias+" is already in use");
        }
   }

    /**
     * Waits for all the active leases to get their executors assigned.
     */
    public void waitForStart() throws ExecutionException, InterruptedException {
        for (QueueTaskFuture<?> f :  tasks.values()) {
            f.waitForStart();
        }
    }

    /**
     * Gets the {@link Executor} that maps to the alias.
     */
    public Executor get(String alias) throws InterruptedException, IOException {
        try {
            QueueTaskFuture<?> t = tasks.get(alias);
            if (t==null)
                throw new AbortException("No such alias: "+alias);

            Executable work = t.waitForStart();

            for (Computer c : Jenkins.getInstance().getComputers()) {
                for (Executor e : c.getExecutors()) {
                    if (e.getCurrentExecutable()==work)
                        return e;
                }
            }
            return null;
        } catch (ExecutionException e) {
            throw new IOException2("Failed to locate the alias: "+alias,e);
        }
    }

    /**
     * Cancels all the in-progress tasks.
     */
    public synchronized void end() {
        for (QueueTaskFuture<?> f :  tasks.values()) {
            f.cancel(true);
        }
        tasks.clear();
        notifyAll();

        LeaseContextMap.get().remove(this);
    }

    /**
     * Removes a single alias.
     */
    public synchronized void remove(String alias) {
        QueueTaskFuture<?> v = tasks.remove(alias);
        if (v==null)    throw new IllegalArgumentException("Invalid alias: "+alias);

        v.cancel(true);
        notifyAll();
    }

    public Set<String> getAliases() {
        return tasks.keySet();
    }
}
