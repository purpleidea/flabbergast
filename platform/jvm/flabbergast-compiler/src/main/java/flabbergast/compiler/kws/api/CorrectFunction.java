package flabbergast.compiler.kws.api;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsFunction;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CorrectFunction
    implements KwsFunction<CorrectFunction, CorrectBlock, CorrectDispatch> {

  private final List<CorrectBlock> blocks = new ArrayList<>();
  private final int captures;
  private final ErrorCollector collector;
  private final CorrectBlock entry;
  private final SourceLocation location;
  private final String name;
  private final ResultType resultType;

  public CorrectFunction(
      SourceLocation location,
      String name,
      String entryBlockName,
      int captures,
      int parameters,
      ResultType resultType,
      ErrorCollector collector) {
    this.location = location;
    this.name = name;
    this.captures = captures;
    this.resultType = resultType;
    this.collector = collector;
    entry = new CorrectBlock(this, entryBlockName, parameters, collector);
    blocks.add(entry);
  }

  @Override
  public CorrectFunction access(CorrectBlock block, Stream<CorrectFunction> captures) {
    block.checkAlive(name);
    block.check(name, captures);
    return block.owner();
  }

  @Override
  public CorrectFunction capture(int i) {
    return this;
  }

  @Override
  public int captures() {
    return captures;
  }

  @Override
  public CorrectBlock createBlock(String name, Stream<KwsType> parameterTypes) {
    final var block = new CorrectBlock(this, name, (int) parameterTypes.count(), collector);
    blocks.add(block);
    return block;
  }

  @Override
  public CorrectBlock entryBlock() {
    return entry;
  }

  @Override
  public void finish() {
    blocks
        .stream()
        .filter(CorrectBlock::isAlive)
        .forEach(
            b ->
                collector.emitError(
                    location, String.format("Block “%s” has no terminal instruction.", b.name())));
  }

  public String name() {
    return name;
  }

  @Override
  public ResultType result() {
    return resultType;
  }
}
