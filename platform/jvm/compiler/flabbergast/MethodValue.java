package flabbergast;

import java.lang.reflect.Method;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class MethodValue extends LoadableValue {
	private final LoadableValue instance;
	private final Method method;

	public MethodValue(LoadableValue instance, Method method) {
		this.method = method;
		this.instance = instance;
	}

	@Override
	public Class<?> getBackingType() {
		return method.getReturnType();
	}

	@Override
	public void load(MethodVisitor generator) throws Exception {
		if (instance == null) {
			generator.visitInsn(Opcodes.ACONST_NULL);
		} else {
			instance.load(generator);
		}
		Generator.visitMethod(method, generator);
	}
}
