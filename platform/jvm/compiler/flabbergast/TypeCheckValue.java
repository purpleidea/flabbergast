package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Type.getInternalName;

class TypeCheckValue extends LoadableValue {
    final Class<?> type;
    final LoadableValue instance;
	public TypeCheckValue(Class<?> type, LoadableValue instance) {
		this.type = type;
		this.instance = instance;
	}
	public Class<?> getBackingType() { return boolean.class; }
	public void load(MethodVisitor generator) {
		instance.load(generator);
		generator.visitTypeInsn(Opcodes.INSTANCEOF, getInternalName(type));
	}
}
