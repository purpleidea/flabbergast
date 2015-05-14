package flabbergast;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Helper to generate code for a particular function or function.
 */
abstract class Generator {
	/**
	 * Generate code with no input.
	 */
	public interface Block {
		void invoke(Generator generator) throws Exception;
	}

	/**
	 * Generate code for an item during a fold operation, using an initial value
	 * and passing the output to a result block.
	 */
	interface FoldBlock<T, R> {
		void invoke(int index, T item, R left, ParameterisedBlock<R> result)
				throws Exception;
	}

	/**
	 * Generate code given a single input.
	 */
	interface ParameterisedBlock<R> {
		void invoke(R result) throws Exception;
	}

	private static final int MAX_DISPATCHES = 6 * 1024;

	static Class<?> getBoxedType(Class<?> type) {
		if (type == boolean.class)
			return Boolean.class;
		if (type == long.class)
			return Long.class;
		if (type == double.class)
			return Double.class;
		return type;
	}

	public static boolean isNumeric(Class<?> type) {
		return type == double.class || type == long.class;
	}

	static String makeSignature(Class<?> returntype, Class<?>... parametertypes) {
		StringBuilder signature = new StringBuilder();
		signature.append('(');
		for (Class<?> clazz : parametertypes) {
			if (clazz != null)
				signature.append(Type.getDescriptor(clazz));
		}
		signature.append(')');
		if (returntype == null) {
			signature.append('V');
		} else {
			signature.append(Type.getDescriptor(returntype));
		}
		return signature.toString();
	}

	static void visitMethod(Constructor<?> method, MethodVisitor builder) {
		builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
				getInternalName(method.getDeclaringClass()), "<init>",
				Type.getConstructorDescriptor(method));
	}

	static void visitMethod(Method method, MethodVisitor builder) {
		int opcode;
		if (method.getDeclaringClass().isInterface()) {
			opcode = Opcodes.INVOKEINTERFACE;
		} else {
			opcode = Modifier.isStatic(method.getModifiers())
					? Opcodes.INVOKESTATIC
					: Opcodes.INVOKEVIRTUAL;
		}
		builder.visitMethodInsn(opcode,
				getInternalName(method.getDeclaringClass()), method.getName(),
				Type.getMethodDescriptor(method));
	}

	protected MethodVisitor builder;

	protected String class_name;

	/**
	 * The labels in the code where the function may enter into a particular
	 * state. The index is the state number.
	 */
	private final List<MethodVisitor> entry_points = new ArrayList<MethodVisitor>();

	/**
	 * The collection of external URIs needed by this computation and where they
	 * are stored.
	 */
	private final Map<String, FieldValue> externals = new HashMap<String, FieldValue>();

	private CodeRegion last_node;

	/**
	 * The number of fields holding temporary variables that cross sleep
	 * boundaries.
	 */
	private int num_fields;

	/**
	 * The compilation unit that created this function.
	 */
	private CompilationUnit<?> owner;

	private final Set<String> owner_externals;

	private int paths;

	/**
	 * A counter for producing unique result consumers names.
	 */
	private int result_consumer;

	private String root_prefix;

	protected final FieldValue task_master;

	private ClassVisitor type_builder;

	Generator(AstNode node, CompilationUnit<?> owner,
			ClassVisitor type_builder, String class_name, String root_prefix,
			Set<String> owner_externals) throws NoSuchMethodException,
			SecurityException {
		this.owner = owner;
		this.type_builder = type_builder;
		paths = 1;
		this.owner_externals = owner_externals;
		this.class_name = class_name;
		this.root_prefix = root_prefix;

		type_builder.visitField(Opcodes.ACC_PRIVATE, "state",
				getDescriptor(int.class), null, null).visitEnd();
		type_builder.visitField(0, "interlock",
				getDescriptor(AtomicInteger.class), null, null).visitEnd();
		task_master = makeField("task_master", TaskMaster.class);

		// Label for load externals
		defineState();
		// Label for main body
		markState(defineState());
	}

	/**
	 * Create a new source reference based on an existing one, updated to
	 * reflect entry into a new AST node.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void amendSourceReference(AstNode node, String message,
			LoadableValue source_reference, LoadableValue source_template)
			throws Exception {
		if (source_template == null) {
			source_reference.load(builder);
		} else {
			builder.visitTypeInsn(Opcodes.NEW,
					getInternalName(JunctionReference.class));
			builder.visitInsn(Opcodes.DUP);
			builder.visitLdcInsn(message);
			builder.visitLdcInsn(node.getFileName());
			builder.visitLdcInsn(node.getStartRow());
			builder.visitLdcInsn(node.getStartColumn());
			builder.visitLdcInsn(node.getEndRow());
			builder.visitLdcInsn(node.getEndColumn());
			source_reference.load(builder);
			source_template.load(builder);
			visitMethod(Template.class.getMethod("getSourceReference"));
			builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
					getInternalName(JunctionReference.class), "<init>", Type
							.getConstructorDescriptor(JunctionReference.class
									.getConstructors()[0]));
		}
	}

	public LoadableValue compare(LoadableValue left, LoadableValue right,
			LoadableValue source_reference) throws Exception {
		if (left.getBackingType() == Object.class
				|| right.getBackingType() == Object.class) {
			throw new IllegalArgumentException(String.format(
					"Can't compare values of type %s and %s.", left
							.getBackingType().getSimpleName(), right
							.getBackingType().getSimpleName()));
		}
		if (left.getBackingType() != right.getBackingType()) {
			if (isNumeric(left.getBackingType())
					&& isNumeric(right.getBackingType())) {
				return new CompareValue(new UpgradeValue(left),
						new UpgradeValue(right));
			} else {
				emitTypeError(source_reference,
						"Cannot compare value of type %s and type %s.", left,
						right);
				return null;
			}
		}
		return new CompareValue(left, right);
	}

	public void conditionalFlow(
			ParameterisedBlock<ParameterisedBlock<LoadableValue>> conditional_part,
			ParameterisedBlock<ParameterisedBlock<LoadableValue>> true_part,
			ParameterisedBlock<ParameterisedBlock<LoadableValue>> false_part,
			ParameterisedBlock<LoadableValue> result_block) throws Exception {
		final int true_state = defineState();
		final int else_state = defineState();
		conditional_part.invoke(new ParameterisedBlock<LoadableValue>() {

			@Override
			public void invoke(LoadableValue condition) throws Exception {
				if (boolean.class != condition.getBackingType())
					throw new IllegalArgumentException(String.format(
							"Use of non-Boolean type %s in conditional.",
							condition.getBackingType().getSimpleName()));
				condition.load(Generator.this);
				Label else_label = new Label();
				builder.visitJumpInsn(Opcodes.IFEQ, else_label);
				jumpToState(true_state);
				builder.visitLabel(else_label);
				jumpToState(else_state);
			}
		});
		final Map<Class<?>, Entry<Integer, FieldValue>> type_dispatch = new HashMap<Class<?>, Entry<Integer, FieldValue>>();
		ParameterisedBlock<LoadableValue> end_handler = new ParameterisedBlock<LoadableValue>() {

			@Override
			public void invoke(LoadableValue result) throws Exception {
				if (!type_dispatch.containsKey(result.getBackingType())) {
					type_dispatch
							.put(result.getBackingType(),
									new AbstractMap.SimpleImmutableEntry<Integer, FieldValue>(
											defineState(), makeField(
													"if_result",
													result.getBackingType())));
				}
				builder.visitVarInsn(Opcodes.ALOAD, 0);
				result.load(Generator.this);
				type_dispatch.get(result.getBackingType()).getValue()
						.store(builder);
				jumpToState(type_dispatch.get(result.getBackingType()).getKey());
			}
		};

		markState(true_state);
		true_part.invoke(end_handler);
		markState(else_state);
		false_part.invoke(end_handler);

		for (Entry<Integer, FieldValue> entry : type_dispatch.values()) {
			markState(entry.getKey());
			result_block.invoke(entry.getValue());
		}
	}

	/**
	 * Copies the contents of one field to another, boxing or unboxing based on
	 * the field types.
	 */
	public void copyField(LoadableValue source, FieldValue target)
			throws Exception {
		if (source != target) {
			builder.visitVarInsn(Opcodes.ALOAD, 0);
			loadReboxed(source, target.getBackingType());
			target.store(builder);
		}
	}

	void copyField(LoadableValue source, String target, Class<?> type)
			throws Exception {
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		loadReboxed(source, type);
		builder.visitFieldInsn(Opcodes.PUTFIELD, class_name, target,
				getDescriptor(type));
	}

	DelegateValue createFunction(AstNode instance, String syntax_id,
			CompilationUnit.FunctionBlock block) throws Exception {
		return owner.createFunction(instance, syntax_id, block, root_prefix,
				owner_externals);
	}

	DelegateValue createFunctionOverride(AstNode instance, String syntax_id,
			CompilationUnit.FunctionOverrideBlock block) throws Exception {
		return owner.createFunctionOverride(instance, syntax_id, block,
				root_prefix, owner_externals);
	}

	protected void createInterlock(MethodVisitor ctor_builder)
			throws NoSuchMethodException {
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitTypeInsn(Opcodes.NEW,
				getInternalName(AtomicInteger.class));
		ctor_builder.visitInsn(Opcodes.DUP);
		visitMethod(AtomicInteger.class.getConstructor(), ctor_builder);
		ctor_builder.visitFieldInsn(Opcodes.PUTFIELD, class_name, "interlock",
				getDescriptor(AtomicInteger.class));
	}

	/**
	 * Insert debugging information based on an AST node.
	 */
	public void debugPosition(CodeRegion node) {
		last_node = node;
		Label label = new Label();
		builder.visitLabel(label);
		builder.visitLineNumber(node.getStartRow(), label);

	}

	public void decrementInterlock(MethodVisitor builder)
			throws NoSuchMethodException, SecurityException {
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		decrementInterlockRaw(builder);
	}

	public void decrementInterlockRaw(MethodVisitor builder)
			throws NoSuchMethodException, SecurityException {
		builder.visitFieldInsn(Opcodes.GETFIELD, class_name, "interlock",
				getDescriptor(AtomicInteger.class));
		visitMethod(AtomicInteger.class.getMethod("decrementAndGet"), builder);
	}

	/**
	 * Create a new state and put it in the dispatch logic.
	 * 
	 * This state must later be attached to a place in the code using
	 * `markState`.
	 */
	public int defineState() {
		int id = entry_points.size();
		entry_points.add(type_builder.visitMethod(Opcodes.ACC_PRIVATE, "run_"
				+ id, makeSignature(int.class), null, null));
		return id;
	}

	/**
	 * Generate a successful return.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void doReturn(LoadableValue result) throws Exception {
		if (result.getBackingType() == Frame.class
				|| result.getBackingType() == Object.class) {
			Label end = new Label();
			if (result.getBackingType() == Object.class) {
				result.load(builder);
				builder.visitTypeInsn(Opcodes.INSTANCEOF,
						getInternalName(Frame.class));
				builder.visitJumpInsn(Opcodes.IFEQ, end);
				result.load(builder);
				builder.visitTypeInsn(Opcodes.CHECKCAST,
						getInternalName(Frame.class));
			} else {
				result.load(builder);
			}
			visitMethod(Frame.class.getMethod("slot"));
			builder.visitLabel(end);
		}
		copyField(result, "result", Object.class);
		builder.visitInsn(Opcodes.ICONST_0);
		builder.visitInsn(Opcodes.IRETURN);
	}

	/**
	 * Generate a runtime dispatch that checks each of the provided types.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public <T> void dynamicTypeDispatch(LoadableValue original,
			LoadableValue source_reference, List<Class<T>> types,
			ParameterisedBlock<LoadableValue> block) throws Exception {
		// In dynamic_type_dispatch_from_stored_mask, we might not
		// have to unbox, in which case, don't.
		if (types == null) {
			block.invoke(original);
			return;
		}
		StringBuilder error_message = new StringBuilder();
		error_message.append("Unexpected type %s instead of ");
		for (int it = 0; it < types.size(); it++) {
			if (it > 0)
				error_message.append(" or ");
			error_message.append(types.get(it).getSimpleName());
		}
		if (original.getBackingType() != Object.class) {
			for (Class<?> type : types) {
				if (original.getBackingType() == type) {
					block.invoke(original);
					return;
				}
			}
			loadTaskMaster();
			source_reference.load(builder);
			builder.visitLdcInsn(String.format(error_message.toString(),
					original.getBackingType().getSimpleName()));
			visitMethod(TaskMaster.class.getMethod("ReportOtherError",
					SourceReference.class, String.class));
			builder.visitInsn(Opcodes.ICONST_0);
			builder.visitInsn(Opcodes.IRETURN);
			return;
		}
		for (int it = 0; it < types.size(); it++) {
			Label label = new Label();
			original.load(builder);
			builder.visitTypeInsn(Opcodes.INSTANCEOF,
					getInternalName(getBoxedType(types.get(it))));
			builder.visitJumpInsn(Opcodes.IFEQ, label);
			MethodVisitor old_builder = builder;
			block.invoke(new AutoUnboxValue(original, types.get(it)));
			setBuilder(old_builder);
			builder.visitLabel(label);
		}
		emitTypeError(source_reference, error_message.toString(), original);
	}

	public void emitTypeError(LoadableValue source_reference, String message,
			LoadableValue... data) throws Exception {
		if (data.length == 0) {
			throw new IllegalArgumentException(
					"Type errors must have at least one argument.");
		}
		loadTaskMaster();
		source_reference.load(builder);
		builder.visitLdcInsn(message);
		builder.visitIntInsn(Opcodes.BIPUSH, data.length);
		builder.visitTypeInsn(Opcodes.ANEWARRAY, getInternalName(Object.class));
		for (int it = 0; it < data.length; it++) {
			builder.visitInsn(Opcodes.DUP);
			builder.visitIntInsn(Opcodes.BIPUSH, it);
			if (data[it].getBackingType() == Object.class) {
				data[it].load(builder);
				visitMethod(Object.class.getMethod("getClass"));
				visitMethod(Stringish.class.getMethod("hideImplementation",
						Class.class));

			} else {
				builder.visitLdcInsn(Stringish.hideImplementation(
						data[it].getBackingType()).getSimpleName());
			}
			builder.visitInsn(Opcodes.AASTORE);
		}
		visitMethod(String.class.getMethod("format", String.class,
				Object[].class));
		visitMethod(TaskMaster.class.getMethod("reportOtherError",
				SourceReference.class, String.class));
		builder.visitInsn(Opcodes.ICONST_0);
		builder.visitInsn(Opcodes.IRETURN);
	}

	/**
	 * Generate code for a list using a fold (i.e., each computation in the list
	 * is made from the previous computation).
	 */

	public <T, R> void fold(R initial, List<? extends T> list,
			FoldBlock<T, R> expand, ParameterisedBlock<R> result)
			throws Exception {
		foldHelper(list, expand, result, initial, 0);
	}

	private <T, R> void foldHelper(final List<? extends T> list,
			final FoldBlock<T, R> expand, final ParameterisedBlock<R> result,
			R curr_result, final int it) throws Exception {
		if (it < list.size()) {
			expand.invoke(it, list.get(it), curr_result,
					new ParameterisedBlock<R>() {

						@Override
						public void invoke(R next_result) throws Exception {
							foldHelper(list, expand, result, next_result,
									it + 1);
						}
					});
		} else {
			result.invoke(curr_result);
		}
	}

	/**
	 * Generate a function to receive a value and request continued computation
	 * from the task master.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void generateConsumeResult(FieldValue result_target)
			throws NoSuchMethodException, SecurityException {
		String getter_class_name = class_name + "$ConsumeResult"
				+ result_consumer++;
		ClassVisitor getter_class = owner.defineClass(0, getter_class_name,
				Object.class, ConsumeResult.class);
		getter_class.visitField(Opcodes.ACC_PRIVATE, "task_master",
				getDescriptor(TaskMaster.class), null, null).visitEnd();
		getter_class.visitField(Opcodes.ACC_PRIVATE, "instance",
				"L" + class_name + ";", null, null).visitEnd();

		String ctor_signature = String.format("(L%s;%s)V", class_name,
				getDescriptor(TaskMaster.class));
		MethodVisitor ctor_builder = getter_class.visitMethod(0, "<init>",
				ctor_signature, null, null);
		ctor_builder.visitCode();
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
				"<init>", "()V");
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 1);
		ctor_builder.visitFieldInsn(Opcodes.PUTFIELD, getter_class_name,
				"instance", "L" + class_name + ";");
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 2);
		ctor_builder.visitFieldInsn(Opcodes.PUTFIELD, getter_class_name,
				"task_master", getDescriptor(TaskMaster.class));
		ctor_builder.visitInsn(Opcodes.RETURN);
		ctor_builder.visitMaxs(0, 0);
		ctor_builder.visitEnd();

		MethodVisitor consume_builder = getter_class.visitMethod(
				Opcodes.ACC_PUBLIC, "consume",
				makeSignature(void.class, Object.class), null, null);
		consume_builder.visitCode();
		consume_builder.visitVarInsn(Opcodes.ALOAD, 0);
		consume_builder.visitFieldInsn(Opcodes.GETFIELD, getter_class_name,
				"instance", "L" + class_name + ";");
		consume_builder.visitInsn(Opcodes.DUP);
		consume_builder.visitVarInsn(Opcodes.ALOAD, 1);
		result_target.store(consume_builder);
		Label return_label = new Label();

		decrementInterlockRaw(consume_builder);
		consume_builder.visitJumpInsn(Opcodes.IFNE, return_label);

		consume_builder.visitVarInsn(Opcodes.ALOAD, 0);
		consume_builder.visitFieldInsn(Opcodes.GETFIELD, getter_class_name,
				"task_master", getDescriptor(TaskMaster.class));
		consume_builder.visitVarInsn(Opcodes.ALOAD, 0);
		consume_builder.visitFieldInsn(Opcodes.GETFIELD, getter_class_name,
				"instance", "L" + class_name + ";");
		consume_builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				getInternalName(TaskMaster.class), "slot",
				makeSignature(null, Computation.class));
		consume_builder.visitLabel(return_label);
		consume_builder.visitInsn(Opcodes.RETURN);
		consume_builder.visitMaxs(0, 0);
		consume_builder.visitEnd();

		getter_class.visitEnd();

		builder.visitTypeInsn(Opcodes.NEW, getter_class_name);
		builder.visitInsn(Opcodes.DUP);
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		loadTaskMaster();
		builder.visitMethodInsn(Opcodes.INVOKESPECIAL, getter_class_name,
				"<init>", ctor_signature);
	}

	public void generateNextId() throws NoSuchMethodException,
			SecurityException {
		loadTaskMaster();
		visitMethod(TaskMaster.class.getMethod("nextId"));
	}

	/**
	 * Finish this function by creating the state dispatch instruction using a
	 * switch (computed goto). Also generate the block that loads all the
	 * external values.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	void generateSwitchBlock(boolean load_owner_externals)
			throws NoSuchMethodException, SecurityException {
		markState(0);
		// If this is a top level function, load all the external values for our
		// children.
		if (load_owner_externals) {
			for (String uri : owner_externals) {
				if (!externals.containsKey(uri)) {
					externals.put(uri, makeField(uri, Object.class));
				}
			}
		}
		if (externals.size() > 0) {
			startInterlock(externals.size());
			for (Entry<String, FieldValue> entry : externals.entrySet()) {
				loadTaskMaster();
				builder.visitLdcInsn(entry.getKey());
				generateConsumeResult(entry.getValue());
				visitMethod(TaskMaster.class.getMethod("getExternal",
						String.class, ConsumeResult.class));
			}
			stopInterlock(1);
		} else {
			jumpToState(1);
		}
		for (MethodVisitor entry_point : entry_points) {
			entry_point.visitMaxs(0, 0);
			entry_point.visitEnd();
		}
		int num_dispatch_routines = entry_points.size() / MAX_DISPATCHES;
		for (int dispatch = 0; dispatch <= num_dispatch_routines; dispatch++) {
			Class<?> io_type = (dispatch == 0) ? null : int.class;
			MethodVisitor run_builder = type_builder.visitMethod(
					Opcodes.ACC_PROTECTED, (dispatch == 0)
							? "run"
							: ("run_dispatch_" + dispatch),
					makeSignature(io_type, io_type), null, null);
			run_builder.visitCode();

			Label[] call_labels = new Label[dispatch == num_dispatch_routines
					? (entry_points.size() % MAX_DISPATCHES)
					: MAX_DISPATCHES];
			for (int it = 0; it < call_labels.length; it++) {
				call_labels[it] = new Label();
			}

			Label error_label = new Label();
			Label end_label = dispatch == 0 ? new Label() : null;
			Label continue_label = dispatch == 0 ? new Label() : null;
			if (dispatch == 0) {
				run_builder.visitVarInsn(Opcodes.ALOAD, 0);
				run_builder.visitFieldInsn(Opcodes.GETFIELD, class_name,
						"state", getDescriptor(int.class));
				run_builder.visitLabel(continue_label);
				run_builder.visitInsn(Opcodes.DUP);
				run_builder.visitVarInsn(Opcodes.ISTORE, 1);
			} else {
				run_builder.visitVarInsn(Opcodes.ILOAD, 1);
			}
			run_builder.visitVarInsn(Opcodes.ALOAD, 0);
			run_builder.visitInsn(Opcodes.SWAP);
			run_builder.visitTableSwitchInsn(dispatch * MAX_DISPATCHES,
					dispatch * MAX_DISPATCHES + call_labels.length - 1,
					error_label, call_labels);
			run_builder.visitLabel(error_label);
			if (dispatch == num_dispatch_routines) {
				run_builder.visitInsn(Opcodes.POP);
				run_builder.visitTypeInsn(Opcodes.NEW,
						getInternalName(IllegalStateException.class));
				run_builder.visitInsn(Opcodes.DUP);
				run_builder.visitVarInsn(Opcodes.ILOAD, 1);
				visitMethod(Integer.class.getMethod("toString", int.class),
						run_builder);
				run_builder.visitLdcInsn(" of " + entry_points.size());
				visitMethod(String.class.getMethod("concat", String.class),
						run_builder);
				run_builder
						.visitMethodInsn(
								Opcodes.INVOKESPECIAL,
								getInternalName(IllegalStateException.class),
								"<init>",
								Type.getConstructorDescriptor(IllegalStateException.class
										.getConstructor(String.class)));
				run_builder.visitInsn(Opcodes.ATHROW);
			} else {
				run_builder.visitVarInsn(Opcodes.ILOAD, 1);
				run_builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, class_name,
						"run_dispatch_" + (dispatch + 1), "(I)I");
				if (end_label == null) {
					run_builder.visitInsn(Opcodes.IRETURN);
				} else {
					run_builder.visitJumpInsn(Opcodes.GOTO, end_label);
				}
			}

			for (int it = 0; it < call_labels.length; it++) {
				run_builder.visitLabel(call_labels[it]);
				run_builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, class_name,
						"run_" + (it + dispatch * MAX_DISPATCHES),
						makeSignature(int.class));
				if (end_label == null) {
					run_builder.visitInsn(Opcodes.IRETURN);
				} else {
					run_builder.visitJumpInsn(Opcodes.GOTO, end_label);
				}
			}
			if (end_label != null) {
				run_builder.visitLabel(end_label);
				run_builder.visitLocalVariable("temp_state", "I", null,
						continue_label, end_label, 1);
				run_builder.visitInsn(Opcodes.DUP);
				run_builder.visitJumpInsn(Opcodes.IFNE, continue_label);
				run_builder.visitInsn(Opcodes.POP);
				run_builder.visitInsn(Opcodes.RETURN);
			}
			run_builder.visitMaxs(0, 0);
			run_builder.visitEnd();
		}
		type_builder.visitEnd();
	}

	/**
	 * The body of the current `Run` method for this function.
	 */
	public MethodVisitor getBuilder() {
		return builder;
	}

	/**
	 * The current number of environment-induced bifurcation points;
	 */
	public int getPaths() {
		return paths;
	}

	public LoadableValue invokeNative(LoadableValue source_reference,
			List<Method> methods, List<LoadableValue> arguments)
			throws Exception {
		Method best_method = null;
		int best_penalty = Integer.MAX_VALUE;
		for (Method method : methods) {
			Ptr<Integer> penalty = new Ptr<Integer>(0);
			Class<?>[] parameters = method.getParameterTypes();
			boolean is_static = Modifier.isStatic(method.getModifiers());
			if (!is_static
					&& !invokeParameterPenalty(method.getDeclaringClass(),
							arguments.get(0).getBackingType(), penalty)) {
				break;
			}
			boolean possible = true;
			for (int it = 0; it < parameters.length && possible; it++) {
				possible = invokeParameterPenalty(parameters[it], arguments
						.get(it + (is_static ? 0 : 1)).getBackingType(),
						penalty);
			}
			if (possible && penalty.get() < best_penalty) {
				best_method = method;
			}
		}
		if (best_method == null) {
			loadTaskMaster();
			source_reference.load(builder);
			StringBuilder arg_types = new StringBuilder();
			for (int it = 0; it < arguments.size(); it++) {
				if (it > 0)
					arg_types.append(", ");
				arg_types.append(arguments.get(it).getBackingType()
						.getCanonicalName());
			}
			builder.visitLdcInsn(String.format(
					"Cannot find overloaded matching method for %s.%s(%s).",
					methods.get(0).getName(), methods.get(0)
							.getDeclaringClass().getCanonicalName(),
					arg_types.toString()));
			visitMethod(TaskMaster.class.getMethod("ReportOtherError",
					SourceReference.class, String.class));
			builder.visitInsn(Opcodes.ICONST_0);
			builder.visitInsn(Opcodes.IRETURN);
			return null;
		}
		Class<?>[] method_parameters = best_method.getParameterTypes();
		boolean is_static = Modifier.isStatic(best_method.getModifiers());
		Class<?>[] method_arguments = new Class<?>[method_parameters.length
				+ (is_static ? 0 : 1)];
		if (!is_static) {
			method_arguments[0] = best_method.getDeclaringClass();
		}
		for (int it = 0; it < method_parameters.length; it++) {
			method_arguments[it + (is_static ? 0 : 1)] = method_parameters[it];
		}

		FieldValue result = makeField(best_method.getName(), flabbergast.Type
				.fromNative(best_method.getReturnType()).getRealClass());
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		for (int it = 0; it < arguments.size(); it++) {
			arguments.get(it).load(this);
			if (arguments.get(it).getBackingType() != method_arguments[it]) {
				if (method_arguments[it] == byte.class
						|| method_arguments[it] == short.class
						|| method_arguments[it] == int.class) {
					builder.visitInsn(Opcodes.L2I);
				} else if (method_arguments[it] == Byte.class
						|| method_arguments[it] == Short.class
						|| method_arguments[it] == Integer.class
						|| method_arguments[it] == Long.class) {
					Class<?> intermediate;
					if (method_arguments[it] == Long.class) {
						intermediate = long.class;
					} else {
						builder.visitInsn(Opcodes.L2I);
						if (method_arguments[it] == Byte.class) {
							intermediate = byte.class;
						} else if (method_arguments[it] == Short.class) {
							intermediate = short.class;
						} else {
							intermediate = int.class;
						}
					}
					builder.visitMethodInsn(Opcodes.INVOKESTATIC,
							getInternalName(method_arguments[it]), "valueOf",
							makeSignature(method_arguments[it], intermediate));
				} else if (method_arguments[it] == float.class) {
					builder.visitInsn(Opcodes.D2F);
				} else if (method_arguments[it] == String.class) {
					visitMethod(Stringish.class.getMethod("toString"));
				} else {
					throw new IllegalArgumentException(
							String.format(
									"No conversation from %s to %s while invoking %s.%s.",
									arguments.get(it).getBackingType()
											.getCanonicalName(),
									method_arguments[it].getName(), best_method
											.getDeclaringClass()
											.getCanonicalName(), best_method
											.getName()));
				}
			}
		}
		visitMethod(best_method);
		if (result.getBackingType() != best_method.getReturnType()) {
			if (result.getBackingType() == long.class) {
				builder.visitInsn(Opcodes.I2L);
			} else if (result.getBackingType() == double.class) {
				builder.visitInsn(Opcodes.F2D);
			} else if (result.getBackingType() == Stringish.class) {
				builder.visitTypeInsn(Opcodes.NEW,
						getInternalName(SimpleStringish.class));
				builder.visitInsn(Opcodes.DUP_X1);
				builder.visitInsn(Opcodes.SWAP);
				builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
						getInternalName(SimpleStringish.class), "<init>", Type
								.getConstructorDescriptor(SimpleStringish.class
										.getConstructors()[0]));
			} else {
				throw new IllegalArgumentException(String.format(
						"No conversation from %s to %s while invoking %s.%s.",
						best_method.getReturnType().getCanonicalName(), result
								.getBackingType().getCanonicalName(),
						best_method.getDeclaringClass().getCanonicalName(),
						best_method.getName()));
			}
		}
		result.store(builder);
		return result;
	}

	private boolean invokeParameterPenalty(Class<?> method, Class<?> given,
			Ptr<Integer> penalty) {
		if (method == given) {
			return true;
		}
		if (method == String.class && given == Stringish.class) {
			return true;
		}
		if (given == long.class) {
			if (method == byte.class || method == Byte.class) {
				penalty.set(penalty.get() + Long.SIZE - Byte.SIZE);
				return true;
			} else if (method == short.class || method == Short.class) {
				penalty.set(penalty.get() + Long.SIZE - Short.SIZE);
				return true;
			} else if (method == int.class || method == Integer.class) {
				penalty.set(penalty.get() + Long.SIZE - Integer.SIZE);
				return true;
			} else if (method == Integer.class) {
				return true;
			}
		} else if (given == double.class) {
			if (method == float.class || method == Float.class) {
				penalty.set(penalty.get() + Double.SIZE - Float.SIZE);
				return true;
			} else if (method == Double.class) {
				return true;
			}
		}
		return false;
	}

	public void jumpToState(int state) {
		builder.visitIntInsn(Opcodes.SIPUSH, state);
		builder.visitInsn(Opcodes.IRETURN);
	}

	/**
	 * Loads a value and repackages to match the target type, as needed.
	 * 
	 * This can be boxing, unboxing, or casting.
	 */
	public void loadReboxed(LoadableValue source, Class<?> target_type)
			throws Exception {
		source.load(builder);
		if (source.getBackingType() != target_type) {
			if (target_type == Object.class) {
				if (source.getBackingType() == boolean.class
						|| source.getBackingType() == double.class
						|| source.getBackingType() == long.class) {
					builder.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							getInternalName(getBoxedType(source
									.getBackingType())),
							"valueOf",
							makeSignature(
									getBoxedType(source.getBackingType()),
									source.getBackingType()));
				}
			} else {
				if (source.getBackingType() == boolean.class
						|| source.getBackingType() == double.class
						|| source.getBackingType() == long.class) {
					builder.visitTypeInsn(Opcodes.CHECKCAST,
							getInternalName(getBoxedType(target_type)));
					builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
							getInternalName(getBoxedType(source
									.getBackingType())),
							target_type.getSimpleName() + "Value",
							makeSignature(target_type));
				} else {
					builder.visitTypeInsn(Opcodes.CHECKCAST,
							getInternalName(target_type));
				}
			}
		}
	}

	/**
	 * Load the task master in the `Run` function.
	 */
	public void loadTaskMaster() {
		loadTaskMaster(builder);
	}

	/**
	 * Load the task master in any method of this class.
	 */
	public void loadTaskMaster(MethodVisitor builder) {
		task_master.load(builder);
	}

	/**
	 * Create an anonymous field with the specified type.
	 */
	public FieldValue makeField(String name, Class<?> type) {
		String n = name + "$" + (num_fields++);
		type_builder.visitField(Opcodes.ACC_PUBLIC, n, getDescriptor(type),
				null, null).visitEnd();
		return new FieldValue(class_name, n, type);
	}

	/**
	 * Mark the current code position as the entry point for a state.
	 */
	public void markState(int id) {
		builder = entry_points.get(id);
		builder.visitCode();
		if (last_node != null) {
			debugPosition(last_node);
		}
	}

	public LoadableValue pushIteratorSourceReference(AstNode node,
			LoadableValue iterator, LoadableValue original_reference)
			throws Exception {
		pushSourceReferenceStart();
		builder.visitLdcInsn("fricassée iteration %s: %s");
		builder.visitInsn(Opcodes.ICONST_2);
		builder.visitTypeInsn(Opcodes.ANEWARRAY, getInternalName(Object.class));
		builder.visitInsn(Opcodes.DUP);
		builder.visitInsn(Opcodes.ICONST_0);
		iterator.load(builder);
		visitMethod(iterator.getBackingType().getMethod("getPosition"));
		visitMethod(Long.class.getMethod("toString", long.class));
		builder.visitInsn(Opcodes.AASTORE);
		builder.visitInsn(Opcodes.DUP);
		builder.visitInsn(Opcodes.ICONST_1);
		iterator.load(builder);
		visitMethod(iterator.getBackingType().getMethod("getCurrent"));
		builder.visitInsn(Opcodes.AASTORE);
		visitMethod(String.class.getMethod("format", String.class,
				Object[].class));
		return pushSourceReferenceEnd(node, original_reference);
	}

	public LoadableValue pushSourceReference(AstNode node,
			LoadableValue original_reference) throws Exception {
		pushSourceReferenceStart();
		if (node instanceof attribute) {
			builder.visitLdcInsn(String.format("%s: %s", node.getPrettyName(),
					((attribute) node).name));
		} else if (node instanceof named_definition) {
			builder.visitLdcInsn(String.format("%s: %s", node.getPrettyName(),
					((named_definition) node).name));
		} else {
			builder.visitLdcInsn(node.getPrettyName());
		}
		return pushSourceReferenceEnd(node, original_reference);
	}

	private LoadableValue pushSourceReferenceEnd(AstNode node,
			LoadableValue original_reference) throws Exception {
		FieldValue reference = makeField("source_reference",
				SourceReference.class);
		builder.visitLdcInsn(node.getFileName());
		builder.visitLdcInsn(node.getStartRow());
		builder.visitLdcInsn(node.getStartColumn());
		builder.visitLdcInsn(node.getEndRow());
		builder.visitLdcInsn(node.getEndColumn());
		if (original_reference == null) {
			builder.visitInsn(Opcodes.ACONST_NULL);
		} else {
			original_reference.load(this);
		}
		builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
				getInternalName(SourceReference.class), "<init>", Type
						.getConstructorDescriptor(SourceReference.class
								.getConstructors()[0]));
		reference.store(builder);
		return reference;
	}

	private void pushSourceReferenceStart() {
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		builder.visitTypeInsn(Opcodes.NEW,
				getInternalName(SourceReference.class));
		builder.visitInsn(Opcodes.DUP);
	}

	public LoadableValue resolveUri(String uri) {
		owner_externals.add(uri);
		if (!externals.containsKey(uri)) {
			FieldValue library_field = makeField(uri, Object.class);
			externals.put(uri, library_field);
		}
		return externals.get(uri);
	}

	public void setBuilder(MethodVisitor value) {
		builder = value;
	}

	public void setPaths(int value) {
		paths = value;
	}

	/**
	 * Change the state that will be entered upon re-entry.
	 */
	public void setState(int state) {
		setState(state, builder);
	}

	void setState(int state, MethodVisitor builder) {
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		builder.visitIntInsn(Opcodes.SIPUSH, state);
		builder.visitFieldInsn(Opcodes.PUTFIELD, class_name, "state",
				getDescriptor(int.class));
	}

	/**
	 * Slot a computation for execution by the task master.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void slot(LoadableValue target) throws Exception {
		loadTaskMaster();
		target.load(builder);
		visitMethod(TaskMaster.class.getMethod("slot", Computation.class));
	}

	/**
	 * Slot a computation for execution and stop execution.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void slotSleep(LoadableValue target) throws Exception {
		slot(target);
		builder.visitInsn(Opcodes.ICONST_0);
		builder.visitInsn(Opcodes.IRETURN);
	}

	public void startInterlock(int count) throws NoSuchMethodException,
			SecurityException {
		builder.visitVarInsn(Opcodes.ALOAD, 0);
		builder.visitFieldInsn(Opcodes.GETFIELD, class_name, "interlock",
				getDescriptor(AtomicInteger.class));
		builder.visitIntInsn(Opcodes.SIPUSH, count + 1);
		visitMethod(AtomicInteger.class.getMethod("set", int.class));

	}

	public void stopInterlock() throws NoSuchMethodException, SecurityException {
		int state = defineState();
		stopInterlock(state);
		markState(state);
	}

	public void stopInterlock(int state) throws NoSuchMethodException,
			SecurityException {
		setState(state);
		decrementInterlock(builder);
		Label label = new Label();
		builder.visitJumpInsn(Opcodes.IFEQ, label);
		builder.visitInsn(Opcodes.ICONST_0);
		builder.visitInsn(Opcodes.IRETURN);
		builder.visitLabel(label);
		jumpToState(state);
	}

	public LoadableValue toStringish(LoadableValue source,
			LoadableValue source_reference) throws Exception {
		LoadableValue result;
		if (source.getBackingType() == Object.class) {
			FieldValue field = makeField("str", Stringish.class);
			Label end = new Label();
			builder.visitVarInsn(Opcodes.ALOAD, 0);
			source.load(builder);
			visitMethod(Stringish.class.getMethod("fromObject", Object.class));
			field.store(builder);
			field.load(builder);
			builder.visitJumpInsn(Opcodes.IFNONNULL, end);
			emitTypeError(source_reference,
					"Cannot convert type %s to String.", source);
			builder.visitLabel(end);
			result = field;
		} else {
			result = toStringishHelper(source);
		}
		return result;
	}

	private LoadableValue toStringishHelper(LoadableValue source) {
		if (source.getBackingType() == boolean.class) {
			return new BooleanStringish(source);
		} else if (source.getBackingType() == long.class
				|| source.getBackingType() == double.class) {
			return new NumericStringish(source);
		} else if (source.getBackingType() == Stringish.class) {
			return source;
		} else {
			throw new IllegalArgumentException(String.format(
					"Cannot convert %s to Stringish.", source.getBackingType()));
		}
	}

	/**
	 * The underlying class builder for this function.
	 */
	public ClassVisitor TypeBuilder() {
		return type_builder;
	}

	void visitMethod(Constructor<?> method) {
		visitMethod(method, builder);
	}

	void visitMethod(Method method) {
		visitMethod(method, builder);
	}
}
