package flabbergast.interop;

import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.*;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

final class EmitDefineFunction extends EmitBaseFunction {

  public static Definition definition(EmitFunctionType type) {
    return LookupAssistant.create(
        () -> new EmitDefineFunction(type),
        Stream.concat(
            lookups(),
            Stream.of(
                LookupAssistant.find(
                    AnyConverter.frameOf(EmitBaseGenerator.KWS_TYPE, false),
                    (i, c) -> i.captures = c,
                    "captures"))));
  }

  private Map<Name, KwsType> captures;
  private final EmitFunctionType type;

  EmitDefineFunction(EmitFunctionType type) {
    this.type = type;
  }

  public <
          V,
          B extends KwsBlock<V, B, D>,
          D extends KwsDispatch<B, V>,
          F extends KwsFunction<V, B, D>>
      F create(KwsFactory<V, B, D, F> factory, Name name, BiConsumer<Name, B> defineBlock) {
    final var function =
        type.create(
            factory,
            new SourceLocation("<emit location>", 0, 0, 0, 0),
            name.apply(
                new NameFunction<>() {
                  @Override
                  public String apply(long ordinal) {
                    return Long.toString(ordinal);
                  }

                  @Override
                  public String apply(String name) {
                    return name;
                  }
                }),
            captures.values().stream());
    createBlocks(function, defineBlock);
    return function;
  }

  @Override
  protected Stream<Name> initials() {
    return Stream.concat(captures.keySet().stream(), type.entryNames());
  }

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
  }
}
