using System;
using System.Collections.Generic;
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

public class InterlockedLookup {
    private int interlock = 1;
    private bool away;
    private readonly Future owner;
    private readonly TaskMaster task_master;
    private readonly SourceReference source_reference;
    private readonly Context context;
    public InterlockedLookup(Future owner, TaskMaster task_master, SourceReference source_reference, Context context) {
        this.owner = owner;
        this.task_master = task_master;
        this.source_reference = source_reference;
        this.context = context;
    }

    private void LookupRaw<T>(Predicate<object>writer, params string[] names) {
        if (away) {
            throw new InvalidOperationException("Cannot lookup after finish.");
        }
        Interlocked.Increment(ref interlock);
        Future input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.Notify(input_result => {
            if (writer(input_result)) {
                if (Interlocked.Decrement(ref interlock) == 0) {
                    task_master.Slot(owner);
                }
            } else {
                task_master.ReportOtherError(source_reference, String.Format("“{0}” has type {1} but expected {2}.", String.Join(",", names), SupportFunctions.NameForType(input_result.GetType()), SupportFunctions.NameForType(typeof(T))));
            }
        });
    }

    public void Lookup<T>(Action<T> writer, params string[] names) {
        var underlying_type = Nullable.GetUnderlyingType(typeof(T));
        if (typeof(T) == typeof(string)) {
            LookupRaw<Stringish>(x => {
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
                    return false;
                }
                writer((T)(object)str);
                return true;
            }, names);
        } else if (typeof(T) == typeof(double)) {
            LookupRaw<double>(x => {
                double d;
                if (x is double) {
                    d = (double)x;
                } else if (x is long) {
                    d = (long)x;
                } else {
                    return false;
                }
                writer((T)(object)d);
                return true;
            }, names);
        } else if (underlying_type == typeof(double)) {
            LookupRaw < double?>(x => {
                double? d;
                if (x is double) {
                    d = (double)x;
                } else if (x is long) {
                    d = (long)x;
                } else if (x == Unit.NULL) {
                    d = null;
                } else {
                    return false;
                }
                writer((T)(object)d);
                return true;
            }, names);
        } else if (underlying_type != null) {
            LookupRaw<T>(x => {
                if (underlying_type.IsInstanceOfType(x)) {
                    writer((T)x);
                    return true;
                } else if (x == Unit.NULL) {
                    writer(default(T));
                    return true;
                } else {
                    return false;
                }
            }, names);
        } else {
            LookupRaw<T>(x => {
                if (x is T) {
                    writer((T)x);
                    return true;
                } else {
                    return false;
                }
            }, names);
        }
    }
    public void LookupStr(Action<String> writer, params string[] names) {
        Lookup<Stringish>(sish => writer(sish.ToString()), names);
    }
    public void LookupMarshalled<T>(String error, Action<T> writer, params string[] names) {
        if (away) {
            throw new InvalidOperationException("Cannot lookup after finish.");
        }
        Interlocked.Increment(ref interlock);
        Future input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.Notify(input_result => {
            if (!(input_result is Frame)) {
                task_master.ReportOtherError(source_reference, String.Format("“{0}” has type {1} but expected Frame.", String.Join(",", names), SupportFunctions.NameForType(input_result.GetType())));
                return;
            }
            if (input_result is ReflectedFrame) {
                Object backing = ((ReflectedFrame) input_result).Backing;
                if (backing is T) {
                    writer((T) backing);
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(owner);
                    }
                    return ;
                }
            }
            task_master.ReportOtherError(source_reference, string.Format(error, string.Join(".", names)));
        });
    }

    public bool Away() {
        if (away) {
            return true;
        }
        away = true;
        return Interlocked.Decrement(ref interlock) == 0;
    }
}
}
