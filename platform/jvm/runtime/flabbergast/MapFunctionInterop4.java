package flabbergast;

public class MapFunctionInterop4<T1, T2, T3, T4, R> extends BaseMapFunctionInterop<T1, R> {
  private Func4<T1, T2, T3, T4, R> func;
  private String parameter1;
  private Class<T2> parameter1Clazz;
  private boolean parameter1Nullable;
  private String parameter2;
  private Class<T3> parameter2Clazz;
  private boolean parameter2Nullable;
  private String parameter3;
  private Class<T4> parameter3Clazz;
  private boolean parameter3Nullable;
  private T2 reference1;
  private T3 reference2;
  private T4 reference3;

  public MapFunctionInterop4(
      Class<R> returnClass,
      Class<T1> clazz,
      Func4<T1, T2, T3, T4, R> func,
      Class<T2> parameter1Clazz,
      boolean parameter1Nullable,
      String parameter1,
      Class<T3> parameter2Clazz,
      boolean parameter2Nullable,
      String parameter2,
      Class<T4> parameter3Clazz,
      boolean parameter3Nullable,
      String parameter3,
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(returnClass, clazz, task_master, source_reference, context, self, container);
    this.func = func;
    this.parameter1Clazz = parameter1Clazz;
    this.parameter2Clazz = parameter2Clazz;
    this.parameter3Clazz = parameter3Clazz;
    this.parameter1Nullable = parameter1Nullable;
    this.parameter2Nullable = parameter2Nullable;
    this.parameter3Nullable = parameter3Nullable;
    this.parameter1 = parameter1;
    this.parameter2 = parameter2;
    this.parameter3 = parameter3;
  }

  @Override
  protected R computeResult(T1 input) throws Exception {
    return func.invoke(input, reference1, reference2, reference3);
  }

  @Override
  protected void setupExtra() {
    Sink<T2> reference1_lookup = find(parameter1Clazz, x -> this.reference1 = x);
    reference1_lookup.allowDefault(parameter1Nullable, null);
    reference1_lookup.lookup(parameter1);
    Sink<T3> reference2_lookup = find(parameter2Clazz, x -> this.reference2 = x);
    reference2_lookup.allowDefault(parameter2Nullable, null);
    reference2_lookup.lookup(parameter2);
    Sink<T4> reference3_lookup = find(parameter3Clazz, x -> this.reference3 = x);
    reference3_lookup.allowDefault(parameter3Nullable, null);
    reference3_lookup.lookup(parameter3);
  }
}
