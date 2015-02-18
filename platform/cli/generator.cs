using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;
using System;

namespace Flabbergast {

/**
 * A handle for generating the needed components in an assembly.
 */
public class CompilationUnit {
	/**
	 * A call back that will populate a function with generated code.
	 */
	public delegate void FunctionBlock(Generator generator, LoadableValue source_reference, LoadableValue context, LoadableValue self, LoadableValue container);
	/**
	 * A call back that will populate a function with generated code.
	 */
	public delegate void FunctionOverrideBlock(Generator generator, LoadableValue source_reference, LoadableValue context, LoadableValue self, LoadableValue container, LoadableValue original);

	public IEnumerable<string> ExternalUris { get { return externals.Keys; } }
	/**
	 * The backing module builder from the library.
	 */
	public ModuleBuilder ModuleBuilder { get; private set; }
	/**
	 * The debugging symbol for this file.
	 */
	public System.Diagnostics.SymbolStore.ISymbolDocumentWriter SymbolDocument { get; private set; }

	internal Dictionary<string, bool> externals = new Dictionary<string, bool>();
	/**
	 * Counter for generating unique class names.
	 */
	private int type_count = 0;

	public CompilationUnit(string filename, ModuleBuilder module_builder, bool debuggable) {
		ModuleBuilder = module_builder;
		SymbolDocument = debuggable ? module_builder.DefineDocument(filename, Guid.Empty, Guid.Empty, Guid.Empty) : null;
	}

	/**
	 * Create a new function, and use the provided block to fill it with code.
	 */
	public MethodInfo CreateFunction(string name, FunctionBlock block) {
		var generator = CreateFunctionGenerator(name, false);
		block(generator, generator.InitialSourceReference, generator.InitialContext, generator.InitialSelfFrame, generator.InitialContainerFrame);
		generator.GenerateSwitchBlock();

		return generator.Initialiser;
	}
	/**
	 * Create a new override function, and use the provided block to fill it with code.
	 */
	public MethodInfo CreateFunctionOverride(string name, FunctionOverrideBlock block) {
		var generator = CreateFunctionGenerator(name, true);
		block(generator, generator.InitialSourceReference, generator.InitialContext, generator.InitialSelfFrame, generator.InitialContainerFrame, generator.InitialOriginal);
		generator.GenerateSwitchBlock();

		return generator.Initialiser;
	}

	private Generator CreateFunctionGenerator(string name, bool has_original) {
		var type_builder = ModuleBuilder.DefineType(name ?? ("Function" + type_count++), TypeAttributes.AutoLayout | TypeAttributes.Class | TypeAttributes.NotPublic | TypeAttributes.Sealed | TypeAttributes.UnicodeClass, typeof(Computation));
		return new Generator(this, type_builder, has_original);
	}
}

/**
 * Helper to generate code for a particular function or override function.
 */
public class Generator {
	/**
	 * Generate code with no input.
	 */
	public delegate void Block();
	/**
	 * Generate code for an item during a fold operation, using an initial value
	 * and passing the output to a result block.
	 */
	public delegate void FoldBlock<T, R>(int index, T item, R left, ParameterisedBlock<R> result);
	/**
	 * Generate code given a single input.
	 */
	public delegate void ParameterisedBlock<R>(R result);
	/**
	 * The compilation unit that created this function.
	 */
	public CompilationUnit Owner { get; private set; }
	/**
	 * The underlying class builder for this function.
	 */
	public TypeBuilder TypeBuilder { get; private set; }
	/**
	 * The body of the `Run` method for this function.
	 */
	public ILGenerator Builder { get; private set; }
	/**
	 * A static method capable of creating a new instance of the class.
	 */
	public MethodBuilder Initialiser { get; private set; }
	/**
	 * The source reference of the caller of this function.
	 */
	public FieldValue InitialSourceReference { get; private set; }
	/**
	 * The lookup context provided by the caller.
	 */
	public FieldValue InitialContext { get; private set; }
	/**
	 * The “This” frame provided by the caller.
	 */
	public FieldValue InitialSelfFrame { get; private set; }
	/**
	 * The “Container” provided by from the caller.
	 */
	public FieldValue InitialContainerFrame { get; private set; }
	/**
	 * The original value to an override function, null otherwise.
	 */
	public FieldValue InitialOriginal { get; private set; }
	/**
	 * The field containing the current state for this function to continue upon
	 * re-entry.
	 */
	private FieldInfo state_field;
	/**
	 * A reference count to control mutual exclusion.
	 */
	private FieldInfo interlock_field;
	/**
	 * The field containing the task master.
	 */
	private FieldInfo task_master;
	/**
	 * The labels in the code where the function may enter into a particular
	 * state. The index is the state number.
	 */
	private List<Label> entry_points = new List<Label>();
	/**
	 * A counter for producing unique result consumers names.
	 */
	private int result_consumer = 0;
	/**
	 * A counter for producing unique block entry names.
	 */
	private int block_entry = 0;
	/**
	 * The branch point, at the end of the function, that does dispatch from the
	 * state field to the correct branch.
	 *
	 * It would be ideal to place the dispatch at the start of the function, but
	 * the number of states is not known, so the function branches to this
	 * address, then branches based on the dispatch.
	 */
	private Label switch_label;

	/**
	 * Any blocks that need to have code generated once the contiguous code block has finished.
	 */
	private Dictionary<Label, Block> pending_generators = new Dictionary<Label, Block>();

	internal Generator(CompilationUnit owner, TypeBuilder type_builder, bool has_original) {
		Owner = owner;
		TypeBuilder = type_builder;
		// Create fields for all information provided by the caller.
		state_field = TypeBuilder.DefineField("state", typeof(long), FieldAttributes.Private);
		interlock_field = TypeBuilder.DefineField("interlock", typeof(int), FieldAttributes.Private);
		task_master = TypeBuilder.DefineField("task_master", typeof(TaskMaster), FieldAttributes.Private);
		InitialSourceReference = new FieldValue(TypeBuilder.DefineField("source_reference", typeof(SourceReference), FieldAttributes.Private));
		InitialContext = new FieldValue(TypeBuilder.DefineField("context", typeof(Context), FieldAttributes.Private));
		InitialSelfFrame = new FieldValue(TypeBuilder.DefineField("self", typeof(Frame), FieldAttributes.Private));
		InitialContainerFrame = new FieldValue(TypeBuilder.DefineField("container", typeof(Frame), FieldAttributes.Private));
		var construct_params = new System.Type[] { typeof(TaskMaster), typeof(SourceReference), typeof(Context), typeof(Frame), typeof(Frame) };
		var initial_information = new FieldInfo[] { task_master, InitialSourceReference.Field, InitialContext.Field, InitialSelfFrame.Field, InitialContainerFrame.Field };

		// Create a constructor the takes all the state information provided by the
		// caller and stores it in appropriate fields.
		var ctor = type_builder.DefineConstructor(MethodAttributes.Public, CallingConventions.Standard, construct_params);
		var ctor_builder = ctor.GetILGenerator();
		for(var it = 0; it < initial_information.Length; it++) {
			ctor_builder.Emit(OpCodes.Ldarg_0);
			ctor_builder.Emit(OpCodes.Ldarg, it + 1);
			ctor_builder.Emit(OpCodes.Stfld, initial_information[it]);
		}
		ctor_builder.Emit(OpCodes.Ldarg_0);
		ctor_builder.Emit(OpCodes.Ldc_I4_0);
		ctor_builder.Emit(OpCodes.Stfld, state_field);
		ctor_builder.Emit(OpCodes.Ret);

		System.Type[] init_params;
		if (has_original) {
			init_params = new System.Type[construct_params.Length + 1];
			for (var it = 0; it < construct_params.Length; it++) {
				init_params[it] = construct_params[it];
			}
			init_params[init_params.Length - 1] = typeof(Computation);
		} else {
			init_params = construct_params;
		}
		// Create a static method that wraps the constructor. This is needed to create a delegate.
		Initialiser = type_builder.DefineMethod("Init", MethodAttributes.Public | MethodAttributes.Static, typeof(Computation), init_params);
		var init_builder = Initialiser.GetILGenerator();
		if (has_original) {
			// If the thing we are overriding is null, create an error and give up.
			var has_instance = init_builder.DefineLabel();
			init_builder.Emit(OpCodes.Ldarg, init_params.Length - 1);
			init_builder.Emit(OpCodes.Brtrue, has_instance);
			init_builder.Emit(OpCodes.Ldarg_0);
			init_builder.Emit(OpCodes.Ldarg_1);
			init_builder.Emit(OpCodes.Ldstr, "Cannot perform override. No value in source tuple to override!");
			init_builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("ReportOtherError"));
			init_builder.Emit(OpCodes.Ldc_I4_0);
			init_builder.Emit(OpCodes.Ret);
			init_builder.MarkLabel(has_instance);
		}
		for (var it = 0; it < initial_information.Length; it++) {
			init_builder.Emit(OpCodes.Ldarg, it);
		}
		init_builder.Emit(OpCodes.Newobj, ctor);

		// If overriding, attach the overriding function to the original computation.
		if (has_original) {
			var init_this = init_builder.DeclareLocal(TypeBuilder);
			init_builder.Emit(OpCodes.Stloc, init_this);

			init_builder.Emit(OpCodes.Ldarg, initial_information.Length);
			init_builder.Emit(OpCodes.Ldloc, init_this);
			GenerateConsumeResult(InitialOriginal, init_builder, false, false);
			init_builder.Emit(OpCodes.Callvirt, typeof(Computation).GetMethod("Notify"));
			init_builder.Emit(OpCodes.Ldloc, init_this);
		}

		init_builder.Emit(OpCodes.Ret);

		// Create a run method with an initial state.
		Builder = type_builder.DefineMethod("Run", MethodAttributes.Public | MethodAttributes.Virtual, typeof(bool), new System.Type[0]).GetILGenerator();
		switch_label = Builder.DefineLabel();
		var start_label = Builder.DefineLabel();
		entry_points.Add(start_label);
		Builder.Emit(OpCodes.Br, switch_label);
		Builder.MarkLabel(start_label);
	}

	/**
	 * Create a new source reference based on an existing one, updated to reflect
	 * entry into a new AST node.
	 */
	public FieldValue AmendSourceReference(AstNode node, string message, LoadableValue source_reference) {
		var field = MakeField("source_reference", typeof(SourceReference));
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(OpCodes.Ldstr, message);
		Builder.Emit(OpCodes.Ldc_I4, node.StartRow);
		Builder.Emit(OpCodes.Ldc_I4, node.StartColumn);
		Builder.Emit(OpCodes.Ldc_I4, node.EndRow);
		Builder.Emit(OpCodes.Ldc_I4, node.EndColumn);
		source_reference.Load(Builder);
		Builder.Emit(OpCodes.Newobj, typeof(SourceReference).GetConstructors()[0]);
		Builder.Emit(OpCodes.Stfld, field.Field);
		return field;
	}
	/**
	 * Clips the range of the Int32 on the stack to -1, 0, or 1.
	 */
	public void Clamp() {
		Builder.Emit(OpCodes.Ldc_I4_1);
		Builder.Emit(OpCodes.Call, typeof(Math).GetMethod("Min", new System.Type[] { typeof(int), typeof(int) }));
		Builder.Emit(OpCodes.Ldc_I4_M1);
		Builder.Emit(OpCodes.Call, typeof(Math).GetMethod("Max", new System.Type[] { typeof(int), typeof(int) }));
	}
	/**
	 * Copies the contents of one field to another, boxing or unboxing based on
	 * the field types.
	 */
	void CopyField(LoadableValue source, FieldValue target) {
		CopyField(source, target.Field);
	}
	void CopyField(LoadableValue source, FieldInfo target) {
		Builder.Emit(OpCodes.Ldarg_0);
		source.Load(Builder);
		if (source.BackingType != target.FieldType) {
			if (target.FieldType == typeof(object)) {
				Builder.Emit(OpCodes.Box);
			} else {
				Builder.Emit(OpCodes.Unbox_Any, target.FieldType);
			}
		}
		Builder.Emit(OpCodes.Stfld, target);
	}
	/**
	 * Create a new state and fill it with the provided code. In the calling
	 * code, provide a delegate capable of directly entering the specified block.
	 */
	public void CreateBlockEntry<T>(Block block_generator) {
		var state = DefineState();
		var method = TypeBuilder.DefineMethod("BlockEntry" + block_entry++, MethodAttributes.Public, typeof(bool), new System.Type[0]);
		var block_builder = method.GetILGenerator();
		SetState(state, block_builder);
		block_builder.Emit(OpCodes.Call, typeof(Computation).GetMethod("Run"));
		block_builder.Emit(OpCodes.Ret);
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(OpCodes.Ldftn, method);
		Builder.Emit(OpCodes.Newobj, typeof(T).GetConstructors()[0]);

		pending_generators[entry_points[state]] = block_generator;
	}
	/**
	 * Create a block and add the resulting entry to a MergeIterator with the
	 * provided attribute name.
	 */
	public void CreateIteratorBlock(FieldValue iterator_instance, string name, Block block_generator) {
		iterator_instance.Load(Builder);
		Builder.Emit(OpCodes.Ldstr, name);
		CreateBlockEntry<MergeIterator.KeyDispatch>(block_generator);
		Builder.Emit(OpCodes.Callvirt, typeof(MergeIterator).GetMethod("AddDispatcher"));
	}
	/**
	 * Insert debugging information based on an AST node.
	 */
	public void DebugPosition(AstNode node) {
		if (Owner.SymbolDocument != null) {
			Builder.MarkSequencePoint(Owner.SymbolDocument, node.StartRow, node.StartColumn, node.EndRow, node.EndColumn);
		}
	}
	public void DecrementInterlock(ILGenerator builder, Label skip) {
		builder.Emit(OpCodes.Ldarg_0);
		builder.Emit(OpCodes.Ldflda, interlock_field);
		builder.Emit(OpCodes.Call, typeof(System.Threading.Interlocked).GetMethod("Decrement", new System.Type[] { typeof(int) }));
		builder.Emit(OpCodes.Brtrue, skip);
	}
	/**
	 * Generate a runtime dispatch that checks each of the provided types.
	 */
	void DynamicTypeDispatch(LoadableValue original, LoadableValue source_reference, System.Type[] types, ParameterisedBlock<LoadableValue> block) {
		var labels = new Label[types.Length];
		for(var it = 0; it < types.Length; it++) {
			labels[it] = Builder.DefineLabel();
			original.Load(Builder);
			Builder.Emit(OpCodes.Isinst, types[it]);
			Builder.Emit(OpCodes.Brtrue, labels[it]);
		}
		EmitTypeError(source_reference, String.Format("Unexpected type {0} instead of {1}.", "{0}", string.Join(", ", (object[]) types)), original);

		for(var it = 0; it < types.Length; it++) {
			Builder.MarkLabel(labels[it]);
			var converted_field = MakeField("converted", types[it]);
			CopyField(original, converted_field);
			block(converted_field);
		}
	}
	/**
	 * Create a new state and put it in the dispatch logic.
	 *
	 * This state must later be attached to a place in the code using `MarkState`.
	 */
	public int DefineState() {
		var label = Builder.DefineLabel();
		var id = entry_points.Count;
		entry_points.Add(label);
		return id;
	}
	public void EmitTypeError(LoadableValue source_reference, string message, params LoadableValue[] data) {
		LoadTaskMaster();
		source_reference.Load(Builder);
		Builder.Emit(OpCodes.Ldstr, message);
		foreach (var item in data) {
			item.Load(Builder);
			Builder.Emit(OpCodes.Call, typeof(object).GetMethod("GetType"));
		}
		var signature = new System.Type[data.Length + 1];
		signature[0] = typeof(string);
		for (var it = 0; it < data.Length; it++) {
			signature[it + 1] =  typeof(object);
		}
		Builder.Emit(OpCodes.Call, typeof(String).GetMethod("Format", signature));
		Builder.Emit(OpCodes.Callvirt, typeof(TaskMaster).GetMethod("ReportOtherError"));
		Builder.Emit(OpCodes.Ldc_I4_0);
		Builder.Emit(OpCodes.Ret);
	}
	/**
	 * Generate code for a list using a fold (i.e., each computation in the list
	 * is made from the previous computation).
	 */
	public void Fold<T, R>(R initial, List<T> list, FoldBlock<T, R> expand, ParameterisedBlock<R> result) {
		FoldHelper(list, expand, result, initial, 0);
	}
	private void FoldHelper<T, R>(List<T> list, FoldBlock<T, R> expand, ParameterisedBlock<R> result, R curr_result, int it) {
		if (it < list.Count) {
			expand(it, list[it], curr_result, (next_result) => FoldHelper(list, expand, result, next_result, it + 1));
		} else {
			result(curr_result);
		}
	}
	/**
	 * Generate code for any pending blocks.
	 */
	public void FlushBlocks() {
		while (pending_generators.Count > 0) {
			var todo = pending_generators;
			pending_generators = new Dictionary<Label, Block>();
			foreach(var entry in todo) {
				Builder.MarkLabel(entry.Key);
				entry.Value();
			}
		}
	}
	/**
	 * Generate a function to receive a value and request continued computation from the task master.
	 */
	public void GenerateConsumeResult(FieldValue result_target, bool interlocked = false) {
		GenerateConsumeResult(result_target, Builder, true, interlocked);
	}
	private void GenerateConsumeResult(FieldValue result_target, ILGenerator builder, bool load_instance, bool interlocked) {
		var method = TypeBuilder.DefineMethod("ConsumeResult" + result_consumer++, MethodAttributes.Public, typeof(void), new System.Type[] { typeof(object)});
		var consume_builder = method.GetILGenerator();
		if (result_target != null) {
			consume_builder.Emit(OpCodes.Ldarg_0);
			consume_builder.Emit(OpCodes.Ldarg_1);
			consume_builder.Emit(OpCodes.Stfld, result_target.Field);
		}
		LoadTaskMaster(consume_builder);
		var return_label = consume_builder.DefineLabel();
		if (interlocked) {
			DecrementInterlock(consume_builder, return_label);
		}
		consume_builder.Emit(OpCodes.Ldarg_0);
		consume_builder.Emit(OpCodes.Callvirt, typeof(TaskMaster).GetMethod("Slot"));
		consume_builder.MarkLabel(return_label);
		consume_builder.Emit(OpCodes.Ret);
		if (load_instance) {
			builder.Emit(OpCodes.Ldarg_0);
		}
		builder.Emit(OpCodes.Ldftn, method);
		builder.Emit(OpCodes.Newobj, typeof(ConsumeResult).GetConstructors()[0]);
	}
	public void GenerateNextId() {
		LoadTaskMaster();
		Builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("NextId"));
	}

	/**
	 * Finish this function by creating the state dispatch instruction using a
	 * switch (computed goto).
	 */
	internal void GenerateSwitchBlock() {
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(OpCodes.Ldfld, state_field);
		Builder.Emit(OpCodes.Switch, entry_points.ToArray());
		Builder.ThrowException(typeof(ArgumentOutOfRangeException));
	}
	/**
	 * Load the key and ordinal from an iterator instance and place them in the appropriate fields.
	 */
	public void LoadIteratorData(LoadableValue iterator, FieldValue key, FieldValue ordinal) {
		Builder.Emit(OpCodes.Ldarg_0);
		iterator.Load(Builder);
		Builder.Emit(OpCodes.Callvirt, typeof(MergeIterator).GetMethod("get_Current"));
		Builder.Emit(OpCodes.Stfld, key.Field);
		Builder.Emit(OpCodes.Ldarg_0);
		iterator.Load(Builder);
		Builder.Emit(OpCodes.Callvirt, typeof(MergeIterator).GetMethod("get_Position"));
		Builder.Emit(OpCodes.Stfld, ordinal.Field);
	}
	/**
	 * Load the task master in the `Run` function.
	 */
	public void LoadTaskMaster() {
		LoadTaskMaster(Builder);
	}
	/**
	 * Load the task master in any method of this class.
	 */
	public void LoadTaskMaster(ILGenerator builder) {
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(System.Reflection.Emit.OpCodes.Ldfld, task_master);
	}
	/**
	 * Create an anonymous field with the specified type.
	 */
	public FieldValue MakeField(string name, System.Type type) {
		return new FieldValue(TypeBuilder.DefineField(name, type, FieldAttributes.PrivateScope));
	}
	/**
	 * Mark the current code position as the entry point for a state.
	 */
	public void MarkState(int id) {
		Builder.MarkLabel(entry_points[id]);
	}
	public LoadableValue PushSourceReference(AstNode node, LoadableValue original_reference) {
		var reference = MakeField("source_reference", typeof(SourceReference));
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(OpCodes.Ldstr, String.Format("expression {0}", node.PrettyName));
		Builder.Emit(OpCodes.Ldstr, node.FileName);
		Builder.Emit(OpCodes.Ldc_I4, node.StartRow);
		Builder.Emit(OpCodes.Ldc_I4, node.StartColumn);
		Builder.Emit(OpCodes.Ldc_I4, node.EndRow);
		Builder.Emit(OpCodes.Ldc_I4, node.EndColumn);
		original_reference.Load(this);
		Builder.Emit(OpCodes.Newobj, typeof(SourceReference).GetConstructors()[0]);
		Builder.Emit(OpCodes.Stfld, reference.Field);
		return reference;
	}
	public LoadableValue ResolveUri(string uri, LoadableValue source_reference) {
		Owner.externals[uri] = true;
		var state = DefineState();
		var library_field = MakeField(uri, typeof(object));
		SetState(state);
		LoadTaskMaster();
		source_reference.Load(this);
		GenerateConsumeResult(library_field);
		Builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("GetExternal"));
		Builder.Emit(OpCodes.Ldc_I4_0);
		Builder.Emit(OpCodes.Ret);
		MarkState(state);
		return library_field;
	}
	/**
	 * Change the state that will be entered upon re-entry.
	 */
	public void SetState(int state) {
		SetState(state, Builder);
	}
	private void SetState(int state, ILGenerator builder) {
		builder.Emit(OpCodes.Ldarg_0);
		builder.Emit(OpCodes.Ldc_I4, state);
		builder.Emit(OpCodes.Stfld, state_field);
	}
	/**
	 * Slot a computation for execution by the task master.
	 */
	public void Slot(LoadableValue target) {
		LoadTaskMaster();
		target.Load(Builder);
		Builder.Emit(OpCodes.Call, typeof(TaskMaster).GetMethod("Slot"));
	}
	/**
	 * Slot a computation for execution and stop execution.
	 */
	public void SlotSleep(LoadableValue target) {
		Slot(target);
		Builder.Emit(OpCodes.Ldc_I4_0);
		Builder.Emit(OpCodes.Ret);
	}
	public void StartInterlock(int count) {
		Builder.Emit(OpCodes.Ldarg_0);
		Builder.Emit(OpCodes.Ldc_I4, count + 1);
		Builder.Emit(OpCodes.Stfld, interlock_field);
	}
	public void StopInterlock() {
		var state = DefineState();
		SetState(state);
		DecrementInterlock(Builder, entry_points[state]);
		Builder.Emit(OpCodes.Ldc_I4_0);
		Builder.Emit(OpCodes.Ret);
		MarkState(state);
	}
	/**
	 * Generate a successful return.
	 */
	public void Return(LoadableValue result) {
		if (result.BackingType == typeof(Frame) || result.BackingType == typeof(object)) {
			var end = Builder.DefineLabel();
			if (result.BackingType == typeof(object)) {
				result.Load(Builder);
				Builder.Emit(OpCodes.Isinst, typeof(Frame));
				result.Load(Builder);
				Builder.Emit(OpCodes.Castclass, typeof(Frame));
			} else {
				result.Load(Builder);
			}
			LoadTaskMaster();
			Builder.Emit(OpCodes.Callvirt, typeof(Frame).GetMethod("Slot"));
			Builder.MarkLabel(end);
		}
		CopyField(result, typeof(Computation).GetField("result"));
		Builder.Emit(OpCodes.Ldc_I4_1);
		Builder.Emit(System.Reflection.Emit.OpCodes.Ret);
	}
	private void ToStringishHelper<T>(bool boxed, LoadableValue source) {
		source.Load(Builder);
		if (boxed) {
			Builder.Emit(OpCodes.Unbox_Any, typeof(T));
		}
		Builder.Emit(OpCodes.Call, typeof(T).GetMethod("ToString", new System.Type[] { typeof(T) }));
	}
	private void ToStringishHelperBool(bool boxed, LoadableValue source) {
		Builder.Emit(OpCodes.Ldsfld, typeof(Stringish).GetField("BOOLEANS"));
		source.Load(Builder);
		if (boxed) {
			Builder.Emit(OpCodes.Unbox_Any, typeof(bool));
		}
		Builder.Emit(OpCodes.Ldelem);
	}
	private void ToStringishHelperStringish(bool boxed, LoadableValue source) {
		source.Load(Builder);
		if (boxed) {
			Builder.Emit(OpCodes.Unbox_Any, typeof(Stringish));
		}
	}
	public void ToStringish(LoadableValue source, LoadableValue source_reference) {
		var boxed = source.BackingType == typeof(object);
		var converters = new Dictionary<System.Type, Action>() {
			{ typeof(bool), () => ToStringishHelper<double>(boxed, source) },
			{ typeof(bool), () => ToStringishHelper<long>(boxed, source) },
			{ typeof(bool), () => ToStringishHelperBool(boxed, source) },
			{ typeof(Stringish), () => ToStringishHelperStringish(boxed, source) }
		};
		if (boxed) {
			var end = Builder.DefineLabel();
			var labels = new Dictionary<System.Type, Label>();
			foreach (var type in converters.Keys) {
				var label = Builder.DefineLabel();
				labels[type] = label;
				source.Load(Builder);
				Builder.Emit(OpCodes.Isinst, type);
				Builder.Emit(OpCodes.Brtrue, label);
			}

			EmitTypeError(source_reference, "Cannot convert type {0} to string.", source);

			foreach (var entry in converters) {
				Builder.MarkLabel(labels[entry.Key]);
				entry.Value();
				Builder.Emit(OpCodes.Br, end);
			}
			Builder.MarkLabel(end);
		} else {
			converters[source.BackingType]();
		}
	}
}
public class LookupCache {
	private LookupCache parent;
	private Dictionary<NameInfo, LoadableValue> defined_values = new Dictionary<NameInfo, LoadableValue>();

	public LookupCache(LookupCache parent) {
		this.parent = parent;
	}

	public LoadableValue this[NameInfo name_info] {
		get {
			if (defined_values.ContainsKey(name_info)) {
				return defined_values[name_info];
			} else if (parent != null) {
				return parent[name_info];
			} else {
				throw new InvalidOperationException("Attempt to lookup cached name “" + name_info.Name + "”, but it was never cached. This is a compiler bug.");
			}
		}
		set { defined_values[name_info] = value; }
	}
}
public abstract class LoadableValue {
	public static LoadableValue NULL_LIST = new NullList();
	public abstract System.Type BackingType { get; }
	public abstract void Load(ILGenerator generator);
	public void Load(Generator generator) {
		Load(generator.Builder);
	}
}
internal class NullList : LoadableValue {
	public override System.Type BackingType { get { return typeof(Context); } }
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldnull);
	}
}
public class FieldValue : LoadableValue {
	public FieldInfo Field { get; private set; }
	public override System.Type BackingType { get { return Field.FieldType; } }
	public FieldValue(FieldInfo field) {
		Field = field;
	}
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldarg_0);
		generator.Emit(OpCodes.Ldfld, Field);
	}
}
public class AutoUnboxValue : LoadableValue {
	private System.Type unbox_type;
	private LoadableValue backing_value;
	public override System.Type BackingType { get { return unbox_type; } }
	public AutoUnboxValue(LoadableValue backing_value, System.Type unbox_type) {
		this.backing_value = backing_value;
		this.unbox_type = unbox_type;
	}
	public override void Load(ILGenerator generator) {
		backing_value.Load(generator);
		generator.Emit(OpCodes.Unbox_Any, unbox_type);
	}
}
public class BoolConstant : LoadableValue {
	private bool number;
	public override System.Type BackingType { get { return typeof(bool); } }
	public BoolConstant(bool number) {
		this.number = number;
	}
	public override void Load(ILGenerator generator) {
		generator.Emit(number ? OpCodes.Ldc_I4_1 : OpCodes.Ldc_I4_0);
	}
}
public class FloatConstant : LoadableValue {
	private double number;
	public readonly static FloatConstant NAN = new FloatConstant(Double.NaN);
	public readonly static FloatConstant INFINITY = new FloatConstant(Double.PositiveInfinity);
	public FloatConstant(double number) {
		this.number = number;
	}
	public override System.Type BackingType { get { return typeof(double); } }
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldc_R8, number);
	}
}
public class IntConstant : LoadableValue {
	private long number;
	public IntConstant(long number) {
		this.number = number;
	}
	public override System.Type BackingType { get { return typeof(long); } }
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldc_I8, number);
	}
}
public class StringishValue : LoadableValue {
	private string str;
	public StringishValue(string str) {
		this.str = str;
	}
	public override System.Type BackingType { get { return typeof(Stringish); } }
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldstr, str);
		generator.Emit(OpCodes.Newobj, typeof(SimpleStringish).GetConstructors()[0]);
	}
}
public class UnitConstant : LoadableValue {
	public readonly static UnitConstant NULL = new UnitConstant();
	private UnitConstant() {}
	public override System.Type BackingType { get { return typeof(Unit); } }
	public override void Load(ILGenerator generator) {
		generator.Emit(OpCodes.Ldsfld, typeof(Unit).GetField("NULL"));
	}
}
}
