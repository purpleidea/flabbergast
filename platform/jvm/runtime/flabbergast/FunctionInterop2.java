package flabbergast;

public class FunctionInterop2<T1, T2, R> extends BaseFunctionInterop<R> {
    private final Class<T1> clazz1;
    private final String parameter1;
    private final Class<T2> clazz2;
    private final String parameter2;
    private final Func2<T1, T2, R> func;
    private T1 input1;
    private T2 input2;

    public FunctionInterop2(Class<R> returnClass, Func2<T1, T2, R> func, Class<T1> clazz1, String parameter1, Class<T2> clazz2, String parameter2, TaskMaster task_master, SourceReference source_ref,
                            Context context, Frame self, Frame container) {
        super(returnClass, task_master, source_ref, context, self, container);
        this.func = func;
        this.clazz1 = clazz1;
        this.parameter1 = parameter1;
        this.clazz2 = clazz2;
        this.parameter2 = parameter2;
    }
    @Override
    protected R computeResult() throws Exception {
        return  func.invoke(input1, input2);
    }


    @Override
    protected  void prepareLookup(InterlockedLookup interlock) {
        interlock.lookup(clazz1, x -> this.input1 = x, parameter1);
        interlock.lookup(clazz2, x -> this.input2 = x, parameter2);
    }
}

