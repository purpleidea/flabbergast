package flabbergast.time;

import flabbergast.*;
import flabbergast.ReflectedFrame.Transform;

import java.text.DateFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    private static final Map<String, Transform<DateTime>> time_accessors = new HashMap<String, Transform<DateTime>>();
    static {
        time_accessors.put("day_of_week", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return DAYS[d.dayOfWeek().get() % 7];
            }
        });
        time_accessors.put("from_midnight", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getSecondOfDay();
            }
        });
        time_accessors.put("milliseconds", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getMillisOfSecond();
            }
        });
        time_accessors.put("second", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getSecondOfMinute();
            }
        });
        time_accessors.put("minute", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getMinuteOfHour();
            }
        });
        time_accessors.put("hour", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getHourOfDay();
            }
        });
        time_accessors.put("day", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getDayOfMonth();
            }
        });
        time_accessors.put("month", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return MONTHS[d.getMonthOfYear() - 1];
            }
        });
        time_accessors.put("year", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getYear();
            }
        });
        time_accessors.put("week", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getWeekOfWeekyear();
            }
        });
        time_accessors.put("day_of_year", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return (long) d.getDayOfYear();
            }
        });
        time_accessors.put("epoch", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return d.getMillis() / 1000;
            }
        });
        time_accessors.put("is_utc", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return d.getZone() == DateTimeZone.UTC;
            }
        });
        time_accessors.put("is_leap_year", new Transform<DateTime>() {
            @Override
            public Object invoke(DateTime d) {
                return d.year().isLeap();
            }
        });
    }

    public static Frame makeTime(DateTime time, TaskMaster task_master) {
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

    protected void getUnixTime(final ConsumeDateTime target,
                               final Context context) {
        new Lookup(task_master, source_reference, new String[] {"epoch"},
        context).listen(new ConsumeResult() {
            @Override
            public void consume(final Object epoch) {
                if (epoch instanceof Long) {
                    DateTime time = new DateTime(1970, 1, 1, 0, 0, 0,
                                                 DateTimeZone.UTC);
                    long epochl = (Long) epoch;
                    target.invoke(time.plusSeconds((int) epochl));
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
    protected void getTime(final ConsumeDateTime target, final String name) {
        Future lookup = new Lookup(task_master, source_reference,
                                   new String[] {name}, context);
        lookup.listen(new ConsumeResult() {
            @Override
            public void consume(Object result) {
                if (result instanceof ReflectedFrame
                        && ((ReflectedFrame) result).getBacking() instanceof DateTime) {
                    target.invoke((DateTime)((ReflectedFrame) result)
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

    protected Frame makeTime(DateTime time) {
        return makeTime(time, task_master);
    }
}
