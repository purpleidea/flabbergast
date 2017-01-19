using System;
using System.Collections.Generic;
using System.Threading;

namespace Flabbergast {
/**
 * A computation that never completes.
 */
public class BlackholeComputation : Computation {
    public readonly static Computation INSTANCE = new BlackholeComputation();
    private BlackholeComputation() : base(null) {
    }
    protected override void Run() {
    }
}

public class FailureComputation : Computation {
    private string message;
    private SourceReference source_reference;
    public FailureComputation(TaskMaster task_master, SourceReference reference, string message) : base(task_master) {
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
public class Precomputation : Computation {
    public static ComputeValue Capture(object result) {
        return new Precomputation(result).ComputeValue;
    }
    public Precomputation(object result) : base(null) {
        this.result = result;
    }
    public Computation ComputeValue(
        TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container) {
        return this;
    }
    protected override void Run() {
    }
}

public class InterlockedLookup {
    private int interlock = 1;
    private bool away;
    private readonly Computation owner;
    private readonly TaskMaster task_master;
    private readonly SourceReference source_reference;
    private readonly Context context;
    public InterlockedLookup(Computation owner, TaskMaster task_master, SourceReference source_reference, Context context) {
        this.owner = owner;
        this.task_master = task_master;
        this.source_reference = source_reference;
        this.context = context;
    }

    private void Lookup<T>(Predicate<T>writer, params string[] names) {
        if (away) {
            throw new InvalidOperationException("Cannot lookup after finish.");
        }
        Interlocked.Increment(ref interlock);
        Computation input_lookup = new Lookup(task_master, source_reference, names, context);
        input_lookup.Notify(input_result => {
            if (input_result is T) {
                if (writer((T) input_result) && Interlocked.Decrement(ref interlock) == 0) {
                    task_master.Slot(owner);
                }
            } else {
                task_master.ReportOtherError(source_reference, String.Format("“{0}” has type {1} but expected {2}.", String.Join(",", names), SupportFunctions.NameForType(input_result.GetType()), SupportFunctions.NameForType(typeof(T))));
            }
        });
    }

    public void Lookup<T>(Action<T> writer, params string[] names) {
        Lookup<T>(x => {
            writer(x);
            return true;
        }, names);
    }
    public void LookupStr(Action<String> writer, params string[] names) {
        Lookup<Stringish>(sish => writer(sish.ToString()), names);
    }
    public void LookupMarshalled<T>(String error, Action<T> writer, params string[] names) {
        Lookup<Frame>(return_value => {
            if (return_value is ReflectedFrame) {
                Object backing = ((ReflectedFrame) return_value).Backing;
                if (backing is T) {
                    writer((T) backing);
                    return true;
                }
            }
            task_master .ReportOtherError(source_reference, string.Format(error, string.Join(".", names)));
            return false;
        }, names);
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
