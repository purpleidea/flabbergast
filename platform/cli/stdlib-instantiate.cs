using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Threading;

namespace Flabbergast {
public class Instantiation : InterlockedLookup, IEnumerable<string> {
    private string[] names;
    private Dictionary<string, object> overrides = new Dictionary<string, object>();
    private Frame container;
    private Template tmpl;
    public Instantiation(TaskMaster task_master, SourceReference source_reference, Context context, Frame container, params string[] names) : base(task_master, source_reference, context) {
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

    protected override void Setup() {
        var template_lookup = Find<Template>(x => tmpl = x);
        template_lookup.AllowDefault();
        template_lookup.Lookup(names);
    }
    protected override void Resolve() {
        var frame = new MutableFrame(task_master, new JunctionReference("instantiation", "<native>", 0, 0, 0, 0, source_reference, tmpl.SourceReference), Context.Append(context, tmpl.Context), container);
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
