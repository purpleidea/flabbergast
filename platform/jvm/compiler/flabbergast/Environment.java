package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class Environment implements CodeRegion {
	public interface Block {
		void invoke(LoadableValue context, LookupCache cache);
	}

	Map<String, NameInfo> children = new HashMap<String, NameInfo>();
	boolean combinatorial_explosion;
	private int end_column;
	private int end_row;
	private String file_name;
	boolean force_back;
	Map<AstNode, Entry<TypeSet, Boolean>> intrinsics = new HashMap<AstNode, Entry<TypeSet, Boolean>>();
	private Environment parent;

	private int priority;

	private int start_column;

	private int start_row;

	boolean top_level;

	public Environment(String filename, int start_row, int start_column,
			int end_row, int end_column, Environment parent,
			boolean force_back, boolean top_level) {
		if (force_back && parent == null) {
			throw new IllegalArgumentException(
					"Parent environment cannot be null when forcing parent-backed creation.");
		}
		file_name = filename;
		this.start_row = start_row;
		this.start_column = start_column;
		this.end_row = end_row;
		this.end_column = end_column;
		this.force_back = force_back;
		this.parent = parent;
		priority = (parent == null ? 0 : parent.getPriority())
				+ (force_back ? 1 : 2);
		this.top_level = top_level;
	}

	void addForbiddenName(String name) {
		children.put(name, null);
	}

	BoundNameInfo addMask(String name, TypeableElement expression) {
		if (children.containsKey(name)) {
			throw new UnsupportedOperationException("The name " + name
					+ " already exists in the environment.");
		}
		BoundNameInfo nameinfo = new BoundNameInfo(this, name, expression);
		children.put(name, nameinfo);
		return nameinfo;
	}

	public RestrictableType addOverrideName(String name) {
		RestrictableType info = new OverrideNameInfo(this, name);
		children.put(name, info);
		return info;
	}

	TypeSet ensureIntrinsic(ErrorCollector collector, AstNode node,
			TypeSet type, boolean must_unbox, Ptr<Boolean> success) {
		if (intrinsics.containsKey(node)) {
			Entry<TypeSet, Boolean> intrinsic = intrinsics.get(node);
			TypeSet original_type = intrinsic.getKey();
			if (original_type.restrict(type)) {
				success.set(false);
				collector.reportExpressionTypeError(node, original_type, type);
			}
			intrinsic.setValue(intrinsic.getValue() & must_unbox);
			return original_type;
		} else {
			intrinsics.put(node, new AbstractMap.SimpleEntry<TypeSet, Boolean>(
					type, must_unbox));
			return type;
		}
	}

	void generateLookupCache(Generator generator,
			RevCons<Entry<String, LoadableCache>> specials,
			LookupCache current, LoadableValue source_reference,
			LoadableValue context, LoadableValue self_frame, Block block)
			throws NoSuchMethodException, SecurityException {
		generator.debugPosition(this);
		MethodVisitor builder = generator.getBuilder();
		List<LoadableCache> lookup_results = new ArrayList<LoadableCache>();
		if (specials != null) {
			FieldValue child_context = generator.makeField("anon_frame",
					Context.class);
			builder.visitVarInsn(Opcodes.ALOAD, 0);
			builder.visitTypeInsn(Opcodes.NEW, getInternalName(Frame.class));
			builder.visitInsn(Opcodes.DUP);
			generator.loadTaskMaster();
			generator.generateNextId();
			source_reference.load(generator);
			context.load(generator);
			self_frame.load(generator);
			builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
					getInternalName(Frame.class), "<init>",
					org.objectweb.asm.Type.getConstructorDescriptor(Frame.class
							.getConstructors()[0]), false);
			for (Entry<String, LoadableCache> entry : specials.toList()) {
				builder.visitInsn(Opcodes.DUP);
				builder.visitLdcInsn(entry.getKey());
				generator
						.loadReboxed(entry.getValue().getValue(), Object.class);
				builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
						getInternalName(Frame.class), "set",
						Generator.makeSignature(null, String.class,
								Object.class), false);
				lookup_results.add(entry.getValue());
			}
			context.load(generator);
			builder.visitMethodInsn(Opcodes.INVOKESTATIC,
					getInternalName(Context.class), "Prepend", Generator
							.makeSignature(Context.class, Frame.class,
									Context.class), false);
			child_context.store(builder);
			// Promote the context with the specials to proper status
			context = child_context;
		}

		LookupCache base_lookup_cache = new LookupCache(current);
		List<NameInfo> all_children = new ArrayList<NameInfo>();
		String narrow_error = null;
		for (NameInfo info : children.values()) {
			if (info == null) {
				continue;
			}
			info.addAll(all_children);
		}
		for (NameInfo info : all_children) {
			String current_narrow_error = info.checkValidNarrowing(
					base_lookup_cache, current);
			if (narrow_error != null && current_narrow_error != null) {
				narrow_error = String.format("%s\n%s", narrow_error,
						current_narrow_error);
			} else if (narrow_error == null) {
				narrow_error = current_narrow_error;
			}
		}
		if (narrow_error != null) {
			generator.loadTaskMaster();
			source_reference.load(generator);
			builder.visitLdcInsn(narrow_error);
			builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
					getInternalName(TaskMaster.class), "ReportOtherError",
					Generator.makeSignature(null, SourceReference.class,
							String.class), false);
			builder.visitInsn(Opcodes.ICONST_0);
			builder.visitInsn(Opcodes.IRETURN);
			return;
		}
		int load_count = 0;
		for (NameInfo info : all_children) {
			load_count += info.needsLoad(current) ? 1 : 0;
		}
		if (load_count > 0) {
			generator.startInterlock(load_count);
			for (NameInfo info : all_children) {
				if (info.needsLoad(current)) {
					lookup_results.add(info.load(generator, source_reference,
							context));
				}
			}
			int state = generator.defineState();
			generator.setState(state);
			generator.decrementInterlock(builder);
			Label end_label = new Label();
			builder.visitJumpInsn(Opcodes.IFEQ, end_label);
			builder.visitInsn(Opcodes.ICONST_0);
			builder.visitInsn(Opcodes.IRETURN);
			builder.visitLabel(end_label);
			generator.jumpToState(state);
			generator.markState(state);
		}
		for (LoadableCache lookup_result : lookup_results) {
			if (!lookup_result.getDirectCopy())
				continue;
			base_lookup_cache.set(lookup_result.getNameInfo(),
					lookup_result.getValue());
		}
		for (LoadableCache lookup_result : lookup_results) {
			if (!(lookup_result.getSinglyTyped() && !lookup_result
					.getDirectCopy()))
				continue;
			base_lookup_cache.set(lookup_result.getNameInfo(),
					new AutoUnboxValue(lookup_result.getValue(), lookup_result
							.getTypes().get(0)));
			Label label = new Label();
			lookup_result.getValue().load(generator);
			builder.visitTypeInsn(Opcodes.INSTANCEOF,
					getInternalName(lookup_result.getTypes().get(0)));
			builder.visitJumpInsn(Opcodes.IFNE, label);
			generator.emitTypeError(source_reference, String.format(
					"Expected type %s for “%s”, but got %s.", lookup_result
							.getTypes().get(0), lookup_result.getNameInfo()
							.getName(), "%s"), lookup_result.getValue());
			builder.visitLabel(label);
		}
		List<LoadableCache> permutable_caches = new ArrayList<LoadableCache>();
		int old_paths = generator.getPaths();
		int paths = old_paths;
		for (LoadableCache x : lookup_results) {
			if (!x.getSinglyTyped() && !x.getDirectCopy()) {
				permutable_caches.add(x);
				paths *= x.getTypes().size();
			}
		}
		generator.setPaths(paths);
		if (generator.getPaths() > 200 && !combinatorial_explosion) {
			System.err
					.printf("%s:%d:%d-%d:%d: There are %d type-derived flows in the generated code. This will be slow to compile.",
							file_name, start_row, start_column, end_row,
							end_column, generator.getPaths());
			combinatorial_explosion = true;
		}
		generateLookupPermutation(generator, context, base_lookup_cache, 0,
				permutable_caches, source_reference, block);
		generator.setPaths(old_paths);
	}

	private void generateLookupPermutation(Generator generator,
			LoadableValue context, LookupCache cache, int index,
			List<LoadableCache> values, LoadableValue source_reference,
			Block block) throws NoSuchMethodException, SecurityException {
		if (index >= values.size()) {
			block.invoke(context, cache);
			generator.debugPosition(this);
			return;
		}
		Label[] labels = new Label[values.get(index).getTypes().size()];
		for (int it = 0; it < labels.length; it++) {
			labels[it] = new Label();
			values.get(index).getValue().load(generator);
			generator.getBuilder().visitTypeInsn(Opcodes.INSTANCEOF,
					getInternalName(values.get(index).getTypes().get(it)));
			generator.getBuilder().visitJumpInsn(Opcodes.IFNE, labels[it]);
		}
		StringBuilder error_message = new StringBuilder();
		error_message.append("Expected type ");
		for (int type_it = 0; type_it < values.get(index).getTypes().size(); type_it++) {
			if (type_it > 0) {
				error_message.append(" or ");
			}
			error_message.append(values.get(index).getTypes().get(type_it)
					.getSimpleName());
		}
		error_message.append("%s for “");
		error_message.append(values.get(index).getNameInfo().getName());
		error_message.append("”, but got %s.");
		generator.emitTypeError(source_reference, error_message.toString(),
				values.get(index).getValue());
		for (int it = 0; it < labels.length; it++) {
			generator.getBuilder().visitLabel(labels[it]);
			LookupCache sub_cache = new LookupCache(cache);
			sub_cache.set(values.get(index).getNameInfo(), new AutoUnboxValue(
					values.get(index).getValue(), values.get(index).getTypes()
							.get(it)));
			MethodVisitor builder = generator.getBuilder();
			generateLookupPermutation(generator, context, sub_cache, index + 1,
					values, source_reference, block);
			generator.setBuilder(builder);
		}
	}

	@Override
	public int getEndColumn() {
		return end_column;
	}

	@Override
	public int getEndRow() {
		return end_row;
	}

	@Override
	public String getFileName() {
		return file_name;
	}

	List<Class<?>> getIntrinsicRealTypes(AstNode node) {
		if (intrinsics.containsKey(node)) {
			return intrinsics.get(node).getKey().getClasses();
		} else if (parent != null) {
			return parent.getIntrinsicRealTypes(node);
		}
		throw new IllegalArgumentException(
				"There is no intrinsic type for the node requested. This a compiler bug.");
	}

	@Override
	public String getPrettyName() {
		return "region of lookups";
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public int getStartColumn() {
		return start_column;
	}

	@Override
	public int getStartRow() {
		return start_row;
	}

	public boolean getTopLevel() {
		return parent == null ? top_level : parent.getTopLevel();
	}

	public boolean hasName(String name) {
		return children.containsKey(name) || parent != null
				&& parent.hasName(name);
	}

	void intrinsicDispatch(Generator generator, AstNode node,
			LoadableValue original, LoadableValue source_reference,
			Generator.ParameterisedBlock<LoadableValue> block)
			throws NoSuchMethodException, SecurityException {
		Entry<TypeSet, Boolean> intrinsic = intrinsics.get(node);
		if (!intrinsic.getValue()) {
			block.invoke(original);
			return;
		}
		List<Class<?>> types = intrinsic.getKey().getClasses();
		for (Class<?> type : types) {
			Label next_label = new Label();
			original.load(generator);
			MethodVisitor builder = generator.getBuilder();
			builder.visitTypeInsn(Opcodes.INSTANCEOF, getInternalName(type));
			builder.visitJumpInsn(Opcodes.IFNE, next_label);
			block.invoke(new AutoUnboxValue(original, type));
			generator.setBuilder(builder);
			builder.visitLabel(next_label);
		}
		StringBuilder error_message = new StringBuilder();
		error_message.append("Expected type ");
		for (int it = 0; it < types.size(); it++) {
			if (it > 0)
				error_message.append(" or ");
			error_message.append(types.get(it).getSimpleName());
		}
		error_message.append(" for ");
		error_message.append(node.getPrettyName());
		error_message.append(", but got %s.");
		generator.emitTypeError(source_reference, error_message.toString(),
				original);
	}

	private NameInfo lookback(String name) {
		if (children.containsKey(name)) {
			return children.get(name);
		}
		NameInfo copy_info = new CopyFromParentInfo(this, name,
				parent.lookback(name), force_back);
		children.put(name, copy_info);
		return copy_info;
	}

	public NameInfo Lookup(ErrorCollector collector, Iterable<String> names,
			Ptr<Boolean> success) {
		Iterator<String> iter = names.iterator();
		if (!iter.hasNext()) {
			throw new IllegalArgumentException("List of names cannot be empty.");
		}
		String current = iter.next();
		if (children.containsKey(current)) {
			if (children.get(current) == null) {
				success.set(false);
				collector.reportForbiddenNameAccess(this, current);
				return new JunkInfo();
			}
			return children.get(current).lookup(collector, iter, success);
		}
		if (force_back) {
			parent.Lookup(collector, names, success);
		}
		if (parent != null && parent.hasName(current)) {
			return lookback(current).lookup(collector, iter, success);
		}
		NameInfo info = new OpenNameInfo(this, current);
		children.put(current, info);
		return info.lookup(collector, iter, success);
	}
}