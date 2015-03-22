package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class TypeCheckValue extends LoadableValue {
	final LoadableValue instance;
	final Class<?> type;

	public TypeCheckValue(Class<?> type, LoadableValue instance) {
		this.type = type;
		this.instance = instance;
	}

	@Override
	public Class<?> getBackingType() {
		return boolean.class;
	}

	@Override
	public void load(MethodVisitor generator) {
		instance.load(generator);
		generator.visitTypeInsn(Opcodes.INSTANCEOF, getInternalName(type));
	}
}
