package flabbergast.compiler.kws.text;

import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsFactory;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TextFactory implements KwsFactory<Printable, TextBlock, TextDispatch, TextFunction> {
  private final Consumer<String> output;
  private final boolean isDefinition;

  public TextFactory(boolean isDefinition, Consumer<String> output) {
    super();
    this.output = output;
    this.isDefinition = isDefinition;
  }

  @Override
  public TextFunction createAccumulator(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new TextFunction(
        output,
        "KWSAccumulator",
        name,
        export,
        entryBlockName,
        Stream.of(KwsType.C, KwsType.A),
        captures,
        ResultType.ACCUMULATOR);
  }

  @Override
  public TextFunction createCollector(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new TextFunction(
        output,
        "KWSCollector",
        name,
        export,
        entryBlockName,
        Stream.of(KwsType.C, KwsType.E),
        captures,
        ResultType.ANY);
  }

  @Override
  public TextFunction createDefinition(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new TextFunction(
        output,
        "KWSDefinition",
        name,
        export,
        entryBlockName,
        Stream.of(KwsType.C),
        captures,
        ResultType.ANY);
  }

  @Override
  public TextFunction createDistributor(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new TextFunction(
        output,
        "KWSDistributor",
        name,
        export,
        entryBlockName,
        Stream.of(KwsType.C),
        captures,
        ResultType.FRICASSEE);
  }

  @Override
  public TextFunction createFile(SourceLocation location, String name, String entryBlockName) {
    return new TextFunction(
        output,
        isDefinition ? "Definition" : "Root",
        "",
        false,
        entryBlockName,
        isDefinition ? Stream.of(KwsType.C) : Stream.empty(),
        Stream.empty(),
        ResultType.ANY);
  }

  @Override
  public TextFunction createOverride(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new TextFunction(
        output,
        "KWSOverride",
        name,
        export,
        entryBlockName,
        Stream.of(KwsType.C, KwsType.A),
        captures,
        ResultType.ANY);
  }
}
