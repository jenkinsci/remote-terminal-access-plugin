package org.jenkinsci.plugins.remote_terminal_access.ssh;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Created a proxied interface whose methods are selectively overridden.
 *
 * @author Kohsuke Kawaguchi
 */
public class InterceptingProxy {
    public static <T> T create(Class<T> type, final T base, final Map<String,Object> overrides) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (overrides.containsKey(method.getName()))
                            return overrides.get(method.getName());

                        try {
                            return method.invoke(base, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    }
                }));
    }
}