package flabbergast.lang;

import flabbergast.util.Pair;
import java.util.List;

/**
 * A step in the resolution of all definitions, overrides and redefinitions in a frame or template
 */
public abstract class Attribute {

  static final class CollectorAttribute extends Attribute {

    private final CollectorDefinition collector;
    private final List<Pair<SourceReference, Context>> items;
    private final Name name;

    public CollectorAttribute(
        Name name, CollectorDefinition collector, List<Pair<SourceReference, Context>> items) {
      this.name = name;
      this.collector = collector;
      this.items = items;
    }

    @Override
    Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
      return batch.launch(
          future ->
              collector.invoke(
                  future,
                  sourceReference,
                  context,
                  new Fricassee() {

                    @Override
                    Context context() {
                      return context;
                    }

                    @Override
                    public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
                      launch(items, 0, thunkerator);
                    }
                  }));
    }

    @Override
    public Name name() {
      return name;
    }

    @Override
    Attribute override(Attribute definitionProcessor) {
      return this;
    }
  }

  /** Prepare a definition for instantiation in a frame or template. */
  private static final class DefinitionAttribute extends Attribute {
    private final Name name;
    private final Definition value;

    DefinitionAttribute(Name name, Definition value) {
      this.name = name;
      this.value = value;
    }

    @Override
    Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
      return batch.launch(future -> value.invoke(future, sourceReference, context));
    }

    @Override
    public Name name() {
      return name;
    }

    @Override
    Attribute override(Attribute originalAttribute) {
      return this;
    }
  }

  private static final class OverrideAttribute extends Attribute {
    private final Attribute inner;
    private final Name name;
    private final OverrideDefinition outer;

    public OverrideAttribute(Name name, OverrideDefinition outer, Attribute inner) {
      this.name = name;
      this.outer = outer;
      this.inner = inner;
    }

    @Override
    boolean isPublic() {
      return inner == null || inner.isPublic();
    }

    @Override
    Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
      if (inner == null) {
        return batch.launch(
            future ->
                () ->
                    future.error(
                        sourceReference,
                        String.format("Attempt to override non-existent attribute “%s”.", name)));
      }
      return batch.launch(
          future ->
              () ->
                  future
                      .real()
                      .await(
                          inner.launch(batch, sourceReference, context),
                          initialValue ->
                              outer.invoke(future, sourceReference, context, initialValue)));
    }

    @Override
    public Name name() {
      return inner.name();
    }

    @Override
    Attribute override(Attribute originalAttribute) {
      return new OverrideAttribute(
          name, outer, inner == null ? originalAttribute : inner.override(originalAttribute));
    }
  }

  private static final class PrivateAttribute extends Attribute {

    private final Attribute inner;

    public PrivateAttribute(Attribute inner) {
      this.inner = inner;
    }

    @Override
    boolean isPublic() {
      return false;
    }

    @Override
    public Promise<Any> launch(
        LaunchBatch batch, SourceReference sourceReference, Context context) {
      return inner.launch(batch, sourceReference, context);
    }

    @Override
    public Name name() {
      return inner.name();
    }

    @Override
    public Attribute override(Attribute originalAttribute) {
      return inner.override(originalAttribute);
    }
  }

  /** Create an attribute that drops a previously defined attribute */
  public static Attribute drop(Name name) {
    return new Attribute() {

      @Override
      Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
        return null;
      }

      @Override
      public Name name() {
        return name;
      }

      @Override
      Attribute override(Attribute originalAttribute) {
        return null;
      }
    };
  }

  /** Create an attribute that drops a previously defined attribute */
  public static Attribute drop(String name) {
    return drop(Name.of(name));
  }

  /** Create an attribute that drops a previously defined attribute */
  public static Attribute drop(Str name) {
    return drop(name.toString());
  }

  /** Create an attribute that uses a predefined value */
  public static Attribute of(long ordinal, Any value) {
    return of(Name.of(ordinal), value);
  }

  /** Create an attribute that computes a value when the frame is instantiated */
  public static Attribute of(long ordinal, Definition value) {
    return new DefinitionAttribute(Name.of(ordinal), value);
  }

  /** Create an attribute that uses a predefined value */
  public static Attribute of(Name name, Any value) {
    return new Attribute() {

      @Override
      Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
        return value;
      }

      @Override
      public Name name() {
        return name;
      }

      @Override
      Attribute override(Attribute originalAttribute) {
        return this;
      }
    };
  }

  /** Create an attribute that computes a value when the frame is instantiated */
  public static Attribute of(Name name, Definition value) {
    return new DefinitionAttribute(name, value);
  }

  /** Create an attribute that overrides an existing a value when the frame is instantiated */
  public static Attribute of(Name name, OverrideDefinition value) {
    return new OverrideAttribute(name, value, null);
  }

  /** Create an attribute that uses a predefined value */
  public static Attribute of(String name, Any value) {
    return of(Name.of(name), value);
  }

  /** Create an attribute that computes a value when the frame is instantiated */
  public static Attribute of(String name, Definition value) {
    return of(Name.of(name), value);
  }

  /** Create an attribute that overrides an existing a value when the frame is instantiated */
  public static Attribute of(String name, OverrideDefinition override) {
    return of(Name.of(name), override);
  }

  /** Create an attribute that uses a predefined value */
  public static Attribute of(Str name, Any value) {
    return of(name.toString(), value);
  }

  /** Create an attribute that computes a value when the frame is instantiated */
  public static Attribute of(Str name, Definition value) {
    return of(name.toString(), value);
  }

  /** Create an attribute that overrides an existing a value when the frame is instantiated */
  public static Attribute of(Str name, OverrideDefinition value) {
    return of(Name.of(name), value);
  }

  /** Create an attribute for an in-progress computation */
  public static Attribute of(Name name, Promise<Any> value) {
    return new Attribute() {
      @Override
      Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context) {
        return value;
      }

      @Override
      public Name name() {
        return name;
      }

      @Override
      Attribute override(Attribute originalAttribute) {
        return this;
      }
    };
  }

  /**
   * Create an attribute that will evaluate to an error stating that the attribute must be
   * overridden
   */
  public static Attribute require(String name) {
    return new DefinitionAttribute(
        Name.of(name), Definition.error(String.format("Attribute “%s” must be overridden.", name)));
  }

  /**
   * Create an attribute that will evaluate to an error stating that the attribute must be
   * overridden
   */
  public static Attribute require(Str name) {
    return require(name.toString());
  }

  private Attribute() {}

  boolean isPublic() {
    return true;
  }

  /**
   * Instantiate a future in the provided context by applying an override to the existing future.
   */
  abstract Promise<Any> launch(LaunchBatch batch, SourceReference sourceReference, Context context);

  /** Get the name of an attribute */
  public abstract Name name();

  /** Perform an override on an existing definition. */
  abstract Attribute override(Attribute originalAttribute);

  /**
   * Create a new attribute that is private instead of public
   *
   * <p>This has no effect on {@link #drop(Name)} attributes
   */
  public final Attribute reduceVisibility() {
    return new PrivateAttribute(this);
  }
}
