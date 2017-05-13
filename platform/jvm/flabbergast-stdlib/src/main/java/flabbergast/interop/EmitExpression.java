package flabbergast.interop;

import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsDispatch;
import flabbergast.compiler.kws.KwsFunction;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.ConversionOperation;
import flabbergast.lang.Name;
import flabbergast.lang.SpecialLocation;
import flabbergast.util.Pair;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

interface EmitExpression {
  AnyConverter<EmitExpression> CONVERTER =
      AnyConverter.of(
          AnyConverter.convertBool(
              value ->
                  ConversionOperation.succeed(
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
                          return value ? block.max_z() : block.min_z();
                        }
                      })),
          AnyConverter.convertFloat(
              value ->
                  ConversionOperation.succeed(
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
                          return block.f(value);
                        }
                      })),
          AnyConverter.convertInt(
              value ->
                  ConversionOperation.succeed(
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
                          return block.i(value);
                        }
                      })),
          AnyConverter.convertStr(
              value ->
                  ConversionOperation.succeed(
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
                          return block.s(value.toString());
                        }
                      })),
          AnyConverter.convertProxyFrame(
              EmitExpression.class,
              SpecialLocation.library("emit").attributes("instructions").any().instantiated()));

  AtomicInteger ID_GENERATOR = new AtomicInteger();

  final class BlockParameter {
    private final Name function;
    private final Name block;
    private final Name parameters;

    public BlockParameter(Name function, Name block, Name parameters) {
      this.function = function;
      this.block = block;
      this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BlockParameter that = (BlockParameter) o;
      return function.equals(that.function)
          && block.equals(that.block)
          && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
      return Objects.hash(function, block, parameters);
    }
  }

  <F extends KwsFunction<V, B, D>, V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>>
      V render(
          B block,
          Function<Name, F> functions,
          Map<Pair<Name, Name>, V> captures,
          Map<BlockParameter, V> parameters,
          Map<Integer, V> values);
}
