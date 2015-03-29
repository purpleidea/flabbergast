package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class CompareValue extends LoadableValue {
	private final LoadableValue left;
	private final LoadableValue right;

	public CompareValue(LoadableValue left, LoadableValue right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public Class<?> getBackingType() {
		return long.class;
	}

	@Override
	public void load(MethodVisitor generator) throws Exception {
		if (left.getBackingType() == boolean.class) {
			left.load(generator);
			right.load(generator);
			generator.visitInsn(Opcodes.ISUB);
		} else {
			left.load(generator);
			right.load(generator);
			generator
					.visitMethodInsn(
							Generator.isNumeric(left.getBackingType()) ? Opcodes.INVOKESTATIC
									: Opcodes.INVOKEVIRTUAL,
							getInternalName(Generator.getBoxedType(left
									.getBackingType())),
							Generator.isNumeric(left.getBackingType()) ? "compare"
									: "compareTo",
							Generator.makeSignature(
									int.class,
									Generator.isNumeric(left.getBackingType()) ? left
											.getBackingType() : null, right
											.getBackingType()), false);
			generator.visitInsn(Opcodes.ICONST_1);
			generator.visitMethodInsn(Opcodes.INVOKESTATIC,
					getInternalName(Math.class), "min",
					Generator.makeSignature(int.class, int.class, int.class),
					false);
			generator.visitInsn(Opcodes.ICONST_M1);
			generator.visitMethodInsn(Opcodes.INVOKESTATIC,
					getInternalName(Math.class), "max",
					Generator.makeSignature(int.class, int.class, int.class),
					false);
		}
		generator.visitInsn(Opcodes.I2L);
	}
}
