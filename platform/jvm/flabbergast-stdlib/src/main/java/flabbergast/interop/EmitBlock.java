package flabbergast.interop;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsDispatch;
import flabbergast.compiler.kws.KwsFunction;
import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface EmitBlock {
  class BrAABlock implements EmitDefineBlock, LookupAssistant.Recipient {
    private Name floatBlock;
    private Map<Name, EmitExpression> floatCaptures;
    private Name intBlock;
    private Map<Name, EmitExpression> intCaptures;
    private EmitExpression left;
    private EmitExpression right;

    @Override
    public EmitBlock bind(Name functionName, Name selfBlock) {
      return new EmitBlock() {
        @Override
        public <
                V,
                B extends KwsBlock<V, B, D>,
                D extends KwsDispatch<B, V>,
                F extends KwsFunction<V, B, D>>
            void render(
                Function<Name, F> functions,
                BiFunction<Name, Name, B> blocks,
                Map<Pair<Name, Name>, V> captures,
                Map<EmitExpression.BlockParameter, V> parameters,
                Map<Integer, V> values) {
          final var block = blocks.apply(functionName, selfBlock);
          final var leftValue = left.render(block, functions, captures, parameters, values);
          final var rightValue = right.render(block, functions, captures, parameters, values);
          final var intTarget = blocks.apply(functionName, intBlock);
          final var floatTarget = blocks.apply(functionName, floatBlock);
          final var intValues =
              intCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());
          final var floatValues =
              floatCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());

          if (leftValue != null
              && rightValue != null
              && intTarget != null
              && floatTarget != null
              && !intValues.contains(null)
              && !floatValues.contains(null)) {
            block.br_aa(
                leftValue,
                rightValue,
                intTarget,
                intValues::stream,
                floatTarget,
                floatValues::stream);
          }
        }
      };
    }

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
    }
  }

  class BrABlock implements EmitDefineBlock, LookupAssistant.Recipient {
    private EmitExpression any;
    private Map<Name, Pair<Name, Map<Name, EmitExpression>>> paths;

    @Override
    public EmitBlock bind(Name functionName, Name selfBlock) {
      return new EmitBlock() {
        @Override
        public <
                V,
                B extends KwsBlock<V, B, D>,
                D extends KwsDispatch<B, V>,
                F extends KwsFunction<V, B, D>>
            void render(
                Function<Name, F> functions,
                BiFunction<Name, Name, B> blocks,
                Map<Pair<Name, Name>, V> captures,
                Map<EmitExpression.BlockParameter, V> parameters,
                Map<Integer, V> values) {
          final var block = blocks.apply(functionName, selfBlock);
          final var value = any.render(block, functions, captures, parameters, values);
          final var dispatch = block.createDispatch();
          for (final var path : paths.entrySet()) {
            final var target = blocks.apply(functionName, path.getValue().first());
            final var arguments =
                path.getValue()
                    .second()
                    .values()
                    .stream()
                    .map(arg -> arg.render(block, functions, captures, parameters, values))
                    .collect(Collectors.toList());
            if (target == null) {
              throw new IllegalArgumentException(
                  String.format(
                      "Target block %s is not defined in function %s.",
                      path.getValue().first(), functionName));
            }
            switch (path.getKey()
                .apply(
                    new NameFunction<String>() {
                      @Override
                      public String apply(long ordinal) {
                        return "";
                      }

                      @Override
                      public String apply(String name) {
                        return name;
                      }
                    })) {
              case "bin":
                dispatch.dispatchBin(target, arguments::stream);
                break;
              case "bool":
                dispatch.dispatchBool(target, arguments::stream);
                break;
              case "float":
                dispatch.dispatchFloat(target, arguments::stream);
                break;
              case "frame":
                dispatch.dispatchFrame(target, arguments::stream);
                break;
              case "int":
                dispatch.dispatchInt(target, arguments::stream);
                break;
              case "lookup_handler":
                dispatch.dispatchLookupHandler(target, arguments::stream);
                break;
              case "null":
                dispatch.dispatchNull(target, arguments::stream);
                break;
              case "str":
                dispatch.dispatchStr(target, arguments::stream);
                break;
              case "template":
                dispatch.dispatchTemplate(target, arguments::stream);
                break;
              default:
                throw new IllegalArgumentException(
                    String.format("Unknown type %s in dispatch.", path.getKey()));
            }
          }
          block.br_a(value, dispatch, Optional.empty());
        }
      };
    }

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
    }
  }

  class BrIABlock implements EmitDefineBlock, LookupAssistant.Recipient {
    private Name floatBlock;
    private Map<Name, EmitExpression> floatCaptures;
    private Name intBlock;
    private Map<Name, EmitExpression> intCaptures;
    private EmitExpression left;
    private EmitExpression right;

    @Override
    public EmitBlock bind(Name functionName, Name selfBlock) {
      return new EmitBlock() {
        @Override
        public <
                V,
                B extends KwsBlock<V, B, D>,
                D extends KwsDispatch<B, V>,
                F extends KwsFunction<V, B, D>>
            void render(
                Function<Name, F> functions,
                BiFunction<Name, Name, B> blocks,
                Map<Pair<Name, Name>, V> captures,
                Map<EmitExpression.BlockParameter, V> parameters,
                Map<Integer, V> values) {
          final var block = blocks.apply(functionName, selfBlock);
          final var leftValue = left.render(block, functions, captures, parameters, values);
          final var rightValue = right.render(block, functions, captures, parameters, values);
          final var intTarget = blocks.apply(functionName, intBlock);
          if (intTarget == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Target block %s for integer path is not defined in function %s.",
                    intBlock, functionName));
          }
          final var floatTarget = blocks.apply(functionName, floatBlock);
          if (floatTarget == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Target block %s for floating-point path is not defined in function %s.",
                    floatBlock, functionName));
          }
          final var intValues =
              intCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());
          final var floatValues =
              floatCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());

          block.br_ia(
              leftValue,
              rightValue,
              intTarget,
              intValues::stream,
              floatTarget,
              floatValues::stream);
        }
      };
    }

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
    }
  }

  class BrZBlock implements EmitDefineBlock, LookupAssistant.Recipient {
    private EmitExpression condition;
    private Name falseBlock;
    private Map<Name, EmitExpression> falseCaptures;
    private Name trueBlock;
    private Map<Name, EmitExpression> trueCaptures;

    @Override
    public EmitBlock bind(Name functionName, Name selfBlock) {
      return new EmitBlock() {
        @Override
        public <
                V,
                B extends KwsBlock<V, B, D>,
                D extends KwsDispatch<B, V>,
                F extends KwsFunction<V, B, D>>
            void render(
                Function<Name, F> functions,
                BiFunction<Name, Name, B> blocks,
                Map<Pair<Name, Name>, V> captures,
                Map<EmitExpression.BlockParameter, V> parameters,
                Map<Integer, V> values) {
          final var block = blocks.apply(functionName, selfBlock);
          final var value = condition.render(block, functions, captures, parameters, values);
          final var trueTarget = blocks.apply(functionName, trueBlock);
          if (trueTarget == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Target block %s for true path is not defined in function %s.",
                    trueBlock, functionName));
          }
          final var falseTarget = blocks.apply(functionName, falseBlock);
          if (falseTarget == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Target block %s for false path is not defined in function %s.",
                    falseBlock, functionName));
          }
          final var trueValues =
              trueCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());
          final var falseValues =
              falseCaptures
                  .values()
                  .stream()
                  .map(arg -> arg.render(block, functions, captures, parameters, values))
                  .collect(Collectors.toList());

          block.br_z(value, trueTarget, trueValues::stream, falseTarget, falseValues::stream);
        }
      };
    }

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      future.complete(Any.of(Frame.proxyOf(sourceReference, context, this, Stream.empty())));
    }
  }

  class LetBlock implements LookupAssistant.Recipient {
    private Map<Name, EmitExpression> definitions;
    private Template inner;

    @Override
    public void run(Future<Any> future, SourceReference sourceReference, Context context) {
      final Map<Name, Integer> ids =
          definitions
              .keySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      Function.identity(), k -> EmitExpression.ID_GENERATOR.incrementAndGet()));
      final var blockFrame =
          Frame.create(
              future,
              sourceReference.specialJunction("Emit Let block", inner.source()),
              context,
              inner,
              ids.entrySet()
                  .stream()
                  .map(
                      e ->
                          Attribute.of(
                              e.getKey(),
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
                                          return values.get(e.getValue());
                                        }
                                      },
                                      Stream.empty()))))
                  .collect(toSource()));
      EmitBaseGenerator.BLOCK_NAME_SOURCE.collect(
          future,
          sourceReference,
          Context.EMPTY.prepend(blockFrame),
          LookupHandler.CONTEXTUAL,
          EmitBaseGenerator.DEFINE_BLOCK_CONVERTER.asConsumer(
              future,
              sourceReference,
              EmitBaseGenerator.BLOCK_LOCATION,
              resultBlock ->
                  future.complete(
                      Any.of(
                          Frame.<EmitDefineBlock>proxyOf(
                              sourceReference,
                              context,
                              (functionName, selfBlock) ->
                                  new EmitBlock() {
                                    final Map<Name, Integer> ids =
                                        definitions
                                            .keySet()
                                            .stream()
                                            .collect(
                                                Collectors.toMap(
                                                    Function.identity(),
                                                    k ->
                                                        EmitExpression.ID_GENERATOR
                                                            .incrementAndGet()));
                                    final EmitBlock innerBlock =
                                        resultBlock.bind(functionName, selfBlock);

                                    @Override
                                    public <
                                            V,
                                            B extends KwsBlock<V, B, D>,
                                            D extends KwsDispatch<B, V>,
                                            F extends KwsFunction<V, B, D>>
                                        void render(
                                            Function<Name, F> functions,
                                            BiFunction<Name, Name, B> blocks,
                                            Map<Pair<Name, Name>, V> captures,
                                            Map<EmitExpression.BlockParameter, V> parameters,
                                            Map<Integer, V> values) {
                                      final var block = blocks.apply(functionName, selfBlock);
                                      for (final var definition : definitions.entrySet()) {
                                        values.put(
                                            ids.get(definition.getKey()),
                                            definition
                                                .getValue()
                                                .render(
                                                    block,
                                                    functions,
                                                    captures,
                                                    parameters,
                                                    values));
                                      }
                                      innerBlock.render(
                                          functions, blocks, captures, parameters, values);
                                    }
                                  },
                              Stream.empty())))));
    }
  }

  Definition BR =
      NativeBinding.<Name, Map<Name, EmitExpression>, EmitDefineBlock>function(
          NativeBinding.asProxy(),
          (blockName, args) ->
              (functionName, selfBlock) ->
                  new EmitBlock() {
                    @Override
                    public <
                            V,
                            B extends KwsBlock<V, B, D>,
                            D extends KwsDispatch<B, V>,
                            F extends KwsFunction<V, B, D>>
                        void render(
                            Function<Name, F> functions,
                            BiFunction<Name, Name, B> blocks,
                            Map<Pair<Name, Name>, V> captures,
                            Map<EmitExpression.BlockParameter, V> parameters,
                            Map<Integer, V> values) {
                      final var block = blocks.apply(functionName, selfBlock);
                      final var target = blocks.apply(functionName, blockName);
                      if (target == null) {
                        throw new IllegalArgumentException(
                            String.format(
                                "Target block %s is not defined in function %s.",
                                blockName, functionName));
                      }
                      final var argValues =
                          args.values()
                              .stream()
                              .map(
                                  arg -> arg.render(block, functions, captures, parameters, values))
                              .collect(Collectors.toList());
                      block.br(target, argValues::stream);
                    }
                  },
          AnyConverter.asName(false),
          "target",
          AnyConverter.frameOf(EmitExpression.CONVERTER, false),
          "target_arguments");
  Definition BR_A =
      LookupAssistant.create(
          BrABlock::new,
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.any = v, "arg"),
          LookupAssistant.find(
              AnyConverter.frameOf(
                  AnyConverter.asPair(
                      AnyConverter.asName(false),
                      "block",
                      AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                      "args"),
                  false),
              (i1, v1) -> i1.paths = v1,
              "paths"));
  Definition BR_AA =
      LookupAssistant.create(
          BrAABlock::new,
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.left = v, "left"),
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.right = v, "right"),
          LookupAssistant.find(AnyConverter.asName(false), (i, v) -> i.intBlock = v, "int_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i1, v1) -> i1.intCaptures = v1,
              "int_args"),
          LookupAssistant.find(
              AnyConverter.asName(false), (i, v) -> i.floatBlock = v, "float_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i2, v2) -> i2.floatCaptures = v2,
              "float_args"));

  Definition BR_FA =
      NativeBinding
          .<EmitExpression, EmitExpression, Name, Map<Name, EmitExpression>, EmitDefineBlock>
              function(
                  NativeBinding.asProxy(),
                  (left, right, target, args) ->
                      (functionName, selfBlock) ->
                          new EmitBlock() {
                            @Override
                            public <
                                    V,
                                    B extends KwsBlock<V, B, D>,
                                    D extends KwsDispatch<B, V>,
                                    F extends KwsFunction<V, B, D>>
                                void render(
                                    Function<Name, F> functions,
                                    BiFunction<Name, Name, B> blocks,
                                    Map<Pair<Name, Name>, V> captures,
                                    Map<EmitExpression.BlockParameter, V> parameters,
                                    Map<Integer, V> values) {
                              final var block = blocks.apply(functionName, selfBlock);
                              final var leftValue =
                                  left.render(block, functions, captures, parameters, values);
                              final var rightValue =
                                  right.render(block, functions, captures, parameters, values);
                              final var targetBlock = blocks.apply(functionName, target);
                              final var argValues =
                                  args.values()
                                      .stream()
                                      .map(
                                          arg ->
                                              arg.render(
                                                  block, functions, captures, parameters, values))
                                      .collect(Collectors.toList());
                              if (leftValue != null
                                  && rightValue != null
                                  && targetBlock != null
                                  && !argValues.contains(null)) {
                                block.br_fa(leftValue, rightValue, targetBlock, argValues::stream);
                              }
                            }
                          },
                  EmitExpression.CONVERTER,
                  "left",
                  EmitExpression.CONVERTER,
                  "right",
                  AnyConverter.asName(false),
                  "target",
                  AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                  "target_args");
  Definition BR_IA =
      LookupAssistant.create(
          BrIABlock::new,
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.left = v, "left"),
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.right = v, "right"),
          LookupAssistant.find(AnyConverter.asName(false), (i, v) -> i.intBlock = v, "int_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i1, v1) -> i1.intCaptures = v1,
              "int_args"),
          LookupAssistant.find(
              AnyConverter.asName(false), (i, v) -> i.floatBlock = v, "float_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i2, v2) -> i2.floatCaptures = v2,
              "float_args"));

  Definition BR_Z =
      LookupAssistant.create(
          BrZBlock::new,
          LookupAssistant.find(EmitExpression.CONVERTER, (i, v) -> i.condition = v, "condition"),
          LookupAssistant.find(AnyConverter.asName(false), (i, v) -> i.trueBlock = v, "true_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i1, v1) -> i1.trueCaptures = v1,
              "true_args"),
          LookupAssistant.find(
              AnyConverter.asName(false), (i, v) -> i.falseBlock = v, "false_block"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i2, v2) -> i2.falseCaptures = v2,
              "false_args"));

  Definition ERROR =
      NativeBinding.<EmitExpression, EmitDefineBlock>function(
          NativeBinding.asProxy(),
          message ->
              (functionName, selfBlock) ->
                  new EmitBlock() {
                    @Override
                    public <
                            V,
                            B extends KwsBlock<V, B, D>,
                            D extends KwsDispatch<B, V>,
                            F extends KwsFunction<V, B, D>>
                        void render(
                            Function<Name, F> functions,
                            BiFunction<Name, Name, B> blocks,
                            Map<Pair<Name, Name>, V> captures,
                            Map<EmitExpression.BlockParameter, V> parameters,
                            Map<Integer, V> values) {
                      final var block = blocks.apply(functionName, selfBlock);
                      final var messageValue =
                          message.render(block, functions, captures, parameters, values);
                      block.error(messageValue);
                    }
                  },
          EmitExpression.CONVERTER,
          "message");
  Definition LET =
      LookupAssistant.create(
          LetBlock::new,
          LookupAssistant.find(AnyConverter.asTemplate(false), (i, v) -> i.inner = v, "in"),
          LookupAssistant.find(
              AnyConverter.frameOf(EmitExpression.CONVERTER, false),
              (i1, v1) -> i1.definitions = v1,
              "args"));

  Definition RETURN =
      NativeBinding.<EmitExpression, EmitDefineBlock>function(
          NativeBinding.asProxy(),
          value ->
              (functionName, selfBlock) ->
                  new EmitBlock() {
                    @Override
                    public <
                            V,
                            B extends KwsBlock<V, B, D>,
                            D extends KwsDispatch<B, V>,
                            F extends KwsFunction<V, B, D>>
                        void render(
                            Function<Name, F> functions,
                            BiFunction<Name, Name, B> blocks,
                            Map<Pair<Name, Name>, V> captures,
                            Map<EmitExpression.BlockParameter, V> parameters,
                            Map<Integer, V> values) {
                      final var block = blocks.apply(functionName, selfBlock);
                      final var resultValue =
                          value.render(block, functions, captures, parameters, values);
                      block.ret(resultValue);
                    }
                  },
          EmitExpression.CONVERTER,
          "result");

  Definition RETURN_ACCUMULATOR =
      NativeBinding.<EmitExpression, Map<Name, EmitExpression>, EmitDefineBlock>function(
          NativeBinding.asProxy(),
          (value, builders) ->
              (functionName, selfBlock) ->
                  new EmitBlock() {
                    @Override
                    public <
                            V,
                            B extends KwsBlock<V, B, D>,
                            D extends KwsDispatch<B, V>,
                            F extends KwsFunction<V, B, D>>
                        void render(
                            Function<Name, F> functions,
                            BiFunction<Name, Name, B> blocks,
                            Map<Pair<Name, Name>, V> captures,
                            Map<EmitExpression.BlockParameter, V> parameters,
                            Map<Integer, V> values) {
                      final var block = blocks.apply(functionName, selfBlock);
                      final var resultValue =
                          value.render(block, functions, captures, parameters, values);
                      final var builderValues =
                          builders
                              .values()
                              .stream()
                              .map(
                                  builder ->
                                      builder.render(
                                          block, functions, captures, parameters, values))
                              .collect(Collectors.toList());
                      block.ret(resultValue, builderValues::stream);
                    }
                  },
          EmitExpression.CONVERTER,
          "result",
          AnyConverter.frameOf(EmitExpression.CONVERTER, false),
          "builders");

  <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
      void render(
          Function<Name, F> functions,
          BiFunction<Name, Name, B> blocks,
          Map<Pair<Name, Name>, V> captures,
          Map<EmitExpression.BlockParameter, V> parameters,
          Map<Integer, V> values);
}
