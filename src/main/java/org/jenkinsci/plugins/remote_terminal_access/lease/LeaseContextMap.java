package org.jenkinsci.plugins.remote_terminal_access.lease;

import hudson.Extension;
import jenkins.model.Jenkins;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that remembers active lease contexts.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class LeaseContextMap {
    private final Map<String,LeaseContext> contexts = new ConcurrentHashMap<String, LeaseContext>();

    void add(LeaseContext context) {
        contexts.put(context.id,context);
    }

    void remove(LeaseContext context) {
        contexts.remove(context);
    }

    LeaseContext get(String id) {
        return contexts.get(id);
    }

    static LeaseContextMap get() {
        return Jenkins.getInstance().getExtensionList(LeaseContextMap.class).get(LeaseContextMap.class);
    }
}
