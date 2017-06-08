package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

import java.util.HashMap;
import java.util.Map;

public abstract class Interop implements UriHandler {
    private static final Frame NOTHING = new FixedFrame("interop", new NativeSourceReference("interop"));

    private Map<String, Future> bindings = new HashMap<>();

    protected void add(String name, ComputeValue compute) {
        Template tmpl = new Template(NOTHING.getSourceReference(), null, NOTHING);
        tmpl.set("value", compute);
        bindings.put(name, new Precomputation(tmpl));
    }

    protected <T, R> void add(Class<R> returnClass, Class<T> clazz, String name, Func<T, R> func, String parameter) {
        add(name,
            (task_master, source_ref, context, self, container) -> new FunctionInterop<>(returnClass, func, clazz, parameter, task_master, source_ref, context, self, container));
    }

    protected <T1, T2 , R>void add(Class<R> returnClass, String name, Func2<T1, T2, R> func, Class<T1> clazz1, String parameter1, Class<T2> clazz2, String parameter2) {
        add(name,
            (task_master, source_ref, context, self, container) -> new FunctionInterop2<>(returnClass, func, clazz1, parameter1, clazz2, parameter2, task_master, source_ref, context, self, container));
    }

    protected <T, R>void addMap(Class<R> returnClass, Class<T> clazz, String name, Func<T, R> func) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop<>(returnClass, clazz, func, task_master, source_ref, context, self, container));
    }

    protected <T1, T2, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func2<T1, T2, R> func, Class<T2> parameterClass, String parameter) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop2<>(returnClass, clazz, func, parameterClass, parameter, task_master, source_ref, context, self, container));
    }

    @Override
    public int getPriority() {
        return 0;
    }
    @Override
    public String getUriName() {
        return "library bindings";
    }

    @Override
    public Future resolveUri(TaskMaster master, String uri, Ptr<LibraryFailure> reason) {
        if (!uri.startsWith("interop:"))
            return null;
        if (bindings.containsKey(uri.substring(8))) {
            reason.set(null);
            return bindings.get(uri.substring(8));
        }
        return null;
    }
}

