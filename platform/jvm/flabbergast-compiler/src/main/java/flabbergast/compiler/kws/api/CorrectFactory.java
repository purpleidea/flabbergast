package flabbergast.compiler.kws.api;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsFactory;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import java.util.stream.Stream;

public class CorrectFactory
    implements KwsFactory<CorrectFunction, CorrectBlock, CorrectDispatch, CorrectFunction> {

  private final ErrorCollector collector;
  private final boolean isDefinition;
  private boolean hasFile;

  public CorrectFactory(ErrorCollector collector, boolean isDefinition) {
    super();
    this.collector = collector;
    this.isDefinition = isDefinition;
  }

  @Override
  public CorrectFunction createAccumulator(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new CorrectFunction(
        location,
        name,
        entryBlockName,
        (int) captures.count(),
        2,
        ResultType.ACCUMULATOR,
        collector);
  }

  @Override
  public CorrectFunction createCollector(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new CorrectFunction(
        location, name, entryBlockName, (int) captures.count(), 2, ResultType.ANY, collector);
  }

  @Override
  public CorrectFunction createDefinition(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new CorrectFunction(
        location, name, entryBlockName, (int) captures.count(), 1, ResultType.ANY, collector);
  }

  @Override
  public CorrectFunction createDistributor(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new CorrectFunction(
        location, name, entryBlockName, (int) captures.count(), 1, ResultType.FRICASSEE, collector);
  }

  @Override
  public CorrectFunction createFile(SourceLocation location, String name, String entryBlockName) {
    if (hasFile) {
      collector.emitError(location, "File function has already been created.");
    }
    hasFile = true;
    return new CorrectFunction(
        location, name, entryBlockName, 0, isDefinition ? 1 : 0, ResultType.ANY, collector);
  }

  @Override
  public CorrectFunction createOverride(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    return new CorrectFunction(
        location, name, entryBlockName, (int) captures.count(), 2, ResultType.ANY, collector);
  }
}
