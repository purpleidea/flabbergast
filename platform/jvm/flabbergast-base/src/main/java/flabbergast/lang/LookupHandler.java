package flabbergast.lang;

import java.util.function.Predicate;
import java.util.stream.Stream;

/** Algorithm to resolve names in the Flabbergast language */
public final class LookupHandler {
  /** The lookup handler for contextual lookup, that is <tt>Contextual</tt> */
  public static final LookupHandler CONTEXTUAL =
      new LookupHandler(LookupExplorer.EXACT, LookupSelector.FIRST);

  /**
   * Processes the selected values through a fricassée operation
   *
   * @param name the name to bind the value to in the operation
   * @param context the context in which the fricassée should be evaluated
   * @param definition the definition that will resolve the lookup-driven fricassée chain to a value
   */
  public static LookupHandler fricassee(Str name, Context context, CollectorDefinition definition) {
    return new LookupHandler(
        LookupExplorer.EXACT, LookupSelector.fricassee(Name.of(name), context, definition));
  }

  private final LookupOperation<LookupSelector> selector;
  private final LookupOperation<LookupExplorer> explorer;
  /**
   * Create a new lookup handler
   *
   * @param explorer a supplier that creates the operation that finds values for names; will be
   *     instantiated once per context frame per lookup
   * @param selector the operation that selects the output from any complete columns; will be
   *     instantiated once per lookup
   */
  public LookupHandler(
      LookupOperation<LookupExplorer> explorer, LookupOperation<LookupSelector> selector) {
    this.selector = selector;
    this.explorer = explorer;
  }

  /** Get the selector of this lookup handler */
  public LookupOperation<LookupSelector> selector() {
    return selector;
  }

  /**
   * Create a lookup for a particular set of names to be performed in a context provided later.
   *
   * @param names the names to be resolved
   */
  public final Definition create(NameSource names) {
    return (future, sourceReference, context) ->
        () -> names.collect(future, sourceReference, context, this, future::complete);
  }

  /**
   * Create a lookup for a particular set of names to be performed in a context provided later.
   *
   * @param names the names to be resolved
   */
  public final Definition create(String... names) {
    if (names.length == 0) {
      return Definition.error("Missing names in lookup.");
    }
    final var attrNames = Stream.of(names).map(Name::of).toArray(Name[]::new);
    return (future, sourceReference, context) ->
        new Lookup(this, future, sourceReference, context, attrNames, future::complete)::start;
  }

  /** A description of this handler for debugging purposes */
  public final String description() {
    return explorer.description() + " selecting " + selector.description();
  }

  /** Get the explorer of this lookup handler */
  public LookupOperation<LookupExplorer> explorer() {
    return explorer;
  }

  /**
   * Create a new lookup handler which uses the explorer of a different lookup handler for a certain
   * number of names.
   *
   * @param count the number of names to use that explorer
   * @param other the lookup handler's explorer to use
   */
  public LookupHandler takeFirst(long count, LookupHandler other) {
    return takeFirst(count, other.explorer);
  }

  /**
   * Create a new lookup handler which uses a different explorer for a certain number of names.
   *
   * @param count the number of names to use that explorer
   * @param first the explorer to use for the first steps
   */
  public LookupHandler takeFirst(long count, LookupOperation<LookupExplorer> first) {
    return new LookupHandler(LookupExplorer.takeFirst(count, first, explorer), selector);
  }

  /**
   * Create a new lookup handler which uses a different lookup handler's explorer for all but a
   * certain number of items
   *
   * <p>This means that a variable number of items are used from the provided lookup handler follow
   * by a fixed number from this handler's explorer
   *
   * @param count the number of this explorer to use
   * @param other the lookup handler's explorer to use
   */
  public LookupHandler takeLast(long count, LookupHandler other) {
    return takeLast(count, other.explorer);
  }

  /**
   * Create a new lookup handler which uses a different explorer for all but a certain number of
   * items
   *
   * <p>This means that a variable number of items are used from the provided explorer follow by a
   * fixed number from this handler's explorer
   *
   * @param count the number of this explorer to use
   * @param explorer the explorer to use for the first steps
   */
  public LookupHandler takeLast(long count, LookupOperation<LookupExplorer> explorer) {
    return new LookupHandler(LookupExplorer.takeLast(count, explorer, this.explorer), selector);
  }

  /**
   * Create a new lookup handler which uses the explorer of a different lookup handler until a
   * certain name is hit
   *
   * @param predicate the test on the name
   * @param other the lookup handler's explorer to use
   */
  public LookupHandler takeUntil(Predicate<Name> predicate, LookupHandler other) {
    return takeUntil(predicate, other.explorer);
  }

  /**
   * Create a new lookup handler which uses a different explorer until a certain name is hit
   *
   * @param predicate the test on the name
   * @param explorer the explorer to use for this first steps
   */
  public LookupHandler takeUntil(
      Predicate<Name> predicate, LookupOperation<LookupExplorer> explorer) {
    return new LookupHandler(
        LookupExplorer.takeUntil(predicate, explorer, this.explorer), selector);
  }

  /** Show the description for the lookup handler */
  @Override
  public String toString() {
    return description();
  }

  /** Create a new lookup handler with the selector of a different lookup handler */
  public LookupHandler withSelector(LookupHandler other) {
    return new LookupHandler(explorer, other.selector);
  }

  /** Create a new lookup handler with a different selector */
  public LookupHandler withSelector(LookupOperation<LookupSelector> selector) {
    return new LookupHandler(explorer, selector);
  }

  /** Create a new lookup handler with the explorer of a different lookup handler */
  public LookupHandler withExplorer(LookupHandler other) {
    return new LookupHandler(other.explorer, selector);
  }

  /** Create a new lookup handler with a different explorer */
  public LookupHandler withExplorer(LookupOperation<LookupExplorer> explorer) {
    return new LookupHandler(explorer, selector);
  }
}
