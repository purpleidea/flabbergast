package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class NullValue extends LoadableValue {
	private final Class<?> backing;

	public NullValue(Class<?> backing) {
		this.backing = backing;
	}

	@Override
	public Class<?> getBackingType() {
		return backing;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitInsn(Opcodes.ACONST_NULL);
	}
}
