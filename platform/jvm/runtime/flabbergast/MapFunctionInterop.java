package flabbergast;

public class MapFunctionInterop<T, R> extends BaseMapFunctionInterop<T, R> {
    private final Func<T, R> func;

    public MapFunctionInterop(Class<R> returnClass, Class<T> clazz, Func<T, R> func, TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) {
        super(returnClass, clazz, task_master, source_ref, context, self, container) ;
        this.func = func;
    }

    @Override
    protected  R computeResult(T input) throws Exception {
        return func.invoke(input);
    }
}

