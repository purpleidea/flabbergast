package flabbergast;

import org.objectweb.asm.MethodVisitor;

class FloatConstant extends LoadableValue {
	public final static FloatConstant INFINITY = new FloatConstant(
			Double.POSITIVE_INFINITY);
	public final static FloatConstant NAN = new FloatConstant(Double.NaN);
	private final double number;

	public FloatConstant(double number) {
		this.number = number;
	}

	@Override
	public Class<?> getBackingType() {
		return double.class;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitLdcInsn(number);
	}
}