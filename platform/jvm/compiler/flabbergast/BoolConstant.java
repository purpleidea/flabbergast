package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class BoolConstant extends LoadableValue {
	private final boolean number;

	public BoolConstant(boolean number) {
		this.number = number;
	}

	@Override
	public Class<?> getBackingType() {
		return boolean.class;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitInsn(number ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
	}
}
