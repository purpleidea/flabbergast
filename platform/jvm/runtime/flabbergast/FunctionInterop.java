package flabbergast;

public class FunctionInterop<T, R> extends BaseFunctionInterop<R> {
    private final Class<T> clazz;
    private final String parameter;
    private final Func<T, R> func;
    private T input;

    public FunctionInterop(Class<R> returnClass, Func<T, R> func, Class<T> clazz, String parameter, TaskMaster task_master, SourceReference source_ref,
                           Context context, Frame self, Frame container)  {
        super(returnClass, task_master, source_ref, context, self, container);
        this.func = func;
        this.clazz = clazz;
        this.parameter = parameter;
    }
    @Override
    protected  R computeResult() throws Exception {
        return func.invoke(input);
    }
    @Override
    protected void prepareLookup(InterlockedLookup interlock) {
        interlock.lookup(clazz, x -> this.input = x, parameter);
    }
}

