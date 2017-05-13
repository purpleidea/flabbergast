package flabbergast.compiler.kws.text;

import flabbergast.compiler.kws.KwsFunction;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import flabbergast.util.Numberer;
import flabbergast.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextFunction implements KwsFunction<Printable, TextBlock, TextDispatch> {

  private final List<String> blocks = new ArrayList<>();
  private final StringBuilder builder = new StringBuilder();
  private final int captures;
  private final TextBlock entryBlock;
  final String name;
  private final Consumer<String> output;
  private final ResultType resultType;

  public TextFunction(
      Consumer<String> output,
      String typeName,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> parameters,
      Stream<KwsType> captures,
      ResultType resultType) {
    this.output = output;
    this.name = name;
    this.resultType = resultType;
    if (export) {
      builder.append("Export ");
    }
    builder.append(typeName).append(" ").append(name).append("(");
    final Numberer<Integer, KwsType> numberer = Pair.number();
    builder.append(
        parameters
            .map(numberer)
            .map(p -> String.format("c%d:%s", p.first(), p.second().name().toLowerCase()))
            .collect(Collectors.joining(", ")));
    this.captures = numberer.size();
    builder.append(")").append(" {\n");
    entryBlock = new TextBlock(builder::append, entryBlockName, parameters);
  }

  @Override
  public Printable access(TextBlock block, Stream<Printable> captures) {
    return block.printr(name, captures.toArray(Printable[]::new));
  }

  @Override
  public Printable capture(int i) {
    return Printable.of("c" + i);
  }

  @Override
  public int captures() {
    return captures;
  }

  @Override
  public TextBlock createBlock(String name, Stream<KwsType> parameterTypes) {
    return new TextBlock(blocks::add, name, parameterTypes);
  }

  @Override
  public TextBlock entryBlock() {
    return entryBlock;
  }

  @Override
  public void finish() {
    blocks.forEach(builder::append);
    builder.append("}\n");
    output.accept(builder.toString());
  }

  @Override
  public ResultType result() {
    return resultType;
  }
}
