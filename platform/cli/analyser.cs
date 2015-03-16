using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
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
	Unit = 64
}
internal abstract class AstTypeableNode : AstNode {
	protected Environment Environment;
	internal virtual int EnvironmentPriority { get { return Environment.Priority; } }
	internal abstract Environment PropagateEnvironment(ErrorCollector collector, List<AstTypeableNode> queue, Environment environment, ref bool success);
	internal abstract void MakeTypeDemands(ErrorCollector collector, ref bool _success);
	public bool Analyse(ErrorCollector collector) {
		var environment = new Environment (FileName, StartRow, StartColumn, EndRow, EndColumn, null, false);
		var queue = new List<AstTypeableNode>();
		var success = true;
		PropagateEnvironment(collector, queue, environment, ref success);
		var sorted_nodes = new SortedDictionary<int, Dictionary<AstTypeableNode, bool>>();

		foreach (var element in queue) {
			if (!sorted_nodes.ContainsKey(element.Environment.Priority)) {
				sorted_nodes[element.Environment.Priority] = new Dictionary<AstTypeableNode, bool>();
			}
			sorted_nodes[element.Environment.Priority][element] = true;
		}
		foreach (var items in sorted_nodes.Values) {
			foreach (var element in items.Keys) {
				element.MakeTypeDemands(collector, ref success);
			}
		}
		return success;
	}
	internal static void ReflectMethod(ErrorCollector collector, AstNode where, string type_name, string method_name, int arity, List<System.Reflection.MethodInfo> methods, ref bool success) {
		var reflected_type = System.Type.GetType(type_name, false);
		if (reflected_type == null) {
			success = false;
			collector.ReportRawError(where, "No such type " + type_name + " found. Perhaps you are missing an assembly reference.");
		} else {
			foreach (var method in reflected_type.GetMethods()) {
				var adjusted_arity = method.GetParameters().Length + (method.IsStatic ? 0 : 1);
				if (method.Name == method_name && adjusted_arity == arity && !method.IsGenericMethod && !method.IsGenericMethodDefinition && AllInParameteres(method)) {
					methods.Add(method);
				}
			}
			if (methods.Count == 0) {
				success = false;
				collector.ReportRawError(where, "The type " + type_name + " has no public method named " + method_name + " which takes " + arity + " parameters.");
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
	internal static Type CheckReflectedMethod(ErrorCollector collector, AstNode where, List<System.Reflection.MethodInfo> methods, List<expression> arguments, Type return_type, ref bool success) {
		/* If there are no candidate methods, don't bother checking the types. */
		if (methods.Count == 0)
			return 0;
		/* Find all the methods that match the needed type. */
		var candidate_methods = from method in methods
			where (TypeFromClrType(method.ReturnType) & return_type) != 0
			select method;

		Type candiate_return = 0;
		foreach (var method in candidate_methods.Count() == 0 ? methods : candidate_methods) {
			candiate_return |= TypeFromClrType(method.ReturnType);
		}
		/* Produce an error for the union of all the types. */
		if (candidate_methods.Count() == 0) {
			collector.ReportExpressionTypeError(where, return_type, candiate_return);
			return 0;
		}
		/* Check that the arguments match the union of the parameters of all the methods. This means that we might still not have a valid method, but we can check again during codegen. */
		for (var it = 0; it < arguments.Count; it++) {
			Type candidate_parameter_type = 0;
			foreach (var method in methods) {
				var param_type = method.IsStatic ? method.GetParameters()[it].ParameterType : (it == 0 ? method.ReflectedType : method.GetParameters()[it - 1].ParameterType);
					candidate_parameter_type |= TypeFromClrType(param_type);
			}
			arguments[it].EnsureType(collector, candidate_parameter_type, ref success, true);
		}
		return candiate_return;
	}
	public static Type TypeFromClrType(System.Type clr_type) {
		if (clr_type == typeof(bool)) {
				return Type.Bool;
		} else if (clr_type == typeof(sbyte) || clr_type == typeof(short) || clr_type == typeof(int) || clr_type == typeof(long) || clr_type == typeof(byte) || clr_type == typeof(ushort) || clr_type == typeof(uint) || clr_type == typeof(ulong)) {
			return Type.Int;
		} else if (clr_type == typeof(float) || clr_type == typeof(double)) {
			return Type.Float;
		} else if (clr_type == typeof(string) || clr_type == typeof(Stringish)) {
			return Type.Str;
		} else if (clr_type == typeof(Frame)) {
			return Type.Frame;
		} else if (clr_type == typeof(Template)) {
			return Type.Template;
		} else if (clr_type == typeof(Unit)) {
			return Type.Unit;
		} else if (clr_type == typeof(object)) {
			return NameInfo.AnyType;
		} else {
			return 0;
		}
	}
	public static System.Type[] ClrTypeFromType(Type type) {
		int count = 0;
		for (var n = (int) type; n > 0; n &= (n - 1)) {
				count++;
		}
		var types = new System.Type[count];
		var index = 0;
		if (type.HasFlag(Type.Bool)) types[index++] = typeof(bool);
		if (type.HasFlag(Type.Float)) types[index++] = typeof(double);
		if (type.HasFlag(Type.Frame)) types[index++] = typeof(Frame);
		if (type.HasFlag(Type.Int)) types[index++] = typeof(long);
		if (type.HasFlag(Type.Str)) types[index++] = typeof(Stringish);
		if (type.HasFlag(Type.Template)) types[index++] = typeof(Template);
		if (type.HasFlag(Type.Unit)) types[index++] = typeof(Unit);
		return types;
	}
	public static Type HorrendousTypeMerge(Type expr_result, Type original) {
		/* This mostly revolves around null coalesence. Imagine we want to ensure
		 * the type of `Null ?? 3` is an integer. The alternate branch is just fine, so
		 * we let it alone. The main path is a problem. It can be integer or unit. If
		 * it's not in the original type mask (i.e., unit), that's okay, because we
		 * expect the collescence expression to fix the problem at run-time. */
		if (expr_result == 0)
			return 0;
		return (expr_result & original) == 0 ? original : expr_result;
	}
}
internal interface ITypeableElement {
	Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox);
}
internal class EnvironmentPrioritySorter : IComparer<AstTypeableNode> {
	public int Compare(AstTypeableNode x, AstTypeableNode y) {
		return x.EnvironmentPriority - y.EnvironmentPriority;
	}
}
internal abstract class NameInfo {
	public const Type AnyType = Type.Bool | Type.Float | Type.Frame | Type.Int | Type.Str | Type.Template | Type.Unit;
	protected Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	public string Name { get; protected set; }
	internal void AddAll(List<NameInfo> target) {
		target.Add(this);
		foreach (var child in Children.Values) {
			child.AddAll(target);
		}
	}
	internal NameInfo Lookup(ErrorCollector collector, string name, ref bool success) {
		EnsureType(collector, Type.Frame, ref success, false);
		if (!Children.ContainsKey(name)) {
			CreateChild(collector, name, Name, ref success);
		}
		return Children[name];
	}
	internal NameInfo Lookup(ErrorCollector collector, IEnumerator<string> names, ref bool success) {
		var info = this;
		while (names.MoveNext()) {
			info.EnsureType(collector, Type.Frame, ref success, false);
			if (!info.Children.ContainsKey(names.Current)) {
				info.CreateChild(collector, names.Current, info.Name, ref success);
			}
			info = info.Children[names.Current];
		}
		return info;
	}
	public virtual bool HasName(string name) {
		return Children.ContainsKey(name);
	}
	public abstract Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox);
	public abstract void CreateChild(ErrorCollector collector, string name, string root, ref bool success);
	public abstract LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context);
	public virtual string CheckValidNarrowing(LookupCache next, LookupCache current) {
		return null;
	}
	public virtual bool NeedsLoad(LookupCache current) {
		return false;
	}
	protected LoadableValue GenerateLookupField(Generator generator, LoadableValue source_reference, LoadableValue context) {
		var lookup_result = generator.MakeField("lookup_" + Name, typeof(object));
		generator.LoadTaskMaster();
		generator.Builder.Emit(OpCodes.Dup);
		source_reference.Load(generator);
		var name_parts = Name.Split('.');
		generator.Builder.Emit(OpCodes.Ldc_I4, name_parts.Length);
		generator.Builder.Emit(OpCodes.Newarr, typeof(string));
		for (var it = 0; it < name_parts.Length; it++) {
			generator.Builder.Emit(OpCodes.Dup);
			generator.Builder.Emit(OpCodes.Ldc_I4, it);
			generator.Builder.Emit(OpCodes.Ldstr, name_parts[it]);
			generator.Builder.Emit(OpCodes.Stelem, typeof(string));
		}
		context.Load(generator);
		generator.Builder.Emit(OpCodes.Newobj, typeof(Lookup).GetConstructors()[0]);
		generator.Builder.Emit(OpCodes.Dup);
		generator.GenerateConsumeResult(lookup_result, true);
		generator.Builder.Emit(OpCodes.Call, typeof(Lookup).GetMethod("Notify", new System.Type[] { typeof(ConsumeResult) }));
		generator.Builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("Slot", new System.Type[] { typeof(Computation) }));
		return lookup_result;
	}
}
internal class OpenNameInfo : NameInfo {
	private Environment Environment;
	protected Type RealType = AnyType;
	private bool must_unbox = false;
	public OpenNameInfo(Environment environment, string name) {
		Environment = environment;
		Name = name;
	}
	public override Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox) {
		this.must_unbox |= must_unbox;
		if ((RealType & type) == 0) {
			success = false;
			collector.ReportLookupTypeError(Environment, Name, RealType, type);
		} else {
			RealType &= type;
		}
		return RealType;
	}
	public override void CreateChild(ErrorCollector collector, string name, string root, ref bool success) {
		Children[name] = new OpenNameInfo(Environment, root + "." + name);
	}
	public override bool NeedsLoad(LookupCache current) {
		return true;
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(GenerateLookupField(generator, source_reference, context), RealType, this, must_unbox);
	}
}
internal class OverrideNameInfo : RestrictableType {
	private Environment Environment;
	protected Type RealType = AnyType;
	private bool must_unbox = false;
	public override Type RestrictedType { get { return RealType; } }
	public override bool MustUnbox { get { return must_unbox; } }
	public OverrideNameInfo(Environment environment, string name) {
		Environment = environment;
		Name = name;
	}
	public override Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox) {
		this.must_unbox |= must_unbox;
		if ((RealType & type) == 0) {
			success = false;
			collector.ReportLookupTypeError(Environment, Name, RealType, type);
		} else {
			RealType &= type;
		}
		return RealType;
	}
	public override void CreateChild(ErrorCollector collector, string name, string root, ref bool success) {
		Children[name] = new OpenNameInfo(Environment, root + "." + name);
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(generator.InitialOriginal, RealType, this, must_unbox);
	}
}
internal class JunkInfo : NameInfo {
	public JunkInfo() {
	}
	public override Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox) {
		return AnyType;
	}
	public override void CreateChild(ErrorCollector collector, string name, string root, ref bool success) {
		Children[name] = new JunkInfo();
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		throw new InvalidOperationException("Attempted to load invalid name.");
	}
}
internal class BoundNameInfo : RestrictableType {
	private Environment Environment;
	ITypeableElement Target;
	private bool must_unbox = false;
	public override Type RestrictedType { get { return restricted_type; } }
	public override bool MustUnbox { get { return must_unbox; } }
	private Type restricted_type = AnyType;
	public BoundNameInfo(Environment environment, string name, ITypeableElement target) {
		Environment = environment;
		Name = name;
		Target = target;
	}
	public override Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox) {
		this.must_unbox |= must_unbox;
		restricted_type &= type;
		return Target.EnsureType(collector, type, ref success, must_unbox);
	}
	public override bool NeedsLoad(LookupCache current) {
		return !current.Has(this);
	}
	public override void CreateChild(ErrorCollector collector, string name, string root, ref bool success) {
		Children[name] = new OpenNameInfo(Environment, root + "." + name);
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		return new LoadableCache(GenerateLookupField(generator, source_reference, context), RestrictedType, this, must_unbox);
	}
}
internal class CopyFromParentInfo : NameInfo {
	Environment Environment;
	NameInfo Source;
	Type Mask = AnyType;
	private bool must_unbox = false;
	bool ForceBack;

	public CopyFromParentInfo(Environment environment, string name, NameInfo source, bool force_back) {
		Environment = environment;
		Name = name;
		Source = source;
		ForceBack = force_back;
	}
	public override Type EnsureType(ErrorCollector collector, Type type, ref bool success, bool must_unbox) {
		this.must_unbox |= must_unbox;
		if (ForceBack) {
			return Source.EnsureType(collector, type, ref success, must_unbox);
		} else {
			if ((Mask & type) == 0) {
				success = false;
				collector.ReportLookupTypeError(Environment, Name, Mask, type);
			}
			Mask &= type;
			return Mask;
		}
	}
	public override void CreateChild(ErrorCollector collector, string name, string root, ref bool success) {
		if (ForceBack) {
			Source.CreateChild(collector, name, root, ref success);
		}
		if (Source.HasName(name)) {
			Children[name] = new CopyFromParentInfo(Environment, root + "." + name, Source.Lookup(collector, name, ref success), ForceBack);
		} else {
			Children[name] = new OpenNameInfo(Environment, root + "." + name);
		}
	}
	public override bool HasName(string name) {
		return base.HasName(name) || Source.HasName(name);
	}
	public override bool NeedsLoad(LookupCache current) {
		return !current.Has(Source) || must_unbox && current[Source].BackingType == typeof(object);
	}
	public override string CheckValidNarrowing(LookupCache next, LookupCache current) {
		if (current.Has(Source)) {
			var parent_value = current[Source];
			var union_type = AstTypeableNode.TypeFromClrType(parent_value.BackingType);
			if ((union_type & Mask) == 0) {
				return String.Format("Value for “{0}” must be to {1}, but it is {2}.", Name, Mask, union_type);
			} else {
				next[this] = parent_value;
				return null;
			}
		} else {
			return null;
		}
	}
	public override LoadableCache Load(Generator generator, LoadableValue source_reference, LoadableValue context) {
		var source_cache = Source.Load(generator, source_reference, context);
		return new LoadableCache(source_cache.Value, source_cache.PossibleTypes & Mask, this, must_unbox);
	}
}
internal abstract class RestrictableType : NameInfo {
	public abstract Type RestrictedType { get; }
	public abstract bool MustUnbox { get; }
}
internal class LoadableCache {
	public LoadableValue Value { get; private set; }
	public Type PossibleTypes { get; private set; }
	public NameInfo NameInfo { get; private set; }
	public bool SinglyTyped { get { return Types.Length == 1; } }
	public System.Type[] Types { get; private set; }
	public bool NeedsUnbox { get; private set; }
	public bool DirectCopy { get {
		return Value.BackingType != typeof(object) || !NeedsUnbox;
	} }
	public LoadableCache(LoadableValue loadable_value, RestrictableType name_info) : this(loadable_value, name_info.RestrictedType, name_info, name_info.MustUnbox) {
	}
	public LoadableCache(LoadableValue loadable_value, Type type, NameInfo name_info, bool must_unbox) {
		Value = loadable_value;
		PossibleTypes = type;
		NameInfo = name_info;
		Types = AstTypeableNode.ClrTypeFromType(type);
		NeedsUnbox = must_unbox;
	}
}
internal class Environment : CodeRegion {
	Environment Parent;
	Dictionary<string, NameInfo> Children = new Dictionary<string, NameInfo>();
	Dictionary<AstNode, Tuple<Type, bool>> Intrinsics = new Dictionary<AstNode, Tuple<Type, bool>>();
	public string PrettyName { get { return "region of lookups"; } }
	public string FileName { get; private set; }
	public int StartRow { get; private set; }
	public int StartColumn { get; private set; }
	public int EndRow { get; private set; }
	public int EndColumn { get; private set; }
	public int Priority { get; private set; }
	public bool TopLevel { get { return Parent == null ? top_level : Parent.TopLevel; } }
	bool top_level;
	bool ForceBack;
	bool combinatorial_explosion;

	public Environment(string filename, int start_row, int start_column, int end_row, int end_column, Environment parent = null, bool force_back = false, bool top_level = false) {
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
		this.top_level = top_level;
	}

	internal BoundNameInfo AddMask(string name, ITypeableElement expression) {
		if (Children.ContainsKey(name)) {
			throw new InvalidOperationException("The name " + name + " already exists in the environment.");
		}
		var nameinfo = new BoundNameInfo(this, name, expression);
		Children[name] = nameinfo;
		return nameinfo;
	}
	public RestrictableType AddOverrideName(string name) {
		var info = new OverrideNameInfo(this, name);
		Children[name] = info;
		return info;
	}
	internal void AddForbiddenName(string name) {
		Children[name] = null;
	}
	public delegate void Block(LoadableValue context, LookupCache cache);
	internal void GenerateLookupCache(Generator generator, RevCons<Tuple<string, LoadableCache>> specials, LookupCache current, LoadableValue source_reference, LoadableValue context, LoadableValue self_frame, Block block) {
		generator.DebugPosition(this);
		var lookup_results = new List<LoadableCache>();
		if (specials != null) {
			var child_context = generator.MakeField("anon_frame", typeof(Context));
			generator.Builder.Emit(OpCodes.Ldarg_0);
			generator.LoadTaskMaster();
			generator.GenerateNextId();
			source_reference.Load(generator);
			context.Load(generator);
			self_frame.Load(generator);
			generator.Builder.Emit(OpCodes.Newobj, typeof(Frame).GetConstructors()[0]);
			foreach (var entry in specials.ToArray()) {
				generator.Builder.Emit(OpCodes.Dup);
				generator.Builder.Emit(OpCodes.Ldstr, entry.Item1);
				generator.LoadReboxed(entry.Item2.Value, typeof(object));
				generator.Builder.Emit(OpCodes.Call, typeof(Frame).GetMethod("set_Item", new System.Type[] { typeof(string), typeof(object) }));
				lookup_results.Add(entry.Item2);
			}
			context.Load(generator);
			generator.Builder.Emit(OpCodes.Call, typeof(Context).GetMethod("Prepend", new System.Type[] { typeof(Frame), typeof(Context) }));
			generator.Builder.Emit(OpCodes.Stfld, child_context.Field);
			// Promote the context with the specials to proper status
			context = child_context;
		}

		var base_lookup_cache = new LookupCache(current);
		var all_children = new List<NameInfo>();
		string narrow_error = null;
		foreach (var info in Children.Values) {
			if (info == null) {
				continue;
			}
			info.AddAll(all_children);
		}
		foreach (var info in all_children) {
			var current_narrow_error = info.CheckValidNarrowing(base_lookup_cache, current);
			if (narrow_error != null && current_narrow_error != null) {
				narrow_error = String.Format("{0}\n{1}", narrow_error, current_narrow_error);
			} else {
				narrow_error = narrow_error ?? current_narrow_error;
			}
		}
		if (narrow_error != null) {
			generator.LoadTaskMaster();
			source_reference.Load(generator);
			generator.Builder.Emit(OpCodes.Ldstr, narrow_error);
			generator.Builder.Emit(OpCodes.Callvirt, typeof(TaskMaster).GetMethod("ReportOtherError", new System.Type[] { typeof(SourceReference), typeof(string) }));
			generator.Builder.Emit(OpCodes.Ldc_I4_0);
			generator.Builder.Emit(OpCodes.Ret);
			return;
		}
		var load_count = 0;
		foreach (var info in all_children) {
			load_count += info.NeedsLoad(current) ? 1 : 0;
		}
		if (load_count > 0) {
			generator.StartInterlock(load_count);
			foreach (var info in all_children) {
				if (info.NeedsLoad(current)) {
					lookup_results.Add(info.Load(generator, source_reference, context));
				}
			}
			var state = generator.DefineState();
			generator.SetState(state);
			generator.DecrementInterlock(generator.Builder);
			var end_label = generator.Builder.DefineLabel();
			generator.Builder.Emit(OpCodes.Brfalse, end_label);
			generator.Builder.Emit(OpCodes.Ldc_I4_0);
			generator.Builder.Emit(OpCodes.Ret);
			generator.Builder.MarkLabel(end_label);
			generator.JumpToState(state);
			generator.MarkState(state);
		}
		foreach (var lookup_result in lookup_results.Where(x => x.DirectCopy)) {
			base_lookup_cache[lookup_result.NameInfo] = lookup_result.Value;
		}
		foreach (var lookup_result in lookup_results.Where(x => x.SinglyTyped && !x.DirectCopy)) {
			base_lookup_cache[lookup_result.NameInfo] = new AutoUnboxValue(lookup_result.Value, lookup_result.Types[0]);
			var label = generator.Builder.DefineLabel();
			lookup_result.Value.Load(generator);
			generator.Builder.Emit(OpCodes.Isinst, lookup_result.Types[0]);
			generator.Builder.Emit(OpCodes.Brtrue, label);
			generator.EmitTypeError(source_reference, String.Format("Expected type {0} for “{1}”, but got {2}.", lookup_result.Types[0], lookup_result.NameInfo.Name, "{0}"), lookup_result.Value);
			generator.Builder.MarkLabel(label);
		}
		var permutable_caches = lookup_results.Where(x => !x.SinglyTyped && !x.DirectCopy).ToArray();
		var old_paths = generator.Paths;
		generator.Paths = permutable_caches.Aggregate(old_paths, (acc, c) => acc * c.Types.Length);
		if (generator.Paths > 200 && !combinatorial_explosion) {
			Console.Error.WriteLine("{0}:{1}:{2}-{3}:{4}: There are {5} type-derived flows in the generated code. This will be slow to compile.", FileName, StartRow, StartColumn, EndRow, EndColumn, generator.Paths);
			combinatorial_explosion = true;
		}
		GenerateLookupPermutation(generator, context, base_lookup_cache, 0, permutable_caches, source_reference, block);
		generator.Paths = old_paths;
	}
	private void GenerateLookupPermutation(Generator generator, LoadableValue context, LookupCache cache, int index, LoadableCache[] values, LoadableValue source_reference, Block block) {
		if (index >= values.Length) {
			block(context, cache);
			generator.DebugPosition(this);
			return;
		}
		var labels = new Label[values[index].Types.Length];
		for (var it = 0; it < labels.Length; it++) {
			labels[it] = generator.Builder.DefineLabel();
			values[index].Value.Load(generator);
			generator.Builder.Emit(OpCodes.Isinst, values[index].Types[it]);
			generator.Builder.Emit(OpCodes.Brtrue, labels[it]);
		}
		generator.EmitTypeError(source_reference, String.Format("Expected type {0} for “{1}”, but got {2}.", String.Join(" or ", (object[]) values[index].Types), values[index].NameInfo.Name, "{0}"), values[index].Value);
		for (var it = 0; it < labels.Length; it++) {
			generator.Builder.MarkLabel(labels[it]);
			var sub_cache = new LookupCache(cache);
			sub_cache[values[index].NameInfo] = new AutoUnboxValue(values[index].Value, values[index].Types[it]);
			var builder = generator.Builder;
			GenerateLookupPermutation(generator, context, sub_cache, index + 1, values, source_reference, block);
			generator.Builder = builder;
		}
	}
	public NameInfo Lookup(ErrorCollector collector, IEnumerable<string> names, ref bool success) {
		IEnumerator<string> enumerator = names.GetEnumerator();
		if (!enumerator.MoveNext()) {
			throw new ArgumentOutOfRangeException("List of names cannot be empty.");
		}
		if (Children.ContainsKey(enumerator.Current)) {
			if (Children[enumerator.Current] == null) {
				success = false;
				collector.ReportForbiddenNameAccess(this, enumerator.Current);
				return new JunkInfo();
			}
			return Children[enumerator.Current].Lookup(collector, enumerator, ref success);
		}
		if (ForceBack) {
			Parent.Lookup(collector, names, ref success);
		}
		if (Parent != null && Parent.HasName(enumerator.Current)) {
			return Lookback(enumerator.Current).Lookup(collector, enumerator, ref success);
		}
		var info = new OpenNameInfo(this, enumerator.Current);
		Children[enumerator.Current] = info;
		return info.Lookup(collector, enumerator, ref success);
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
	internal Type EnsureIntrinsic(ErrorCollector collector, AstNode node, Type type, bool must_unbox, ref bool success) {
		if (Intrinsics.ContainsKey(node)) {
			var intrinsic = Intrinsics[node];
			var original_type = intrinsic.Item1;
			var result = original_type & type;
			if (result == 0) {
				success = false;
				collector.ReportExpressionTypeError(node, original_type, type);
			} else {
				Intrinsics[node] = new Tuple<Type, bool>(result, intrinsic.Item2 & must_unbox);
			}
			return result;
		} else {
			Intrinsics[node] = new Tuple<Type, bool>(type, must_unbox);
			return type;
		}
	}
	internal System.Type[] GetIntrinsicRealTypes(AstNode node) {
		if (Intrinsics.ContainsKey(node)) {
			return AstTypeableNode.ClrTypeFromType(Intrinsics[node].Item1);
		} else if (Parent != null) {
			return Parent.GetIntrinsicRealTypes(node);
		}
		throw new InvalidOperationException("There is no intrinsic type for the node requested. This a compiler bug.");
	}
	internal void IntrinsicDispatch(Generator generator, AstNode node, LoadableValue original, LoadableValue source_reference, Generator.ParameterisedBlock<LoadableValue> block) {
		var intrinsic = Intrinsics[node];
		if (!intrinsic.Item2) {
			block(original);
			return;
		}
		var types = AstTypeableNode.ClrTypeFromType(intrinsic.Item1);
		foreach (var type in types) {
			var next_label = generator.Builder.DefineLabel();
			original.Load(generator);
			generator.Builder.Emit(OpCodes.Isinst, type);
			generator.Builder.Emit(OpCodes.Brfalse, next_label);
			var builder = generator.Builder;
			block(new AutoUnboxValue(original, type));
			generator.Builder = builder;
			generator.Builder.MarkLabel(next_label);
		}
		generator.EmitTypeError(source_reference, String.Format("Expected type {0} for {1}, but got {2}.", String.Join("or", (object[]) types), node.PrettyName, "{0}"), original);
	}
}
}
