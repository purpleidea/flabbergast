package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class AutoUnboxValue extends LoadableValue {
	private final LoadableValue backing_value;
	private final Class<?> unbox_type;

	public AutoUnboxValue(LoadableValue backing_value, Class<?> unbox_type) {
		this.backing_value = backing_value;
		this.unbox_type = unbox_type;
	}

	@Override
	public Class<?> getBackingType() {
		return unbox_type;
	}

	@Override
	public void load(MethodVisitor generator) throws Exception {
		backing_value.load(generator);
		generator.visitTypeInsn(Opcodes.CHECKCAST,
				getInternalName(Generator.getBoxedType(unbox_type)));
		if (unbox_type.isPrimitive()) {
			generator.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
					getInternalName(Generator.getBoxedType(unbox_type)),
					unbox_type.getName() + "Value",
					Generator.makeSignature(unbox_type), false);
		}
	}
}
