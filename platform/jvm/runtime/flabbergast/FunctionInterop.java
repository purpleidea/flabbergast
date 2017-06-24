package flabbergast;

public class FunctionInterop<T, R> extends BaseFunctionInterop<R> {
  private final Class<T> clazz;
  private final Func<T, R> func;
  private T input;
  private final String parameter;
  private final boolean parameterNullable;

  public FunctionInterop(
      Class<R> returnClass,
      Func<T, R> func,
      Class<T> clazz,
      boolean parameterNullable,
      String parameter,
      TaskMaster task_master,
      SourceReference source_reference,
      Context context,
      Frame self,
      Frame container) {
    super(task_master, source_reference, context, self, container);
    this.func = func;
    this.clazz = clazz;
    this.parameterNullable = parameterNullable;
    this.parameter = parameter;
  }

  @Override
  protected R computeResult() throws Exception {
    return func.invoke(input);
  }

  @Override
  protected void setup() {
    Sink<T> reference_lookup = find(clazz, x -> this.input = x);
    reference_lookup.allowDefault(parameterNullable, null);
    reference_lookup.lookup(parameter);
  }
}
