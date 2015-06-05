using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;

namespace Flabbergast {
/**
 * The collection of frames in which lookup should be performed.
 */

	public class Context {
		/**
	 * The total number of frames in this context.
	 */

		public int Length {
			get { return frames.Count; }
		}

		private readonly List<Frame> frames;

		private Context(List<Frame> frames) {
			this.frames = frames;
		}

		/**
	 * Conjoin two contexts, placing all the frames of the provided context after
	 * all the frames in the original context.
	 */

		public static Context Append(Context original, Context new_tail) {
			if (original == null) {
				throw new InvalidOperationException("Cannot append to null context.");
			}
			if (new_tail == null || original == new_tail) {
				return original;
			}
			int filter = 0;
			var list = new List<Frame>(original.Length + new_tail.Length);
			foreach (var frame in original.frames) {
				list.Add(frame);
				filter |= frame.GetHashCode();
			}
			foreach (var frame in new_tail.frames) {
				int hash = frame.GetHashCode();
				if ((hash & filter) != hash || !list.Contains(frame)) {
					list.Add(frame);
					filter |= hash;
				}
			}
			return new Context(list);
		}

		public IEnumerable<Frame> Fill() {
			return frames;
		}

		public static Context Prepend(Frame head, Context tail) {
			if (head == null) {
				throw new InvalidOperationException("Cannot prepend a null frame to a context.");
			}
			var list = new List<Frame>(tail == null ? 1 : (tail.Length + 1));
			list.Add(head);
			if (tail != null) {
				list.AddRange(tail.frames.Where(frame => head != frame));
			}
			return new Context(list);
		}
	}

/**
 * The null type.
 *
 * For type dispatch, there are plenty of reasons to distinguish between the
 * underlying VM's null and Flabbergast's Null, and this type makes this
 * possible.
 */

	public class Unit {
		public static readonly Unit NULL = new Unit();
		private Unit() {}

		public override string ToString() {
			return "Null";
		}
	}

/**
 * Objects which have strings that can be iterated in alphabetical order for
 * the MergeIterator.
 */

	public interface IAttributeNames {
		/**
	 * Provide all the attribute names (keys) in the collection. They need not be
	 * ordered.
	 */
		IEnumerable<string> GetAttributeNames();
	}

/**
 * A Flabbergast Template, holding functions for computing attributes.
 */

	public class Template : IAttributeNames {
		public Frame Container { get; private set; }
		/**
	 * The context in which this template was created.
	 */
		public Context Context { get; private set; }
		/**
	 * Access the functions in the template. Templates should not be mutated, but
	 * this policy is not enforced by this class; it must be done in the calling
	 * code.
	 */

		public ComputeValue this[string name] {
			get { return attributes.ContainsKey(name) ? attributes[name] : null; }
			set {
				if (value == null) {
					return;
				}
				if (attributes.ContainsKey(name)) {
					throw new InvalidOperationException("Redefinition of attribute " + name + ".");
				}
				attributes[name] = value;
			}
		}

		/**
	 * The stack trace at the time of creation.
	 */
		public SourceReference SourceReference { get; private set; }
		private readonly IDictionary<string, ComputeValue> attributes = new SortedDictionary<string, ComputeValue>();

		public Template(SourceReference source_ref, Context context, Frame container) {
			SourceReference = source_ref;
			Context = context;
			Container = container;
		}

		public IEnumerable<string> GetAttributeNames() {
			return attributes.Keys;
		}

		public ComputeValue Get(Stringish name) {
			return this[name.ToString()];
		}
	}

/**
 * A Frame in the Flabbergast language.
 */

	public class Frame : DynamicObject, IAttributeNames {
		/**
	 * The containing frame, or null for file-level frames.
	 */
		public Frame Container { get; private set; }
		/**
	 * The lookup context when this frame was created and any of its ancestors.
	 */
		public Context Context { get; private set; }
		public long Count { get { return attributes.Count; } }
		public Stringish Id { get; private set; }
		/**
	 * Access the functions in the frames. Frames should not be mutated, but this
	 * policy is not enforced by this class; it must be done in the calling code.
	 */

		public object this[string name] {
			get { return attributes.ContainsKey(name) ? attributes[name] : null; }
			set {
				if (value == null) {
					return;
				}
				if (attributes.ContainsKey(name)) {
					throw new InvalidOperationException("Redefinition of attribute " + name + ".");
				}
				if (value is ComputeValue) {
					var computation = ((ComputeValue) value)(task_master, SourceReference, Context, this, Container);
					attributes[name] = computation;
					/*
				 * When this computation has completed, replace its value in the frame.
				 */
					computation.Notify(result => {
						attributes[name] = result;
					});
					/*
				 * If the value is a computation, it cannot be slotted for execution
				 * since it might depend on lookups that reference this frame. Therefore, put
				 * it in a queue for later activation.
				 */
					unslotted.Add(computation);
				} else {
					if (value is Frame) {
						/*
					 * If the value added is a frame, it might be in a complicated
					 * slotting arrangement. The safest thing to do is to steal its
					 * unslotted children and slot them when we are slotted (or absorbed
					 * into another frame.
					 */

						var other = (Frame) value;
						unslotted.AddRange(other.unslotted);
						other.unslotted = unslotted;
					}
					attributes[name] = value;
				}
			}
		}

		/**
	 * The stack trace when this frame was created.
	 */
		public SourceReference SourceReference { get; private set; }
		private readonly IDictionary<string, Object> attributes = new SortedDictionary<string, Object>();
		private readonly TaskMaster task_master;
		private List<Computation> unslotted = new List<Computation>();

		public Frame(TaskMaster task_master, long id, SourceReference source_ref, Context context, Frame container) {
			this.task_master = task_master;
			SourceReference = source_ref;
			Context = Context.Prepend(this, context);
			Container = container ?? this;
			Id = TaskMaster.OrdinalName(id);
		}

		public IEnumerable<string> GetAttributeNames() {
			return attributes.Keys;
		}

		public override IEnumerable<string> GetDynamicMemberNames() {
			return attributes.Keys;
		}

		/**
	 * Access a value if available, or be notified upon completion.
	 * Returns: true if the value was available, false if the caller should wait to be reinvoked.
	 */

		internal bool GetOrSubscribe(string name, ConsumeResult consumer) {
			// If this frame is being looked at, then all its pending attributes should
			// be slotted.
			Slot();
			object value;
			if (attributes.TryGetValue(name, out value)) {
				if (value is Computation) {
					((Computation) value).Notify(consumer);
				} else {
					consumer(value);
				}
				return true;
			}
			return false;
		}

		/**
	 * Check if an attribute name is present in the frame.
	 */
		public bool Has(string name) {
			return attributes.ContainsKey(name);
		}

		public bool Has(Stringish name) {
			return Has(name.ToString());
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

		public void Slot() {
			foreach (var computation in unslotted) {
				task_master.Slot(computation);
			}
			unslotted.Clear();
		}

		public static Frame Through(TaskMaster task_master, long id, SourceReference source_ref, long start, long end,
			Context context, Frame container) {
			var result = new Frame(task_master, id, source_ref, context, container);
			if (end < start)
				return result;
			for (long it = 0; it <= (end - start); it++) {
				result[TaskMaster.OrdinalNameStr(it + 1)] = start + it;
			}
			return result;
		}

		public override bool TryGetIndex(GetIndexBinder binder, Object[] indexes, out Object result) {
			result = null;
			return false;
		}

		public Stringish RenderTrace(Stringish prefix) {
			var writer = new System.IO.StringWriter();
			var seen = new Dictionary<SourceReference, bool>();
			SourceReference.Write(writer, prefix.ToString(), seen);
			return new SimpleStringish(writer.ToString());
		}

		public override bool TryGetMember(GetMemberBinder binder, out Object result) {
			var name = binder.Name;
			if (binder.IgnoreCase && char.IsUpper(name, 0)) {
				name = char.ToLower(name[0]) + name.Substring(1);
			}
			return attributes.TryGetValue(name, out result);
		}
	}
}
