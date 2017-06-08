using System;
using System.Collections.Generic;

namespace Flabbergast {

public abstract class BaseFunctionInterop<R> : Future {
    private InterlockedLookup interlock;
    protected readonly SourceReference source_reference;
    protected readonly Context context;
    protected readonly Frame self;
    protected readonly Frame container;
    public BaseFunctionInterop(TaskMaster task_master, SourceReference source_ref,
                               Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
        this.self = self;
        this.container = container;
    }

    protected abstract R ComputeResult();

    protected abstract void PrepareLookup(InterlockedLookup interlock);

    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            PrepareLookup(interlock);
        }
        if (!interlock.Away()) return;
        try {
            R output = ComputeResult();
            if (!typeof(R).IsValueType && output.Equals(default(R))) {
                result = Unit.NULL;
            } else {
                result = typeof(R) == typeof(string) ? (object)new SimpleStringish((string)(object) output) : (object) output;
            }
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}

public class FunctionInterop<T, R> : BaseFunctionInterop<R> {
    private readonly string parameter;
    private readonly Func<T, R> func;
    private T input;

    public FunctionInterop(Func<T, R> func, string parameter, TaskMaster task_master, SourceReference source_ref,
                           Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
        this.parameter = parameter;
    }

    protected override R ComputeResult() {
        return func(input);
    }
    protected override void PrepareLookup(InterlockedLookup interlock) {
        interlock.Lookup<T>(x => this.input = x, parameter);
    }
}

public class FunctionInterop<T1, T2, R> : BaseFunctionInterop<R> {
    private readonly string parameter1;
    private readonly string parameter2;
    private readonly Func<T1, T2, R> func;
    private T1 input1;
    private T2 input2;

    public FunctionInterop(Func<T1, T2, R> func, string parameter1, string parameter2, TaskMaster task_master, SourceReference source_ref,
                           Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }
    protected override R ComputeResult() {
        return  func(input1, input2);
    }


    protected override void PrepareLookup(InterlockedLookup interlock) {
        interlock.Lookup<T1>(x => this.input1 = x, parameter1);
        interlock.Lookup<T2>(x => this.input2 = x, parameter2);
    }
}

public abstract class BaseMapFunctionInterop<T, R> : Future {

    private class MapFunction : Future {
        private readonly BaseMapFunctionInterop<T, R> owner;
        private readonly string arg_name;
        public MapFunction(TaskMaster task_master, BaseMapFunctionInterop<T, R> owner, string arg_name): base(task_master) {
            this.owner = owner;
            this.arg_name = arg_name;
        }

        public void Process(object input_value)  {
            bool correct;
            if (input_value is T) {
                correct = true;
            }
            else

                if (typeof(T) == typeof(string)) {
                    if (input_value is Stringish) {
                        input_value = input_value.ToString();
                        correct = true;
                    } else if (input_value is bool) {
                        input_value = ((bool)input_value) ?  "True" : "False";
                        correct = true;
                    } else if (input_value is long) {
                        input_value = ((long)input_value).ToString();
                        correct = true;
                    } else if (input_value is double) {
                        input_value = ((double)input_value).ToString();
                        correct = true;
                    } else {
                        correct = false;
                    }
                } else if (typeof(T) == typeof(double)) {
                    if (input_value is long) {
                        double d = (long)input_value;
                        input_value = d;
                        correct = true;
                    } else {
                        correct = false;
                    }
                } else {
                    correct = false;
                }
            if (correct) {
                try {
                    R output = owner.ComputeResult((T) input_value);
                    if (!typeof(R).IsValueType && output.Equals(default(R))) {
                        result = Unit.NULL;
                    } else {
                        result = typeof(R) == typeof(string) ? (object)new SimpleStringish((string)(object) output) : (object) output;
                    }
                    WakeupListeners();
                } catch (Exception e) {
                    task_master.ReportOtherError(owner.source_reference, e.Message);
                }
            } else {
                task_master.ReportOtherError(owner.source_reference, String.Format("“{0}” has type {1} but expected {2}.", arg_name, SupportFunctions.NameForType(input_value.GetType()), SupportFunctions.NameForType(typeof(T))));
            }

        }
        protected override void Run() {}
    }
    private InterlockedLookup interlock;
    protected readonly SourceReference source_reference;
    protected readonly Context context;
    protected readonly Frame self;
    protected readonly Frame container;
    private Frame input;

    public BaseMapFunctionInterop(TaskMaster task_master, SourceReference source_ref,
                                  Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
        this.self = self;
        this.container = container;
    }

    protected abstract R ComputeResult(T input);

    protected virtual void PrepareLookup(InterlockedLookup interlock) {
    }
    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.Lookup<Frame>(x => this.input = x, "args");
            PrepareLookup(interlock);
        }
        if (!interlock.Away()) return;
        var output_frame = new MutableFrame(task_master, source_reference, context, self);
        foreach (var name in input.GetAttributeNames()) {
            MapFunction arg_value = new MapFunction(task_master, this, name);
            ComputeValue thunk = (task_master, source_reference, context, self, container) => arg_value;
            output_frame.Set(name, thunk);
            input.GetOrSubscribe(name, arg_value.Process);
        }
        result = output_frame;
    }
}
public class MapFunctionInterop<T, R> : BaseMapFunctionInterop<T, R> {
    private readonly Func<T, R> func;

    public MapFunctionInterop(Func<T, R> func, TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
    }

    protected override R ComputeResult(T input) {
        return func(input);
    }
}
public class MapFunctionInterop<T1, T2, R> : BaseMapFunctionInterop<T1, R> {
    private Func<T1, T2, R> func;
    private string parameter;
    private T2 reference;

    public MapFunctionInterop(Func<T1, T2, R> func, string parameter, TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
        this.parameter = parameter;
    }
    protected override R ComputeResult(T1 input) {
        return  func(input, reference);
    }
    protected override void PrepareLookup(InterlockedLookup interlock) {
        interlock.Lookup<T2>(x => this.reference = x, parameter);
    }
}
}
