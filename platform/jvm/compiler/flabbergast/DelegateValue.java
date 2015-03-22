package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class DelegateValue extends LoadableValue {
	private final Class<?> backing_type;
	private final String clazz;

	public DelegateValue(String clazz, Class<?> backing_type) {
		this.clazz = clazz;
		this.backing_type = backing_type;
	}

	@Override
	public Class<?> getBackingType() {
		return backing_type;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitTypeInsn(Opcodes.NEW, clazz);
		generator.visitInsn(Opcodes.DUP);
		generator.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>",
				Generator.makeSignature(null), false);
	}
}
