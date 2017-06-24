package flabbergast;

import flabbergast.ReflectedFrame.Transform;
import java.text.DateFormatSymbols;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class InterlockedLookup extends Future {
  public abstract class BaseSink<T> implements ConsumeResult {
    private final Class<? extends T> clazz;
    private List<Matcher<T>> handlers = new ArrayList<Matcher<T>>();
    protected String names;
    private Set<Class<?>> types = new HashSet<Class<?>>();

    BaseSink(Class<? extends T> clazz) {
      this.clazz = clazz;
    }

    protected <X> void add(Class<? extends X> clazz, Matcher<T> matcher) {
      types.add(clazz);
      handlers.add(matcher);
    }

    public <X> void allow(
        Class<? extends X> clazz,
        Function<X, T> converter,
        boolean allow_null,
        String custom_error) {
      Matcher<X> matcher = findMatcher(clazz, allow_null, custom_error);
      add(
          clazz,
          (input, writer, error) ->
              matcher.invoke(input, x -> writer.accept(converter.apply(x)), error));
    }

    public void allowDefault(boolean allow_null, String custom_error) {
      add(clazz, findMatcher(clazz, allow_null, custom_error));
      if (clazz == ZonedDateTime.class) {
        ObjectSink<Ptr<Long>> timeObj =
            allowObject(
                x -> (T) ZonedDateTime.ofInstant(Instant.ofEpochSecond(x.get()), ZoneId.of("Z")),
                Ptr::new);
        timeObj.add(
            Long.class, "epoch", false, Ptr::set, "Time-based tuple must have “epoch” attribute.");
      }
    }

    public <X> ObjectSink<X> allowObject(Function<X, T> converter, Supplier<X> ctor) {
      ObjectSink<X> obj_sink = new ObjectSink<>(ctor);
      add(
          Frame.class,
          (input, writer, error) ->
              obj_sink.invoke(input, x -> writer.accept(converter.apply(x)), error));
      return obj_sink;
    }

    protected void dispatch(Object input_result, Consumer<T> writer) {
      String custom_error = null;
      for (Matcher<T> handler : handlers) {
        Ptr<String> current_error = new Ptr<>();
        if (handler.invoke(input_result, writer, current_error)) {
          if (interlock.decrementAndGet() == 0) {
            task_master.slot(InterlockedLookup.this);
          }
          return;
        } else {
          custom_error = custom_error == null ? current_error.get() : custom_error;
        }
      }
      task_master.reportOtherError(
          source_reference,
          custom_error != null
              ? custom_error
              : String.format(
                  "“%s” has type %s but expected %s.",
                  names,
                  SupportFunctions.nameForClass(input_result.getClass()),
                  types
                      .stream()
                      .map(SupportFunctions::nameForClass)
                      .collect(Collectors.joining(" or "))));
    }

    public void lookup(String... names) {
      if (!first) {
        throw new IllegalStateException("Cannot lookup after setup.");
      }
      this.names = String.join(",", names);
      interlock.incrementAndGet();
      Future input_lookup = new Lookup(task_master, source_reference, names, context);
      input_lookup.listen(this);
    }
  }

  private interface ConsumeField<T> {
    boolean invoke(T obj, Object result, Ptr<String> error);
  }

  public class ListSink<T> extends BaseSink<T> {
    private final Consumer<Map<String, T>> writer;

    ListSink(Class<? extends T> clazz, Consumer<Map<String, T>> writer) {
      super(clazz);
      this.writer = writer;
    }

    @Override
    public void consume(Object input_result) {
      if (!(input_result instanceof Frame)) {
        task_master.reportOtherError(
            source_reference, String.format("“%s” has type %s but expected Frame.", names));
        return;
      }
      Frame frame = (Frame) input_result;
      Map<String, T> results = new TreeMap<>();
      interlock.addAndGet(frame.count());
      AtomicInteger listInterlock = new AtomicInteger(frame.count());
      for (String name : frame) {
        String arg_name = name;
        frame.getOrSubscribe(
            arg_name,
            arg_result ->
                dispatch(
                    arg_result,
                    x -> {
                      results.put(arg_name, x);
                      if (listInterlock.decrementAndGet() == 0) {

                        writer.accept(results);
                      }
                    }));
      }
      if (interlock.decrementAndGet() == 0) {
        task_master.slot(InterlockedLookup.this);
      }
    }
  }

  public interface Matcher<T> {
    boolean invoke(Object input, Consumer<T> writer, Ptr<String> error);
  }

  public class ObjectSink<T> implements Matcher<T> {
    private final Supplier<T> ctor;
    private final Map<String, ConsumeField<T>> writers = new HashMap<>();

    ObjectSink(Supplier<T> ctor) {
      this.ctor = ctor;
    }

    public <X> void add(
        Class<? extends X> clazz,
        String name,
        boolean allow_null,
        BiConsumer<T, X> writer,
        String custom_error) {
      Matcher<X> matcher = findMatcher(clazz, allow_null, custom_error);
      writers.put(
          name, (obj, val, error) -> matcher.invoke(val, x -> writer.accept(obj, x), error));
    }

    @Override
    public boolean invoke(Object input, Consumer<T> writer, Ptr<String> error) {
      error.set(null);
      if (!(input instanceof Frame)) {
        return false;
      }
      Frame frame = (Frame) input;
      T result = ctor.get();
      interlock.addAndGet(writers.size());
      writers
          .entrySet()
          .stream()
          .forEach(
              entry -> {
                if (!frame.getOrSubscribe(
                    entry.getKey(),
                    arg_result -> {
                      Ptr<String> writer_error = new Ptr<>();
                      if (entry.getValue().invoke(result, arg_result, writer_error)) {

                        if (interlock.decrementAndGet() == 0) {
                          task_master.slot(InterlockedLookup.this);
                        }
                      } else {
                        task_master.reportOtherError(
                            frame.getSourceReference(), writer_error.get());
                      }
                    })) {
                  task_master.reportOtherError(
                      frame.getSourceReference(),
                      String.format("Attribute “%s” is not defined in frame.", entry.getKey()));
                }
              });
      return true;
    }
  }

  public class Sink<T> extends BaseSink<T> {
    private final Consumer<T> writer;

    Sink(Class<? extends T> clazz, Consumer<T> writer) {
      super(clazz);
      this.writer = writer;
    }

    @Override
    public void consume(Object input_result) {
      dispatch(input_result, writer);
    }
  }

  public static final Frame[] DAYS =
      makeFrames(
          new String[] {
            "sunday", "monday", "tuesday", "wednesday", "thrusday", "friday", "saturday"
          },
          DateFormatSymbols.getInstance().getShortWeekdays(),
          DateFormatSymbols.getInstance().getWeekdays());

  public static final Frame[] MONTHS =
      makeFrames(
          new String[] {
            "january",
            "februrary",
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

  public static final Map<String, Transform<ZonedDateTime>> TIME_ACCESSORS =
      new HashMap<String, Transform<ZonedDateTime>>();

  static {
    TIME_ACCESSORS.put("day_of_week", d -> DAYS[d.getDayOfWeek().getValue() % 7]);
    TIME_ACCESSORS.put("from_midnight", d -> (long) d.toLocalTime().toSecondOfDay());
    TIME_ACCESSORS.put("milliseconds", d -> (long) d.get(ChronoField.MILLI_OF_SECOND));
    TIME_ACCESSORS.put("second", d -> (long) d.getSecond());
    TIME_ACCESSORS.put("minute", d -> (long) d.getMinute());
    TIME_ACCESSORS.put("hour", d -> (long) d.getHour());
    TIME_ACCESSORS.put("day", d -> (long) d.getDayOfMonth());
    TIME_ACCESSORS.put("month", d -> MONTHS[d.getMonthValue() - 1]);
    TIME_ACCESSORS.put("year", d -> (long) d.getYear());
    TIME_ACCESSORS.put("week", d -> (long) d.get(WeekFields.ISO.weekOfWeekBasedYear()));
    TIME_ACCESSORS.put("day_of_year", d -> (long) d.getDayOfYear());
    TIME_ACCESSORS.put("epoch", d -> d.toEpochSecond());
    TIME_ACCESSORS.put("is_utc", d -> d.getOffset().equals(ZoneOffset.UTC));
    TIME_ACCESSORS.put("is_leap_year", d -> Year.isLeap(d.getYear()));
  }

  private static <T> Matcher<T> findMatcher(
      Class<? extends T> clazz, boolean allow_null, String custom_error) {
    if (clazz == String.class) {
      return (x, writer, error) -> {
        String str;
        if (x instanceof Stringish) {
          str = x.toString();
        } else if (x instanceof Boolean) {
          str = ((Boolean) x) ? "True" : "False";
        } else if (x instanceof Long) {
          str = x.toString();
        } else if (x instanceof Double) {
          str = x.toString();
        } else if (allow_null && x == Unit.NULL) {
          str = null;
        } else {
          error.set(custom_error);
          return false;
        }
        writer.accept((T) str);
        error.set(null);
        return true;
      };
    } else if (clazz == Stringish.class) {
      return (x, writer, error) -> {
        if (allow_null && x == Unit.NULL) {
          writer.accept(null);
          error.set(null);
          return true;
        }

        Stringish sish = Stringish.fromObject(x);
        if (sish == null) {
          error.set(custom_error);
          return false;
        }
        writer.accept((T) sish);
        error.set(null);
        return true;
      };
    } else if (clazz == Double.class) {
      return (x, writer, error) -> {
        if (allow_null && x == Unit.NULL) {
          writer.accept(null);
          error.set(null);
          return true;
        }
        double d;
        if (x instanceof Double) {
          d = (Double) x;
        } else if (x instanceof Long) {
          d = ((Long) x).doubleValue();
        } else {
          error.set(custom_error);
          return false;
        }
        writer.accept((T) (Double) d);
        error.set(null);
        return true;
      };
    } else if (clazz == Long.class
        || clazz == Boolean.class
        || clazz == byte[].class
        || clazz == Frame.class
        || clazz == Template.class
        || clazz == Object.class) {
      return (x, writer, error) -> {
        if (allow_null && x == Unit.NULL) {
          writer.accept(null);
          error.set(null);
          return true;
        } else if (clazz.isInstance(x)) {
          writer.accept((T) x);
          error.set(null);
          return true;
        } else {
          error.set(custom_error);
          return false;
        }
      };
    } else {
      return (input_result, writer, error) -> {
        if (allow_null && input_result == Unit.NULL) {
          writer.accept(null);
          error.set(null);
          return true;
        }
        if (!(input_result instanceof Frame)) {
          error.set(custom_error);
          return false;
        }
        if (input_result instanceof ReflectedFrame) {
          Object backing = ((ReflectedFrame) input_result).getBacking();
          if (clazz.isInstance(backing)) {
            writer.accept((T) backing);
            error.set(null);
            return true;
          }
        }
        error.set(custom_error);
        return false;
      };
    }
  }

  private static Frame[] makeFrames(String[] attrs, String[] short_names, String[] long_names) {
    final SourceReference time_src = new NativeSourceReference("<the big bang>");
    Frame[] result = new Frame[attrs.length];
    for (int i = 0; i < attrs.length; i++) {
      FixedFrame item = new FixedFrame(attrs[i], time_src);
      item.add("short_name", short_names[i]);
      item.add("long_name", long_names[i]);
      item.add("ordinal", i + 1);
      result[i] = item;
    }
    return result;
  }

  protected final Context context;
  private boolean first = true;
  private AtomicInteger interlock = new AtomicInteger(1);
  protected final SourceReference source_reference;

  public InterlockedLookup(
      TaskMaster task_master, SourceReference source_reference, Context context) {
    super(task_master);
    this.source_reference = source_reference;
    this.context = context;
  }

  public <R> Object correctOutput(Callable<R> compute) {
    try {
      R output = compute.call();
      if (output == null) {
        return Unit.NULL;
      } else if (output instanceof String) {
        return new SimpleStringish((String) output);
      } else if (output instanceof ZonedDateTime) {
        return ReflectedFrame.create(task_master, (ZonedDateTime) output, TIME_ACCESSORS);
      } else {
        return output;
      }
    } catch (Exception e) {
      task_master.reportOtherError(source_reference, e.getMessage());
      return BlackholeFuture.INSTANCE;
    }
  }

  public <T> Sink<T> find(Class<? extends T> clazz, Consumer<T> writer) {
    return new Sink<>(clazz, writer);
  }

  public <T> ListSink<T> findAll(Class<? extends T> clazz, Consumer<Map<String, T>> writer) {
    return new ListSink<>(clazz, writer);
  }

  protected abstract void resolve();

  @Override
  protected final void run() {
    if (first) {
      setup();
      first = false;
      if (interlock.decrementAndGet() > 0) {
        return;
      }
    }
    resolve();
  }

  protected abstract void setup();
}
