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
}
