package flabbergast.lang;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A Flabbergast Template, holding functions for computing attributes. */
public final class Template extends AttributeSource {

  /**
   * Create a definition that performs amending of a template with the attributes provided.
   *
   * <p>Essentially, this is a programmatic way of generating code equivalent to <tt>Template
   * name0.name1 { ... }</tt> where the attributes are provided.
   *
   * @param source the attributes to amend on to the template
   * @param message the special operation doing the instantiation
   * @param names the names in the lookup that should be performed
   */
  public static Definition amend(AttributeSource source, String message, String... names) {
    final var lookup = LookupHandler.CONTEXTUAL.create(names);

    return (future, sourceReference, context) ->
        () ->
            future.launch(
                lookup,
                sourceReference,
                context,
                new WhinyAnyConsumer() {
                  @Override
                  public void accept(Template template) {
                    future.complete(
                        Any.of(
                            new Template(
                                sourceReference.specialJunction(message, template.source()),
                                context,
                                Stream.empty(),
                                template,
                                source)));
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference,
                        String.format(
                            "Expected Template for “%s” in template amending, but got %s.",
                            String.join(".", names), type));
                  }
                });
  }

  /**
   * Create a definition that performs instantiation of a function-like template with the attributes
   * provided.
   *
   * <p>Essentially, this is a programmatic way of generating code equivalent to <tt>name0.name1 (
   * ... )</tt> where the attributes are provided.
   *
   * @param source the attributes to amend on to the template before instantiation
   * @param message the special operation doing the instantiation
   * @param names the names in the lookup that should be performed
   */
  public static Definition function(AttributeSource source, String message, String... names) {
    final var lookup = LookupHandler.CONTEXTUAL.create(names);

    return (future, sourceReference, context) ->
        () ->
            future.launch(
                lookup,
                sourceReference,
                context,
                new WhinyAnyConsumer() {
                  @Override
                  public void accept(Template template) {
                    NameSource.EMPTY
                        .add("value")
                        .collect(
                            future,
                            sourceReference,
                            Context.EMPTY.prepend(
                                Frame.create(
                                    future,
                                    sourceReference.specialJunction(message, template.source()),
                                    Context.EMPTY,
                                    template,
                                    source)),
                            LookupHandler.CONTEXTUAL,
                            future::complete);
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference,
                        String.format(
                            "Expected Template for “%s” in function-like template instantiation, but got %s.",
                            String.join(".", names), type));
                  }
                });
  }

  /**
   * Create a definition that performs instantiation of a template with the attributes provided.
   *
   * <p>Essentially, this is a programmatic way of generating code equivalent to <tt>name0.name1 {
   * ... }</tt> where the attributes are provided.
   *
   * @param source the attributes to amend on to the template before instantiation
   * @param message the special operation doing the instantiation
   * @param names the names in the lookup that should be performed
   */
  public static Definition instantiate(AttributeSource source, String message, String... names) {
    final var lookup = LookupHandler.CONTEXTUAL.create(names);
    return (future, sourceReference, context) ->
        () ->
            future.launch(
                lookup,
                sourceReference,
                context,
                new WhinyAnyConsumer() {
                  @Override
                  public void accept(Template template) {
                    future.complete(
                        Any.of(
                            Frame.create(
                                future,
                                sourceReference.specialJunction(message, template.source()),
                                context,
                                template,
                                source)));
                  }

                  @Override
                  protected void fail(String type) {
                    future.error(
                        sourceReference,
                        String.format(
                            "Expected Template for “%s” in template instantiation, but got %s.",
                            String.join(".", names), type));
                  }
                });
  }

  private final Context context;
  private final List<Attribute> definitions;
  private final Set<Name> gatherers;
  private final boolean isFunctionLike;
  private final SourceReference sourceReference;

  /**
   * Create a new template
   *
   * @param sourceReference the execution trace for the template's definition; if amending an
   *     existing template, use {@link #joinSourceReference(String, int, int, int, int,
   *     SourceReference)} against that template
   * @param context the context in which the template is defined
   * @param sources the attributes that are defined in this template
   */
  public Template(SourceReference sourceReference, Context context, AttributeSource... sources) {
    this(sourceReference, context, Stream.empty(), sources);
  }
  /**
   * Create a new template
   *
   * @param sourceReference the execution trace for the template's definition; if amending an
   *     existing template, use {@link #joinSourceReference(String, int, int, int, int,
   *     SourceReference)} against that template
   * @param context the context in which the template is defined
   * @param sources the attributes that are defined in this template
   */
  public Template(
      SourceReference sourceReference,
      Context context,
      Name[] gatherers,
      AttributeSource... sources) {
    this(sourceReference, context, Stream.of(gatherers), sources);
  }

  /**
   * Create a new template
   *
   * @param sourceReference the execution trace for the template's definition; if amending an
   *     existing template, use {@link #joinSourceReference(String, int, int, int, int,
   *     SourceReference)} against that template
   * @param context the context in which the template is defined
   * @param sources the attributes that are defined in this template
   */
  public Template(
      SourceReference sourceReference,
      Context context,
      Stream<Name> gatherers,
      AttributeSource... sources) {
    for (final var builder : sources) {
      context = builder.context(context);
    }
    this.sourceReference = sourceReference;
    this.context = context;
    this.gatherers =
        Stream.concat(gatherers, Stream.of(sources).flatMap(AttributeSource::gatherers))
            .collect(Collectors.toSet());
    final var check =
        new Consumer<Attribute>() {
          boolean isFunctionLike;

          @Override
          public void accept(Attribute attribute) {
            if (attribute
                .name()
                .apply(
                    new NameFunction<>() {
                      @Override
                      public Boolean apply(long ordinal) {
                        return false;
                      }

                      @Override
                      public Boolean apply(String name) {
                        return name.equals("value");
                      }
                    })) {
              isFunctionLike = true;
            }
          }
        };
    definitions =
        AttributeSource.squash(Stream.of(sources)).peek(check).collect(Collectors.toList());
    isFunctionLike = check.isFunctionLike;
  }

  /**
   * Create a definition that performs amending of a template with the attributes provided.
   *
   * @param source the attributes to amend on to the template
   * @param message the special operation doing the instantiation
   */
  public Definition amendWith(AttributeSource source, String message) {
    return (future, sourceReference, context) ->
        () ->
            future.complete(
                Any.of(
                    new Template(
                        sourceReference.specialJunction(message, source()),
                        context,
                        Stream.empty(),
                        this,
                        source)));
  }

  /** List all attributes in the template */
  @Override
  public Stream<Attribute> attributes() {
    return definitions.stream();
  }

  /** The frame designated <tt>This</tt> when this template was created. */
  public Frame container() {
    return context.self();
  }

  @Override
  Context context(Context context) {
    return context.append(this.context);
  }

  /** The gathers that have been attached to this template or its ancestors */
  @Override
  Stream<Name> gatherers() {
    return gatherers.stream();
  }

  /**
   * Create a definition that performs instantiation of this template with the attributes provided
   * as overrides.
   *
   * @param message the name of the special operation
   * @param source the attributes to amend on to the template before instantiation
   */
  public Definition instantiateWith(String message, AttributeSource source) {

    return (future, sourceReference, context) ->
        () ->
            future.complete(
                Any.of(
                    Frame.create(
                        future,
                        sourceReference.specialJunction(message, source()),
                        context,
                        this,
                        source)));
  }

  /** Checks if this template has a <tt>value</tt> attribute */
  public boolean isFunctionLike() {
    return isFunctionLike;
  }

  /** Create a source reference having amended this template at the location specified */
  public SourceReference joinSourceReference(
      String filename,
      int startLine,
      int startColumn,
      int endLine,
      int endColumn,
      SourceReference caller) {
    return caller.junction(
        "amend template", filename, startLine, startColumn, endLine, endColumn, source());
  }

  /** The stack trace at the time of creation. */
  public SourceReference source() {
    return sourceReference;
  }
}
