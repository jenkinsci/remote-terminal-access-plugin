package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.Util;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Queue.Executable;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * @author Kohsuke Kawaguchi
 */
public class LeaseContext {
    /*
        How to maintain context
            UUID -> LeaseContext?
                WeakReference doesn't buy us anything because it's the executors that get blocked

     */
    /**
     * Currently active leases.
     */
    private final ConcurrentMap<String,QueueTaskFuture<?>> tasks = new ConcurrentHashMap<String,QueueTaskFuture<?>>();

    public final String id = Util.getDigestOf(UUID.randomUUID().toString()).substring(0,8);

    /**
     * Set to true if this lease context has ended.
     */
    private boolean done;

    public LeaseContext() {
        LeaseContextMap.get().add(this);
    }

    public static LeaseContext getById(String id) {
        return LeaseContextMap.get().get(id);
    }

    public void add(String alias, Label label, String displayName, long duration) {
        LeasedTask t = new LeasedTask(label,displayName,duration,new Runnable() {
            public void run() {
                try {
                    Object lock = LeaseContext.this;
                    synchronized (lock) {
                        while (!done)
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
    public Executor get(String alias) throws ExecutionException, InterruptedException {
        QueueTaskFuture<?> t = tasks.get(alias);
        Executable work = t.waitForStart();

        for (Computer c : Jenkins.getInstance().getComputers()) {
            for (Executor e : c.getExecutors()) {
                if (e.getCurrentExecutable()==work)
                    return e;
            }
        }
        return null;
    }

    /**
     * Cancels all the in-progress tasks.
     */
    public synchronized void end() {
        // if there's anyone already execcuting, abort them
        done = true;
        notifyAll();

        for (QueueTaskFuture<?> f :  tasks.values()) {
            f.cancel(true);
        }
        LeaseContextMap.get().remove(this);
    }
}
