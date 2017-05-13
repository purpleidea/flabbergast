package flabbergast.lang;

import flabbergast.export.NativeBinding;
import flabbergast.util.ConcurrentMapper;
import flabbergast.util.Pair;
import java.text.DateFormatSymbols;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/** A Frame in the Flabbergast language. */
public class Frame extends AttributeSource {
  private enum GatherResult {
    CONTINUE,
    FINISHED,
    ERROR
  }

  /**
   * Callback to accept attribute values
   *
   * <p>The ordinal provided is the numbered position of the attribute in standard sort order.
   * Although iteration may proceed out of order, the ordinals will always be in the correct order.
   */
  public interface AttributeConsumer<T> {
    /**
     * Collect one attribute from the frame.
     *
     * @param name the attribute name as defined in the frame
     * @param ordinal the position in the frame (identical to <tt>Ordinal</tt> in a fricassée)
     * @param value the attribute value
     */
    T accept(Name name, long ordinal, Any value);
    /** Invoked once all the attributes in this frame have been iterated over. */
    void complete(List<T> results);

    /**
     * Produce a user-friendly description for an attribute to appear in the case of deadlock
     *
     * <p>This is called for every element before waiting to evaluate that element. If deadlock
     * occurs, this name will be visible in the trace.
     *
     * @param name the attribute name
     */
    String describe(Name name);
  }

  private static class ActiveGatherer extends Gatherer {
    private List<Pair<Name, Consumer<? super Frame>>> consumers = new ArrayList<>();
    private final GathererHolder holder;
    private final AtomicInteger interlock = new AtomicInteger();
    private final Map<Name, List<Any>> values;

    ActiveGatherer(Stream<Name> names) {
      holder = new GathererHolder(this);
      values =
          names.collect(Collectors.toMap(Function.identity(), k -> new ArrayList<>(), (a, b) -> a));
    }

    @Override
    void accept(
        Future<?> future,
        Frame frame,
        SourceReference sourceReference,
        Name name,
        Consumer<? super Frame> consumer) {
      if (!values.containsKey(name)) {
        super.accept(future, frame, sourceReference, name, consumer);
        return;
      }
      synchronized (this) {
        if (consumers != null) {
          consumers.add(Pair.of(name, future.real().park(frame, sourceReference, name, consumer)));
          return;
        }
      }
      holder.accept(future, frame, sourceReference, name, consumer);
    }

    @Override
    public void acquire() {
      interlock.incrementAndGet();
    }

    @Override
    GatherResult add(Future<?> future, SourceReference sourceReference, Name name, Any value) {
      synchronized (this) {
        if (consumers == null) {
          future.error(
              sourceReference,
              String.format(
                  "Value dispersed to %s in inactive frame. This is an illegal cross-frame dispersion.",
                  name));
          return GatherResult.ERROR;
        }
        final var list = values.get(name);
        if (list != null) {
          list.add(value);
          return GatherResult.FINISHED;
        }
        return GatherResult.CONTINUE;
      }
    }

    public Gatherer reference() {
      return values.isEmpty() ? NULL_GATHERER : holder;
    }

    @Override
    public void release(Frame frame) {
      if (interlock.decrementAndGet() == 0) {
        final Map<Name, Frame> values;
        final List<Pair<Name, Consumer<? super Frame>>> consumers;
        synchronized (this) {
          values =
              this.values
                  .entrySet()
                  .stream()
                  .collect(
                      Collectors.toMap(
                          Map.Entry::getKey,
                          e ->
                              createFromPromises(
                                  frame.source(),
                                  frame.context(),
                                  e.getValue().stream().map(Pair.ordinate()))));

          holder.set(new FinishedGatherer(values));
          consumers = this.consumers;
          this.consumers = null;
        }
        for (final var consumer : consumers) {
          consumer.second().accept(values.getOrDefault(consumer.first(), EMPTY));
        }
      }
    }
  }

  private static class FinishedGatherer extends Gatherer {
    private final Map<Name, Frame> values;

    FinishedGatherer(Map<Name, Frame> values) {
      this.values = values;
    }

    @Override
    void accept(
        Future<?> future,
        Frame frame,
        SourceReference sourceReference,
        Name name,
        Consumer<? super Frame> consumer) {
      final var output = values.get(name);
      if (output == null) {
        super.accept(future, frame, sourceReference, name, consumer);
      } else {
        consumer.accept(output);
      }
    }

    @Override
    GatherResult add(Future<?> future, SourceReference sourceReference, Name name, Any value) {
      future.error(
          sourceReference,
          String.format(
              "Value dispersed to %s in inactive frame. This is an illegal cross-frame dispersion.",
              name));
      return GatherResult.ERROR;
    }
  }

  abstract static class Gatherer {

    void accept(
        Future<?> future,
        Frame frame,
        SourceReference sourceReference,
        Name name,
        Consumer<? super Frame> consumer) {
      future.error(sourceReference, String.format("Frame does not gather %s values", name));
    }

    void acquire() {
      // Do nothing
    }

    abstract GatherResult add(
        Future<?> future, SourceReference sourceReference, Name name, Any value);

    void release(Frame frame) {
      // Do nothing.
    }
  }

  private static class GathererHolder extends Gatherer {
    private final AtomicReference<Gatherer> real;

    GathererHolder(Gatherer inner) {
      real = new AtomicReference<>(inner);
    }

    @Override
    void accept(
        Future<?> future,
        Frame frame,
        SourceReference sourceReference,
        Name name,
        Consumer<? super Frame> consumer) {
      real.get().accept(future, frame, sourceReference, name, consumer);
    }

    @Override
    public void acquire() {
      real.get().acquire();
    }

    @Override
    GatherResult add(Future<?> future, SourceReference sourceReference, Name name, Any value) {
      return real.get().add(future, sourceReference, name, value);
    }

    @Override
    public void release(Frame frame) {
      real.get().release(frame);
    }

    void set(Gatherer inner) {
      real.set(inner);
    }
  }

  static final Gatherer ILLEGAL_GATHERER =
      new Gatherer() {

        @Override
        public void acquire() {
          // Do nothing.
        }

        @Override
        GatherResult add(Future<?> future, SourceReference sourceReference, Name name, Any value) {
          future.error(
              sourceReference,
              String.format(
                  "Value dispersed to %s in frame which never allowed gathering. This is an illegal cross-frame dispersion.",
                  name));
          return GatherResult.ERROR;
        }

        @Override
        public void release(Frame frame) {
          // Do nothing.
        }
      };
  private static final Frame[] DAYS =
      makeFrames(
          new String[] {
            "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
          },
          DateFormatSymbols.getInstance().getShortWeekdays(),
          DateFormatSymbols.getInstance().getWeekdays());
  /** A frame that contains no attributes and has the id <tt>empty</tt>. */
  public static final Frame EMPTY =
      new Frame(
          Str.from("empty"),
          SourceReference.EMPTY,
          new Context(null, Stream.empty()),
          ILLEGAL_GATHERER,
          true);

  private static final Frame[] MONTHS =
      makeFrames(
          new String[] {
            "january",
            "february",
            "march",
            "april",
            "may",
            "june",
            "july",
            "august",
            "september",
            "october",
            "november",
            "december"
          },
          DateFormatSymbols.getInstance().getShortMonths(),
          DateFormatSymbols.getInstance().getMonths());
  static final Gatherer NULL_GATHERER =
      new Gatherer() {

        @Override
        GatherResult add(Future<?> future, SourceReference sourceReference, Name name, Any value) {
          return GatherResult.CONTINUE;
        }
      };
  private static final List<ProxyAttribute<ZonedDateTime>> TIME_TRANSFORMS =
      List.of(
          ProxyAttribute.extractFrame("day_of_week", d -> DAYS[d.getDayOfWeek().getValue() % 7]),
          ProxyAttribute.extractInt("from_midnight", d -> (long) d.toLocalTime().toSecondOfDay()),
          ProxyAttribute.extractInt("milliseconds", d -> (long) d.get(ChronoField.MILLI_OF_SECOND)),
          ProxyAttribute.extractInt("second", d -> (long) d.getSecond()),
          ProxyAttribute.extractInt("minute", d -> (long) d.getMinute()),
          ProxyAttribute.extractInt("hour", d -> (long) d.getHour()),
          ProxyAttribute.extractInt("day", d -> (long) d.getDayOfMonth()),
          ProxyAttribute.extractFrame("month", d -> MONTHS[d.getMonthValue() - 1]),
          ProxyAttribute.extractInt("year", d -> (long) d.getYear()),
          ProxyAttribute.extractInt(
              "week", d -> (long) d.get(WeekFields.ISO.weekOfWeekBasedYear())),
          ProxyAttribute.extractInt("day_of_year", d -> (long) d.getDayOfYear()),
          ProxyAttribute.extractFloat("epoch", d -> d.toEpochSecond() + d.getNano() / 1_000_000.0),
          ProxyAttribute.extractBool("is_utc", d -> d.getOffset().equals(ZoneOffset.UTC)),
          ProxyAttribute.extractBool("is_leap_year", d -> Year.isLeap(d.getYear())));
  static final UriHandler TIME_INTEROP =
      NativeBinding.create(
          "time",
          NativeBinding.of(
              "days",
              Any.of(createFromValues("days", "the big bang", Stream.of(DAYS).map(Frame::toPair)))),
          NativeBinding.of(
              "months",
              Any.of(
                  createFromValues(
                      "months", "the big bang", Stream.of(MONTHS).map(Frame::toPair)))),
          NativeBinding.of(
              "now.local",
              Any.of(
                  proxyOf(
                      "now_local", "the big bang", ZonedDateTime.now(), TIME_TRANSFORMS.stream()))),
          NativeBinding.of(
              "now.utc",
              Any.of(
                  proxyOf(
                      "now_utc",
                      "the big bang",
                      ZonedDateTime.now(ZoneId.of("Z")),
                      TIME_TRANSFORMS.stream()))));

  /**
   * Construct a new frame by concatenating the items existing frames and renumbering the contents.
   *
   * @param first the frame whose attributes should be first in the new frame
   * @param second the frame whose attributes should be second in the new frame
   */
  public static Frame concat(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Frame first,
      Frame second) {
    return createFromPromises(
        sourceReference,
        context,
        Stream.concat(first.attributes.values().stream(), second.attributes.values().stream())
            .filter(Pair::second)
            .map(Pair::first)
            .map(Pair.ordinate()));
  }

  /**
   * Construct a frame from the specified builders in the provided context.
   *
   * @param future the future in which the frame is being generated
   * @param sourceReference the caller's execution trace
   * @param context the containing context
   * @param sources the attributes to populate the frame with
   * @return a newly created frame
   */
  public static Frame create(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      AttributeSource... sources) {
    return create(future, sourceReference, context, true, Stream.empty(), sources);
  }
  /**
   * Construct a frame from the specified builders in the provided context.
   *
   * @param selfIsThis if true <tt>This</tt> should refer to the newly created frame; otherwise,
   *     <tt>This</tt> will retain the value from the provided context
   * @param future the future in which the frame is being generated
   * @param sourceReference the caller's execution trace
   * @param context the containing context
   * @param gatherers the names that this frame will collect from disperse instructions
   * @param sources the attributes to populate the frame with
   * @return a newly created frame
   */
  public static Frame create(
      boolean selfIsThis,
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Name[] gatherers,
      AttributeSource... sources) {
    return create(future, sourceReference, context, selfIsThis, Stream.of(gatherers), sources);
  }

  /**
   * Construct a frame from the specified builders in the provided context
   *
   * @param future the future in which the frame is being generated
   * @param sourceReference the caller's execution trace
   * @param context the containing context
   * @param selfIsThis whether <tt>This</tt> should refer to the newly constructed frame. Generally
   *     only false for <tt>Let</tt> and other binding operations
   * @param gatherers the names that this frame will collect from disperse instructions
   * @param sources the attributes to populate the frame with
   * @return a newly created frame
   */
  public static Frame create(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      boolean selfIsThis,
      Stream<Name> gatherers,
      AttributeSource... sources) {
    for (final var builder : sources) {
      context = builder.context(context);
    }
    final var frame =
        new Frame(
            generateId(),
            sourceReference,
            context,
            new ActiveGatherer(
                    Stream.concat(
                        gatherers, Stream.of(sources).flatMap(AttributeSource::gatherers)))
                .reference(),
            selfIsThis);

    final var batch = future.launch();
    frame.acquireGatherer();
    AttributeSource.squash(Stream.of(sources))
        .forEach(
            d -> {
              final var promise = d.launch(batch, sourceReference, frame.context);
              if (promise != null) {
                frame.attributes.put(d.name(), Pair.of(promise, d.isPublic()));
                frame.acquireGatherer();
                future
                    .real()
                    .awaitDisbursement(
                        promise,
                        frame.sourceReference,
                        String.format("Wait for all disbursements from %s in frame", d.name()),
                        frame::releaseGatherer);
              }
            });
    batch.execute();
    frame.releaseGatherer();
    return frame;
  }

  /** Create a new frame from existing promises */
  private static Frame createFromPromises(
      SourceReference sourceReference, Context context, Stream<Pair<Name, Promise<Any>>> promises) {
    final var frame = new Frame(generateId(), sourceReference, context, ILLEGAL_GATHERER, true);
    promises.forEach(p -> frame.attributes.put(p.first(), Pair.of(p.second(), true)));
    return frame;
  }

  /**
   * Construct a frame for fixed data generated by native code.
   *
   * @param id an identification string for this frame which must be unique and not overlap with
   *     automatically generated names
   * @param source the value to write in the stack trace (e.g., "sql")
   * @param values the builders to populate the attributes of this frame with
   * @return a newly created frame
   */
  public static Frame createFromValues(String id, String source, Stream<Pair<Name, Any>> values) {
    final var frame =
        new Frame(
            Str.from(id), SourceReference.root(source), Context.EMPTY, ILLEGAL_GATHERER, true);
    values.forEachOrdered(p -> frame.attributes.put(p.first(), Pair.of(p.second(), true)));
    return frame;
  }

  /** Create a frame to be instantiated later in another context. */
  public static Definition define(AttributeSource... sources) {
    return (future, sourceReference, context) ->
        () -> future.complete(Any.of(Frame.create(future, sourceReference, context, sources)));
  }

  private static final AtomicInteger NEXT_ID = new AtomicInteger();

  /** Create unique identifier for a frame. */
  private static Str generateId() {
    return Str.from(String.format("f%019d", NEXT_ID.incrementAndGet()));
  }

  private static Frame[] makeFrames(String[] attrs, String[] shortNames, String[] longNames) {
    final var result = new Frame[attrs.length];
    for (var i = 0; i < attrs.length; i++) {
      result[i] =
          Frame.createFromValues(
              attrs[i],
              "the big bang",
              Stream.of(
                  Pair.of(Name.of("short_name"), Any.of(shortNames[i])),
                  Pair.of(Name.of("long_name"), Any.of(longNames[i])),
                  Pair.of(Name.of("ordinal"), Any.of(i + 1))));
    }
    return result;
  }

  /** Create a definition that will convert a time into a frame. */
  public static Definition of(ZonedDateTime time) {
    return (f, s, c) -> () -> of(f, s, c, time);
  }

  /** Create a frame containing the information for a time */
  public static Frame of(
      Future<?> future, SourceReference sourceReference, Context context, ZonedDateTime time) {
    return proxyOf(sourceReference, context, time, TIME_TRANSFORMS.stream());
  }

  /**
   * Create a definition for a Java-object proxying frame
   *
   * <p>The frame will be instantiated in an appropriate context and filled with the value provided.
   *
   * @param value the object to be held
   * @param extractors the attributes to generate for this value
   */
  public static <T> Definition proxyOf(T value, Stream<ProxyAttribute<T>> extractors) {
    return (future, sourceReference, context) ->
        () -> future.complete(Any.of(proxyOf(sourceReference, context, value, extractors)));
  }

  /**
   * Create a Java-object proxying frame with a fixed ID
   *
   * @param id a valid Flabbergast identifier; there must be only one frame with this ID
   * @param caller a “filename” to appear in back traces
   * @param value the object to be held
   * @param extractors the attributes to generate for this value
   */
  public static <T> Frame proxyOf(
      String id, String caller, T value, Stream<ProxyAttribute<T>> extractors) {
    final var frame =
        new ProxyFrame(Str.from(id), SourceReference.root(caller), Context.EMPTY, value);
    extractors
        .map(extractor -> extractor.apply(value))
        .forEach(p -> frame.attributes.put(p.first(), Pair.of(p.second(), true)));
    return frame;
  }

  /**
   * Create a Java-object proxying frame
   *
   * @param sourceReference the caller that generated this frame
   * @param context the current context
   * @param value the object to be held
   * @param extractors the attributes to generate for this value
   */
  public static <T> Frame proxyOf(
      SourceReference sourceReference,
      Context context,
      T value,
      Stream<ProxyAttribute<T>> extractors) {
    final var frame = new ProxyFrame(generateId(), sourceReference, context, value);
    extractors
        .map(extractor -> extractor.apply(value))
        .forEach(p -> frame.attributes.put(p.first(), Pair.of(p.second(), true)));
    return frame;
  }

  /**
   * Construct a frame containing a range of numbers for the <tt>Through</tt> operation.
   *
   * <p>If the end of the range is before the beginning, an empty list is produced.
   *
   * @param start the start of the range, inclusive
   * @param end the end of the range, inclusive
   */
  public static Frame through(
      Future<?> future, SourceReference sourceReference, long start, long end, Context context) {
    return createFromPromises(
        sourceReference,
        context,
        LongStream.rangeClosed(start, end).mapToObj(Any::of).map(Pair.ordinate()));
  }

  final TreeMap<Name, Pair<Promise<Any>, Boolean>> attributes = new TreeMap<>();
  private final Frame container;
  private final Context context;
  private final Gatherer gatherers;
  private final Str id;
  private final SourceReference sourceReference;

  Frame(
      Str id,
      SourceReference sourceReference,
      Context context,
      Gatherer gatherers,
      boolean selfIsThis) {
    this.id = id;
    this.sourceReference = sourceReference;
    this.gatherers = gatherers;
    this.context = selfIsThis ? context.prependPrivate(this) : context.prependHidden(this);
    container = context.self() == null ? this : context.self();
  }

  private void acquireGatherer() {
    var current = this;
    while (true) {
      current.gatherers.acquire();
      if (current.container == current) break;
      current = current.container;
    }
  }

  void addNamesTo(Set<Name> names) {
    names.addAll(attributes.keySet());
  }

  /** Create attributes that contains the attributes in this frame */
  @Override
  final Stream<Attribute> attributes() {
    return attributes
        .entrySet()
        .stream()
        .filter(e -> e.getValue().second())
        .map(e -> Attribute.of(e.getKey(), e.getValue().first()));
  }

  @Override
  Stream<Name> gatherers() {
    return Stream.empty();
  }

  @Override
  Context context(Context context) {
    return context;
  }

  /** The containing frame, or itself for file-level frames. */
  public final Frame container() {
    return container;
  }

  /**
   * The lookup context containing this frame, its containers, its ancestors, and their containers.
   */
  final Context context() {
    return context;
  }

  /** True if there are no attributes in this frame */
  public final boolean isEmpty() {
    return attributes.isEmpty();
  }

  /**
   * Offer a value to be consumed by a gatherer.
   *
   * @param future the future in which the offer is being executed
   * @param sourceReference the source trace of the caller
   * @param name the name of the gatherer
   * @param value the value offered
   * @return true if successful; false is an error occured
   */
  public boolean disperse(Future<?> future, SourceReference sourceReference, Name name, Any value) {
    var current = this;
    while (true) {
      switch (current.gatherers.add(future, sourceReference, name, value)) {
        case CONTINUE:
          break;
        case FINISHED:
          return true;
        case ERROR:
          return false;
      }

      if (current.container == current) return true;
      current = current.container;
    }
  }
  /**
   * Offer a value to be consumed by a gatherer.
   *
   * @param future the future in which the offer is being executed
   * @param sourceReference the source trace of the caller
   * @param ordinal the name of the gatherer
   * @param value the value offered
   * @return true if successful; false is an error occurred
   */
  public boolean disperse(
      Future<?> future, SourceReference sourceReference, long ordinal, Any value) {
    return disperse(future, sourceReference, Name.of(ordinal), value);
  }
  /**
   * Offer a value to be consumed by a gatherer.
   *
   * @param future the future in which the offer is being executed
   * @param sourceReference the source trace of the caller
   * @param name the name of the gatherer
   * @param value the value offered
   * @return true if successful; false is an error occurred
   */
  public boolean disperse(Future<?> future, SourceReference sourceReference, Str name, Any value) {
    return disperse(future, sourceReference, Name.of(name), value);
  }

  /**
   * This converts a Java object held in a frame or another type that can be converted to the same
   * type
   *
   * @param type the class object for the proxy type
   * @param <T> the proxy type
   * @return an optional containing the proxied value if the frame contained it; empty otherwise
   */
  public <T> Optional<? extends T> extractProxy(Class<T> type) {
    return Optional.empty();
  }

  /**
   * Evaluate all values in this frame and invoke a call back for each one.
   *
   * <p>There is no guaranteed order for the values since they may not be evaluated at the time of
   * invoking this method. The ordinal provided to the consumer is the order in the frame; not the
   * order observed.
   *
   * @param future the future to use for waiting for the promises
   * @param source the caller of the iterator; this is used to create execution traces in the case
   *     of deadlock
   * @param consumer the consumer of the iterated items
   */
  public <T> void forAll(Future<?> future, SourceReference source, AttributeConsumer<T> consumer) {
    ConcurrentMapper.process(
        attributes.entrySet().stream().filter(e -> e.getValue().second()),
        new ConcurrentMapper<Entry<Name, Pair<Promise<Any>, Boolean>>, T>() {
          @Override
          public void emit(List<T> output) {
            consumer.complete(output);
          }

          @Override
          public void process(
              Entry<Name, Pair<Promise<Any>, Boolean>> item, int index, Consumer<T> output) {
            future.await(
                item.getValue().first(),
                source,
                consumer.describe(item.getKey()),
                value -> consumer.accept(item.getKey(), index, value));
          }
        });
  }

  /**
   * Gather all values dispersed in the descendants of this frame
   *
   * @param future the future of the caller
   * @param sourceReference the execution trace of the caller
   * @param name the name of the bucket
   * @param consumer the consumer of the contents of the bucket
   */
  public void gather(
      Future<?> future,
      SourceReference sourceReference,
      Name name,
      Consumer<? super Frame> consumer) {
    gatherers.accept(future, this, sourceReference, name, consumer);
  }
  /**
   * Gather all values dispersed in the descendants of this frame
   *
   * @param future the future of the caller
   * @param sourceReference the execution trace of the caller
   * @param name the name of the bucket
   * @param consumer the consumer of the contents of the bucket
   */
  public void gather(
      Future<?> future,
      SourceReference sourceReference,
      Str name,
      Consumer<? super Frame> consumer) {
    gather(future, sourceReference, Name.of(name), consumer);
  }
  /**
   * Gather all values dispersed in the descendants of this frame
   *
   * @param future the future of the caller
   * @param sourceReference the execution trace of the caller
   * @param ordinal the name of the bucket
   * @param consumer the consumer of the contents of the bucket
   */
  public void gather(
      Future<?> future,
      SourceReference sourceReference,
      long ordinal,
      Consumer<? super Frame> consumer) {
    gather(future, sourceReference, Name.of(ordinal), consumer);
  }
  /**
   * Get a value in this frame.
   *
   * @return the promise or null if the attribute does not exist
   */
  public final Optional<Promise<Any>> get(Name name) {
    return Optional.ofNullable(attributes.get(name)).filter(Pair::second).map(Pair::first);
  }

  Optional<Promise<Any>> getPrivate(Name name) {
    return Optional.ofNullable(attributes.get(name)).map(Pair::first);
  }

  /** Checks if this frame contains an attribute */
  final boolean has(Name name) {
    return attributes.containsKey(name);
  }

  /** The <tt>GenerateId</tt> value for this frame. */
  public final Str id() {
    return id;
  }

  void intersectNames(Set<Name> names) {
    names.retainAll(attributes.keySet());
  }

  /** Counts how many of the names in this frame are strings (first) or ordinals (second) */
  public final Pair<Long, Long> nameTypes() {
    final var partition =
        names()
            .collect(
                Collectors.partitioningBy(name -> name.test(true, false), Collectors.counting()));
    return Pair.of(partition.get(true), partition.get(false));
  }

  /** The names in this frame */
  public final Stream<Name> names() {
    return attributes.entrySet().stream().filter(e -> e.getValue().second()).map(Map.Entry::getKey);
  }

  Stream<Name> namesPrivate() {
    return attributes.keySet().stream();
  }

  /**
   * Create a frame with the same attributes except redefining those provided.
   *
   * <p>This is not terribly useful in most contexts. It is meant for the REPL.
   */
  public final Frame redefine(Future<?> future, AttributeSource redefinitions) {
    return create(future, sourceReference, context, this, redefinitions);
  }

  private void releaseGatherer() {
    var current = this;
    while (true) {
      current.gatherers.release(current);

      if (current.container == current) break;
      current = current.container;
    }
  }

  /** The execution trace when this frame was created. */
  public final SourceReference source() {
    return sourceReference;
  }

  /**
   * Create a pair for this frame where the name is its unique ID and the value is the frame itself.
   */
  public final Pair<Name, Any> toPair() {
    return Pair.of(Name.of(id), Any.of(this));
  }
}
