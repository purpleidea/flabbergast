using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading;

namespace Flabbergast {
/**
 * A computation that never completes.
 */
public class BlackholeFuture : Future {
    public readonly static Future INSTANCE = new BlackholeFuture();
    private BlackholeFuture() : base(null) {
    }
    protected override void Run() {
    }
}

public class FailureFuture : Future {
    private string message;
    private SourceReference source_reference;
    public FailureFuture(TaskMaster task_master, SourceReference reference, string message) : base(task_master) {
        this.source_reference = reference;
        this.message = message;
    }

    protected override void Run() {
        task_master.ReportOtherError(source_reference, message);
    }
}

/**
 * Holds a value for inclusion of a pre-computed value in a template.
 */
public class Precomputation : Future {
    public static ComputeValue Capture(object result) {
        return new Precomputation(result).ComputeValue;
    }
    public Precomputation(object result) : base(null) {
        this.result = result;
    }
    public Future ComputeValue(
        TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container) {
        return this;
    }
    protected override void Run() {
    }
}

public class Holder<T> {
    public T Value;
}

public abstract class InterlockedLookup : Future {
    public delegate bool Matcher<T>(object input, Action<T> writer, out string error);

    public static readonly Frame[] Days = MakeFrames(new [] {"sunday", "monday", "tuesday", "wednesday", "thrusday", "friday", "saturday"}, CultureInfo.CurrentCulture.DateTimeFormat.AbbreviatedDayNames, CultureInfo.CurrentCulture.DateTimeFormat.DayNames);
    public static readonly Frame[] Months = MakeFrames(new [] {"january", "februrary", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"}, CultureInfo.CurrentCulture.DateTimeFormat.AbbreviatedMonthNames, CultureInfo.CurrentCulture.DateTimeFormat.MonthNames);

    public static readonly Dictionary<string, Func<DateTime, object>> TIME_ACCESSORS = new Dictionary<string, Func<DateTime, object>> {
        { "day_of_week", d => Days[(int) d.DayOfWeek] },
        { "from_midnight", d => d.TimeOfDay.TotalSeconds },
        { "milliseconds", d => (long) d.Millisecond },
        { "second", d => (long) d.Second },
        { "minute", d => (long) d.Minute },
        { "hour", d => (long) d.Hour },
        { "day", d => (long) d.Day },
        { "month", d => Months[d.Month - 1] },
        { "year", d => (long) d.Year },
        { "week", d => (long) new GregorianCalendar().GetWeekOfYear(d, CalendarWeekRule.FirstDay, DayOfWeek.Sunday) },
        { "day_of_year", d => (long) d.DayOfYear },
        { "epoch",  d => (d - new DateTime(1970, 1, 1, 0, 0, 0, d.Kind)).TotalSeconds },
        { "is_utc", d => d.Kind == DateTimeKind.Utc },
        { "is_leap_year", d => DateTime.IsLeapYear(d.Year) }
    };

    private static Frame[] MakeFrames(string[] attrs, string[] short_names, string[] long_names) {
        var time_src = new NativeSourceReference("<the big bang>");
        var result = new Frame[attrs.Length];
        for (var i = 0; i < attrs.Length; i++) {
            result[i] = new FixedFrame(attrs[i], time_src) {
                { "short_name", short_names[i] },
                { "long_name", long_names[i] },
                { "ordinal", i + 1 }
            };
        }
        return result;
    }


    private static Matcher<T> FindMatcher<T>(string custom_error) {
        var underlying_type = Nullable.GetUnderlyingType(typeof(T));
        if (typeof(T) == typeof(string)) {
            return (object x, Action<T> writer, out string error) => {
                String str;
                if (x is Stringish) {
                    str = x.ToString();
                } else if (x is bool) {
                    str = ((bool)x) ?  "True" : "False";
                } else if (x is long) {
                    str = ((long)x).ToString();
                } else if (x is double) {
                    str = ((double)x).ToString();
                } else {
                    error = custom_error;
                    return false;
                }
                writer((T)(object)str);
                error = null;
                return true;
            };
        } else if (typeof(T) == typeof(Stringish)) {
            return (object x, Action<T> writer, out string error) => {
                var sish = Stringish.FromObject(x);
                if (sish == null) {
                    error = custom_error;
                    return false;
                }
                writer((T)(object)sish);
                error = null;
                return true;
            };
        } else if (typeof(T) == typeof(double)) {
            return (object x, Action<T> writer, out string error) => {
                double d;
                if (x is double) {
                    d = (double)x;
                } else if (x is long) {
                    d = (long)x;
                } else {
                    error = custom_error;
                    return false;
                }
                writer((T)(object)d);
                error = null;
                return true;
            };
        } else if (underlying_type == typeof(double)) {
            return (object x, Action<T> writer, out string error) => {
                double? d;
                if (x is double) {
                    d = (double)x;
                } else if (x is long) {
                    d = (long)x;
                } else if (x == Unit.NULL) {
                    d = null;
                } else {
                    error = custom_error;
                    return false;
                }
                writer((T)(object)d);
                error = null;
                return true;
            };
        } else if (underlying_type != null && (underlying_type == typeof(long) || underlying_type == typeof(bool))) {
            return (object x, Action<T> writer, out string error) => {
                if (underlying_type.IsInstanceOfType(x)) {
                    writer((T)x);
                    error = null;
                    return true;
                } else if (x == Unit.NULL) {
                    writer(default(T));
                    error = null;
                    return true;
                } else {
                    error = custom_error;
                    return false;
                }
            };
        } else if (typeof(T) == typeof(long) || typeof(T) == typeof(bool) || typeof(T) == typeof(byte[]) || typeof(T) == typeof(Frame) || typeof(T) == typeof(Template) || typeof(T) == typeof(object)) {
            return (object x, Action<T> writer, out string error) => {
                if (x is T) {
                    writer((T)x);
                    error = null;
                    return true;
                } else {
                    error = custom_error;
                    return false;
                }
            };
        } else {
            return (object input_result, Action<T> writer, out string error) => {
                if (!(input_result is Frame)) {
                    error = custom_error;
                    return false;
                }
                if (input_result is ReflectedFrame) {
                    object backing = ((ReflectedFrame) input_result).Backing;
                    if (backing is T) {
                        writer((T) backing);
                        error = null;
                        return true;
                    }
                }
                error = custom_error;
                return false;
            };
        }
    }
    public abstract class BaseSink<T> {
        protected readonly InterlockedLookup owner;
        private readonly Dictionary<Type, bool> types = new Dictionary<Type, bool>();
        private readonly List<Matcher<T>> handlers = new List<Matcher<T>>();
        protected string names;

        internal BaseSink(InterlockedLookup owner) {
            this.owner = owner;
        }

        protected void Add<X>(Matcher<T> matcher) {
            types[typeof(X)] = true;
            handlers.Add(matcher);
        }
        public void Allow<X>(Func<X, T> converter, string custom_error = null) {
            var matcher =  FindMatcher<X>(custom_error);
            Add<X>((object input, Action<T> writer, out string error) => matcher(input, x => writer(converter(x)), out error));

        }

        public void AllowDefault(string custom_error = null) {
            Add<T>(FindMatcher<T>(custom_error));
            if (typeof(T) == typeof(DateTime)) {
                var timeObj = AllowObject<Holder<long>>(x => (T)(object) new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc).AddSeconds(x.Value));
                timeObj.Add<long>("epoch", (x, v) => x.Value = v, "Time-based tuple must have “epoch” attribute.");
            }

        }

        public ObjectSink<X> AllowObject<X>(Func<X, T> converter) where X: new() {
            var obj_sink = new ObjectSink<X>(owner);
            Add<Frame>((object input,  Action<T>writer, out string error) => obj_sink.Matcher(input, x => writer(converter(x)), out error));
            return obj_sink;
        }


        protected void Dispatch(object input_result, Action<T> writer) {
            string custom_error = null;
            foreach (var handler in handlers) {
                string current_error;
                if (handler(input_result, writer, out current_error)) {
                    if (Interlocked.Decrement(ref owner.interlock) == 0) {
                        owner.task_master.Slot(owner);
                    }
                    return;
                } else {
                    custom_error = custom_error ?? current_error;
                }
            }
            owner.task_master.ReportOtherError(owner.source_reference, custom_error ?? String.Format("“{0}” has type {1} but expected {2}.", names, SupportFunctions.NameForType(input_result.GetType()),
                                               types.Keys.Select(type => SupportFunctions.NameForType(type)).Aggregate((l, r) => l + " or " + r)));
        }

        protected abstract void HandleResult(object result);
        public void Lookup(params string[] names) {
            if (!owner.first) {
                throw new InvalidOperationException("Cannot lookup after setup.");
            }
            this.names = String.Join(",", names);
            Interlocked.Increment(ref owner.interlock);
            Future input_lookup = new Lookup(owner.task_master, owner.source_reference, names, owner.context);
            input_lookup.Notify(HandleResult);
        }

    }
    public class Sink<T> : BaseSink<T> {
        private readonly Action<T> writer;
        internal Sink(InterlockedLookup owner, Action<T> writer) : base(owner) {
            this.writer = writer;
        }
        protected override void HandleResult(object input_result) {
            Dispatch(input_result, writer);
        }
    }
    public class ListSink<T> : BaseSink<T> {
        private readonly Action<SortedDictionary<string, T>> writer;

        internal ListSink(InterlockedLookup owner, Action<SortedDictionary<string, T>> writer) : base(owner) {
            this.writer = writer;
        }

        protected override void HandleResult(object input_result) {
            if (!(input_result is Frame)) {
                owner.task_master.ReportOtherError(owner.source_reference, String.Format("“{0}” has type {1} but expected Frame.", names));
                return;
            }
            var frame = (Frame) input_result;
            var results = new SortedDictionary<string, T>();
            var listInterlock = (int) frame.Count;
            Interlocked.Add(ref owner.interlock, (int) frame.Count);
            foreach (var name in frame.GetAttributeNames()) {
                var arg_name = name;
                frame.GetOrSubscribe(arg_name, arg_result => Dispatch(arg_result, x => {
                    results[arg_name] = x;
                    if (Interlocked.Decrement(ref listInterlock) == 0) {

                        writer(results);
                    }
                }));
            }
            if (Interlocked.Decrement(ref owner.interlock) == 0) {
                owner.task_master.Slot(owner);
            }
            return;
        }
    }

    public class ObjectSink<T> where T: new() {
        private delegate bool ConsumeField(T obj, object result, out string error);
        private readonly InterlockedLookup owner;
        private readonly Dictionary<string, ConsumeField> writers = new Dictionary<string, ConsumeField>();

        internal ObjectSink(InterlockedLookup owner) {
            this.owner = owner;
        }

        internal bool Matcher(object input, Action<T> writer, out string error) {
            error = null;
            if (!(input is Frame)) {
                return false;
            }
            var frame = (Frame) input;
            var result = new T();
            Interlocked.Add(ref owner.interlock, writers.Count);
            foreach (var entry in writers) {
                var sink = entry.Value;
                if (!frame.GetOrSubscribe(entry.Key, arg_result => {
                string writer_error;
                if (sink(result, arg_result, out writer_error)) {

                        if (Interlocked.Decrement(ref owner.interlock) == 0) {
                            owner.task_master.Slot(owner);
                        }
                    } else {
                        owner.task_master.ReportOtherError(frame.SourceReference, writer_error);

                    }
                })) {
                    owner.task_master.ReportOtherError(frame.SourceReference, String.Format("Attribute “{0}” is not defined in frame.", entry.Key));
                }
            }
            return true;
        }

        public void Add<X>(string name, Action<T, X> writer, string custom_error = null) {
            var matcher = FindMatcher<X>(custom_error);
            writers[name] = (T obj, object val, out string error) => matcher(val, x => writer(obj, x), out error) ;
        }
    }

    private bool first = true;
    private int interlock = 1;
    protected readonly SourceReference source_reference;
    protected readonly Context context;
    public InterlockedLookup(TaskMaster task_master, SourceReference source_reference, Context context) : base(task_master) {
        this.source_reference = source_reference;
        this.context = context;
    }
    public object CorrectOutput<R>(Func<R> compute) {
        try {
            R output = compute();
            if (!typeof(R).IsValueType && EqualityComparer<R>.Default.Equals(output, default(R))) {
                return Unit.NULL;
            } else if (typeof(R) == typeof(string)) {
                return new SimpleStringish((string)(object) output);
            } else if (typeof(R) == typeof(DateTime)) {
                return ReflectedFrame.Create<DateTime>(task_master, (DateTime)(object)output, TIME_ACCESSORS);
            } else {
                return  output;
            }
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
            return null;
        }
    }

    public Sink<T> Find<T>(Action<T> writer) {
        return new Sink<T>(this, writer);
    }

    public ListSink<T> FindAll<T>(Action<SortedDictionary<string, T>> writer) {
        return new ListSink<T>(this, writer);
    }


    protected abstract void Setup();

    protected abstract void Resolve();

    protected sealed override void Run() {
        if (first) {
            Setup();
            first = false;
            if (Interlocked.Decrement(ref interlock) > 0) {
                return;
            }
        }
        Resolve();
    }
}
}
