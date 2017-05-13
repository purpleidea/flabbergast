package flabbergast.interop;

import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.*;
import flabbergast.lang.Name;
import java.util.stream.Stream;

enum EmitFunctionType {
  ACCUMULATOR("context", "previous") {
    @Override
    <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
        F create(
            KwsFactory<V, B, D, F> factory,
            SourceLocation location,
            String name,
            Stream<KwsType> captures) {
      return factory.createAccumulator(location, name, false, "entry", captures);
    }
  },
  COLLECTOR("context", "root") {
    @Override
    <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
        F create(
            KwsFactory<V, B, D, F> factory,
            SourceLocation location,
            String name,
            Stream<KwsType> captures) {
      return factory.createCollector(location, name, false, "entry", captures);
    }
  },
  DEFINITION("context") {
    @Override
    <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
        F create(
            KwsFactory<V, B, D, F> factory,
            SourceLocation location,
            String name,
            Stream<KwsType> captures) {
      return factory.createDefinition(location, name, false, "entry", captures);
    }
  },
  DISTRIBUTOR("context") {
    @Override
    <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
        F create(
            KwsFactory<V, B, D, F> factory,
            SourceLocation location,
            String name,
            Stream<KwsType> captures) {
      return factory.createDistributor(location, name, false, "entry", captures);
    }
  },
  OVERRIDE("context", "original") {
    @Override
    <V, B extends KwsBlock<V, B, D>, D extends KwsDispatch<B, V>, F extends KwsFunction<V, B, D>>
        F create(
            KwsFactory<V, B, D, F> factory,
            SourceLocation location,
            String name,
            Stream<KwsType> captures) {
      return factory.createOverride(location, name, false, "entry", captures);
    }
  };

  private final String[] entryNames;

  EmitFunctionType(String... entryNames) {
    this.entryNames = entryNames;
  }

  abstract <
          V,
          B extends KwsBlock<V, B, D>,
          D extends KwsDispatch<B, V>,
          F extends KwsFunction<V, B, D>>
      F create(
          KwsFactory<V, B, D, F> factory,
          SourceLocation location,
          String name,
          Stream<KwsType> captures);

  public Stream<Name> entryNames() {
    return Stream.of(entryNames).map(Name::of);
  }
}
