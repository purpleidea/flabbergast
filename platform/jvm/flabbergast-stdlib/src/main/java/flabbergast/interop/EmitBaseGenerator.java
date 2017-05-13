package flabbergast.interop;

import static flabbergast.lang.AttributeSource.toSource;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.*;
import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import flabbergast.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class EmitBaseGenerator extends EmitBaseFunction {
  static final String BLOCK_ATTRIBUTE_NAME = "block definition";
  static final TypeErrorLocation BLOCK_LOCATION = TypeErrorLocation.lookup(BLOCK_ATTRIBUTE_NAME);
  static final NameSource BLOCK_NAME_SOURCE = NameSource.EMPTY.add(BLOCK_ATTRIBUTE_NAME);
  static final AnyConverter<EmitDefineBlock> DEFINE_BLOCK_CONVERTER =
      AnyConverter.asProxy(
          EmitDefineBlock.class,
          false,
          SpecialLocation.library("emit").attributes("block").any().instantiated());
  static final AnyConverter<KwsType> KWS_TYPE =
      AnyConverter.asProxy(
          KwsType.class, false, SpecialLocation.library("emit").attributes("types").any());

  static Stream<NativeBinding> bindings() {
    return Stream.of(
            Stream.of(
                NativeBinding.of("load", EmitLoadingGenerator.DEFINITION),
                NativeBinding.of("load_definition", EmitLoadingDefinitionGenerator.DEFINITION),
                NativeBinding.of("jar", EmitJarGenerator.DEFINITION),
                blockDefinition("block.br", EmitBlock.BR),
                blockDefinition("block.br_a", EmitBlock.BR_A),
                blockDefinition("block.br_aa", EmitBlock.BR_AA),
                blockDefinition("block.br_fa", EmitBlock.BR_FA),
                blockDefinition("block.br_ia", EmitBlock.BR_IA),
                blockDefinition("block.br_z", EmitBlock.BR_Z),
                blockDefinition("block.error", EmitBlock.ERROR),
                blockDefinition("block.let", EmitBlock.LET),
                blockDefinition("block.return", EmitBlock.RETURN),
                blockDefinition("block.return_accumulator", EmitBlock.RETURN_ACCUMULATOR),
                NativeBinding.of(
                    "instruction.add_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.add_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.add_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.add_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.add_n",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, names) ->
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
                                return block.add_n(
                                    source.render(block, functions, captures, parameters, values),
                                    names.values()::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        AnyConverter.frameOf(AnyConverter.asString(false), false),
                        "args")),
                NativeBinding.of(
                    "instruction.add_n_a",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, value) ->
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
                                return block.add_n_a(
                                    source.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.add_n_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, value) ->
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
                                return block.add_n_i(
                                    source.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "ordinal")),
                NativeBinding.of(
                    "instruction.add_n_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, value) ->
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
                                return block.add_n_r(
                                    source.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "frame")),
                NativeBinding.of(
                    "instruction.add_n_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, value) ->
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
                                return block.add_n_s(
                                    source.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.adjacent_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.adjacent_f(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.adjacent_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.adjacent_i(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.adjacent_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.adjacent_s(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.adjacent_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.adjacent_z(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.alwaysinclude_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.alwaysinclude_f(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.alwaysinclude_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.alwaysinclude_i(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.alwaysinclude_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.alwaysinclude_s(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.alwaysinclude_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.alwaysinclude_z(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.and_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        groupers ->
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
                                return block.and_g(
                                    groupers
                                            .values()
                                            .stream()
                                            .map(
                                                grouper ->
                                                    grouper.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "args")),
                NativeBinding.of(
                    "instruction.and_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.and_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.atos",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.atos(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.atoz",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (value, types) ->
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
                                return block.atoz(
                                    value.render(block, functions, captures, parameters, values),
                                    t -> types.containsValue(t.attributeName()));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg",
                        AnyConverter.frameOf(AnyConverter.asString(false), false),
                        "types")),
                NativeBinding.of(
                    "instruction.boundary",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, trailing) ->
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
                                return block.boundary(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    trailing.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "trailing")),
                NativeBinding.of(
                    "instruction.btoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.btoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.buckets_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, count) ->
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
                                return block.buckets_f(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.buckets_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, count) ->
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
                                return block.buckets_i(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.buckets_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, count) ->
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
                                return block.buckets_s(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.call_d",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, context) ->
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
                                return block.call_d(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.call_o",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, context, original) ->
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
                                return block.call_o(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values),
                                    original.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "original")),
                NativeBinding.of(
                    "instruction.cat_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, chains) ->
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
                                return block.cat_e(
                                    context.render(block, functions, captures, parameters, values),
                                    chains
                                            .values()
                                            .stream()
                                            .map(
                                                chain ->
                                                    chain.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "chains")),
                NativeBinding.of(
                    "instruction.cat_ke",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, chain) ->
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
                                return block.cat_ke(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    chain.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "chain")),
                NativeBinding.of(
                    "instruction.cat_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, first, second) ->
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
                                return block.cat_r(
                                    context.render(block, functions, captures, parameters, values),
                                    first.render(block, functions, captures, parameters, values),
                                    second.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "first",
                        EmitExpression.CONVERTER,
                        "second")),
                NativeBinding.of(
                    "instruction.cat_rc",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (first, second) ->
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
                                return block.cat_rc(
                                    first.render(block, functions, captures, parameters, values),
                                    second.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "first",
                        EmitExpression.CONVERTER,
                        "second")),
                NativeBinding.of(
                    "instruction.cat_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (first, second) ->
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
                                return block.cat_s(
                                    first.render(block, functions, captures, parameters, values),
                                    second.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "first",
                        EmitExpression.CONVERTER,
                        "second")),
                NativeBinding.of(
                    "instruction.chunk_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        length ->
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
                                return block.chunk_e(
                                    length.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "length")),
                NativeBinding.of(
                    "instruction.cmp_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.cmp_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.cmp_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.cmp_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.cmp_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.cmp_s(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.cmp_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.cmp_z(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.contextual",
                    Any.of(
                        Frame.proxyOf(
                            "emit contextual",
                            "emit",
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
                                return block.contextual();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.count_w",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        count ->
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
                                return block.count_w(
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.crosstab_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        definition ->
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
                                return block.crosstab_f(
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.crosstab_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        definition ->
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
                                return block.crosstab_i(
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.crosstab_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        definition ->
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
                                return block.crosstab_s(
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.crosstab_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        definition ->
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
                                return block.crosstab_z(
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.ctr_c",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        context ->
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
                                return block.ctr_c(
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.ctr_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        frame ->
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
                                return block.ctr_r(
                                    frame.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame")),
                NativeBinding.of(
                    "instruction.ctxt_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, frame) ->
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
                                return block.ctxt_r(
                                    context.render(block, functions, captures, parameters, values),
                                    frame.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "frame")),
                NativeBinding.of(
                    "instruction.debug_d",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, context) ->
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
                                return block.debug_d(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.disc_g_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.disc_g_f(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.disc_g_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.disc_g_i(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.disc_g_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.disc_g_s(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.disc_g_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.disc_g_z(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.disperse_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (frame, name, arg) ->
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
                                final var value =
                                    arg.render(block, functions, captures, parameters, values);
                                block.disperse_i(
                                    frame.render(block, functions, captures, parameters, values),
                                    name.render(block, functions, captures, parameters, values),
                                    value);
                                return value;
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame",
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.disperse_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (frame, name, arg) ->
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
                                final var value =
                                    arg.render(block, functions, captures, parameters, values);
                                block.disperse_s(
                                    frame.render(block, functions, captures, parameters, values),
                                    name.render(block, functions, captures, parameters, values),
                                    value);
                                return value;
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame",
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.div_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.div_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.div_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.div_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.drop_ed",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, clause) ->
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
                                return block.drop_ed(
                                    source.render(block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.drop_ei",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, count) ->
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
                                return block.drop_ei(
                                    source.render(block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.drop_x",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        name ->
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
                                return block.drop_x(
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.dropl_ei",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, count) ->
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
                                return block.dropl_ei(
                                    source.render(block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.duration_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, duration) ->
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
                                return block.duration_f(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    duration.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "duration")),
                NativeBinding.of(
                    "instruction.duration_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, duration) ->
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
                                return block.duration_i(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    duration.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "duration")),
                NativeBinding.of(
                    "instruction.etoa_ao",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, initial, reducer) ->
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
                                return block.etoa_ao(
                                    source.render(block, functions, captures, parameters, values),
                                    initial.render(block, functions, captures, parameters, values),
                                    reducer.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "initial",
                        EmitExpression.CONVERTER,
                        "reducer")),
                NativeBinding.of(
                    "instruction.etoa_d",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, extractor) ->
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
                                return block.etoa_d(
                                    source.render(block, functions, captures, parameters, values),
                                    extractor.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "extractor")),
                NativeBinding.of(
                    "instruction.etoa_dd",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, extractor, alternate) ->
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
                                return block.etoa_dd(
                                    source.render(block, functions, captures, parameters, values),
                                    extractor.render(
                                        block, functions, captures, parameters, values),
                                    alternate.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "extractor",
                        EmitExpression.CONVERTER,
                        "alternate")),
                NativeBinding.of(
                    "instruction.etod",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, definition) ->
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
                                return block.etod(
                                    source.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.etod_a",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, definition, empty) ->
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
                                return block.etod_a(
                                    source.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    empty.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "empty")),
                NativeBinding.of(
                    "instruction.etoe_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, groupers) ->
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
                                return block.etoe_g(
                                    source.render(block, functions, captures, parameters, values),
                                    groupers
                                            .values()
                                            .stream()
                                            .map(
                                                grouper ->
                                                    grouper.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "groupers")),
                NativeBinding.of(
                    "instruction.etoe_m",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, initial, reducer) ->
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
                                return block.etoe_m(
                                    source.render(block, functions, captures, parameters, values),
                                    initial.render(block, functions, captures, parameters, values),
                                    reducer.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "initial",
                        EmitExpression.CONVERTER,
                        "reducer")),
                NativeBinding.of(
                    "instruction.etoe_u",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, flattener) ->
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
                                return block.etoe_u(
                                    source.render(block, functions, captures, parameters, values),
                                    flattener.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "flattener")),
                NativeBinding.of(
                    "instruction.etoi",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        source ->
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
                                return block.etoi(
                                    source.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source")),
                NativeBinding.of(
                    "instruction.etor_am",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, initial, reducer) ->
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
                                return block.etor_ao(
                                    source.render(block, functions, captures, parameters, values),
                                    initial.render(block, functions, captures, parameters, values),
                                    reducer.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "initial",
                        EmitExpression.CONVERTER,
                        "reducer")),
                NativeBinding.of(
                    "instruction.etor_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, computeValue) ->
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
                                return block.etor_i(
                                    source.render(block, functions, captures, parameters, values),
                                    computeValue.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "compute_value")),
                NativeBinding.of(
                    "instruction.etor_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, computeName, computeValue) ->
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
                                return block.etor_s(
                                    source.render(block, functions, captures, parameters, values),
                                    computeName.render(
                                        block, functions, captures, parameters, values),
                                    computeValue.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "compute_name",
                        EmitExpression.CONVERTER,
                        "compute_value")),
                NativeBinding.of(
                    "instruction.ext",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        uri ->
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
                                return block.ext(uri);
                              }
                            },
                        AnyConverter.asString(false),
                        "uri")),
                NativeBinding.of(
                    "instruction.filt_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, clause) ->
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
                                return block.filt_e(
                                    source.render(block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.ftoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ftoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.ftoi",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ftoi(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.ftos",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ftos(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.function",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, args) ->
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
                                final var function = functions.apply(name);
                                if (function == null) {
                                  throw new IllegalArgumentException(
                                      String.format("Cannot find function %s.", name));
                                }
                                return function.access(
                                    block,
                                    args.values()
                                        .stream()
                                        .map(
                                            arg ->
                                                arg.render(
                                                    block,
                                                    functions,
                                                    captures,
                                                    parameters,
                                                    values)));
                              }
                            },
                        AnyConverter.asName(false),
                        "name",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "args")),
                NativeBinding.of(
                    "instruction.gather_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (frame, name) ->
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
                                return block.gather_i(
                                    frame.render(block, functions, captures, parameters, values),
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame",
                        EmitExpression.CONVERTER,
                        "ordinal")),
                NativeBinding.of(
                    "instruction.gather_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (frame, name) ->
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
                                return block.gather_s(
                                    frame.render(block, functions, captures, parameters, values),
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame",
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.id",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.id(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "frame")),
                NativeBinding.of(
                    "instruction.import_function",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, returnType, arguments) ->
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
                                return block.importFunction(
                                    name,
                                    returnType,
                                    arguments
                                            .values()
                                            .stream()
                                            .map(
                                                arg ->
                                                    arg.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        AnyConverter.asString(false),
                        "name",
                        KWS_TYPE,
                        "return_type",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "args")),
                NativeBinding.of(
                    "instruction.inf_f",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.inf_f();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.is_finite",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.is_finite(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.is_nan",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.is_nan(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.itoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.itoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.itof",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.itof(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.itos",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.itos(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.itoz",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (reference, value) ->
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
                                return block.itoz(
                                    reference,
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        AnyConverter.asInt(false),
                        "reference",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.ktol",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, context, definition) ->
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
                                return block.ktol(
                                    name.render(block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.len_b",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.len_b(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.len_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.len_s(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.let",
                    LookupAssistant.create(
                        () ->
                            new LookupAssistant.Recipient() {
                              private Map<Name, EmitExpression> definitions;
                              private Template template;

                              @Override
                              public void run(
                                  Future<Any> future,
                                  SourceReference sourceReference,
                                  Context context) {
                                final var bindings =
                                    definitions
                                        .keySet()
                                        .stream()
                                        .collect(
                                            Collectors.toMap(
                                                Function.identity(),
                                                k ->
                                                    EmitExpression.ID_GENERATOR.incrementAndGet()));
                                final var frame =
                                    Frame.create(
                                        future,
                                        sourceReference,
                                        context,
                                        template,
                                        bindings
                                            .entrySet()
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
                                                                          F extends
                                                                              KwsFunction<V, B, D>,
                                                                          V,
                                                                          B extends
                                                                              KwsBlock<V, B, D>,
                                                                          D extends
                                                                              KwsDispatch<B, V>>
                                                                      V render(
                                                                          B block,
                                                                          Function<Name, F>
                                                                              functions,
                                                                          Map<Pair<Name, Name>, V>
                                                                              captures,
                                                                          Map<BlockParameter, V>
                                                                              parameters,
                                                                          Map<Integer, V> values) {
                                                                    return values.get(e.getValue());
                                                                  }
                                                                },
                                                                Stream.empty()))))
                                            .collect(toSource()));
                                NameSource.EMPTY
                                    .add("value")
                                    .collect(
                                        future,
                                        sourceReference,
                                        Context.EMPTY.prepend(frame),
                                        LookupHandler.CONTEXTUAL,
                                        EmitExpression.CONVERTER.asConsumer(
                                            future,
                                            sourceReference,
                                            TypeErrorLocation.lookup("value"),
                                            inner ->
                                                future.complete(
                                                    Any.of(
                                                        Frame.proxyOf(
                                                            sourceReference,
                                                            context,
                                                            new EmitExpression() {

                                                              @Override
                                                              public <
                                                                      F extends
                                                                          KwsFunction<V, B, D>,
                                                                      V,
                                                                      B extends KwsBlock<V, B, D>,
                                                                      D extends KwsDispatch<B, V>>
                                                                  V render(
                                                                      B block,
                                                                      Function<Name, F> functions,
                                                                      Map<Pair<Name, Name>, V>
                                                                          captures,
                                                                      Map<BlockParameter, V>
                                                                          parameters,
                                                                      Map<Integer, V> values) {
                                                                for (final var definition :
                                                                    definitions.entrySet()) {
                                                                  values.put(
                                                                      bindings.get(
                                                                          definition.getKey()),
                                                                      definition
                                                                          .getValue()
                                                                          .render(
                                                                              block,
                                                                              functions,
                                                                              captures,
                                                                              parameters,
                                                                              values));
                                                                }
                                                                return inner.render(
                                                                    block,
                                                                    functions,
                                                                    captures,
                                                                    parameters,
                                                                    values);
                                                              }
                                                            },
                                                            Stream.empty())))));
                              }
                            },
                        LookupAssistant.find(
                            AnyConverter.asTemplate(false), (i, t) -> i.template = t, "in"),
                        LookupAssistant.<LookupAssistant.Recipient, Map<Name, EmitExpression>>find(
                            AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                            (i1, d) -> i1.definitions = d,
                            "definitions"))),
                NativeBinding.of(
                    "instruction.let_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, builders) ->
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
                                return block.let_e(
                                    source.render(block, functions, captures, parameters, values),
                                    builders
                                            .values()
                                            .stream()
                                            .map(
                                                builder ->
                                                    builder.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "builders")),
                NativeBinding.of(
                    "instruction.lookup",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, names) ->
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
                                return block.lookup(
                                    source.render(block, functions, captures, parameters, values),
                                    names.values()::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        AnyConverter.frameOf(AnyConverter.asString(false), false),
                        "names")),
                NativeBinding.of(
                    "instruction.lookup_l",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (handler, context, names) ->
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
                                return block.lookup_l(
                                    handler.render(block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values),
                                    names.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "handler",
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "names")),
                NativeBinding.of(
                    "instruction.ltoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ltoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.max_f",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.max_f();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.max_i",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.max_i();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.max_z",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.max_z();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.min_f",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.min_f();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.min_i",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.min_i();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.min_z",
                    Any.of(
                        Frame.proxyOf(
                            "emit infinity",
                            "emit",
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
                                return block.min_z();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.mod_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.mod_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.mod_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.mod_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.mtoe",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, initial, definition) ->
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
                                return block.mtoe(
                                    context.render(block, functions, captures, parameters, values),
                                    initial.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "initial",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.mul_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.mul_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.mul_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.mul_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.nan_f",
                    Any.of(
                        Frame.proxyOf(
                            "emit NaN",
                            "emit",
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
                                return block.nan_f();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.neg_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.neg_f(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.neg_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.neg_i(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.new_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, collector) ->
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
                                return block.new_g(
                                    name.render(block, functions, captures, parameters, values),
                                    collector.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.new_g_a",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, collector) ->
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
                                return block.new_g_a(
                                    name.render(block, functions, captures, parameters, values),
                                    collector.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.new_p",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, intersect, zippers) ->
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
                                return block.new_p(
                                    context.render(block, functions, captures, parameters, values),
                                    intersect.render(
                                        block, functions, captures, parameters, values),
                                    zippers
                                            .values()
                                            .stream()
                                            .map(
                                                merger ->
                                                    merger.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "intersect",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "zippers")),
                NativeBinding.of(
                    "instruction.new_p_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        name ->
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
                                return block.new_p_i(
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.new_p_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, frame) ->
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
                                return block.new_p_r(
                                    name.render(block, functions, captures, parameters, values),
                                    frame.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "frame")),
                NativeBinding.of(
                    "instruction.new_p_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        name ->
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
                                return block.new_p_s(
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.new_r",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (selfIsThis, context, gatherers, builders) ->
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
                                return block.new_r(
                                    selfIsThis.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values),
                                    gatherers
                                            .values()
                                            .stream()
                                            .map(
                                                gatherer ->
                                                    gatherer.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream,
                                    builders
                                            .values()
                                            .stream()
                                            .map(
                                                builder ->
                                                    builder.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "self_is_this",
                        EmitExpression.CONVERTER,
                        "context",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "gatherers",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "builders")),
                NativeBinding.of(
                    "instruction.new_r_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, start, end) ->
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
                                return block.new_r_i(
                                    context.render(block, functions, captures, parameters, values),
                                    start.render(block, functions, captures, parameters, values),
                                    end.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        EmitExpression.CONVERTER,
                        "start",
                        EmitExpression.CONVERTER,
                        "end")),
                NativeBinding.of(
                    "instruction.new_t",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (context, gatherers, builders) ->
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
                                return block.new_t(
                                    context.render(block, functions, captures, parameters, values),
                                    gatherers
                                            .values()
                                            .stream()
                                            .map(
                                                gatherer ->
                                                    gatherer.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream,
                                    builders
                                            .values()
                                            .stream()
                                            .map(
                                                builder ->
                                                    builder.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        EmitExpression.CONVERTER,
                        "context",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "gatherers",
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "builders")),
                NativeBinding.of(
                    "instruction.new_x_ia",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (ordinal, value) ->
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
                                return block.new_x_ia(
                                    ordinal.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "ordinal",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.new_x_sa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, value) ->
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
                                return block.new_x_sa(
                                    name.render(block, functions, captures, parameters, values),
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.new_x_sd",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.new_x_sd(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.new_x_so",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (name, definition) ->
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
                                return block.new_x_so(
                                    name.render(block, functions, captures, parameters, values),
                                    definition.render(
                                        block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name",
                        EmitExpression.CONVERTER,
                        "definition")),
                NativeBinding.of(
                    "instruction.nil_a",
                    Any.of(
                        Frame.proxyOf(
                            "emit Null",
                            "emit",
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
                                return block.nil_a();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.nil_c",
                    Any.of(
                        Frame.proxyOf(
                            "emit empty context",
                            "emit",
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
                                return block.nil_c();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.nil_n",
                    Any.of(
                        Frame.proxyOf(
                            "emit empty name source",
                            "emit",
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
                                return block.nil_n();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.nil_r",
                    Any.of(
                        Frame.proxyOf(
                            "emit empty frame",
                            "emit",
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
                                return block.nil_r();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.nil_w",
                    Any.of(
                        Frame.proxyOf(
                            "emit empty window",
                            "emit",
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
                                return block.nil_w();
                              }
                            },
                            Stream.empty()))),
                NativeBinding.of(
                    "instruction.not_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.not_g(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.not_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.not_i(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.not_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.not_z(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.or_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        groupers ->
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
                                return block.or_g(
                                    groupers
                                            .values()
                                            .stream()
                                            .map(
                                                grouper ->
                                                    grouper.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "groupers")),
                NativeBinding.of(
                    "instruction.or_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.or_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.ord_e_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, ascending, clause) ->
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
                                return block.ord_e_f(
                                    source.render(block, functions, captures, parameters, values),
                                    ascending.render(
                                        block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "ascending",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.ord_e_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, ascending, clause) ->
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
                                return block.ord_e_i(
                                    source.render(block, functions, captures, parameters, values),
                                    ascending.render(
                                        block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "ascending",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.ord_e_s",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, ascending, clause) ->
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
                                return block.ord_e_s(
                                    source.render(block, functions, captures, parameters, values),
                                    ascending.render(
                                        block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "ascending",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.ord_e_z",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, ascending, clause) ->
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
                                return block.ord_e_z(
                                    source.render(block, functions, captures, parameters, values),
                                    ascending.render(
                                        block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "ascending",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.powerset",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        groupers ->
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
                                return block.powerset(
                                    groupers
                                            .values()
                                            .stream()
                                            .map(
                                                grouper ->
                                                    grouper.render(
                                                        block,
                                                        functions,
                                                        captures,
                                                        parameters,
                                                        values))
                                            .collect(Collectors.toList())
                                        ::stream);
                              }
                            },
                        AnyConverter.frameOf(EmitExpression.CONVERTER, false),
                        "groupers")),
                NativeBinding.of(
                    "instruction.priv_x",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        inner ->
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
                                return block.priv_x(
                                    inner.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "inner")),
                NativeBinding.of(
                    "instruction.require_x",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        name ->
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
                                return block.require_x(
                                    name.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "name")),
                NativeBinding.of(
                    "instruction.rev_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        source ->
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
                                return block.rev_e(
                                    source.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source")),
                NativeBinding.of(
                    "instruction.ring_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (primitive, size) ->
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
                                return block.ring_g(
                                    primitive.render(
                                        block, functions, captures, parameters, values),
                                    size.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "primitive",
                        EmitExpression.CONVERTER,
                        "size")),
                NativeBinding.of(
                    "instruction.rtoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.rtoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.rtoe",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, context) ->
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
                                return block.rtoe(
                                    source.render(block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.seal_d",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, context) ->
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
                                return block.seal_d(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.seal_o",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, context) ->
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
                                return block.seal_o(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    context.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "context")),
                NativeBinding.of(
                    "instruction.session_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, adjacent, maximum) ->
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
                                return block.session_f(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    adjacent.render(block, functions, captures, parameters, values),
                                    maximum.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "adjacent",
                        EmitExpression.CONVERTER,
                        "maximum")),
                NativeBinding.of(
                    "instruction.session_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (definition, adjacent, maximum) ->
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
                                return block.session_i(
                                    definition.render(
                                        block, functions, captures, parameters, values),
                                    adjacent.render(block, functions, captures, parameters, values),
                                    maximum.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "definition",
                        EmitExpression.CONVERTER,
                        "adjacent",
                        EmitExpression.CONVERTER,
                        "maximum")),
                NativeBinding.of(
                    "instruction.sh_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (value, offset) ->
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
                                return block.sh_i(
                                    value.render(block, functions, captures, parameters, values),
                                    offset.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg",
                        EmitExpression.CONVERTER,
                        "offset")),
                NativeBinding.of(
                    "instruction.shuf_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        source ->
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
                                return block.shuf_e(
                                    source.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source")),
                NativeBinding.of(
                    "instruction.stoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.stoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.stripe_e",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        width ->
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
                                return block.stripe_e(
                                    width.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "width")),
                NativeBinding.of(
                    "instruction.sub_f",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.sub_f(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.sub_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.sub_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.take_ed",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, clause) ->
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
                                return block.take_ed(
                                    source.render(block, functions, captures, parameters, values),
                                    clause.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "clause")),
                NativeBinding.of(
                    "instruction.take_ei",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, count) ->
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
                                return block.take_ei(
                                    source.render(block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.takel_ei",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (source, count) ->
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
                                return block.takel_ei(
                                    source.render(block, functions, captures, parameters, values),
                                    count.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "source",
                        EmitExpression.CONVERTER,
                        "count")),
                NativeBinding.of(
                    "instruction.ttoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ttoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.window_g",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.window_g(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "length",
                        EmitExpression.CONVERTER,
                        "next")),
                NativeBinding.of(
                    "instruction.xor_i",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        (left, right) ->
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
                                return block.xor_i(
                                    left.render(block, functions, captures, parameters, values),
                                    right.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "left",
                        EmitExpression.CONVERTER,
                        "right")),
                NativeBinding.of(
                    "instruction.ztoa",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ztoa(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg")),
                NativeBinding.of(
                    "instruction.ztos",
                    NativeBinding.function(
                        NativeBinding.asProxy(),
                        value ->
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
                                return block.ztos(
                                    value.render(block, functions, captures, parameters, values));
                              }
                            },
                        EmitExpression.CONVERTER,
                        "arg"))),
            Stream.of(KwsType.values())
                .map(
                    t ->
                        NativeBinding.of(
                            "type." + t.name().toLowerCase(),
                            Any.of(
                                Frame.proxyOf(
                                    "emit type " + t.name(), "emit", t, Stream.empty())))),
            Stream.of(EmitFunctionType.values())
                .map(
                    t ->
                        NativeBinding.of(
                            "function." + t.name().toLowerCase(),
                            EmitDefineFunction.definition(t))))
        .flatMap(Function.identity());
  }

  private static NativeBinding blockDefinition(String name, Definition value) {
    return NativeBinding.of(name, AttributeSource.of(Attribute.of(BLOCK_ATTRIBUTE_NAME, value)));
  }

  static <T extends EmitBaseGenerator> Stream<LookupAssistant<T>> emitterLookups() {
    return Stream.concat(
        lookups(),
        Stream.of(
            LookupAssistant.find(
                AnyConverter.asString(false), (e, f) -> e.fileName = f, "file_name"),
            LookupAssistant.find(
                AnyConverter.frameOf(
                    AnyConverter.asProxy(
                        EmitDefineFunction.class,
                        false,
                        SpecialLocation.library("emit").attributes("functions").invoked()),
                    false),
                (e1, f1) -> e1.functions = f1)));
  }

  protected String fileName;
  protected Map<Name, EmitDefineFunction> functions;

  protected abstract void finish(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      KwsRenderer renderer,
      ErrorCollector errorCollector)
      throws Exception;

  @Override
  public final void run(Future<Any> future, SourceReference sourceReference, Context context) {
    final var blocks = new ConcurrentLinkedDeque<EmitBlock>();
    final var finish =
        new Runnable() {
          private final AtomicInteger interlock = new AtomicInteger(functions.size() + 1);

          public void run() {
            if (interlock.decrementAndGet() == 0) {
              try {
                finish(
                    future,
                    sourceReference,
                    context,
                    new KwsRenderer() {
                      @Override
                      public <
                              V,
                              B extends KwsBlock<V, B, D>,
                              D extends KwsDispatch<B, V>,
                              F extends KwsFunction<V, B, D>>
                          void render(KwsFactory<V, B, D, F> factory) {
                        final var functionDefinitions = new TreeMap<Name, F>();
                        final var blockDefinitions = new HashMap<Pair<Name, Name>, B>();
                        final var rootFunction =
                            factory.createFile(
                                new SourceLocation(fileName, 0, 0, 0, 0), "Emitted File", "entry");
                        functionDefinitions.put(Name.of(""), rootFunction);
                        createBlocks(
                            rootFunction,
                            (blockName, block) ->
                                blockDefinitions.put(Pair.of(Name.of(""), blockName), block));
                        for (final var function : functions.entrySet()) {
                          functionDefinitions.put(
                              function.getKey(),
                              function
                                  .getValue()
                                  .create(
                                      factory,
                                      function.getKey(),
                                      (blockName, block) ->
                                          blockDefinitions.put(
                                              Pair.of(function.getKey(), blockName), block)));
                        }
                        final var captures = new HashMap<Pair<Name, Name>, V>();
                        final var parameters = new HashMap<EmitExpression.BlockParameter, V>();
                        final var values = new HashMap<Integer, V>();
                        for (final var block : blocks) {
                          block.render(
                              functionName -> {
                                final var foundFunction = functionDefinitions.get(functionName);
                                if (foundFunction == null) {
                                  future.error(
                                      sourceReference,
                                      String.format("Cannot find function %s.", functionName));
                                }
                                return foundFunction;
                              },
                              (functionName, blockName) -> {
                                final var foundBlock =
                                    blockDefinitions.get(Pair.of(functionName, blockName));
                                if (foundBlock == null) {
                                  future.error(
                                      sourceReference,
                                      String.format(
                                          "Cannot find block %s in function %s.",
                                          blockName, functionName));
                                }
                                return foundBlock;
                              },
                              captures,
                              parameters,
                              values);
                        }
                      }
                    },
                    new ErrorCollector() {
                      @Override
                      public void emitConflict(
                          String error, Stream<Pair<SourceLocation, String>> locations) {
                        future.error(sourceReference, error);
                      }

                      @Override
                      public void emitError(SourceLocation location, String error) {
                        future.error(sourceReference, error);
                      }
                    });
              } catch (Exception e) {
                future.error(sourceReference, e.getMessage());
              }
            }
          }
        };
    for (final var entry : functions.entrySet()) {
      entry
          .getValue()
          .blocks(entry.getKey(), future, sourceReference, context, blocks::add, finish);
    }
    blocks(Name.of(""), future, sourceReference, context, blocks::add, finish);
  }
}
