package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;
import java.util.function.Consumer;
import java.util.stream.Stream;

class FilterLookupExplorer implements LookupExplorer {

  private final Template filter;
  private final Future<?> future;
  private final LookupExplorer innerExplorer;
  private final SourceReference sourceReference;

  FilterLookupExplorer(
      LookupExplorer innerExplorer,
      Future<?> future,
      SourceReference sourceReference,
      Template filter) {
    this.innerExplorer = innerExplorer;
    this.future = future;
    this.sourceReference = sourceReference;
    this.filter = filter;
  }

  @Override
  public LookupExplorer duplicate() {
    return new flabbergast.lang.FilterLookupExplorer(
        innerExplorer.duplicate(), future, sourceReference, filter);
  }

  @Override
  public void process(
      Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next) {
    innerExplorer.process(
        targetName,
        frame,
        seen,
        remaining,
        new LookupForkOperation() {
          @Override
          public void await(Promise<Any> promise, Consumer<Any> consumer) {
            next.await(promise, consumer);
          }

          @Override
          public void fail() {
            next.fail();
          }

          @Override
          public void finish(Any result) {
            Frame.create(
                    future,
                    sourceReference.specialJunction(
                        "instantiate template inside filter lookup handler", filter.source()),
                    Context.EMPTY,
                    false,
                    Stream.empty(),
                    AttributeSource.of(
                        Attribute.of("input", result), Attribute.of("name", targetName.any())),
                    filter)
                .get(Name.of("value"))
                .ifPresentOrElse(
                    promise ->
                        next.await(
                            promise,
                            r ->
                                r.accept(
                                    new WhinyAnyConsumer() {
                                      @Override
                                      public void accept(boolean value) {
                                        if (value) {
                                          next.finish(result);
                                        } else {
                                          next.next();
                                        }
                                      }

                                      @Override
                                      protected void fail(String type) {
                                        next.fail();
                                      }
                                    })),
                    next::fail);
          }

          @Override
          public void fork(Stream<Any> values) {
            next.fork(values);
          }

          @Override
          public void forkPromises(Stream<Promise<Any>> values) {
            next.forkPromises(values);
          }

          @Override
          public void next() {
            next.next();
          }
        });
  }
}
