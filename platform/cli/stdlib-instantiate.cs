using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class Instantiation : Computation, IEnumerable<string> {
    private string[] names;
    private Dictionary<string, object> overrides = new Dictionary<string, object>();
    private Context context;
    private Frame container;
    private Template tmpl;
    private SourceReference src_ref;
    private int interlock = 2;
    public Instantiation(TaskMaster task_master, SourceReference src_ref, Context context, Frame container, params string[] names) : base(task_master) {
        this.src_ref = src_ref;
        this.context = context;
        this.container = container;
        this.names = names;
    }

    public void Add(string name, object val) {
        overrides.Add(name, val);
    }

    public IEnumerator<string> GetEnumerator() {
        return overrides.Keys.GetEnumerator();
    }

    System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() {
        return this.GetEnumerator();
    }

    protected override void Run() {
        if (tmpl == null) {
            new Lookup(task_master, src_ref, names, context).Notify(tmpl_result => {
                if (tmpl_result is Template) {
                    tmpl = (Template) tmpl_result;
                    if (Interlocked.Decrement(ref interlock) == 0) {
                        task_master.Slot(this);
                    }
                } else {
                    task_master.ReportOtherError(src_ref, string.Format("Expected “{0}” to be a Template but got {1}.", string.Join(".", names), Stringish.NameForType(tmpl_result.GetType())));
                }
            });
            if (Interlocked.Decrement(ref interlock) > 0) {
                return;
            }
        }
        var frame = new MutableFrame(task_master, new JunctionReference("instantiation", "<native>", 0, 0, 0, 0, src_ref, tmpl.SourceReference), Context.Append(context, tmpl.Context), container);
        foreach (var entry in overrides) {
            frame.Set(entry.Key, entry.Value);
        }
        foreach (var name in tmpl.GetAttributeNames()) {
            if (!overrides.ContainsKey(name)) {
                frame.Set(name, tmpl[name]);
            }
        }
        result = frame;
    }
}
}
