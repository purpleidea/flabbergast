package flabbergast;

public class MapFunctionInterop3<T1, T2, T3, R> extends BaseMapFunctionInterop<T1, R> {
    private Func3<T1, T2, T3, R> func;
    private Class<T2> parameter1Clazz;
    private Class<T3> parameter2Clazz;
    private String parameter1;
    private String parameter2;
    private T2 reference1;
    private T3 reference2;

    public MapFunctionInterop3(Class<R> returnClass, Class<T1> clazz, Func3<T1, T2, T3, R> func, Class<T2> parameter1Clazz, String parameter1, Class<T3> parameter2Clazz, String parameter2, TaskMaster task_master, SourceReference source_ref,
                               Context context, Frame self, Frame container) {
        super(returnClass, clazz , task_master, source_ref, context, self, container);
        this.func = func;
        this.parameter1Clazz = parameter1Clazz;
        this.parameter2Clazz = parameter2Clazz;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }
    @Override
    protected R computeResult(T1 input) throws Exception {
        return  func.invoke(input, reference1, reference2);
    }
    @Override
    protected  void prepareLookup(InterlockedLookup interlock) {
        interlock.lookup(parameter1Clazz , x -> this.reference1 = x, parameter1);
        interlock.lookup(parameter2Clazz , x -> this.reference2 = x, parameter2);
    }
}

