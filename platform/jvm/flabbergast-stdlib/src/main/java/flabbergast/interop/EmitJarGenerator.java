package flabbergast.interop;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.time.Instant;
import java.util.stream.Stream;

final class EmitJarGenerator extends EmitBaseGenerator {
  static final Definition DEFINITION =
      LookupAssistant.create(
          EmitJarGenerator::new,
          Stream.concat(
              emitterLookups(),
              Stream.of(
                  LookupAssistant.find(
                      AnyConverter.asString(false), (i, v) -> i.libraryName = v, "library_name"),
                  LookupAssistant.find(
                      AnyConverter.asDateTime(false),
                      (i, v) -> i.libraryModificationTime = v.toInstant(),
                      "library_modification_time"))));
  private Instant libraryModificationTime;
  private String libraryName;

  @Override
  protected void finish(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      KwsRenderer renderer,
      ErrorCollector errorCollector)
      throws Exception {
    future.complete(
        Any.of(
            KwsRenderer.generateJar(
                renderer, libraryName, libraryModificationTime, fileName, errorCollector)));
  }

  @Override
  protected final Stream<Name> initials() {
    return Stream.empty();
  }
}
