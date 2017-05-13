package flabbergast.interop;

import flabbergast.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class UtilTraceRenderer implements SourceReferenceRenderer<NameSource> {

  static final Definition DEFINITION =
      (future, sourceReference, context) ->
          () -> {
            final var renderer = new UtilTraceRenderer(NameSource.EMPTY.add("value"));
            sourceReference.write(renderer);
            future.complete(
                Any.of(
                    Frame.create(
                        future,
                        context.self().source().caller(),
                        context,
                        AttributeSource.of(renderer.attributes))));
          };
  private final List<Attribute> attributes = new ArrayList<>();
  private long id;
  private final NameSource rootName;

  private UtilTraceRenderer(NameSource rootName) {
    this.rootName = rootName;
  }

  @Override
  public void backReference(NameSource reference) {
    attributes.add(
        Attribute.of(
            next(),
            Template.instantiate(
                AttributeSource.of(Attribute.of("to", LookupHandler.CONTEXTUAL.create(reference))),
                "execution trace renderer",
                "trace",
                "backreference")));
  }

  @Override
  public NameSource junction(
      boolean terminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message,
      Consumer<SourceReferenceRenderer<NameSource>> branch) {
    final var name = next();
    final var fullName = rootName.add(name);
    final var branchRenderer = new UtilTraceRenderer(fullName.add("branch"));
    branch.accept(branchRenderer);

    attributes.add(
        Attribute.of(
            name,
            Template.instantiate(
                AttributeSource.of(
                    Attribute.of("filename", Any.of(filename)),
                    Attribute.of("start_line", Any.of(startLine)),
                    Attribute.of("start_column", Any.of(startColumn)),
                    Attribute.of("end_line", Any.of(endLine)),
                    Attribute.of("end_column", Any.of(endColumn)),
                    Attribute.of("message", Any.of(message)),
                    Attribute.of("branch", branchRenderer.toFrame())),
                "execution trace renderer",
                "trace",
                "junction")));
    return fullName;
  }

  @Override
  public NameSource jvm(boolean terminal, StackWalker.StackFrame frame) {
    final var name = next();
    final var fullName = rootName.add(name);
    attributes.add(
        Attribute.of(
            name,
            Template.instantiate(
                AttributeSource.of(
                    Attribute.of("filename", Any.of(frame.getFileName())),
                    Attribute.of(
                        "line",
                        frame.getLineNumber() < 0 ? Any.NULL : Any.of(frame.getLineNumber())),
                    Attribute.of("class", Any.of(frame.getClassName())),
                    Attribute.of("method", Any.of(frame.getMethodName())),
                    Attribute.of("method_type", Any.of(frame.getMethodType().toString())),
                    Attribute.of("bytecode_index", Any.of(frame.getByteCodeIndex()))),
                "execution trace renderer",
                "trace",
                "jvm")));
    return fullName;
  }

  private Name next() {
    return Name.of(++id);
  }

  @Override
  public NameSource normal(
      boolean terminal,
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      String message) {

    final var name = next();
    final var fullName = rootName.add(name);
    attributes.add(
        Attribute.of(
            name,
            Template.instantiate(
                AttributeSource.of(
                    Attribute.of("filename", Any.of(filename)),
                    Attribute.of("start_line", Any.of(startLine)),
                    Attribute.of("start_column", Any.of(startColumn)),
                    Attribute.of("end_line", Any.of(endLine)),
                    Attribute.of("end_column", Any.of(endColumn)),
                    Attribute.of("message", Any.of(message))),
                "execution trace renderer",
                "trace",
                "normal")));
    return fullName;
  }

  @Override
  public NameSource special(boolean terminal, String message) {
    final var name = next();
    final var fullName = rootName.add(name);
    attributes.add(
        Attribute.of(
            name,
            Template.instantiate(
                AttributeSource.of(Attribute.of("message", Any.of(message))),
                "execution trace renderer",
                "trace",
                "special")));
    return fullName;
  }

  @Override
  public NameSource specialJunction(
      boolean terminal, String message, Consumer<SourceReferenceRenderer<NameSource>> branch) {
    final var name = next();
    final var fullName = rootName.add(name);

    final var branchRenderer = new UtilTraceRenderer(fullName.add("branch"));
    branch.accept(branchRenderer);

    attributes.add(
        Attribute.of(
            name,
            Template.instantiate(
                AttributeSource.of(
                    Attribute.of("message", Any.of(message)),
                    Attribute.of("branch", branchRenderer.toFrame())),
                "execution trace renderer",
                "trace",
                "special_junction")));
    return fullName;
  }

  private Definition toFrame() {
    return Frame.define(AttributeSource.of(attributes));
  }
}
