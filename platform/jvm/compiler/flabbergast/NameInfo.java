package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

abstract class NameInfo {
	protected Map<String, NameInfo> children = new HashMap<String, NameInfo>();

	protected String name;

	void addAll(List<NameInfo> target) {
		target.add(this);
		for (NameInfo child : children.values()) {
			child.addAll(target);
		}
	}

	public String checkValidNarrowing(LookupCache next, LookupCache current) {
		return null;
	}

	public void collectUses(ApiGenerator apigen) {
	}

	public abstract void createChild(ErrorCollector collector, String name,
			String root, Ptr<Boolean> success);

	public abstract TypeSet ensureType(ErrorCollector collector, TypeSet type,
			Ptr<Boolean> success, boolean must_unbox);

	protected LoadableValue generateLookupField(Generator generator,
			LoadableValue source_reference, LoadableValue context)
			throws Exception {
		FieldValue lookup_result = generator.makeField("lookup_"
				+ getName().replace('.', '$'), Object.class);
		MethodVisitor builder = generator.getBuilder();
		builder.visitTypeInsn(Opcodes.NEW, getInternalName(Lookup.class));
		builder.visitInsn(Opcodes.DUP);
		generator.loadTaskMaster();
		source_reference.load(builder);
		String[] name_parts = getName().split("\\.");
		builder.visitIntInsn(Opcodes.BIPUSH, name_parts.length);
		builder.visitTypeInsn(Opcodes.ANEWARRAY, getInternalName(String.class));
		for (int it = 0; it < name_parts.length; it++) {
			builder.visitInsn(Opcodes.DUP);
			builder.visitIntInsn(Opcodes.BIPUSH, it);
			builder.visitLdcInsn(name_parts[it]);
			builder.visitInsn(Opcodes.AASTORE);
		}
		context.load(generator);
		builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
				getInternalName(Lookup.class), "<init>", org.objectweb.asm.Type
						.getConstructorDescriptor(Lookup.class
								.getConstructors()[0]));
		generator.generateConsumeResult(lookup_result);
		builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				getInternalName(Lookup.class), "listen",
				Generator.makeSignature(null, ConsumeResult.class));
		return lookup_result;
	}

	public String getName() {
		return name;
	}

	public boolean hasName(String name) {
		return children.containsKey(name);
	}

	public abstract LoadableCache load(Generator generator,
			LoadableValue source_reference, LoadableValue context)
			throws Exception;

	NameInfo lookup(ErrorCollector collector,
			Iterator<? extends CharSequence> names, Ptr<Boolean> success) {
		NameInfo info = this;
		while (names.hasNext()) {
			String current = names.next().toString();
			info.ensureType(collector, Type.Frame.toSet(), success, false);
			if (!info.children.containsKey(current)) {
				info.createChild(collector, current, info.getName(), success);
			}
			info = info.children.get(current);
		}
		return info;
	}

	NameInfo lookup(ErrorCollector collector, String name, Ptr<Boolean> success) {
		ensureType(collector, Type.Frame.toSet(), success, false);
		if (!children.containsKey(name)) {
			createChild(collector, name, getName(), success);
		}
		return children.get(name);
	}

	public boolean needsLoad(LookupCache current) {
		return false;
	}
}
