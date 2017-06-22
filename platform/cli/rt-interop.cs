using System;
using System.Collections.Generic;

namespace Flabbergast {

public abstract class BaseFunctionInterop<R> : InterlockedLookup {
    protected readonly Frame self;
    protected readonly Frame container;
    public BaseFunctionInterop(TaskMaster task_master, SourceReference source_reference,
                               Context context, Frame self, Frame container) : base(task_master, source_reference, context) {
        this.self = self;
        this.container = container;
    }

    protected abstract R ComputeResult();

    protected sealed override void Resolve() {
        result = CorrectOutput(ComputeResult);
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
    protected override void Setup() {
        var input_lookup = Find<T>(x => this.input = x);
        input_lookup.AllowDefault();
        input_lookup.Lookup(parameter);
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


    protected override void Setup() {
        var input1_lookup = Find<T1>(x => this.input1 = x);
        input1_lookup.AllowDefault();
        input1_lookup.Lookup(parameter1);
        var input2_lookup = Find<T2>(x => this.input2 = x);
        input2_lookup.AllowDefault();
        input2_lookup.Lookup(parameter2);
    }
}

public abstract class BaseMapFunctionInterop<T, R> : InterlockedLookup {

    protected readonly Frame self;
    protected readonly Frame container;
    private SortedDictionary<string, T> input;

    public BaseMapFunctionInterop(TaskMaster task_master, SourceReference source_ref,
                                  Context context, Frame self, Frame container) : base(task_master, source_ref, context) {
        this.self = self;
        this.container = container;
    }

    protected sealed override void Resolve() {
        var output_frame = new MutableFrame(task_master, source_reference, context, self);
        foreach (var entry in input) {
            var output = CorrectOutput(() => ComputeResult(entry.Value));
            if (output != null) {
                output_frame.Set(entry.Key, output);
            }
        }
        result = output_frame;
    }
    protected abstract R ComputeResult(T input);

    protected sealed override void Setup() {
        var args = FindAll<T>(x => input = x);
        args.AllowDefault();
        args.Lookup("args");
        SetupExtra();
    }
    protected virtual void SetupExtra() {
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
        return func(input, reference);
    }
    protected override void SetupExtra() {
        var ref_lookup = Find<T2>(x => this.reference = x);
        ref_lookup.AllowDefault();
        ref_lookup.Lookup(parameter);
    }
}
public class MapFunctionInterop<T1, T2, T3, R> : BaseMapFunctionInterop<T1, R> {
    private Func<T1, T2, T3, R> func;
    private string parameter1;
    private string parameter2;
    private T2 reference1;
    private T3 reference2;

    public MapFunctionInterop(Func<T1, T2, T3, R> func, string parameter1, string parameter2, TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }
    protected override R ComputeResult(T1 input) {
        return  func(input, reference1, reference2);
    }
    protected override void SetupExtra() {
        var ref1_lookup = Find<T2>(x => this.reference1 = x);
        ref1_lookup.AllowDefault();
        ref1_lookup.Lookup(parameter1);
        var ref2_lookup = Find<T3>(x => this.reference2 = x);
        ref2_lookup.AllowDefault();
        ref2_lookup.Lookup(parameter2);
    }
}
public class MapFunctionInterop<T1, T2, T3, T4, R> : BaseMapFunctionInterop<T1, R> {
    private Func<T1, T2, T3, T4, R> func;
    private string parameter1;
    private string parameter2;
    private string parameter3;
    private T2 reference1;
    private T3 reference2;
    private T4 reference3;

    public MapFunctionInterop(Func<T1, T2, T3, T4, R> func, string parameter1, string parameter2, string parameter3, TaskMaster task_master, SourceReference source_ref,
                              Context context, Frame self, Frame container) : base(task_master, source_ref, context, self, container) {
        this.func = func;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
        this.parameter3 = parameter3;
    }
    protected override R ComputeResult(T1 input) {
        return func(input, reference1, reference2, reference3);
    }
    protected override void SetupExtra() {
        var ref1_lookup = Find<T2>(x => this.reference1 = x);
        ref1_lookup.AllowDefault();
        ref1_lookup.Lookup(parameter1);
        var ref2_lookup = Find<T3>(x => this.reference2 = x);
        ref2_lookup.AllowDefault();
        ref2_lookup.Lookup(parameter2);
        var ref3_lookup = Find<T4>(x => this.reference3 = x);
        ref3_lookup.AllowDefault();
        ref3_lookup.Lookup(parameter3);
    }
}

public abstract class BaseReflectedInterop<R> : InterlockedLookup {
    protected readonly Frame self;
    protected readonly Frame container;
    public BaseReflectedInterop(TaskMaster task_master, SourceReference source_reference,
                                Context context, Frame self, Frame container)  : base(task_master, source_reference, context) {
        this.self = self;
        this.container = container;
    }

    protected abstract Dictionary<string, Func<R, object>> GetAccessors();

    protected abstract R ComputeResult();

    protected sealed override void Resolve() {
        try {
            result = ReflectedFrame.Create<R>(task_master, ComputeResult(), GetAccessors());
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}
}
