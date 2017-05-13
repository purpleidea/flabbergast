package flabbergast.interop;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsDispatch;
import flabbergast.compiler.kws.KwsFunction;
import flabbergast.compiler.kws.KwsType;
import flabbergast.export.LookupAssistant;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

abstract class EmitBaseFunction implements LookupAssistant.Recipient {

  private static Attribute capture(
      SourceReference sourceReference, Context context, Name functionName, Name capture) {
    return Attribute.of(
        capture,
        Any.of(
            Frame.proxyOf(
                sourceReference,
                context,
                new EmitExpression() {
                  @Override
                  public <
                          F extends KwsFunction<V, B, D>,
                          V,
                          B extends KwsBlock<V, B, D>,
                          D extends KwsDispatch<B, V>>
                      V render(
                          B block,
                          Function<Name, F> functions,
                          Map<Pair<Name, Name>, V> captures,
                          Map<BlockParameter, V> parameters,
                          Map<Integer, V> values) {
                    return captures.get(Pair.of(functionName, capture));
                  }
                },
                Stream.empty())));
  }

  static <T extends EmitBaseFunction> Stream<LookupAssistant<T>> lookups() {
    return Stream.of(
        LookupAssistant.find(
            AnyConverter.asTemplate(false), EmitBaseFunction::setEntryBlock, "body"),
        LookupAssistant.find(
            AnyConverter.frameOf(
                AnyConverter.asPair(
                    AnyConverter.asTemplate(false),
                    "body",
                    AnyConverter.frameOf(EmitBaseGenerator.KWS_TYPE, false),
                    "parameters"),
                false),
            EmitBaseFunction::setBlocks,
            "blocks"));
  }

  private static Attribute parameter(
      SourceReference sourceReference,
      Context context,
      Name functionName,
      Name blockName,
      Name parameter) {
    return Attribute.of(
        parameter,
        Any.of(
            Frame.proxyOf(
                sourceReference,
                context,
                new EmitExpression() {
                  @Override
                  public <
                          F extends KwsFunction<V, B, D>,
                          V,
                          B extends KwsBlock<V, B, D>,
                          D extends KwsDispatch<B, V>>
                      V render(
                          B block,
                          Function<Name, F> functions,
                          Map<Pair<Name, Name>, V> captures,
                          Map<BlockParameter, V> parameters,
                          Map<Integer, V> values) {
                    return parameters.get(
                        new EmitExpression.BlockParameter(functionName, blockName, parameter));
                  }
                },
                Stream.empty())));
  }

  private Map<Name, Pair<Template, Map<Name, KwsType>>> blocks;
  private Template entryBlock;

  public final void blocks(
      Name functionName,
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Consumer<EmitBlock> consumer,
      Runnable complete) {
    final var interlock = new AtomicInteger(blocks.size() + 1);
    final var captureDefinitions =
        initials()
            .map(capture -> capture(sourceReference, context, functionName, capture))
            .collect(toSource());

    final var entryFrame =
        Frame.create(future, sourceReference, context, entryBlock, captureDefinitions);
    EmitBaseGenerator.BLOCK_NAME_SOURCE.collect(
        future,
        sourceReference,
        Context.EMPTY.prepend(entryFrame),
        LookupHandler.CONTEXTUAL,
        EmitBaseGenerator.DEFINE_BLOCK_CONVERTER.asConsumer(
            future,
            sourceReference,
            EmitBaseGenerator.BLOCK_LOCATION,
            block -> {
              consumer.accept(block.bind(functionName, Name.of("")));
              if (interlock.decrementAndGet() == 0) {
                complete.run();
              }
            }));

    final var blockFrame =
        Frame.create(
            future,
            sourceReference,
            context,
            blocks
                .entrySet()
                .stream()
                .map(
                    block ->
                        Attribute.of(
                            block.getKey(),
                            block
                                .getValue()
                                .first()
                                .instantiateWith(
                                    "Emit template instantiation",
                                    block
                                        .getValue()
                                        .second()
                                        .keySet()
                                        .stream()
                                        .map(
                                            parameter ->
                                                parameter(
                                                    sourceReference,
                                                    context,
                                                    functionName,
                                                    block.getKey(),
                                                    parameter))
                                        .collect(toSource()))))
                .collect(toSource()));
    for (final var block : blocks.keySet()) {
      EmitBaseGenerator.BLOCK_NAME_SOURCE
          .add(block)
          .collect(
              future,
              sourceReference,
              Context.EMPTY.prepend(blockFrame),
              LookupHandler.CONTEXTUAL,
              EmitBaseGenerator.DEFINE_BLOCK_CONVERTER.asConsumer(
                  future,
                  sourceReference,
                  EmitBaseGenerator.BLOCK_LOCATION,
                  resultBlock -> {
                    consumer.accept(resultBlock.bind(functionName, block));
                    if (interlock.decrementAndGet() == 0) {
                      complete.run();
                    }
                  }));
    }
  }

  protected final <
          F extends KwsFunction<V, B, D>,
          B extends KwsBlock<V, B, D>,
          V,
          D extends KwsDispatch<B, V>>
      void createBlocks(F function, BiConsumer<Name, B> defineBlock) {
    defineBlock.accept(Name.of(""), function.entryBlock());
    for (final var block : blocks.entrySet()) {
      defineBlock.accept(
          block.getKey(),
          function.createBlock(
              block
                  .getKey()
                  .apply(
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
              block.getValue().second().values().stream()));
    }
  }

  protected abstract Stream<Name> initials();

  final void setBlocks(Map<Name, Pair<Template, Map<Name, KwsType>>> blocks) {
    this.blocks = blocks;
  }

  final void setEntryBlock(Template entryBlock) {
    this.entryBlock = entryBlock;
  }
}
