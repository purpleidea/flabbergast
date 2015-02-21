using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System;

namespace Flabbergast {

/**
 * The collection of frames in which lookup should be performed.
 */
public abstract class Context {
	public delegate void SetFrame(int i, Frame frame);

	/**
	 * The total number of frames in this context.
	 */
	public int Length {
		get;
		protected set;
	}
	public static Context Prepend(Frame head, Context tail) {
		return new LinkedContext(head, tail);
	}
	/**
	 * Conjoin two contexts, placing all the frames of the provided context after
	 * all the frames in the original context.
	 */
	public static Context Append(Context original, Context new_tail) {
		if (new_tail == null) {
			return original;
		}
		return new ForkedContext(original, new_tail);
	}
	/**
	 * Visit all the frames in a context.
	 */
	public void Fill(SetFrame f) {
		Fill(f, 0);
	}
	internal abstract void Fill(SetFrame f, int start_index);
}

/**
 * An element in a single-linked list of contexts.
 */
internal class LinkedContext : Context {
	private Frame Frame;
	private Context Tail;

	internal LinkedContext(Frame frame, Context tail) {
		this.Frame = frame;
		this.Tail = tail;
		Length = tail == null ? 1 : (tail.Length + 1);
	}
	internal override void Fill(SetFrame set_frame, int start_index) {
		set_frame(start_index, Frame);
		if (Tail != null) {
			Tail.Fill(set_frame, start_index + 1);
		}
	}
}

/**
 * A join between two contexts.
 *
 * This is an optimisation: rather than creating a new linked-list of contexts,
 * store a fork and it can be linearised when needed.
 */
internal class ForkedContext : Context {
	private Context Head;
	private Context Tail;

	internal ForkedContext(Context head, Context tail) {
		this.Head = head;
		this.Tail = tail;
		Length = Head.Length + Tail.Length;
	}
	internal override void Fill(SetFrame set_frame, int start_index) {
		Head.Fill(set_frame, start_index);
		Tail.Fill(set_frame, start_index + Head.Length);
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
	private IDictionary<string, ComputeValue> attributes = new SortedDictionary<string, ComputeValue>();

	public Frame Container {
		get;
		private set;
	}
	/**
	 * The context in which this template was created.
	 */
	public Context Context {
			get;
			private set;
	}

	/**
	 * The stack trace at the time of creation.
	 */
	public SourceReference SourceReference {
		get;
		private set;
	}

	public Template(SourceReference source_ref, Context context, Frame container) {
		this.SourceReference = source_ref;
		this.Context = context;
		this.Container = container;
	}

	/**
	 * Access the functions in the template. Templates should not be mutated, but
	 * this policy is not enforced by this class; it must be done in the calling
	 * code.
	 */
	public ComputeValue this[string name] {
		get {
			return attributes.ContainsKey(name) ? attributes[name] : null;
		}
		set {
			if (value == null) {
				return;
			}
			if(attributes.ContainsKey(name)) {
				throw new InvalidOperationException("Redefinition of attribute " + name + ".");
			}
			attributes[name] = value;
		}
	}
	public ComputeValue Get(Stringish name) {
		return this[name.ToString()];
	}
	public IEnumerable<string> GetAttributeNames() {
		return attributes.Keys;
	}
}

/**
 * A Frame in the Flabbergast language.
 */
public class Frame : DynamicObject, IAttributeNames {
	/**
	 * The lookup context when this frame was created and any of its ancestors.
	 */
	public Context Context {
			get;
			private set;
	}
	/**
	 * The containing frame, or null for file-level frames.
	 */
	public Frame Container {
		get;
		private set;
	}

	public Stringish Id {
		get;
		private set;
	}

	/**
	 * The stack trace when this frame was created.
	 */
	public SourceReference SourceReference {
		get;
		private set;
	}

	private IDictionary<string, Computation> pending = new SortedDictionary<string, Computation>();
	private List<Computation> unslotted = new List<Computation>();
	private IDictionary<string, Object> attributes = new SortedDictionary<string, Object>();

	public static Frame Through(long id, SourceReference source_ref, long start, long end, Context context, Frame container) {
		var result = new Frame(id, source_ref, context, container);
		if (end > start)
			return result;
		for (long it = 0; it <= (end - start); it++) {
			result[TaskMaster.OrdinalNameStr(it)] = start + it;
		}
		return result;
	}

	public Frame(long id, SourceReference source_ref, Context context, Frame container) {
		this.SourceReference = source_ref;
		this.Context = Context.Prepend(this, context);
		this.Container = container;
		this.Id = TaskMaster.OrdinalName(id);
	}

	/**
	 * Access the functions in the frames. Frames should not be mutated, but this
	 * policy is not enforced by this class; it must be done in the calling code.
	 */
	public object this[string name] {
		get {
			return attributes[name];
		}
		set {
			if (value == null) {
				return;
			}
			if (pending.ContainsKey(name) || attributes.ContainsKey(name)) {
				throw new InvalidOperationException("Redefinition of attribute " + name + ".");
			}
			if (value is Computation) {
				var computation = (Computation)value;
				pending[name] = computation;
				/*
				 * When this computation has completed, replace its value in the frame.
				 */
				computation.Notify((result) => {
					attributes[name] = result;
					pending.Remove(name);
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

					var other = value as Frame;
					unslotted.AddRange(other.unslotted);
					other.unslotted = unslotted;
				}
				attributes[name] = value;
			}
		}
	}

	public IEnumerable<string> GetAttributeNames() {
		return attributes.Keys.Concat(pending.Keys);
	}

	public override IEnumerable<string> GetDynamicMemberNames() {
		return attributes.Keys;
	}

	/**
	 * Access a value if available, or be notified upon completion.
	 * Returns: true if the value was available, false if the caller should wait to be reinvoked.
	 */
	internal bool GetOrSubscribe(string name, ConsumeResult consumer) {
		if (pending.ContainsKey(name)) {
			pending[name].Notify(consumer);
			return false;
		}
		if (attributes.ContainsKey(name)) {
			consumer(attributes[name]);
			return true;
		}
		return true;
	}

	/**
	 * Check if an attribute name is present in the frame.
	 */
	public bool Has(string name, out bool is_pending) {
		is_pending = pending.ContainsKey(name);
		return is_pending || attributes.ContainsKey(name);
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
	public void Slot(TaskMaster master) {
		foreach(var computation in unslotted) {
			master.Slot(computation);
		}
	}

	public override bool TryGetIndex(GetIndexBinder binder, Object[] indexes, out Object result) {
		result = null;
		return false;
	}

	public override bool TryGetMember(GetMemberBinder binder, out Object result) {
		var name = binder.Name;
		if (binder.IgnoreCase && char.IsUpper(name, 0)) {
			name = char.ToLower(name[0]) + name.Substring(1);
		}
		if (pending.ContainsKey(name)) {
			throw new InvalidOperationException("Incomplete evaluation.");
		}
		return attributes.TryGetValue(name, out result);
	}
}
}
