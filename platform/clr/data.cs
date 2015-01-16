using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System;

namespace Flabbergast {

public delegate void SetFrame(int i, Frame frame);

public abstract class Context {
	public int Length {
		get;
		protected set;
	}
	public Context Append(Context new_tail) {
		if (new_tail == null) {
			return this;
		}
		return new ForkedContext(this, new_tail);
	}
	public abstract void Fill(SetFrame f, int start_index = 0);
}

public class LinkedContext : Context {
	private Frame Frame;
	private Context Tail;

	public LinkedContext(Frame frame, Context tail) {
		this.Frame = frame;
		this.Tail = tail;
		Length = tail == null ? 1 : (tail.Length + 1);
	}
	public override void Fill(SetFrame set_frame, int start_index) {
		set_frame(start_index, Frame);
		if (Tail != null) {
			Tail.Fill(set_frame, start_index + 1);
		}
	}
}

public class ForkedContext : Context {
	private Context Head;
	private Context Tail;

	public ForkedContext(Context head, Context tail) {
		this.Head = head;
		this.Tail = tail;
		Length = Head.Length + Tail.Length;
	}
	public override void Fill(SetFrame set_frame, int start_index) {
		Head.Fill(set_frame, start_index);
		Tail.Fill(set_frame, start_index + Head.Length);
	}
}

public class Unit {
		public static readonly Unit NULL = new Unit();
		private Unit() {}
}

public interface IAttributeNames {
	IEnumerable<string> GetAttributeNames();
}
public class Template : IAttributeNames {
	private IDictionary<string, ComputeValue> attributes = new Dictionary<string, ComputeValue>();
	public Context Context {
			get;
			private set;
	}

	public SourceReference SourceReference {
		get;
		private set;
	}

	public Template(SourceReference source_ref, Context context) {
		this.SourceReference = source_ref;
		this.Context = context;
	}

	public ComputeValue this[string name] {
		get {
			return attributes[name];
		}
		set {
			if(attributes.ContainsKey(name)) {
				throw new InvalidOperationException("Redefinition of attribute " + name + ".");
			}
			attributes[name] = value;
		}
	}
	public IEnumerable<string> GetAttributeNames() {
		return attributes.Keys;
	}
}

public class Frame : DynamicObject, IAttributeNames {
	private IDictionary<string, Computation> pending = new Dictionary<string, Computation>();
	private IDictionary<string, Object> attributes = new Dictionary<string, Object>();
	public Context Context {
			get;
			private set;
	}
	public Frame Container {
		get;
		private set;
	}

	public SourceReference SourceReference {
		get;
		private set;
	}

	public Frame(SourceReference source_ref, Context context, Frame container) {
		this.SourceReference = source_ref;
		this.Context = context;
		this.Container = container;
	}

	public override IEnumerable<string> GetDynamicMemberNames() {
		return attributes.Keys;
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

	public override bool TryGetIndex(GetIndexBinder binder, Object[] indexes, out Object result) {
		result = null;
		return false;
	}

	public void Close(TaskMaster master) {
		foreach(var computation in pending.Values) {
			master.Slot(computation);
		}
	}

	public bool Has(string name, out bool is_pending) {
		is_pending = pending.ContainsKey(name);
		return is_pending || attributes.ContainsKey(name);
	}
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

	public object this[string name] {
		get {
			return attributes[name];
		}
		set {
			if (pending.ContainsKey(name) || attributes.ContainsKey(name)) {
				throw new InvalidOperationException("Redefinition of attribute " + name + ".");
			}
			if (value is Computation) {
				var computation = (Computation)value;
				pending[name] = computation;
				computation.Notify((result) => {
					attributes[name] = result;
					pending.Remove(name);
				});
			} else {
				attributes[name] = value;
			}
		}
	}
	public IEnumerable<string> GetAttributeNames() {
		return attributes.Keys.Concat(pending.Keys);
	}
}
}
