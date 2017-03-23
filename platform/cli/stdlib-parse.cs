using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class ParseDouble : Future {

    private InterlockedLookup interlock;
    private String input;

    private SourceReference source_reference;
    private Context context;

    public ParseDouble(TaskMaster master, SourceReference source_ref,
                       Context context, Frame self, Frame container) : base(master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => input = x, "arg");
        }
        if (!interlock.Away()) return;

        try {
            result = Convert.ToDouble(input);
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}

public class ParseInt : Future {

    private InterlockedLookup interlock;
    private String input;
    private int radix;

    private SourceReference source_reference;
    private Context context;

    public ParseInt(TaskMaster task_master, SourceReference source_ref,
                    Context context, Frame self, Frame container) : base(task_master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override void Run() {
        if (interlock == null) {
            interlock = new InterlockedLookup(this, task_master, source_reference, context);
            interlock.LookupStr(x => input = x, "arg");
            interlock.Lookup<long>(x => radix = (int) x, "radix");
        }
        if (!interlock.Away()) return;

        try {
            result = Convert.ToInt64(input, radix);
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}
}
