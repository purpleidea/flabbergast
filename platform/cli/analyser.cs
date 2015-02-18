using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;

namespace Flabbergast {
[Flags]
public enum Type {
	Bool = 1,
	Float = 2,
	Frame = 4,
	Int = 8,
	Str = 16,
	Template = 32,
	Unit = 64,
	Any = 127
}
public abstract class AstTypeableNode : AstNode {
	protected Environment Environment;
	internal virtual int EnvironmentPriority { get { return Environment.Priority; } }
	internal abstract void PropagateEnvironment(ErrorCollector collector, List<AstTypeableNode> queue, Environment environment);
	internal abstract void MakeTypeDemands(ErrorCollector collector);
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
		foreach (var items in sorted_nodes.Values) {
			foreach (var element in items.Keys) {
				element.MakeTypeDemands(collector);
			}
		}
	}
	internal static void ReflectMethod(ErrorCollector collector, AstNode where, string type_name, string method_name, int arity, List<System.Reflection.MethodInfo> methods) {
		var reflected_type = System.Type.GetType(type_name, false);
		if (reflected_type == null) {
			collector.RawError(where, "No such type " + type_name + " found. Perhaps you are missing an assembly reference.");
		} else {
			foreach (var method in reflected_type.GetMethods()) {
				var adjusted_arity = method.GetParameters().Length + (method.IsStatic ? 0 : 1);
				if (method.Name == method_name && adjusted_arity == arity && !method.IsGenericMethod && !method.IsGenericMethodDefinition && AllInParameteres(method)) {
					methods.Add(method);
				}
			}
			if (methods.Count == 0) {
				collector.RawError(where, "The type " + type_name + " has no public method named " + method_name + " which takes " + arity + " parameters.");
			}
		}
	}
	internal static bool AllInParameteres (System.Reflection.MethodInfo method) {
		foreach(var parameter in method.GetParameters()) {
			if (parameter.IsOut) {
				return false;
			}
		}
		return true;
	}
	internal static void CheckReflectedMethod(ErrorCollector collector, AstNode where, List<System.Reflection.MethodInfo> methods, List<expression> arguments, Type return_type) {
		/* If there are no candidate methods, don't bother checking the types. */
		if (methods.Count == 0)
			return;
		/* Find all the methods that match the needed type. */
		var candidate_methods = from method in methods
			where (TypeFromClrType(method.ReturnType) & return_type) != 0
			select method;
		if (candidate_methods.Count() == 0) {
			/* Produce an error for the union of all the types. */
			Type candiate_return = 0;
			foreach (var method in methods) {
				candiate_return |= TypeFromClrType(method.ReturnType);
			}
			collector.ReportTypeError(where, return_type, candiate_return);
			return;
		}
		/* Check that the arguments match the union of the parameters of all the methods. This means that we might still not have a valid method, but we can check again during codegen. */
		for (var it = 0; it < arguments.Count; it++) {
			Type candidate_parameter_type = 0;
			foreach (var method in methods) {
				var param_type = method.IsStatic ? method.GetParameters()[it].ParameterType : (it == 0 ? method.ReflectedType : method.GetParameters()[it - 1].ParameterType);
					candidate_parameter_type |= TypeFromClrType(param_type);
			}
			arguments[it].EnsureType(collector, candidate_parameter_type);
		}
	}
	public static Type TypeFromClrType(System.Type clr_type) {
		if (clr_type == typeof(bool) || clr_type == typeof(Boolean)) {
				return Type.Bool;
		} else if ( clr_type == typeof(sbyte) || clr_type == typeof(short) || clr_type == typeof(int) || clr_type == typeof(long) || clr_type == typeof(SByte) || clr_type == typeof(Int16) || clr_type == typeof(Int32) || clr_type == typeof(Int64)) {
			return Type.Int;
		} else if (clr_type == typeof(float) || clr_type == typeof(double) || clr_type == typeof(Single) || clr_type == typeof(Double)) {
			return Type.Float;
		} else if (clr_type == typeof(string) || clr_type == typeof(String) || clr_type == typeof(Stringish)) {
			return Type.Str;
		} else if (clr_type == typeof(Frame)) {
			return Type.Frame;
		} else if (clr_type == typeof(Template)) {
			return Type.Template;
		} else {
			return 0;
		}
	}
	public static System.Type[] ClrTypeFromType(Type type) {
		var types = new System.Type[(((ulong) type) * 0x200040008001UL & 0x111111111111111UL) % 0xf];
		var index = 0;
		if (type.HasFlag(Type.Bool)) types[index++] = typeof(bool);
		if (type.HasFlag(Type.Float)) types[index++] = typeof(double);
		if (type.HasFlag(Type.Frame)) types[index++] = typeof(Frame);
		if (type.HasFlag(Type.Int)) types[index++] = typeof(long);
		if (type.HasFlag(Type.Template)) types[index++] = typeof(Template);
		if (type.HasFlag(Type.Unit)) types[index++] = typeof(Unit);
		return types;
	}
}
public interface ErrorCollector {
	void ReportTypeError(AstNode where, Type new_type, Type existing_type);
	void ReportTypeError(Environment environment, string name, Type new_type, Type existing_type);
	void ReportForbiddenNameAccess(Environment environment, string name);
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
		EnsureType(collector, Type.Frame);
		if (!Children.ContainsKey(name)) {
			CreateChild(collector, name, Name);
		}
		return Children[name];
	}
	internal NameInfo Lookup(ErrorCollector collector, IEnumerator<string> names) {
		var info = this;
		while (names.MoveNext()) {
			info.EnsureType(collector, Type.Frame);
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
	public virtual LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
	return null;
}
	public virtual string CheckValidNarrowing(LookupCache next, LookupCache current) {
		return null;
	}
	public virtual bool NeedsLoad() {
		return false;
	}
}
public class OpenNameInfo : NameInfo {
	private Environment Environment;
	protected Type RealType = Type.Any;
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
	public override bool NeedsLoad() {
		return true;
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		var lookup_result = generator.MakeField("lookup", typeof(object));
		generator.LoadTaskMaster();
		generator.Builder.Emit(OpCodes.Dup);
		source_reference.Load(generator);
		context.Load(generator);
		var name_parts = Name.Split('.');
		generator.Builder.Emit(OpCodes.Ldc_I4, name_parts.Length);
		generator.Builder.Emit(OpCodes.Newarr, typeof(string));
		for (var it = 0; it < name_parts.Length; it++) {
			generator.Builder.Emit(OpCodes.Dup);
			generator.Builder.Emit(OpCodes.Ldc_I4, it);
			generator.Builder.Emit(OpCodes.Ldstr, name_parts[it]);
			generator.Builder.Emit(OpCodes.Stelem);
		}
		generator.Builder.Emit(OpCodes.Newobj, typeof(Lookup).GetConstructors()[0]);
		generator.Builder.Emit(OpCodes.Dup);
		generator.GenerateConsumeResult(lookup_result, true);
		generator.Builder.Emit(OpCodes.Call, typeof(Lookup).GetMethod("Notify"));
		generator.Builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("Slot"));
		return new LoadableCache(lookup_result, RealType, this);
	}
}
public class OverrideNameInfo : OpenNameInfo {
	public OverrideNameInfo(Environment environment, string name) : base(environment, name) {}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(generator.InitialOriginal, RealType, this);
	}
}
public class JunkInfo : NameInfo {
	public JunkInfo() {
	}
	public override void EnsureType(ErrorCollector collector, Type type) {
	}
	public override void CreateChild(ErrorCollector collector, string name, string root) {
		Children[name] = new JunkInfo();
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		throw new InvalidOperationException("Attempted to load invalid name.");
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
	public override string CheckValidNarrowing(LookupCache next, LookupCache current) {
		var parent_value = current[Source];
		var union_type = AstTypeableNode.TypeFromClrType(parent_value.BackingType);
		if ((union_type & Mask) == 0) {
			return String.Format("Value for “{0}” must be to {1}, but it is {2}.", Name, Mask, union_type);
		} else {
			next[this] = parent_value;
			return null;
		}
	}
}
public class LoadableCache {
	public LoadableValue Value { get; private set; }
	public Type PossibleTypes { get; private set; }
	public NameInfo NameInfo { get; private set; }
	public bool SinglyTyped { get { return (PossibleTypes & (PossibleTypes - 1)) == 0; } }
	public System.Type[] Types { get; private set; }
	public LoadableCache(LoadableValue loadable_value, Type type, NameInfo name_info) {
		Value = loadable_value;
		PossibleTypes = type;
		NameInfo = name_info;
		Types = AstTypeableNode.ClrTypeFromType(type);
	}
}
public class Environment {
	Environment Parent;
	Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	Dictionary<ITypeableElement, Type> IntrinsicTypes = new Dictionary<ITypeableElement, Type>();
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
		Priority = (parent == null ? 0 : parent.Priority) + (force_back ? 1 : 2);
	}

	internal NameInfo AddMask(string name, ITypeableElement expression) {
		if (Children.ContainsKey(name)) {
			throw new InvalidOperationException("The name " + name + " already exists in the environment.");
		}
		return Children[name] = new BoundNameInfo(this, name, expression);
	}
	public NameInfo AddOverrideName(string name) {
		return Children[name] = new OverrideNameInfo(this, name);
	}
	internal void AddForbiddenName(string name) {
		Children[name] = null;
	}
	internal void GenerateLookupCache(Generator generator, LookupCache current, LoadableValue source_reference, LoadableValue context, Generator.ParameterisedBlock<LookupCache> block) {
		var base_lookup_cache = new LookupCache(current);
		string narrow_error = null;
		foreach (var info in Children.Values) {
			var current_narrow_error = info.CheckValidNarrowing(base_lookup_cache, current);
			if (narrow_error != null && current_narrow_error != null) {
				narrow_error = narrow_error + current_narrow_error;
			} else {
				narrow_error = narrow_error ?? current_narrow_error;
			}
		}
		if (narrow_error != null) {
			generator.Builder.Emit(OpCodes.Ldstr, narrow_error);
			generator.Builder.Emit(OpCodes.Callvirt, typeof(TaskMaster).GetMethod("ReportOtherError"));
			generator.Builder.Emit(OpCodes.Ldc_I4_0);
			generator.Builder.Emit(OpCodes.Ret);
			return;
		}
		var load_count = 0;
		foreach (var info in Children.Values) {
			load_count += info.NeedsLoad() ? 1 : 0;
		}
		var lookup_results = new List<LoadableCache>();
		if (load_count > 0) {
			generator.StartInterlock(load_count);
			foreach (var info in Children.Values) {
				lookup_results.Add(info.Load(generator, source_reference, context));
			}
			generator.StopInterlock();
		}
		foreach (var lookup_result in lookup_results.Where(x => x.SinglyTyped)) {
			base_lookup_cache[lookup_result.NameInfo] = new AutoUnboxValue(lookup_result.Value, lookup_result.Types[0]);
			var label = generator.Builder.DefineLabel();
			lookup_result.Value.Load(generator);
			generator.Builder.Emit(OpCodes.Isinst, lookup_result.Types[0]);
			generator.Builder.Emit(OpCodes.Brtrue, label);
			generator.EmitTypeError(source_reference, String.Format("Expected type {0} for “{1}”, but got {2}.", lookup_result.Value, lookup_result.NameInfo.Name, "{0}"));
			generator.Builder.MarkLabel(label);
		}
		GenerateLookupPermutation(generator, base_lookup_cache, 0, lookup_results.Where(x => !x.SinglyTyped).ToArray(), source_reference, block);
	}
	private void GenerateLookupPermutation(Generator generator, LookupCache cache, int index, LoadableCache[] values, LoadableValue source_reference, Generator.ParameterisedBlock<LookupCache> block) {
		if (index >= values.Length) {
			block(cache);
			return;
		}
		var labels = new Label[values[index].Types.Length];
		for (var it = 0; it < labels.Length; it++) {
			labels[it] = generator.Builder.DefineLabel();
			values[index].Value.Load(generator);
			generator.Builder.Emit(OpCodes.Isinst, values[index].Types[it]);
			generator.Builder.Emit(OpCodes.Brtrue, labels[it]);
			generator.EmitTypeError(source_reference, String.Format("Expected type {0} for “{1}”, but got {2}.", values[it].Value, values[it].NameInfo.Name, "{0}"));
		}
		for (var it = 0; it < labels.Length; it++) {
			generator.Builder.MarkLabel(labels[it]);
			var sub_cache = new LookupCache(cache);
			sub_cache[values[index].NameInfo] = new AutoUnboxValue(values[index].Value, values[index].Types[it]);
			GenerateLookupPermutation(generator, sub_cache, index + 1, values, source_reference, block);
		}
	}
	public NameInfo Lookup(ErrorCollector collector, IEnumerable<string> names) {
		IEnumerator<string> enumerator = names.GetEnumerator();
		if (!enumerator.MoveNext()) {
			throw new ArgumentOutOfRangeException("List of names cannot be empty.");
		}
		if (Children.ContainsKey(enumerator.Current)) {
			if (Children[enumerator.Current] == null) {
				collector.ReportForbiddenNameAccess(this, enumerator.Current);
				return new JunkInfo();
			}
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
	internal void EnsureIntrinsic<T>(ErrorCollector collector, T node, Type type) where T:AstNode, ITypeableElement {
		if (IntrinsicTypes.ContainsKey(node)) {
			var original_type = IntrinsicTypes[node];
			var result = original_type & type;
			if (result == 0) {
				collector.ReportTypeError(node, original_type, type);
			} else {
				IntrinsicTypes[node] = result;
			}
		} else {
			IntrinsicTypes[node] = type;
		}
	}
}
}
