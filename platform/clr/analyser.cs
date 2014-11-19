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
public abstract class AstTypeableNode : AstNode {
	protected Environment Environment;
	internal virtual int EnvironmentPriority { get { return Environment.Priority; } }
	internal abstract void PropagateEnvironment(ErrorCollector collector, List<AstTypeableNode> queue, Environment environment);
	public void Analyse(ErrorCollector collector) {
		var environment = new Environment (FileName, StartRow, StartColumn, EndRow, EndColumn, null, false);
		var queue = new List<AstTypeableNode>();
		PropagateEnvironment(collector, queue, environment);
		var sorted_nodes = new SortedDictionary<int, Dictionary<AstTypeableNode, bool>>();

		foreach (var element in queue) {
			if (!sorted_nodes.ContainsKey(element.Environment.Priority)) {
				sorted_nodes[element.Environment.Priority] = new Dictionary<AstTypeableNode, bool>();
			}
			sorted_nodes[element.Environment.Priority][element] = true;
		}
	}
	internal static void ReflectMethod(ErrorCollector collector, AstNode where, string type_name, string method_name, out System.Type reflected_type, out System.Reflection.MethodInfo method_info) {
		reflected_type = System.Type.GetType(type_name, false);
		if (reflected_type == null) {
			collector.RawError(where, "No such type " + type_name + " found. Perhaps you are missing an assembly reference.");
			method_info = null;
		} else {
			method_info = reflected_type.GetMethod(method_name);
			if (method_info == null) {
				collector.RawError(where, "The type " + type_name + " has no method named " + method_name + ".");
			}
		}
	}
	internal static void CheckReflectedMethod(ErrorCollector collector, AstNode where, System.Reflection.MethodInfo method, List<expression> arguments, Type return_type) {
		if (!method.IsPublic) {
			collector.RawError(where, "The method " + method + " is not public.");
		}
		if (method.IsGenericMethod || method.IsGenericMethodDefinition) {
			collector.RawError(where, "The method " + method + " is generic. This is not supported.");
		}
		var real_return_type = TypeFromClrType(method.ReturnType);
		if ((real_return_type & return_type) == 0) {
			collector.ReportTypeError(where, return_type, real_return_type);
		}
		var parameters = method.GetParameters();
		if (parameters.Length != arguments.Count) {
			collector.RawError(where, "The method " + method + " takes " + parameters.Length + " but " + arguments.Count + " are given.");
		} else {
			foreach(var parameter in parameters) {
				if (!parameter.IsIn) {
					collector.RawError(where, "The parameter " + parameter.Position + " of method " + method + " is not an `in' parameter.");
				} else {
					arguments[parameter.Position].EnsureType(collector, TypeFromClrType(parameter.ParameterType));
				}
			}
		}
	}
	public static Type TypeFromClrType(System.Type clr_type) {
		if (clr_type == typeof(bool) || clr_type == typeof(Boolean)) {
				return Type.Bool;
		} else if ( clr_type == typeof(sbyte) || clr_type == typeof(short) || clr_type == typeof(int) || clr_type == typeof(long) || clr_type == typeof(SByte) || clr_type == typeof(Int16) || clr_type == typeof(Int32) || clr_type == typeof(Int64)) {
			return Type.Int;
		} else if (clr_type == typeof(float) || clr_type == typeof(double) || clr_type == typeof(Single) || clr_type == typeof(Double)) {
			return Type.Float;
		} else if (clr_type == typeof(string) || clr_type == typeof(String)) {
			return Type.Str;
		// TODO: Template and tuple
		} else {
			return 0;
		}
	}
}
public interface ErrorCollector {
	void ReportTypeError(AstNode where, Type new_type, Type existing_type);
	void ReportTypeError(Environment environment, string name, Type new_type, Type existing_type);
	void RawError(AstNode where, string message);
}
public interface ITypeableElement {
	void EnsureType(ErrorCollector collector, Type type);
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
	internal abstract void PropagateSpecialEnvironment(ErrorCollector collector, List<AstTypeableNode> queue, Environment special_environment);
}
internal class EnvironmentPrioritySorter : IComparer<AstTypeableNode> {
	public int Compare(AstTypeableNode x, AstTypeableNode y) {
		return x.EnvironmentPriority - y.EnvironmentPriority;
	}
}
public abstract class NameInfo {
	protected Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	public string Name { get; protected set; }
	internal NameInfo Lookup(ErrorCollector collector, string name) {
		EnsureType(collector, Type.Tuple);
		if (!Children.ContainsKey(name)) {
			CreateChild(collector, name, Name);
		}
		return Children[name];
	}
	internal NameInfo Lookup(ErrorCollector collector, IEnumerator<string> names) {
		var info = this;
		while (names.MoveNext()) {
			info.EnsureType(collector, Type.Tuple);
			if (!info.Children.ContainsKey(names.Current)) {
				info.CreateChild(collector, names.Current, this.Name);
			}
			info = info.Children[names.Current];
		}
		return info;
	}
	public virtual bool HasName(string name) {
		return Children.ContainsKey(name);
	}
	public abstract void EnsureType(ErrorCollector collector, Type type);
	public abstract void CreateChild(ErrorCollector collector, string name, string root);
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
	private Environment Environment;
	Type RealType = Type.Any;
	public OpenNameInfo(Environment environment, string name) {
		Environment = environment;
		Name = name;
	}
	public override void EnsureType(ErrorCollector collector, Type type) {
		if ((RealType & type) == 0) {
			collector.ReportTypeError(Environment, Name, RealType, type);
		} else {
			RealType &= type;
		}
	}
	public override void CreateChild(ErrorCollector collector, string name, string root) {
		Children[name] = new OpenNameInfo(Environment, root + "." + name);
	}
	public override bool NeedsToBreakFlow() {
		return true;
	}
}
internal class BoundNameInfo : NameInfo {
	private Environment Environment;
	ITypeableElement Target;
	public BoundNameInfo(Environment environment, string name, ITypeableElement target) {
		Environment = environment;
		Name = name;
		Target = target;
	}
	public override void EnsureType(ErrorCollector collector, Type type) {
		Target.EnsureType(collector, type);
	}
	public override void CreateChild(ErrorCollector collector, string name, string root) {
		Children[name] = new OpenNameInfo(Environment, root + "." + name);
	}
}
internal class CopyFromParentInfo : NameInfo {
	Environment Environment;
	NameInfo Source;
	Type Mask = Type.Any;
	bool ForceBack;

	public CopyFromParentInfo(Environment environment, string name, NameInfo source, bool force_back) {
		Environment = environment;
		Name = name;
		Source = source;
		ForceBack = force_back;
	}
	public override void EnsureType(ErrorCollector collector, Type type) {
		if (ForceBack) {
			Source.EnsureType(collector, type);
		} else {
			if ((Mask & type) == 0) {
				collector.ReportTypeError(Environment, Name, Mask, type);
			}
			Mask &= type;
		}
	}
	public override void CreateChild(ErrorCollector collector, string name, string root) {
		if (ForceBack) {
			Source.CreateChild(collector, name, root);
		}
		if (Source.HasName(name)) {
			Children[name] = new CopyFromParentInfo(Environment, root + "." + name, Source.Lookup(collector, name), ForceBack);
		} else {
			Children[name] = new OpenNameInfo(Environment, root + "." + name);
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

	internal NameInfo AddMask(string name, ITypeableElement expression) {
		if (Children.ContainsKey(name)) {
			throw new InvalidOperationException("The name " + name + " already exists in the environment.");
		}
		return Children[name] = new BoundNameInfo(this, name, expression);
	}
	public NameInfo AddFreeName(string name) {
		return Children[name] = new OpenNameInfo(this, name);
	}
	public NameInfo Lookup(ErrorCollector collector, IEnumerable<string> names) {
		IEnumerator<string> enumerator = names.GetEnumerator();
		if (!enumerator.MoveNext()) {
			throw new ArgumentOutOfRangeException("List of names cannot be empty.");
		}
		if (Children.ContainsKey(enumerator.Current)) {
			return Children[enumerator.Current].Lookup(collector, enumerator);
		}
		if (ForceBack) {
			Parent.Lookup(collector, names);
		}
		if (Parent != null && Parent.HasName(enumerator.Current)) {
			return Lookback(enumerator.Current).Lookup(collector, enumerator);
		}
		var info = new OpenNameInfo(this, enumerator.Current);
		Children[enumerator.Current] = info;
		return info.Lookup(collector, enumerator);
	}
	public bool HasName(string name) {
		return Children.ContainsKey(name) || Parent != null && Parent.HasName(name);
	}
	private NameInfo Lookback(string name) {
		if (Children.ContainsKey(name)) {
			return Children[name];
		}
		var copy_info = new CopyFromParentInfo(this, name, Parent.Lookback(name), ForceBack);
		Children[name] = copy_info;
		return copy_info;
	}
}
}
