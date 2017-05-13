package flabbergast.compiler.kws.codegen;

import static org.objectweb.asm.Type.VOID_TYPE;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

interface Invokable {
  @SuppressWarnings("SameParameterValue")
  static Invokable constructMethod(Type owner, Type... parameters) {
    final var method = new Method("<init>", VOID_TYPE, parameters);
    return generator -> generator.invokeConstructor(owner, method);
  }

  @SuppressWarnings("unused")
  static Invokable interfaceMethod(Type owner, Type returnType, String name, Type... parameters) {
    final var method = new Method(name, returnType, parameters);
    return generator -> generator.invokeInterface(owner, method);
  }

  static Invokable staticInterfaceMethod(
      Type owner, Type returnType, String name, Type... parameters) {
    final var method = new Method(name, returnType, parameters);
    return generator ->
        generator.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            owner.getInternalName(),
            method.getName(),
            method.getDescriptor(),
            true);
  }

  static Invokable staticMethod(Type owner, Type returnType, String name, Type... parameters) {
    final var method = new Method(name, returnType, parameters);
    return generator -> generator.invokeStatic(owner, method);
  }

  static Invokable virtualMethod(Type owner, Type returnType, String name, Type... parameters) {
    final var method = new Method(name, returnType, parameters);
    return generator -> generator.invokeVirtual(owner, method);
  }

  void invoke(GeneratorAdapter generator);
}
