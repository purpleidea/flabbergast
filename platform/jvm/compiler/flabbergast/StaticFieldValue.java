package flabbergast;

import java.lang.reflect.Field;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class StaticFieldValue extends LoadableValue {
	private Field field;

	public StaticFieldValue(Field field) {
		this.field = field;
	}

	@Override
	public Class<?> getBackingType() {
		if (field.getType() == String.class) {
			return SimpleStringish.class;
		} else if (field.getType() == int.class
				|| field.getType() == byte.class
				|| field.getType() == short.class) {
			return long.class;
		} else if (field.getType() == float.class) {
			return double.class;
		} else {
			return field.getType();
		}
	}

	@Override
	public void load(MethodVisitor generator) {
		if (field.getType() == String.class) {
			generator.visitTypeInsn(Opcodes.NEW,
					Type.getInternalName(SimpleStringish.class));
			generator.visitInsn(Opcodes.DUP);
		}
		generator.visitFieldInsn(Opcodes.GETSTATIC,
				Type.getInternalName(field.getDeclaringClass()),
				field.getName(), Type.getDescriptor(field.getType()));
		if (field.getType() == String.class) {
			Generator.visitMethod(SimpleStringish.class.getConstructors()[0],
					generator);
		} else if (field.getType() == int.class
				|| field.getType() == byte.class
				|| field.getType() == short.class) {
			generator.visitInsn(Opcodes.I2L);
		} else if (field.getType() == float.class) {
			generator.visitInsn(Opcodes.F2D);
		}
	}
}
