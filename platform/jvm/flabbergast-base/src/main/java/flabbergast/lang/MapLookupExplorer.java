package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;
import java.util.function.Consumer;
import java.util.stream.Stream;

class MapLookupExplorer implements LookupExplorer {

  private final Future<?> future;
  private final LookupExplorer innerExplorer;
  private final Template mapper;
  private final SourceReference sourceReference;

  MapLookupExplorer(
      LookupExplorer innerExplorer,
      Future<?> future,
      SourceReference sourceReference,
      Template mapper) {
    this.innerExplorer = innerExplorer;
    this.future = future;
    this.sourceReference = sourceReference;
    this.mapper = mapper;
  }

  @Override
  public LookupExplorer duplicate() {
    return new flabbergast.lang.MapLookupExplorer(
        innerExplorer.duplicate(), future, sourceReference, mapper);
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
                        "instantiate template inside map lookup handler", mapper.source()),
                    Context.EMPTY,
                    false,
                    Stream.empty(),
                    AttributeSource.of(
                        Attribute.of("input", result), Attribute.of("name", targetName.any())),
                    mapper)
                .get(Name.of("value"))
                .ifPresentOrElse(promise -> next.await(promise, next::finish), next::fail);
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
