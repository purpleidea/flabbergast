package flabbergast.interop;

import flabbergast.lang.*;
import flabbergast.lang.Context.FrameAccessor;

final class CustomLookupExplorer implements LookupOperation<LookupExplorer> {

  private static class CustomExplorer implements LookupExplorer {

    private final Context context;
    private Template currentTemplate;
    private final Future<?> future;
    private final SourceReference sourceReference;

    public CustomExplorer(
        Future<?> future,
        SourceReference sourceReference,
        Context context,
        Template currentTemplate) {
      this.future = future;
      this.sourceReference = sourceReference;
      this.context = context;
      this.currentTemplate = currentTemplate;
    }

    @Override
    public LookupExplorer duplicate() {
      return new CustomExplorer(future, sourceReference, context, currentTemplate);
    }

    @Override
    public void process(
        Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next) {
      Frame.create(
              future,
              sourceReference,
              context,
              currentTemplate,
              AttributeSource.of(
                  Attribute.of("name", targetName.any()),
                  Attribute.of("seen", Any.of(seen)),
                  Attribute.of("remaining", Any.of(remaining))))
          .get(Name.of("value"))
          .ifPresentOrElse(
              promise ->
                  next.await(
                      promise,
                      any ->
                          any.accept(
                              LookupAction.CONVERTER.asConsumer(
                                  future,
                                  sourceReference,
                                  TypeErrorLocation.lookup(targetName),
                                  action ->
                                      action.performForked(
                                          future,
                                          sourceReference,
                                          frame,
                                          next,
                                          t -> currentTemplate = t)))),
              next::fail);
    }
  }

  private final Template explorer;

  public CustomLookupExplorer(Template explorer) {
    this.explorer = explorer;
  }

  @Override
  public String description() {
    return "custom";
  }

  @Override
  public LookupExplorer start(Future<?> future, SourceReference sourceReference, Context context) {
    return new CustomExplorer(future, sourceReference, context, explorer);
  }
}
