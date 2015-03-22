package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.lang.reflect.Field;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class FieldValue extends LoadableValue {
	private String class_name;
	private String name;
	private Class<?> type;

	public FieldValue(Field field) {
		this(getInternalName(field.getDeclaringClass()), field.getName(), field
				.getType());
	}

	public FieldValue(String class_name, String name, Class<?> type) {
		this.class_name = class_name;
		this.name = name;
		this.type = type;
	}

	@Override
	public Class<?> getBackingType() {
		return type;
	}

	@Override
	public void load(MethodVisitor generator) {
		generator.visitVarInsn(Opcodes.ALOAD, 0);
		generator.visitFieldInsn(Opcodes.GETFIELD, class_name, name,
				Type.getDescriptor(type));
	}

	public void store(MethodVisitor generator) {
		generator.visitFieldInsn(Opcodes.PUTFIELD, class_name, name,
				Type.getDescriptor(type));
	}
}
