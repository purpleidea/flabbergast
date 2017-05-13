package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.I;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

interface InputArgument {
  void access(JvmBlock block, int index, ErrorCollector errorCollector);

  static <T> InputArgument of(Streamable<T> items, Function<? super T, LoadableValue> converter) {
    return new InputArgument() {
      private final List<LoadableValue> results =
          items.stream().map(converter).collect(Collectors.toList());

      @Override
      public void access(JvmBlock block, int index, ErrorCollector errorCollector) {
        // Do nothing.
      }

      @Override
      public void typeCheck(
          InputParameterType input, SourceLocation location, ErrorCollector collector) {
        input.typeCheck(results.stream().map(LoadableValue::type), location, collector);
      }

      @Override
      public void load(JvmBlock block, InputParameterType parameterType) {
        parameterType.load(block, results::stream);
      }
    };
  }

  static InputArgument of(Streamable<Value> items) {
    return new InputArgument() {

      @Override
      public void access(JvmBlock block, int index, ErrorCollector errorCollector) {
        items.stream().forEach(item -> item.access(block, index, errorCollector));
      }

      @Override
      public void typeCheck(
          InputParameterType input, SourceLocation location, ErrorCollector collector) {
        input.typeCheck(items.stream().map(Value::type), location, collector);
      }

      @Override
      public void load(JvmBlock block, InputParameterType parameterType) {
        parameterType.load(block, () -> items.stream().map(x -> x));
      }
    };
  }

  static InputArgument of(long value) {
    return new InputArgument() {
      @Override
      public void access(JvmBlock block, int index, ErrorCollector errorCollector) {
        // Do nothing.
      }

      @Override
      public void typeCheck(
          InputParameterType input, SourceLocation location, ErrorCollector collector) {
        input.typeCheck(I, location, collector);
      }

      @Override
      public void load(JvmBlock block, InputParameterType parameterType) {
        block.generator().push(value);
      }
    };
  }

  void typeCheck(InputParameterType input, SourceLocation location, ErrorCollector collector);

  void load(JvmBlock block, InputParameterType parameterType);
}
