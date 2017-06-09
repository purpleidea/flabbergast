using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class ParseDouble : BaseMapFunctionInterop<string, double> {
    public ParseDouble(TaskMaster task_master, SourceReference source_ref,
                       Context context, Frame self, Frame container) : base(task_master, source_ref,
                               context, self, container) {
    }
    protected override double ComputeResult(string input) {
        return Convert.ToDouble(input);
    }
}

public class ParseInt : BaseMapFunctionInterop<string, long> {

    private int radix;

    public ParseInt(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) : base(task_master, source_ref,
                            context, self, container) {
    }
    protected override long ComputeResult(string input) {
        return Convert.ToInt64(input, radix);
    }
    protected override void PrepareLookup(InterlockedLookup interlock) {
        interlock.Lookup<long>(x => radix = (int) x, "radix");
    }
}
}
