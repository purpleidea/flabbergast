package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class BooleanStringish extends LoadableValue {
	private final LoadableValue source;

	public BooleanStringish(LoadableValue source) {
		this.source = source;
	}

	@Override
	public Class<?> getBackingType() {
		return Stringish.class;
	}

	@Override
	public void load(MethodVisitor generator) throws Exception{
		generator.visitFieldInsn(Opcodes.GETSTATIC,
				Type.getInternalName(Stringish.class), "BOOLEANS",
				Type.getDescriptor(Stringish[].class));
		source.load(generator);
		generator.visitInsn(Opcodes.AALOAD);
	}
}
