using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System;

namespace Flabbergast {

public class Context {
	public Frame Frame {
		get;
		private set;
	}
	public Context Tail {
		get;
		private set;
	}
	public int Length {
		get;
		private set;
	}

	public Context(Frame frame, Context tail) {
		this.Frame = frame;
		this.Tail = tail;
		Length = tail == null ? 1 : (tail.Length + 1);
	}
	
	public Context Append(Context new_tail) {
		if (new_tail == null) {
			return this;
		}
		if (Tail == null) {
			return new Context(Frame, new_tail);
		}
		return new Context(Frame, Tail.Append(new_tail));
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
