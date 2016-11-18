using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class ParseDouble : Computation {

    private int interlock = 2;
    private String input;

    private SourceReference source_reference;
    private Context context;

    public ParseDouble(TaskMaster master, SourceReference source_ref,
                       Context context, Frame self, Frame container) : base(master) {
        this.source_reference = source_ref;
        this.context = context;
    }

    protected override void Run() {
        if (input == null) {
            var input_lookup = new Lookup(task_master, source_reference,
                                          new String[] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference,
                                                 "Input argument must be a string.");
                }
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return;
            }
        }

        try {
            result = Convert.ToDouble(input);
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}

public class ParseInt : Computation {

    private int interlock = 3;
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
        if (input == null) {
            var input_lookup = new Lookup(task_master, source_reference,
                                          new String[] {"arg"}, context);
            input_lookup.Notify(input_result => {
                if (input_result is Stringish) {
                    input = input_result.ToString();
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference,
                                                 "Input argument must be a string.");
                }
            });

            var radix_lookup = new Lookup(task_master, source_reference,
                                          new String[] {"radix"}, context);
            radix_lookup.Notify(radix_result => {
                if (radix_result is Int64) {
                    radix = (int)(long)radix_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(source_reference,
                                                 "Input argument must be a string.");
                }
            });

            if (Interlocked.Decrement(ref interlock) > 0) {
                return;
            }
        }

        try {
            result = Convert.ToInt64(input, radix);
        } catch (Exception e) {
            task_master.ReportOtherError(source_reference, e.Message);
        }
    }
}
}
