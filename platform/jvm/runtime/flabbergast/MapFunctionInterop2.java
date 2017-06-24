package flabbergast;

public class MapFunctionInterop2<T1, T2, R> extends BaseMapFunctionInterop<T1, R> {
  private final Func2<T1, T2, R> func;
  private final String parameter;
  private final Class<T2> parameterClazz;
  private final boolean parameterNullable;
  private T2 reference;

  public MapFunctionInterop2(
      Class<R> returnClass,
      Class<T1> clazz,
      Func2<T1, T2, R> func,
      Class<T2> parameterClazz,
      boolean parameterNullable,
      String parameter,
      TaskMaster task_master,
      SourceReference source_ref,
      Context context,
      Frame self,
      Frame container) {
    super(returnClass, clazz, task_master, source_ref, context, self, container);
    this.func = func;
    this.parameterClazz = parameterClazz;
    this.parameterNullable = parameterNullable;
    this.parameter = parameter;
  }

  @Override
  protected R computeResult(T1 input) throws Exception {
    return func.invoke(input, reference);
  }

  @Override
  protected void setupExtra() {
    Sink<T2> reference_lookup = find(parameterClazz, x -> this.reference = x);
    reference_lookup.allowDefault(parameterNullable, null);
    reference_lookup.lookup(parameter);
  }
}
