package flabbergast.time;

import flabbergast.*;
import flabbergast.ReflectedFrame.Transform;

import java.time.Instant;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.text.DateFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class BaseTime extends Future {
    private static final DateFormatSymbols symbols = new DateFormatSymbols();
    public static final Frame[] DAYS = makeFrames(
                                           new String[] {"sunday", "monday", "tuesday", "wednesday",
                                                   "thrusday", "friday", "saturday"
                                                        },
                                           symbols.getShortWeekdays(), symbols.getWeekdays());
    public static final Frame[] MONTHS = makeFrames(new String[] {"january",
                                         "februrary", "march", "april", "may", "june", "july", "august",
                                         "september", "october", "november", "december"
                                                                 },
                                         symbols.getShortMonths(), symbols.getMonths());

    private static final SourceReference time_src = new NativeSourceReference(
        "<the big bang>");
    private static final Frame ALL_DAYS = new FixedFrame("days", time_src)
    .add(DAYS);
    private static final Frame ALL_MONTHS = new FixedFrame("months", time_src)
    .add(MONTHS);

    public static Frame getDays() {
        return ALL_DAYS;
    }
    public static Frame getMonths() {
        return ALL_MONTHS;
    }

    private static Frame[] makeFrames(String[] attrs, String[] short_names,
                                      String[] long_names) {
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

    private static final Map<String, Transform<ZonedDateTime>> time_accessors = new HashMap<String, Transform<ZonedDateTime>>();
    static {
        time_accessors.put("day_of_week", d -> DAYS[d.getDayOfWeek().getValue() % 7]);
        time_accessors.put("from_midnight", d -> (long) d.toLocalTime().toSecondOfDay());
        time_accessors.put("milliseconds", d -> (long) d.get(ChronoField.MILLI_OF_SECOND));
        time_accessors.put("second", d-> (long) d.getSecond());
        time_accessors.put("minute", d -> (long) d.getMinute());
        time_accessors.put("hour", d -> (long) d.getHour());
        time_accessors.put("day", d -> (long) d.getDayOfMonth());
        time_accessors.put("month", d -> MONTHS[d.getMonthValue() - 1]);
        time_accessors.put("year", d -> (long) d.getYear());
        time_accessors.put("week", d -> (long) d.get(WeekFields.ISO.weekOfWeekBasedYear()));
        time_accessors.put("day_of_year", d -> (long) d.getDayOfYear());
        time_accessors.put("epoch", d -> d.toEpochSecond());
        time_accessors.put("is_utc", d -> d.getOffset().equals(ZoneOffset.UTC));
        time_accessors.put("is_leap_year", d -> Year.isLeap(d.getYear()));
    }

    public static Frame makeTime(ZonedDateTime time, TaskMaster task_master) {
        return ReflectedFrame.create(task_master, time, time_accessors);
    }

    protected AtomicInteger interlock = new AtomicInteger();
    protected final SourceReference source_reference;
    protected final Context context;
    protected final Frame container;

    public BaseTime(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) {
        super(task_master);
        this.source_reference = source_ref;
        this.context = context;
        this.container = self;
    }

    protected void getUnixTime(final Consumer<ZonedDateTime> target,
                               final Context context) {
        new Lookup(task_master, source_reference, new String[] {"epoch"},
        context).listen(new ConsumeResult() {
            @Override
            public void consume(final Object epoch) {
                if (epoch instanceof Long) {
                    long epochl = (Long) epoch;
                    target.accept(ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochl), ZoneId.of("UTC")));
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(BaseTime.this);
                    }
                } else {
                    task_master.reportOtherError(source_reference,
                                                 "“epoch” must be an Int.");
                }
            }
        });
    }
    protected void getTime(final Consumer<ZonedDateTime> target, final String name) {
        Future lookup = new Lookup(task_master, source_reference,
                                   new String[] {name}, context);
        lookup.listen(new ConsumeResult() {
            @Override
            public void consume(Object result) {
                if (result instanceof ReflectedFrame
                        && ((ReflectedFrame) result).getBacking() instanceof ZonedDateTime) {
                    target.accept((ZonedDateTime)((ReflectedFrame) result)
                                  .getBacking());
                    if (interlock.decrementAndGet() == 0) {
                        task_master.slot(BaseTime.this);
                    }
                } else if (result instanceof Frame) {
                    getUnixTime(target, Context.prepend((Frame) result, null));
                } else {
                    task_master.reportOtherError(source_reference,
                                                 String.format("“%s” must be a Frame.", name));
                }
            }
        });
    }

    protected Frame makeTime(ZonedDateTime time) {
        return makeTime(time, task_master);
    }
}
