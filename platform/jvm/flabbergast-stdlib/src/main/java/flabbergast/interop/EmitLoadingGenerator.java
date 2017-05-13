package flabbergast.interop;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.util.stream.Stream;

final class EmitLoadingGenerator extends EmitBaseGenerator {
  static final Definition DEFINITION =
      LookupAssistant.create(EmitLoadingGenerator::new, emitterLookups());

  @Override
  protected void finish(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      KwsRenderer renderer,
      ErrorCollector errorCollector)
      throws Exception {
    KwsRenderer.instantiate(
        renderer, result -> future.launch(result, future::complete), fileName, errorCollector);
  }

  @Override
  protected final Stream<Name> initials() {
    return Stream.empty();
  }
}
