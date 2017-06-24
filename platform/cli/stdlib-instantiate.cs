using System.Collections;
using System.Collections.Generic;

namespace Flabbergast
{
    public class Instantiation : InterlockedLookup, IEnumerable<string>
    {
        private readonly Frame container;
        private readonly string[] names;
        private readonly Dictionary<string, object> overrides = new Dictionary<string, object>();
        private Template tmpl;

        public Instantiation(TaskMaster task_master, SourceReference source_reference, Context context, Frame container,
            params string[] names) : base(task_master, source_reference, context)
        {
            this.container = container;
            this.names = names;
        }

        public IEnumerator<string> GetEnumerator()
        {
            return overrides.Keys.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        public void Add(string name, object val)
        {
            overrides.Add(name, val);
        }

        protected override void Setup()
        {
            var template_lookup = Find<Template>(x => tmpl = x);
            template_lookup.AllowDefault();
            template_lookup.Lookup(names);
        }

        protected override void Resolve()
        {
            var frame = new MutableFrame(task_master,
                new JunctionReference("instantiation", "<native>", 0, 0, 0, 0, source_reference, tmpl.SourceReference),
                Context.Append(context, tmpl.Context), container);
            foreach (var entry in overrides)
                frame.Set(entry.Key, entry.Value);
            foreach (var name in tmpl.GetAttributeNames())
                if (!overrides.ContainsKey(name))
                    frame.Set(name, tmpl[name]);
            result = frame;
        }
    }
}