package flabbergast;

public class FunctionInterop2<T1, T2, R> extends BaseFunctionInterop<R> {
    private final Class<T1> clazz1;
    private final String parameter1;
    private final boolean parameter1Nullable;
    private final Class<T2> clazz2;
    private final String parameter2;
    private final boolean parameter2Nullable;
    private final Func2<T1, T2, R> func;
    private T1 input1;
    private T2 input2;

    public FunctionInterop2(Class<R> returnClass, Func2<T1, T2, R> func, Class<T1> clazz1, boolean parameter1Nullable, String parameter1, Class<T2> clazz2, boolean parameter2Nullable, String parameter2, TaskMaster task_master, SourceReference source_reference,
                            Context context, Frame self, Frame container) {
        super(task_master, source_reference, context, self, container);
        this.func = func;
        this.clazz1 = clazz1;
        this.parameter1 = parameter1;
        this.parameter1Nullable = parameter1Nullable;
        this.clazz2 = clazz2;
        this.parameter2 = parameter2;
        this.parameter2Nullable = parameter2Nullable;
    }
    @Override
    protected R computeResult() throws Exception {
        return  func.invoke(input1, input2);
    }


    @Override
    protected  void setup() {
        Sink<T1> input1_lookup = find(clazz1, x -> this.input1 = x);
        input1_lookup.allowDefault(parameter1Nullable, null);
        input1_lookup.lookup(parameter1);
        Sink<T2> input2_lookup = find(clazz2, x -> this.input2 = x);
        input2_lookup.allowDefault(parameter2Nullable, null);
        input2_lookup.lookup(parameter2);
    }
}

