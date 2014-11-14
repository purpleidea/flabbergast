using System;
using System.Collections.Generic;

namespace Flabbergast {
[Flags]
public enum Type {
	Bool = 1,
	Int = 2,
	Float = 4,
	Str = 8,
	Template = 16,
	Tuple = 32,
	Unit = 64,
	Any = 127
}
public class TypeConflictException : Exception {
	public TypeConflictException(string name, Type type1, Type type2) : base(name + " has type " + type1 + " which is not compatible with type " + type2) {
	}
}
public abstract class AstTypeableNode : AstNode {
	protected Environment Environment;
	internal virtual int EnvironmentPriority { get { return Environment.Priority; } }
	// TODO implement
	internal virtual Type Type { get { return 0; } }
	internal abstract void PropagateEnvironment(SortedDictionary<AstTypeableNode, bool> queue, Environment environment);
	public void Analyse() {
		var environment = new Environment (FileName, StartRow, StartColumn, EndRow, EndColumn, null, false);
		var queue = new SortedDictionary<AstTypeableNode, bool>(new EnvironmentPrioritySorter ());
		PropagateEnvironment(queue, environment);
	}
	// TODO implement
	internal virtual void EnsureType(Type type) { }
}
internal abstract class AstTypeableSpecialNode : AstTypeableNode {
	protected Environment SpecialEnvironment;
	internal override int EnvironmentPriority {
		get {
			var ep = Environment == null ? 0 : Environment.Priority;
			var esp = SpecialEnvironment == null ? 0 : SpecialEnvironment.Priority;
			return Math.Max(ep, esp);
		}
	}
	internal abstract void PropagateSpecialEnvironment(SortedDictionary<AstTypeableNode, bool> queue, Environment special_environment);
}
internal class EnvironmentPrioritySorter : IComparer<AstTypeableNode> {
	public int Compare(AstTypeableNode x, AstTypeableNode y) {
		return x.EnvironmentPriority - y.EnvironmentPriority;
	}
}
public abstract class NameInfo {
	protected Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	public string Name { get; protected set; }
	public abstract Type Type { get; }
	internal NameInfo Lookup(string name) {
		EnsureType(Type.Tuple);
		if (!Children.ContainsKey(name)) {
			CreateChild(name, Name);
		}
		return Children[name];
	}
	internal NameInfo Lookup(IEnumerator<string> names) {
		var info = this;
		while (names.MoveNext()) {
			info.EnsureType(Type.Tuple);
			if (!info.Children.ContainsKey(names.Current)) {
				info.CreateChild(names.Current, this.Name);
			}
			info = info.Children[names.Current];
		}
		return info;
	}
	public virtual bool HasName(string name) {
		return Children.ContainsKey(name);
	}
	public abstract void EnsureType(Type type);
	public abstract void CreateChild(string name, string root);
	public virtual bool NeedsToBreakFlow() {
		foreach (var info in Children.Values) {
			if (info.NeedsToBreakFlow()) {
				return true;
			}
		}
		return false;
	}
}
public class OpenNameInfo : NameInfo {
	Type RealType = Type.Any;
	public override Type Type { get { return RealType; } }
	public OpenNameInfo(string name) {
		Name = name;
	}
	public override void EnsureType(Type type) {
		if ((RealType & type) == 0) {
			throw new TypeConflictException(Name, RealType, type);
		}
		RealType &= type;
	}
	public override void CreateChild(string name, string root) {
		Children[name] = new OpenNameInfo(root + "." + name);
	}
	public override bool NeedsToBreakFlow() {
		return true;
	}
}
internal class BoundNameInfo : NameInfo {
	AstTypeableNode Expression;
	public override Type Type { get { return Expression.Type; } }
	public BoundNameInfo(string name, AstTypeableNode expression) {
		Name = name;
		Expression = expression;
	}
	public override void EnsureType(Type type) {
		Expression.EnsureType(type);
	}
	public override void CreateChild(string name, string root) {
		Children[name] = new OpenNameInfo(root + "." + name);
	}
}
internal class CopyFromParentInfo : NameInfo {
	NameInfo Source;
	Type Mask = Type.Any;
	bool ForceBack;

	public override Type Type { get { return Source.Type & Mask; } }
	public CopyFromParentInfo(string name, NameInfo source, bool force_back) {
		Name = name;
		Source = source;
		ForceBack = force_back;
	}
	public override void EnsureType(Type type) {
		if (ForceBack) {
			Source.EnsureType(type);
		} else {
			if ((Type & type) == 0) {
				throw new TypeConflictException(Name, Type, type);
			}
			Mask &= type;
		}
	}
	public override void CreateChild(string name, string root) {
		if (ForceBack) {
			Source.CreateChild(name, root);
		}
		if (Source.HasName(name)) {
			Children[name] = new CopyFromParentInfo(root + "." + name, Source.Lookup(name), ForceBack);
		} else {
			Children[name] = new OpenNameInfo(root + "." + name);
		}
	}
	public override bool HasName(string name) {
		return base.HasName(name) || Source.HasName(name);
	}
}
public class Environment {
	Environment Parent;
	Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	public string FileName { get; private set; }
	public int StartRow { get; private set; }
	public int StartColumn { get; private set; }
	public int EndRow { get; private set; }
	public int EndColumn { get; private set; }
	public int Priority { get; private set; }
	bool ForceBack;

	public Environment(string filename, int start_row, int start_column, int end_row, int end_column, Environment parent = null, bool force_back = false) {
		if (force_back && parent == null) {
			throw new ArgumentException("Parent environment cannot be null when forcing parent-backed creation.");
		}
		FileName = filename;
		StartRow = start_row;
		StartColumn = start_column;
		EndRow = end_row;
		EndColumn = end_column;
		ForceBack = force_back;
		Parent = parent;
		Priority = parent == null ? 0 : parent.Priority + 1;
	}

	internal NameInfo AddMask(string name, AstTypeableNode expression) {
		if (Children.ContainsKey(name)) {
			throw new InvalidOperationException("The name " + name + " already exists in the environment.");
		}
		return Children[name] = new BoundNameInfo(name, expression);
	}
	public NameInfo AddFreeName(string name) {
		return Children[name] = new OpenNameInfo(name);
	}
	public NameInfo Lookup(IEnumerable<string> names) {
		IEnumerator<string> enumerator = names.GetEnumerator();
		if (!enumerator.MoveNext()) {
			throw new ArgumentOutOfRangeException("List of names cannot be empty.");
		}
		if (Children.ContainsKey(enumerator.Current)) {
			return Children[enumerator.Current].Lookup(enumerator);
		}
		if (ForceBack) {
			Parent.Lookup(names);
		}
		if (Parent != null && Parent.HasName(enumerator.Current)) {
			return Lookback(enumerator.Current).Lookup(enumerator);
		}
		var info = new OpenNameInfo(enumerator.Current);
		Children[enumerator.Current] = info;
		return info.Lookup(enumerator);
	}
	public bool HasName(string name) {
		return Children.ContainsKey(name) || Parent != null && Parent.HasName(name);
	}
	private NameInfo Lookback(string name) {
		if (Children.ContainsKey(name)) {
			return Children[name];
		}
		var copy_info = new CopyFromParentInfo(name, Parent.Lookback(name), ForceBack);
		Children[name] = copy_info;
		return copy_info;
	}
}
}
