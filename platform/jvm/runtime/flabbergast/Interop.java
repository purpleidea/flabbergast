package flabbergast;

import flabbergast.TaskMaster.LibraryFailure;

import java.util.HashMap;
import java.util.Map;

public abstract class Interop implements UriHandler {
    private static final Frame NOTHING = new FixedFrame("interop", new NativeSourceReference("interop"));

    private Map<String, Future> bindings = new HashMap<>();

    protected void add(String name, Frame frame) {
        bindings.put(name, new Precomputation(frame));
    }

    protected void add(String name, ComputeValue compute) {
        Template tmpl = new Template(NOTHING.getSourceReference(), null, NOTHING);
        tmpl.set("value", compute);
        bindings.put(name, new Precomputation(tmpl));
    }

    protected <T, R> void add(Class<R> returnClass, String name, Func<T, R> func, Class<T> clazz, boolean nullable, String parameter) {
        add(name,
            (task_master, source_ref, context, self, container) -> new FunctionInterop<>(returnClass, func, clazz, nullable, parameter, task_master, source_ref, context, self, container));
    }

    protected <T, R> void add(Class<R> returnClass, Class<T> clazz, String name, Func<T, R> func, String parameter) {
        add(returnClass, name, func, clazz, false, parameter);
    }

    protected <T1, T2 , R>void add(Class<R> returnClass, String name, Func2<T1, T2, R> func, Class<T1> clazz1, boolean parameter1Nullable, String parameter1, Class<T2> clazz2, boolean parameter2Nullable, String parameter2) {
        add(name,
            (task_master, source_ref, context, self, container) -> new FunctionInterop2<>(returnClass, func, clazz1, parameter1Nullable, parameter1, clazz2, parameter2Nullable,  parameter2, task_master, source_ref, context, self, container));
    }

    protected <T1, T2 , R>void add(Class<R> returnClass, String name, Func2<T1, T2, R> func, Class<T1> clazz1, String parameter1, Class<T2> clazz2, String parameter2) {
        add(returnClass, name,  func, clazz1, false, parameter1, clazz2, false, parameter2);
    }

    protected <T, R>void addMap(Class<R> returnClass, Class<T> clazz, String name, Func<T, R> func) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop<>(returnClass, clazz, func, task_master, source_ref, context, self, container));
    }

    protected <T1, T2, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func2<T1, T2, R> func, Class<T2> parameterClass, boolean parameterNullable, String parameter) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop2<>(returnClass, clazz, func, parameterClass, parameterNullable, parameter, task_master, source_ref, context, self, container));
    }

    protected <T1, T2, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func2<T1, T2, R> func, Class<T2> parameterClass, String parameter) {
        addMap(returnClass, clazz, name,  func,  parameterClass, false, parameter);
    }

    protected <T1, T2, T3, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func3<T1, T2, T3, R> func, Class<T2> parameter1Class, boolean parameter1Nullable,  String parameter1, Class<T3> parameter2Class, boolean parameter2Nullable, String parameter2) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop3<>(returnClass, clazz, func, parameter1Class, parameter1Nullable, parameter1, parameter2Class, parameter2Nullable, parameter2, task_master, source_ref, context, self, container));
    }
    protected <T1, T2, T3, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func3<T1, T2, T3, R> func, Class<T2> parameter1Class, String parameter1, Class<T3> parameter2Class, String parameter2) {
        addMap(returnClass, clazz, name,  func,  parameter1Class, false, parameter1,  parameter2Class, false, parameter2);
    }

    protected <T1, T2, T3, T4, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func4<T1, T2, T3, T4, R> func, Class<T2> parameter1Class, boolean parameter1Nullable, String parameter1, Class<T3> parameter2Class, boolean parameter2Nullable, String parameter2, Class<T4> parameter3Class, boolean parameter3Nullable, String parameter3) {
        add(name,
            (task_master, source_ref, context, self, container) -> new MapFunctionInterop4<>(returnClass, clazz, func, parameter1Class, parameter1Nullable, parameter1, parameter2Class, parameter2Nullable, parameter2, parameter3Class, parameter3Nullable, parameter3, task_master, source_ref, context, self, container));
    }


    protected <T1, T2, T3, T4, R>void addMap(Class<R> returnClass, Class<T1> clazz, String name, Func4<T1, T2, T3, T4, R> func, Class<T2> parameter1Class, String parameter1, Class<T3> parameter2Class, String parameter2, Class<T4> parameter3Class, String parameter3) {
        addMap(returnClass, clazz, name,  func,  parameter1Class, false, parameter1, parameter2Class, false, parameter2, parameter3Class, false, parameter3);
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

