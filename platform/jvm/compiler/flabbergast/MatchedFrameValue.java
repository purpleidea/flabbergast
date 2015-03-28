package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MatchedFrameValue extends LoadableValue {

	final LoadableValue array;
	final int index;

	public MatchedFrameValue(int index, LoadableValue array) {
		this.index = index;
		this.array = array;
	}

	@Override
	public Class<?> getBackingType() {
		return Frame.class;
	}

	@Override
	public void load(MethodVisitor generator) throws Exception {
		array.load(generator);
		generator.visitIntInsn(Opcodes.SIPUSH, index);
		generator.visitInsn(Opcodes.AALOAD);
		generator
				.visitTypeInsn(Opcodes.CHECKCAST, getInternalName(Frame.class));
	}
}