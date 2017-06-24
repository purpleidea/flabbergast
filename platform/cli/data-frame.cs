using System;
using System.Collections;
using System.Collections.Generic;
using System.Dynamic;
using System.IO;
using System.Linq;

namespace Flabbergast
{
    /**
     * A Frame in the Flabbergast language.
     */

    public abstract class Frame : DynamicObject, IAttributeNames
    {
        public Frame(TaskMaster task_master, SourceReference source_ref, Context context, Frame container) : this(
            SupportFunctions.OrdinalName(task_master.NextId()), source_ref, context, container)
        {
        }

        public Frame(string id, SourceReference source_ref, Context context, Frame container) : this(
            new SimpleStringish(id), source_ref, context, container)
        {
        }

        public Frame(Stringish id, SourceReference source_ref, Context context, Frame container)
        {
            SourceReference = source_ref;
            Context = Context.Prepend(this, context);
            Container = container ?? this;
            Id = id;
        }

        /**
        * The containing frame, or null for file-level frames.
        */
        public Frame Container { get; }

        /**
        * The lookup context when this frame was created and any of its ancestors.
        */
        public Context Context { get; }

        public abstract long Count { get; }

        public Stringish Id { get; }
        /**
        * Access the functions in the frames. Frames should not be mutated, but this
        * policy is not enforced by this class; it must be done in the calling code.
        */

        public abstract object this[string name] { get; }

        /**
        * The stack trace when this frame was created.
        */
        public SourceReference SourceReference { get; }

        public abstract IEnumerable<string> GetAttributeNames();

        public override IEnumerable<string> GetDynamicMemberNames()
        {
            return GetAttributeNames();
        }

        /**
        * Access a value if available, or be notified upon completion.
        * Returns: true if the value was available, false if the name does not exist.
        */

        public bool GetOrSubscribe(string name, ConsumeResult consumer)
        {
            var result = this[name];
            if (result == null)
                return false;

            if (result is Future)
                ((Future)result).Notify(consumer);
            else
                consumer(result);
            return true;
        }

        /**
        * Check if an attribute name is present in the frame.
        */
        public abstract bool Has(string name);

        public bool Has(Stringish name)
        {
            return Has(name.ToString());
        }

        public static Frame Through(TaskMaster task_master, SourceReference source_ref, long start, long end,
            Context context, Frame container)
        {
            var result = new MutableFrame(task_master, source_ref, context, container);
            if (end < start)
                return result;
            for (long it = 0; it <= end - start; it++)
                result.Set(it + 1, start + it);
            return result;
        }

        public override bool TryGetIndex(GetIndexBinder binder, object[] indexes, out object result)
        {
            result = null;
            return false;
        }

        public Stringish RenderTrace(Stringish prefix)
        {
            var writer = new StringWriter();
            var seen = new Dictionary<SourceReference, bool>();
            SourceReference.Write(writer, prefix.ToString(), seen);
            return new SimpleStringish(writer.ToString());
        }

        public override bool TryGetMember(GetMemberBinder binder, out object result)
        {
            var name = binder.Name;
            if (binder.IgnoreCase && char.IsUpper(name, 0))
                name = char.ToLower(name[0]) + name.Substring(1);
            result = this[name];
            return result != null;
        }
    }

    public class MutableFrame : Frame
    {
        private readonly IDictionary<string, object> attributes = new SortedDictionary<string, object>();
        protected readonly TaskMaster task_master;
        private List<Future> unslotted = new List<Future>();

        public MutableFrame(TaskMaster task_master, SourceReference source_ref, Context context, Frame container) :
            base(task_master, source_ref, context, container)
        {
            this.task_master = task_master;
        }

        public override long Count => attributes.Count;

        public override object this[string name]
        {
            get
            {
                // If this frame is being looked at, then all its pending attributes should
                // be slotted.
                Slot();
                object result;
                attributes.TryGetValue(name, out result);
                return result;
            }
        }

        public override IEnumerable<string> GetAttributeNames()
        {
            return attributes.Keys;
        }

        public override bool Has(string name)
        {
            return attributes.ContainsKey(name);
        }

        public void Set(long ordinal, object value)
        {
            Set(SupportFunctions.OrdinalNameStr(ordinal), value);
        }

        public void Set(string name, object value)
        {
            if (value == null)
                return;
            if (attributes.ContainsKey(name))
                throw new InvalidOperationException("Redefinition of attribute " + name + ".");
            if (value is ComputeValue)
            {
                var computation = ((ComputeValue)value)(task_master, SourceReference, Context, this, Container);
                attributes[name] = computation;
                /*
                * When this computation has completed, replace its value in the frame.
                */
                computation.NotifyDelayed(result => { attributes[name] = result; });
                /*
                * If the value is a computation, it cannot be slotted for execution
                * since it might depend on lookups that reference this frame. Therefore, put
                * it in a queue for later activation.
                */
                unslotted.Add(computation);
            }
            else
            {
                if (value is MutableFrame)
                {
                    /*
                    * If the value added is a frame, it might be in a complicated
                    * slotting arrangement. The safest thing to do is to steal its
                    * unslotted children and slot them when we are slotted (or absorbed
                    * into another frame.
                    */

                    var other = (MutableFrame)value;
                    unslotted.AddRange(other.unslotted);
                    other.unslotted = unslotted;
                }
                attributes[name] = value;
            }
        }

        /**
        * Trigger any unfinished computations contained in this frame to be executed.
        *
        * When a frame is being filled, unfinished computations may be added. They
        * cannot be started immediately, since the frame may still have members to
        * be added and those changes will be visible to the lookup environments of
        * those computations. Only when a frame is “returned” can the computations
        * be started. This should be called before returning to trigger computation.
        */

        public void Slot()
        {
            foreach (var computation in unslotted)
                computation.Slot();
            unslotted.Clear();
        }
    }

    /**
     * A Frame wrapper over a CLR object.
     */
    public class ReflectedFrame : Frame
    {
        private readonly IDictionary<string, object> attributes;

        private ReflectedFrame(string id, SourceReference source_ref,
            object backing, IDictionary<string, object> attributes) : base(id, source_ref, null, null)
        {
            Backing = backing;
            this.attributes = attributes;
        }

        public object Backing { get; }

        public override long Count => attributes.Count;

        public override object this[string name]
        {
            get
            {
                object result;
                attributes.TryGetValue(name, out result);
                return result;
            }
        }

        public static ReflectedFrame Create<T>(TaskMaster task_master, T backing,
            IDictionary<string, Func<T, object>> accessors)
        {
            return Create(SupportFunctions.OrdinalNameStr(task_master.NextId()), backing, accessors);
        }

        public static ReflectedFrame Create<T>(string id, T backing,
            IDictionary<string, Func<T, object>> accessors)
        {
            var attributes = accessors.ToDictionary(pair => pair.Key, pair =>
            {
                var result = pair.Value(backing);
                if (result == null)
                {
                    result = Unit.NULL;
                }
                else if (result is bool || result is double
                         || result is long || result is Frame
                         || result is Stringish
                         || result is Template || result is Unit)
                {
                }
                else if (result is string)
                {
                    result = new SimpleStringish((string)result);
                }
                else
                {
                    throw new InvalidCastException("Value for " + pair.Key
                                                   + " is non-Flabbergast type "
                                                   + result.GetType() + ".");
                }
                return result;
            });
            return new ReflectedFrame(id, new ClrSourceReference(),
                backing, attributes);
        }

        /**
         * Check if an attribute name is present in the frame.
         */
        public override bool Has(string name)
        {
            return attributes.ContainsKey(name);
        }

        public override IEnumerable<string> GetAttributeNames()
        {
            return attributes.Keys;
        }

        public void Set(string name, object value)
        {
            if (attributes.ContainsKey(name))
                throw new InvalidOperationException();
            attributes[name] = value;
        }
    }

    /**
     * A Frame of fixed data with an easy API.
     */
    public class FixedFrame : Frame, IEnumerable<string>
    {
        private readonly IDictionary<string, object> attributes = new Dictionary<string, object>();

        public FixedFrame(string id, SourceReference source_ref) : base(id, source_ref, null, null)
        {
        }

        public override long Count => attributes.Count;

        public override object this[string name]
        {
            get
            {
                object result;
                attributes.TryGetValue(name, out result);
                return result;
            }
        }

        public IEnumerator<string> GetEnumerator()
        {
            return attributes.Keys.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        public void Add(string name, long val)
        {
            attributes[name] = val;
        }

        public void Add(string name, byte[] val)
        {
            attributes[name] = val;
        }

        public void Add(string name, string val)
        {
            attributes[name] = new SimpleStringish(val);
        }

        public void Add(IEnumerable<Frame> frames)
        {
            foreach (var frame in frames)
                attributes[frame.Id.ToString()] = frame;
        }

        /**
         * Check if an attribute name is present in the frame.
         */
        public override bool Has(string name)
        {
            return attributes.ContainsKey(name);
        }

        public override IEnumerable<string> GetAttributeNames()
        {
            return attributes.Keys;
        }
    }
}