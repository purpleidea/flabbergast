package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class StringishValue extends LoadableValue {
	private final String str;

	public StringishValue(String str) {
		this.str = str;
	}

	@Override
	public Class<?> getBackingType() {
		return Stringish.class;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitTypeInsn(Opcodes.NEW,
				getInternalName(SimpleStringish.class));
		generator.visitInsn(Opcodes.DUP);
		generator.visitLdcInsn(str);
		generator.visitMethodInsn(Opcodes.INVOKESPECIAL,
				getInternalName(SimpleStringish.class), "<init>",
				Generator.makeSignature(null, String.class));
	}
}
