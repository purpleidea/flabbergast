package flabbergast.interop;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.kws.KwsRenderer;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.util.stream.Stream;

final class EmitLoadingDefinitionGenerator extends EmitBaseGenerator {
  static final Definition DEFINITION =
      LookupAssistant.create(
          EmitLoadingDefinitionGenerator::new,
          Stream.concat(
              emitterLookups(),
              Stream.of(
                  LookupAssistant.find(AnyConverter.asName(false), (i, n) -> i.name = n, "name"))));
  private Name name;

  @Override
  protected final Stream<Name> initials() {
    return Stream.of(Name.of("context"));
  }

  @Override
  protected void finish(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      KwsRenderer renderer,
      ErrorCollector errorCollector)
      throws Exception {
    KwsRenderer.instantiateDefinition(
        renderer,
        result ->
            future.complete(
                Any.of(
                    new Template(
                        SourceReference.EMPTY.basic(
                            "emit generated template", fileName, 0, 0, 0, 0),
                        Context.EMPTY,
                        AttributeSource.of(Attribute.of(name, result))))),
        fileName,
        errorCollector);
  }
}
