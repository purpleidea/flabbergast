package flabbergast;

public class StringToCodepoints extends BaseMapFunctionInterop<String, Frame> {
  public StringToCodepoints(
      TaskMaster task_master,
      SourceReference source_ref,
      Context context,
      Frame self,
      Frame container) {
    super(Frame.class, String.class, task_master, source_ref, context, self, container);
  }

  @Override
  protected Frame computeResult(String input) throws Exception {
    MutableFrame frame =
        new MutableFrame(
            task_master, source_reference,
            context, container);
    for (int it = 0; it < input.length(); it++) {
      frame.set(SupportFunctions.ordinalNameStr(it + 1), (long) input.codePointAt(it));
    }
    return frame;
  }
}
