package flabbergast;

import org.objectweb.asm.*;
class UpgradeValue extends LoadableValue {
	private final LoadableValue original;
	public UpgradeValue(LoadableValue original) {
		this.original = original;
	}
	public Class<?> getBackingType() { return double.class; }
	public void load(MethodVisitor generator) {
		original.load(generator);
		if (original.getBackingType() == long.class) {
			generator.visitInsn(Opcodes.L2D);
		}
	}
}
