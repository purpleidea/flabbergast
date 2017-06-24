package flabbergast;

public class MapFunctionInterop3<T1, T2, T3, R> extends BaseMapFunctionInterop<T1, R> {
  private Func3<T1, T2, T3, R> func;
  private String parameter1;
  private Class<T2> parameter1Clazz;
  private boolean parameter1Nullable;
  private String parameter2;
  private Class<T3> parameter2Clazz;
  private boolean parameter2Nullable;
  private T2 reference1;
  private T3 reference2;

  public MapFunctionInterop3(
      Class<R> returnClass,
      Class<T1> clazz,
      Func3<T1, T2, T3, R> func,
      Class<T2> parameter1Clazz,
      boolean parameter1Nullable,
      String parameter1,
      Class<T3> parameter2Clazz,
      boolean parameter2Nullable,
      String parameter2,
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(returnClass, clazz, task_master, source_reference, context, self, container);
    this.func = func;
    this.parameter1Clazz = parameter1Clazz;
    this.parameter2Clazz = parameter2Clazz;
    this.parameter1Nullable = parameter1Nullable;
    this.parameter2Nullable = parameter2Nullable;
    this.parameter1 = parameter1;
    this.parameter2 = parameter2;
  }

  @Override
  protected R computeResult(T1 input) throws Exception {
    return func.invoke(input, reference1, reference2);
  }

  @Override
  protected void setupExtra() {
    Sink<T2> reference1_lookup = find(parameter1Clazz, x -> this.reference1 = x);
    reference1_lookup.allowDefault(parameter1Nullable, null);
    reference1_lookup.lookup(parameter1);
    Sink<T3> reference2_lookup = find(parameter2Clazz, x -> this.reference2 = x);
    reference2_lookup.allowDefault(parameter2Nullable, null);
    reference2_lookup.lookup(parameter2);
  }
}
