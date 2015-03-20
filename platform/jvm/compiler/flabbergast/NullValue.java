package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


class NullValue extends LoadableValue {
	public Class<?> getBackingType(){ return backing; }
	private final Class<?> backing;
	public NullValue(Class<?> backing) {
		this.backing = backing;
	}
	public void load(MethodVisitor generator) {
		generator.visitInsn(Opcodes.ACONST_NULL);
	}
}
