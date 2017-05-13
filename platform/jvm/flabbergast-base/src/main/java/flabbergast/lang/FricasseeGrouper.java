package flabbergast.lang;

import flabbergast.lang.Fricassee.GroupConsumer;
import flabbergast.util.ConcurrentConsumer;
import flabbergast.util.ConcurrentMapper;
import flabbergast.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Fricassée <tt>Group</tt> operation */
public abstract class FricasseeGrouper {
  private static final class AdjacentGrouper<T> extends FricasseeGrouper {
    private final AnyBidiConverter<T> converter;
    private final Definition definition;
    private final Name name;

    private AdjacentGrouper(AnyBidiConverter<T> converter, Name name, Definition definition) {
      this.converter = converter;
      this.name = name;
      this.definition = definition;
    }

    @Override
    final void dummy(List<Attribute> attributes) {
      attributes.add(Attribute.of(name, Any.NULL));
    }

    @Override
    Function<AttributeSource, Fricassee.GroupConsumer> prepare(
        Map<Name, CollectorDefinition> collections,
        Function<AttributeSource, Fricassee.GroupConsumer> inner) {
      return source ->
          new Fricassee.GroupConsumer() {
            private Pair<T, Fricassee.GroupConsumer> current;

            @Override
            public void accept(
                Future<?> future,
                SourceReference sourceReference,
                Context context,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              future.launch(
                  definition,
                  sourceReference,
                  context,
                  converter.asConsumer(
                      future,
                      sourceReference,
                      TypeErrorLocation.ADJACENT,
                      value -> {
                        if (current == null) {
                          current =
                              Pair.of(
                                  value,
                                  inner.apply(
                                      attributes(
                                          future, callingSourceReference, callingContext, value)));
                          current
                              .second()
                              .accept(
                                  future,
                                  sourceReference,
                                  context,
                                  callingSourceReference,
                                  callingContext,
                                  complete);
                        } else if (current.first().equals(value)) {
                          current
                              .second()
                              .accept(
                                  future,
                                  sourceReference,
                                  context,
                                  callingSourceReference,
                                  callingContext,
                                  complete);
                        } else {
                          current
                              .second()
                              .finish(
                                  future,
                                  callingSourceReference,
                                  callingContext,
                                  () -> {
                                    current =
                                        Pair.of(
                                            value,
                                            inner.apply(
                                                attributes(
                                                    future,
                                                    callingSourceReference,
                                                    callingContext,
                                                    value)));
                                    current
                                        .second()
                                        .accept(
                                            future,
                                            sourceReference,
                                            context,
                                            callingSourceReference,
                                            callingContext,
                                            complete);
                                  });
                        }
                      }));
            }

            private AttributeSource attributes(
                Future<?> future,
                SourceReference callingSourceReference,
                Context callingContext,
                T value) {
              return Stream.concat(
                      Stream.of(
                          Attribute.of(
                              name,
                              converter.box(
                                  future, callingSourceReference, callingContext, value))),
                      source.attributes())
                  .collect(AttributeSource.toSource());
            }

            @Override
            public void finish(
                Future<?> future,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              if (current == null) {
                complete.run();
              } else {
                current.second().finish(future, callingSourceReference, callingContext, complete);
              }
            }
          };
    }
  }

  private static final class AlwaysIncludeGrouper<T>
      extends BaseByGrouper<T, ArrayList<Pair<SourceReference, Context>>> {

    private AlwaysIncludeGrouper(AnyBidiConverter<T> anyConverter, Name name, Definition clause) {
      super(anyConverter, name, clause);
    }

    @Override
    protected ArrayList<Pair<SourceReference, Context>> create() {
      return new ArrayList<>();
    }

    @Override
    protected void finish(
        Future<?> future,
        Map<T, Fricassee.GroupConsumer> groups,
        ArrayList<Pair<SourceReference, Context>> common,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete) {
      if (groups.isEmpty()) {
        complete.run();
        return;
      }
      ConcurrentConsumer.process(
          groups.values(),
          new ConcurrentConsumer<>() {
            @Override
            public void complete() {
              complete.run();
            }

            @Override
            public void process(GroupConsumer group, int index, Runnable complete) {
              ConcurrentConsumer.process(
                  common,
                  new ConcurrentConsumer<>() {
                    @Override
                    public void complete() {
                      complete.run();
                    }

                    @Override
                    public void process(
                        Pair<SourceReference, Context> item, int index, Runnable complete) {
                      group.accept(
                          future,
                          item.first(),
                          item.second(),
                          callingSourceReference,
                          callingContext,
                          complete);
                    }
                  });
            }
          });
    }

    @Override
    protected boolean store(
        SourceReference sourceReference,
        Context context,
        ArrayList<Pair<SourceReference, Context>> common,
        T value) {
      if (value == null) {
        common.add(Pair.of(sourceReference, context));
        return false;
      }
      return true;
    }
  }

  private abstract static class BaseByGrouper<T, S> extends FricasseeGrouper {
    private final AnyBidiConverter<T> anyConverter;
    private final Definition clause;
    private final Name name;

    private BaseByGrouper(AnyBidiConverter<T> anyConverter, Name name, Definition clause) {
      super();
      this.name = name;
      this.anyConverter = anyConverter;
      this.clause = clause;
    }

    protected abstract S create();

    @Override
    final void dummy(List<Attribute> attributes) {
      attributes.add(Attribute.of(name, Any.NULL));
    }

    protected abstract void finish(
        Future<?> future,
        Map<T, Fricassee.GroupConsumer> groups,
        S state,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete);

    @Override
    final Function<AttributeSource, Fricassee.GroupConsumer> prepare(
        Map<Name, CollectorDefinition> collections,
        Function<AttributeSource, Fricassee.GroupConsumer> inner) {
      return attributes ->
          new Fricassee.GroupConsumer() {
            private final Map<T, Fricassee.GroupConsumer> groups = new TreeMap<>(anyConverter);
            private final S state = create();

            @Override
            public void accept(
                Future<?> future,
                SourceReference sourceReference,
                Context context,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              future.launch(
                  clause,
                  sourceReference,
                  context,
                  AnyFunction.compose(
                      anyConverter.function(),
                      result ->
                          result.resolve(
                              future,
                              sourceReference,
                              anyConverter,
                              TypeErrorLocation.by(name),
                              value -> {
                                if (store(sourceReference, context, state, value)) {
                                  groups
                                      .computeIfAbsent(
                                          value,
                                          k ->
                                              inner.apply(
                                                  Stream.concat(
                                                          attributes.attributes(),
                                                          Stream.of(
                                                              Attribute.of(
                                                                  name,
                                                                  anyConverter.box(
                                                                      future,
                                                                      callingSourceReference,
                                                                      callingContext,
                                                                      k))))
                                                      .collect(AttributeSource.toSource())))
                                      .accept(
                                          future,
                                          sourceReference,
                                          context,
                                          callingSourceReference,
                                          callingContext,
                                          complete);
                                } else {
                                  complete.run();
                                }
                              })));
            }

            @Override
            public void finish(
                Future<?> future,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              BaseByGrouper.this.finish(
                  future, groups, state, callingSourceReference, callingContext, complete);
            }
          };
    }

    protected abstract boolean store(
        SourceReference sourceReference, Context context, S state, T value);
  }

  private static final class ByGrouper<T> extends BaseByGrouper<T, Void> {

    private ByGrouper(AnyBidiConverter<T> anyConverter, Name name, Definition clause) {
      super(anyConverter, name, clause);
    }

    @Override
    protected Void create() {
      return null;
    }

    @Override
    protected void finish(
        Future<?> future,
        Map<T, Fricassee.GroupConsumer> groups,
        Void state,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete) {
      if (groups.isEmpty()) {
        complete.run();
        return;
      }
      ConcurrentConsumer.process(
          groups.values(),
          new ConcurrentConsumer<>() {

            @Override
            public void complete() {
              complete.run();
            }

            @Override
            public void process(GroupConsumer group, int index, Runnable complete) {
              group.finish(future, callingSourceReference, callingContext, complete);
            }
          });
    }

    @Override
    protected boolean store(SourceReference sourceReference, Context context, Void state, T value) {
      return true;
    }
  }

  private static class CrosstabGrouper<T> extends FricasseeGrouper {
    private final AnyConverter<T> converter;
    private final Definition subgroup;

    public CrosstabGrouper(AnyConverter<T> converter, Definition subgroup) {
      this.subgroup = subgroup;
      this.converter = converter;
    }

    @Override
    final void dummy(List<Attribute> attributes) {
      // Do nothing
    }

    @Override
    Function<AttributeSource, Fricassee.GroupConsumer> prepare(
        Map<Name, CollectorDefinition> collections,
        Function<AttributeSource, Fricassee.GroupConsumer> inner) {
      return attributes ->
          new Fricassee.GroupConsumer() {
            final Map<T, List<Pair<SourceReference, Context>>> groups = new HashMap<>();

            @Override
            public void accept(
                Future<?> future,
                SourceReference sourceReference,
                Context context,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              future.launch(
                  subgroup,
                  sourceReference,
                  context,
                  AnyFunction.compose(
                      converter.function(),
                      result ->
                          result.resolve(
                              future,
                              sourceReference,
                              converter,
                              TypeErrorLocation.CROSSTAB,
                              value -> {
                                groups
                                    .computeIfAbsent(value, k -> new ArrayList<>())
                                    .add(Pair.of(sourceReference, context));
                                complete.run();
                              })));
            }

            @Override
            public void finish(
                Future<?> future,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              if (groups.size() < 2) {
                complete.run();
                return;
              }
              List<List<Pair<SourceReference, Context>>> supplier = List.of(List.of());
              for (final var items : groups.values()) {
                final var currentSupplier = supplier;
                supplier =
                    items
                        .stream()
                        .flatMap(
                            i ->
                                currentSupplier
                                    .stream()
                                    .map(
                                        parent ->
                                            Stream.concat(parent.stream(), Stream.of(i))
                                                .collect(Collectors.toList())))
                        .collect(Collectors.toList());
              }
              ConcurrentConsumer.process(
                  supplier,
                  new ConcurrentConsumer<>() {
                    @Override
                    public void complete() {
                      complete.run();
                    }

                    @Override
                    public void process(
                        List<Pair<SourceReference, Context>> item, int index, Runnable complete) {
                      final var subgroup = inner.apply(attributes);
                      ConcurrentConsumer.iterate(
                          item.iterator(),
                          new ConcurrentConsumer<>() {
                            @Override
                            public void complete() {
                              subgroup.finish(
                                  future, callingSourceReference, callingContext, complete);
                            }

                            @Override
                            public void process(
                                Pair<SourceReference, Context> item, int index, Runnable complete) {
                              subgroup.accept(
                                  future,
                                  item.first(),
                                  item.second(),
                                  callingSourceReference,
                                  callingContext,
                                  complete);
                            }
                          });
                    }
                  });
            }
          };
    }
  }

  private abstract static class ModularGrouper extends FricasseeGrouper {
    private final long size;

    protected ModularGrouper(long size) {
      this.size = size;
    }

    @Override
    final void dummy(List<Attribute> attributes) {
      // Do nothing
    }

    protected abstract long key(long index, long size);

    @Override
    Function<AttributeSource, Fricassee.GroupConsumer> prepare(
        Map<Name, CollectorDefinition> collections,
        Function<AttributeSource, Fricassee.GroupConsumer> inner) {
      if (size < 1) {
        return attributes ->
            new Fricassee.GroupConsumer() {
              @Override
              public void accept(
                  Future<?> future,
                  SourceReference sourceReference,
                  Context context,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                finish(future, sourceReference, callingContext, complete);
              }

              @Override
              public void finish(
                  Future<?> future,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                future.error(
                    callingSourceReference,
                    String.format("Invalid size %d for grouping operation.", size));
              }
            };
      }

      return attributes ->
          new Fricassee.GroupConsumer() {
            private final List<Fricassee.GroupConsumer> children =
                Stream.generate(() -> inner.apply(attributes))
                    .limit(size)
                    .collect(Collectors.toList());
            private long index;

            @Override
            public void accept(
                Future<?> future,
                SourceReference sourceReference,
                Context context,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              children
                  .get((int) key(index++, size))
                  .accept(
                      future,
                      sourceReference,
                      context,
                      callingSourceReference,
                      callingContext,
                      complete);
            }

            @Override
            public void finish(
                Future<?> future,
                SourceReference callingSourceReference,
                Context callingContext,
                Runnable complete) {
              complete.run();
            }
          };
    }
  }

  private static class ParallelConsumer implements Fricassee.GroupConsumer {
    private final List<Fricassee.GroupConsumer> subgroups;

    ParallelConsumer(Stream<Fricassee.GroupConsumer> subgroups) {
      this.subgroups = subgroups.collect(Collectors.toList());
    }

    @Override
    public void accept(
        Future<?> future,
        SourceReference sourceReference,
        Context context,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete) {
      ConcurrentConsumer.process(
          subgroups,
          new ConcurrentConsumer<>() {

            @Override
            public void complete() {
              complete.run();
            }

            @Override
            public void process(GroupConsumer subgroup, int index, Runnable complete) {
              subgroup.accept(
                  future,
                  sourceReference,
                  context,
                  callingSourceReference,
                  callingContext,
                  complete);
            }
          });
    }

    @Override
    public void finish(
        Future<?> future,
        SourceReference callingSourceReference,
        Context callingContext,
        Runnable complete) {
      ConcurrentMapper.process(
          subgroups,
          new ConcurrentMapper<Fricassee.GroupConsumer, Void>() {

            @Override
            public void emit(List<Void> output) {
              complete.run();
            }

            @Override
            public void process(GroupConsumer subgroup, int index, Consumer<Void> output) {
              subgroup.finish(
                  future, callingSourceReference, callingContext, () -> output.accept(null));
            }
          });
    }
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param type the converter for the type
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be of the type specified
   * @param <T> te type of the keys in the group
   */
  public static <T> FricasseeGrouper adjacent(
      AnyBidiConverter<T> type, Name name, Definition subgroup) {
    return new AdjacentGrouper<>(type, name, subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a boolean
   */
  public static FricasseeGrouper adjacentBool(Name name, Definition subgroup) {
    return adjacent(AnyConverter.asBool(false), name, subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a boolean
   */
  public static FricasseeGrouper adjacentBool(Str name, Definition subgroup) {
    return adjacentBool(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a floating-point number
   */
  public static FricasseeGrouper adjacentFloat(Name name, Definition subgroup) {
    return adjacent(AnyConverter.asFloat(false), name, subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a floating-point number
   */
  public static FricasseeGrouper adjacentFloat(Str name, Definition subgroup) {
    return adjacentFloat(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be an integer
   */
  public static FricasseeGrouper adjacentInt(Name name, Definition subgroup) {
    return adjacent(AnyConverter.asInt(false), name, subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be an integer
   */
  public static FricasseeGrouper adjacentInt(Str name, Definition subgroup) {
    return adjacentInt(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a string
   */
  public static FricasseeGrouper adjacentStr(Name name, Definition subgroup) {
    return adjacent(AnyConverter.asStr(false), name, subgroup);
  }

  /**
   * Create a grouper that will create groups from and ordered chain as long as the key is the same
   * as the previous one
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a string
   */
  public static FricasseeGrouper adjacentStr(Str name, Definition subgroup) {
    return adjacentStr(Name.of(name), subgroup);
  }

  /**
   * Perform all of the grouping operations provided
   *
   * <p>This is equivalent to having them passed to {@link Fricassee#groupBy(SourceReference,
   * FricasseeGrouper...)}, but can be used for other complex grouping
   *
   * @param groupers the list of groupers to use
   */
  public static FricasseeGrouper all(FricasseeGrouper... groupers) {
    if (groupers.length == 0) throw new IllegalArgumentException("No groupers supplied");
    if (groupers.length == 1) return groupers[0];
    return new FricasseeGrouper() {
      @Override
      final void dummy(List<Attribute> attributes) {
        for (final var group : groupers) {
          group.dummy(attributes);
        }
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        var result = inner;
        for (var i = groupers.length - 1; i >= 0; i--) {
          result = groupers[i].prepare(collections, result);
        }
        return result;
      }
    };
  }

  /**
   * Perform multiple independent grouping operations in parallel
   *
   * <p>Takes every input item and applies each of the provided grouping operations on it and then
   * collects all the resulting groups for downstream operations
   *
   * @param groupers the operations to perform
   */
  public static FricasseeGrouper alternate(FricasseeGrouper... groupers) {
    if (groupers.length == 0) throw new IllegalArgumentException("No groupers supplied");
    if (groupers.length == 1) return groupers[0];
    return new FricasseeGrouper() {

      @Override
      final void dummy(List<Attribute> attributes) {
        for (final var grouper : groupers) {
          grouper.dummy(attributes);
        }
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        return parallel(
            Stream.of(groupers)
                .map(grouper -> grouper.prepare(collections, inner))
                .collect(Collectors.toList()));
      }
    };
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param type the converter for the type
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be of the type specified
   * @param <T> the type of the keys in the group
   */
  public static <T> FricasseeGrouper alwaysInclude(
      AnyBidiConverter<T> type, Name name, Definition subgroup) {
    return new AlwaysIncludeGrouper<>(type, name, subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a boolean
   */
  public static FricasseeGrouper alwaysIncludeBool(Name name, Definition subgroup) {
    return alwaysInclude(AnyConverter.asBool(true), name, subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a boolean
   */
  public static FricasseeGrouper alwaysIncludeBool(Str name, Definition subgroup) {
    return alwaysIncludeBool(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a float
   */
  public static FricasseeGrouper alwaysIncludeFloat(Name name, Definition subgroup) {
    return alwaysInclude(AnyConverter.asFloat(true), name, subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a float
   */
  public static FricasseeGrouper alwaysIncludeFloat(Str name, Definition subgroup) {
    return alwaysIncludeFloat(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be an integer
   */
  public static FricasseeGrouper alwaysIncludeInt(Name name, Definition subgroup) {
    return alwaysInclude(AnyConverter.asInt(false), name, subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be an integer
   */
  public static FricasseeGrouper alwaysIncludeInt(Str name, Definition subgroup) {
    return alwaysIncludeInt(Name.of(name), subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a string
   */
  public static FricasseeGrouper alwaysIncludeStr(Name name, Definition subgroup) {
    return alwaysInclude(AnyConverter.asStr(false), name, subgroup);
  }

  /**
   * Create a grouper that will separate items into groups by a key but put items with the specified
   * key into all groups
   *
   * @param name the name for the key to be added to the output
   * @param subgroup a definition that produces the key which must be a string
   */
  public static FricasseeGrouper alwaysIncludeStr(Str name, Definition subgroup) {
    return alwaysIncludeStr(Name.of(name), subgroup);
  }

  /**
   * Put items into a group until a boundary is detected and then start a new group
   *
   * @param detector a definition to detect if a new group should be created; must return a boolean
   * @param trailing whether to add the item that signals a new group into the previous group (true)
   *     or the next group (false)
   */
  public static FricasseeGrouper boundary(Definition detector, boolean trailing) {

    return new FricasseeGrouper() {

      @Override
      final void dummy(List<Attribute> attributes) {
        // Do nothing
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        return source ->
            new Fricassee.GroupConsumer() {
              private Fricassee.GroupConsumer current;

              @Override
              public void accept(
                  Future<?> future,
                  SourceReference sourceReference,
                  Context context,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {

                future.launch(
                    detector,
                    sourceReference,
                    context,
                    new WhinyAnyConsumer() {
                      @Override
                      public void accept(boolean value) {
                        if (value) {
                          if (trailing) {
                            if (current == null) {
                              current = inner.apply(source);
                            }
                            current.accept(
                                future,
                                sourceReference,
                                context,
                                callingSourceReference,
                                callingContext,
                                () ->
                                    current.finish(
                                        future,
                                        callingSourceReference,
                                        callingContext,
                                        () -> {
                                          current = null;
                                          complete.run();
                                        }));
                          } else {
                            if (current == null) {
                              current = inner.apply(source);
                              current.accept(
                                  future,
                                  sourceReference,
                                  context,
                                  callingSourceReference,
                                  callingContext,
                                  complete);
                            } else {
                              current.finish(
                                  future,
                                  callingSourceReference,
                                  callingContext,
                                  () -> {
                                    current = inner.apply(source);
                                    current.accept(
                                        future,
                                        sourceReference,
                                        context,
                                        callingSourceReference,
                                        callingContext,
                                        complete);
                                  });
                            }
                          }

                        } else {
                          if (current == null) {
                            current = inner.apply(source);
                          }
                          current.accept(
                              future,
                              sourceReference,
                              context,
                              callingSourceReference,
                              callingContext,
                              complete);
                        }
                      }

                      @Override
                      protected void fail(String type) {
                        future.error(
                            sourceReference,
                            String.format("Expected Bool but got %s in Boundary grouper.", type));
                      }
                    });
              }

              @Override
              public void finish(
                  Future<?> future,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                if (current == null) {
                  complete.run();
                } else {
                  current.finish(future, callingSourceReference, callingContext, complete);
                }
              }
            };
      }
    };
  }

  /**
   * Divide the key space into a specified number of buckets
   *
   * @param converter the converter for the type
   * @param subgroup a definition that produces the key
   * @param count the number of buckets to create
   * @param <T> the type of the key
   */
  public static <T> FricasseeGrouper buckets(
      AnyBidiConverter<T> converter, Definition subgroup, long count) {
    return new FricasseeGrouper() {

      @Override
      final void dummy(List<Attribute> attributes) {
        // Do nothing
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        return source ->
            new Fricassee.GroupConsumer() {
              private final Map<T, List<Pair<SourceReference, Context>>> results =
                  new TreeMap<>(converter);

              @Override
              public void accept(
                  Future<?> future,
                  SourceReference sourceReference,
                  Context context,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                future.launch(
                    subgroup,
                    sourceReference,
                    context,
                    converter.asConsumer(
                        future,
                        sourceReference,
                        TypeErrorLocation.BUCKETS,
                        value -> {
                          results
                              .computeIfAbsent(value, k -> new ArrayList<>())
                              .add(Pair.of(sourceReference, context));
                          complete.run();
                        }));
              }

              @Override
              public void finish(
                  Future<?> future,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                final var itemsPerBucket = results.size() / count;
                ConcurrentConsumer.process(
                    results.values(),
                    new ConcurrentConsumer<>() {
                      private Fricassee.GroupConsumer current;

                      @Override
                      public void complete() {
                        complete.run();
                      }

                      private void fill(
                          Fricassee.GroupConsumer group,
                          List<Pair<SourceReference, Context>> items,
                          Runnable complete) {
                        ConcurrentConsumer.process(
                            items,
                            new ConcurrentConsumer<>() {
                              @Override
                              public void complete() {
                                complete.run();
                              }

                              @Override
                              public void process(
                                  Pair<SourceReference, Context> current,
                                  int index,
                                  Runnable complete) {
                                group.accept(
                                    future,
                                    current.first(),
                                    current.second(),
                                    callingSourceReference,
                                    callingContext,
                                    complete);
                              }
                            });
                      }

                      @Override
                      public void process(
                          List<Pair<SourceReference, Context>> items,
                          int index,
                          Runnable complete) {
                        if (current == null) {
                          current = inner.apply(source);
                          fill(current, items, complete);
                        } else if (index % itemsPerBucket == 0) {
                          current.finish(
                              future,
                              callingSourceReference,
                              callingContext,
                              () -> {
                                current = inner.apply(source);
                                fill(current, items, complete);
                              });
                        } else {
                          fill(current, items, complete);
                        }
                      }
                    });
              }
            };
      }
    };
  }

  /**
   * Divide a floating-point number key space into a specified number of buckets
   *
   * @param subgroup a definition that produces the key which must be a floating-point number
   * @param count the number of buckets to create
   */
  public static FricasseeGrouper bucketsFloat(Definition subgroup, long count) {
    return buckets(AnyConverter.asFloat(false), subgroup, count);
  }

  /**
   * Divide an integer key space into a specified number of buckets
   *
   * @param subgroup a definition that produces the key which must be an integer
   * @param count the number of buckets to create
   */
  public static FricasseeGrouper bucketsInt(Definition subgroup, long count) {
    return buckets(AnyConverter.asInt(false), subgroup, count);
  }

  /**
   * Divide a string key space into a specified number of buckets
   *
   * @param subgroup a definition that produces the key which must be a string
   * @param count the number of buckets to create
   */
  public static FricasseeGrouper bucketsStr(Definition subgroup, long count) {
    return buckets(AnyConverter.asStr(false), subgroup, count);
  }

  /**
   * Divide the input into chunk, each containing at most the specified number of items
   *
   * @param size the maximum number of items in a chunk
   */
  public static FricasseeGrouper chunk(long size) {
    return new ModularGrouper(size) {

      @Override
      protected long key(long index, long size) {
        return index / size;
      }
    };
  }

  /** Add a collection attribute, where a frame will be produced from the value computed */
  public static FricasseeGrouper collect(String name, CollectorDefinition definition) {
    return new FricasseeGrouper() {

      @Override
      final void dummy(List<Attribute> attributes) {
        // Do nothing
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        collections.put(Name.of(name), definition);
        return inner;
      }
    };
  }

  /**
   * Create a crosstab that uses an item to puts items into bins and then creates groups with all
   * combinations of one item from each bin
   *
   * @param type the type to expect from the definition
   * @param subgroup a definition that produces the key used for binning which must be the type
   *     specified
   * @param <T> the Java type to use for the grouping
   */
  public static <T> FricasseeGrouper crosstab(AnyBidiConverter<T> type, Definition subgroup) {
    return new CrosstabGrouper<>(type, subgroup);
  }

  /**
   * Create a crosstab that uses an item to puts items into bins and then creates groups with all
   * combinations of one item from each bin
   *
   * @param subgroup a definition that produces the key used for binning which must be a boolean
   */
  public static FricasseeGrouper crosstabBool(Definition subgroup) {
    return crosstab(AnyConverter.asBool(false), subgroup);
  }

  /**
   * Create a crosstab that uses an item to puts items into bins and then creates groups with all
   * combinations of one item from each bin
   *
   * @param subgroup a definition that produces the key used for binning which must be a float
   */
  public static FricasseeGrouper crosstabFloat(Definition subgroup) {
    return crosstab(AnyConverter.asFloat(false), subgroup);
  }

  /**
   * Create a crosstab that uses an item to puts items into bins and then creates groups with all
   * combinations of one item from each bin
   *
   * @param subgroup a definition that produces the key used for binning which must be an integer
   */
  public static FricasseeGrouper crosstabInt(Definition subgroup) {
    return crosstab(AnyConverter.asInt(false), subgroup);
  }

  /**
   * Create a crosstab that uses an item to puts items into bins and then creates groups with all
   * combinations of one item from each bin
   *
   * @param subgroup a definition that produces the key used for binning which must be a string
   */
  public static FricasseeGrouper crosstabStr(Definition subgroup) {
    return crosstab(AnyConverter.asStr(false), subgroup);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Bool</tt> value */
  public static FricasseeGrouper disciminateByBool(Name name, Definition definition) {
    return discriminate(AnyConverter.asBool(true), name, definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Bool</tt> value */
  public static FricasseeGrouper disciminateByBool(Str name, Definition definition) {
    return disciminateByBool(Name.of(name), definition);
  }

  /** Add a <tt>By</tt> clause that returns a sortable type */
  public static <T> FricasseeGrouper discriminate(
      AnyBidiConverter<T> type, Name name, Definition definition) {
    return new ByGrouper<>(type, name, definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Float</tt> value */
  public static FricasseeGrouper discriminateByFloat(Name name, Definition definition) {
    return discriminate(AnyConverter.asFloat(false), name, definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Float</tt> value */
  public static FricasseeGrouper discriminateByFloat(Str name, Definition definition) {
    return discriminateByFloat(Name.of(name), definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Int</tt> value */
  public static FricasseeGrouper discriminateByInt(Name name, Definition definition) {
    return discriminate(AnyConverter.asInt(false), name, definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Int</tt> value */
  public static FricasseeGrouper discriminateByInt(Str name, Definition definition) {
    return discriminateByInt(Name.of(name), definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Str</tt> value */
  public static FricasseeGrouper discriminateByStr(Name name, Definition definition) {
    return discriminate(AnyConverter.asStr(false), name, definition);
  }

  /** Add a <tt>By</tt> clause that returns a <tt>Str</tt> value */
  public static FricasseeGrouper discriminateByStr(Str name, Definition definition) {
    return discriminateByStr(Name.of(name), definition);
  }

  /**
   * Creates a fake grouping operation that has exactly one group
   *
   * <p>This behaves different from {@link #collect(String, CollectorDefinition)} collect operations
   * are aggregated across all complex grouping operations. This will behave more like a “By” clause
   * that returns a fixed value. The purpose of this operation is to allow multiple branches of a
   * complex operation to define the same variables.
   *
   * @param name the attribute name
   * @param value the fixed value for that attribute
   */
  public static FricasseeGrouper fixed(Name name, Any value) {
    return new FricasseeGrouper() {
      @Override
      void dummy(List<Attribute> attributes) {
        attributes.add(Attribute.of(name, value));
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        return source ->
            inner.apply(
                Stream.concat(source.attributes(), Stream.of(Attribute.of(name, value)))
                    .collect(AttributeSource.toSource()));
      }
    };
  }

  /**
   * Creates a fake grouping operation that has exactly one group
   *
   * <p>This behaves different from {@link #collect(String, CollectorDefinition)} collect operations
   * are aggregated across all complex grouping operations. This will behave more like a “By” clause
   * that returns a fixed value. The purpose of this operation is to allow multiple branches of a
   * complex operation to define the same variables.
   *
   * @param name the attribute name
   * @param value the fixed value for that attribute
   */
  public static FricasseeGrouper fixed(Str name, Any value) {
    return fixed(Name.of(name), value);
  }

  private static Function<AttributeSource, Fricassee.GroupConsumer> parallel(
      List<Function<AttributeSource, Fricassee.GroupConsumer>> children) {
    return source -> new ParallelConsumer(children.stream().map(f -> f.apply(source)));
  }

  /**
   * Produce a power set of the groupers
   *
   * <p>This creates all combinations of these groupers (substituting keys with nulls if necessary
   * to create the same output attribute names), performs the grouping, and then collects all the
   * groups produced.
   *
   * @param groupers the groupers to use
   */
  public static FricasseeGrouper powerset(FricasseeGrouper... groupers) {
    return new FricasseeGrouper() {
      @Override
      void dummy(List<Attribute> attributes) {
        for (final var grouper : groupers) {
          grouper.dummy(attributes);
        }
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        var children = List.of(inner);
        for (int i = groupers.length - 1; i >= 0; i--) {
          final var grouper = groupers[i];
          final var dummy = new ArrayList<Attribute>();
          grouper.dummy(dummy);
          children =
              children
                  .stream()
                  .flatMap(
                      child ->
                          Stream.of(
                              grouper.prepare(collections, child),
                              source ->
                                  child.apply(
                                      Stream.concat(dummy.stream(), source.attributes())
                                          .collect(AttributeSource.toSource()))))
                  .collect(Collectors.toList());
        }
        return parallel(children);
      }
    };
  }

  /**
   * Distribute the input over a ring using the primitive value raised to the current index
   *
   * @param primitive the primitive value to raise; this must be less than the size of the ring
   * @param size the maximum number of items in a chunk
   */
  public static FricasseeGrouper ringExponent(long primitive, long size) {
    if (primitive < 1 || primitive >= size) {
      throw new IllegalArgumentException("Invalid primitive for field.");
    }
    return new ModularGrouper(size) {

      @Override
      protected long key(long index, long size) {
        return (long) (Math.pow(primitive, index) % size);
      }
    };
  }

  /**
   * Divide the input into stripes, put the items alternately into each strip
   *
   * @param size the number of stripes to createFromValues
   */
  public static FricasseeGrouper stripe(long size) {
    return new ModularGrouper(size) {
      @Override
      protected long key(long index, long size) {
        return index % size;
      }
    };
  }

  /**
   * Create a grouper that divides the input into windows
   *
   * <p>A key is calculated for each input and then inputs are ordered by their keys. The inputs are
   * then sorted into windows and each window is output as a group.
   *
   * @param length the algorithm to compute the length of a window
   * @param next the algorithm to compute the start of the next window
   * @param <L> the type of the window length key
   * @param <N> the type of the next window key
   */
  public static <L, N> FricasseeGrouper windowed(
      FricasseeWindow<L> length, FricasseeWindow<N> next) {

    return new FricasseeGrouper() {

      @Override
      final void dummy(List<Attribute> attributes) {
        // Do nothing.
      }

      @Override
      final Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        return source ->
            new Fricassee.GroupConsumer() {

              private final List<WindowItem<L, N>> items = new ArrayList<>();

              @Override
              public void accept(
                  Future<?> future,
                  SourceReference sourceReference,
                  Context context,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                length.compute(
                    future,
                    sourceReference,
                    context,
                    lengthValue ->
                        next.compute(
                            future,
                            sourceReference,
                            context,
                            nextValue -> {
                              items.add(
                                  new WindowItem<>(
                                      sourceReference, context, lengthValue, nextValue));
                              complete.run();
                            }));
              }

              @Override
              public void finish(
                  Future<?> future,
                  SourceReference callingSourceReference,
                  Context callingContext,
                  Runnable complete) {
                if (items.isEmpty()) {
                  complete.run();
                  return;
                }
                var start = Optional.of(0);
                var ranges = new ArrayList<Pair<Integer, Integer>>();
                while (start.isPresent()) {
                  start =
                      start.flatMap(
                          startIndex -> {
                            var end = length.findEnd(startIndex, items);
                            end.ifPresent(endIndex -> ranges.add(Pair.of(startIndex, endIndex)));
                            return end.flatMap(
                                endIndex -> next.findNext(startIndex, endIndex, items));
                          });
                }
                ConcurrentConsumer.process(
                    ranges,
                    new ConcurrentConsumer<>() {
                      @Override
                      public void complete() {
                        complete.run();
                      }

                      @Override
                      public void process(
                          Pair<Integer, Integer> range, int index, Runnable complete) {
                        final var group = inner.apply(source);
                        ConcurrentConsumer.process(
                            items.subList(range.first(), range.second()),
                            new ConcurrentConsumer<>() {
                              @Override
                              public void complete() {
                                group.finish(
                                    future, callingSourceReference, callingContext, complete);
                              }

                              @Override
                              public void process(
                                  WindowItem<L, N> item, int index, Runnable complete) {
                                group.accept(
                                    future,
                                    item.sourceReference(),
                                    item.context(),
                                    callingSourceReference,
                                    callingContext,
                                    complete);
                              }
                            });
                      }
                    });
              }
            };
      }
    };
  }

  private FricasseeGrouper() {}

  abstract void dummy(List<Attribute> attributes);

  /** Create a grouper that defines the same variables but does no grouping. */
  public FricasseeGrouper invert() {
    final var original = this;
    return new FricasseeGrouper() {
      @Override
      void dummy(List<Attribute> attributes) {
        original.dummy(attributes);
      }

      @Override
      public FricasseeGrouper invert() {
        return original;
      }

      @Override
      Function<AttributeSource, Fricassee.GroupConsumer> prepare(
          Map<Name, CollectorDefinition> collections,
          Function<AttributeSource, Fricassee.GroupConsumer> inner) {
        final var attributes = new ArrayList<Attribute>();
        original.dummy(attributes);
        return source ->
            inner.apply(
                Stream.concat(attributes.stream(), source.attributes())
                    .collect(AttributeSource.toSource()));
      }
    };
  }

  abstract Function<AttributeSource, Fricassee.GroupConsumer> prepare(
      Map<Name, CollectorDefinition> collections,
      Function<AttributeSource, Fricassee.GroupConsumer> inner);
}
