package flabbergast.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Hold a list of names for use in a {@link LookupHandler} */
public abstract class NameSource {
  /** An empty set of names. */
  public static final NameSource EMPTY =
      new NameSource() {

        @Override
        void populate(
            Future<?> future,
            SourceReference sourceReference,
            Consumer<Name> output,
            Runnable complete) {
          complete.run();
        }
      };

  private NameSource() {}

  /** Add a name for the type of a boxed value. */
  public final NameSource add(Any value) {
    return add(
        value.apply(
            new AnyFunction<String>() {
              @Override
              public String apply() {
                return "null";
              }

              @Override
              public String apply(boolean value) {
                return "bool";
              }

              @Override
              public String apply(byte[] value) {
                return "bin";
              }

              @Override
              public String apply(double value) {
                return "float";
              }

              @Override
              public String apply(Frame value) {
                return "frame";
              }

              @Override
              public String apply(long value) {
                return "int";
              }

              @Override
              public String apply(LookupHandler value) {
                return "lookup_handler";
              }

              @Override
              public String apply(Str value) {
                return "str";
              }

              @Override
              public String apply(Template value) {
                return "template";
              }
            }));
  }

  /**
   * Add all values in a frame as names.
   *
   * <p>All values in the frame must be an <tt>Int</tt> or a <tt>Str</tt> which is a valid
   * identifier.
   *
   * @param frame the frame to extract
   */
  public final NameSource add(Frame frame) {
    final var owner = this;
    return new NameSource() {

      @Override
      void populate(
          Future<?> future,
          SourceReference sourceReference,
          Consumer<Name> output,
          Runnable complete) {
        owner.populate(
            future,
            sourceReference,
            output,
            () ->
                frame.forAll(
                    future,
                    sourceReference,
                    new Frame.AttributeConsumer<Name>() {
                      @Override
                      public Name accept(Name attributeName, long ordinal, Any value) {
                        return value.apply(
                            new AnyFunction<>() {
                              @Override
                              public Name apply() {
                                fail("Null");
                                return null;
                              }

                              @Override
                              public Name apply(boolean value) {
                                fail("Bool");
                                return null;
                              }

                              @Override
                              public Name apply(byte[] value) {
                                fail("Bin");
                                return null;
                              }

                              @Override
                              public Name apply(double value) {
                                fail("Float");
                                return null;
                              }

                              @Override
                              public Name apply(Frame value) {
                                fail("Frame");
                                return null;
                              }

                              @Override
                              public Name apply(long value) {
                                return Name.of(value);
                              }

                              @Override
                              public Name apply(LookupHandler value) {
                                fail("LookupHandler");
                                return null;
                              }

                              @Override
                              public Name apply(Str value) {
                                return Name.of(value);
                              }

                              @Override
                              public Name apply(Template value) {
                                fail("Template");
                                return null;
                              }

                              private void fail(String type) {
                                future.error(
                                    sourceReference,
                                    String.format(
                                        "Expected Int or Str for “%s” (%d) in Flatten in Lookup, but got %s.",
                                        attributeName, ordinal, type));
                              }
                            });
                      }

                      @Override
                      public void complete(List<Name> results) {
                        for (final var name : results) {
                          if (name == null) {
                            return;
                          }
                          output.accept(name);
                        }
                        complete.run();
                      }

                      @Override
                      public String describe(Name name) {
                        return String.format("“%s” in Flatten in Lookup", name);
                      }
                    }));
      }
    };
  }

  /** Add an ordinal name. */
  public final NameSource add(long value) {
    return add(Name.of(value));
  }

  /** Add a name */
  public final NameSource add(Name value) {
    final var owner = this;
    return new NameSource() {

      @Override
      void populate(
          Future<?> future,
          SourceReference sourceReference,
          Consumer<Name> names,
          Runnable complete) {
        owner.populate(
            future,
            sourceReference,
            names,
            () -> {
              names.accept(value);
              complete.run();
            });
      }
    };
  }
  /** Add a literal name. */
  public final NameSource add(String name) {
    return add(Name.of(name));
  }

  /**
   * Add a string literal supplied by the user.
   *
   * <p>This first verifies that it is a valid identifier.
   *
   * @param name the user-provided name
   */
  public final NameSource add(Str name) {
    return add(Name.of(name));
  }

  /**
   * Collect all the names and perform a lookup
   *
   * @param future the future to use for evaluating and dynamic names and the lookup
   * @param sourceReference the calling source
   * @param context the context in which evaluation should be done
   * @param lookupHandler the lookup handler
   * @param consumer the consumer of the result of the lookup
   */
  public final void collect(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      LookupHandler lookupHandler,
      Consumer<? super Any> consumer) {
    collect(
        future,
        sourceReference,
        item ->
            new Lookup(lookupHandler, future, sourceReference, context, item, consumer).start());
  }
  /**
   * Collect all the names and perform a lookup
   *
   * @param future the future to use for evaluating and dynamic names and the lookup
   * @param sourceReference the calling source
   * @param context the context in which evaluation should be done
   * @param lookupHandler the lookup handler
   * @param consumer the consumer of the result of the lookup
   */
  public final void collect(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      LookupHandler lookupHandler,
      AnyConsumer consumer) {
    collect(future, sourceReference, context, lookupHandler, item -> item.accept(consumer));
  }

  /**
   * Collect all the names into an array for down-steam use
   *
   * @param future the future to use for evaluating any dynamic names
   * @param sourceReference the calling source
   * @param consumer the callback to invoke with the completed set of names
   */
  public final void collect(
      Future<?> future, SourceReference sourceReference, Consumer<Name[]> consumer) {
    final var collector = new ArrayList<Name>();
    populate(
        future,
        sourceReference,
        collector::add,
        () -> consumer.accept(collector.toArray(Name[]::new)));
  }

  abstract void populate(
      Future<?> future,
      SourceReference sourceReference,
      Consumer<Name> collector,
      Runnable complete);
}
