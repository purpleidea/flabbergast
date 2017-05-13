package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.A;
import static flabbergast.compiler.kws.KwsType.Z;
import static flabbergast.compiler.kws.codegen.InputParameterType.single;
import static flabbergast.compiler.kws.codegen.Invokable.virtualMethod;
import static flabbergast.compiler.kws.codegen.JvmBuildCollector.BC_VERSION;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static org.objectweb.asm.Type.BOOLEAN_TYPE;

import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.kws.KwsType;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class TestTypeOpcode implements GeneralOpcode {
  private final Predicate<FlabbergastType> include;

  public TestTypeOpcode(Predicate<FlabbergastType> include) {
    this.include = include;
  }

  @Override
  public boolean isCallback() {
    return false;
  }

  @Override
  public String opcode() {
    return "atoz";
  }

  @Override
  public InputParameterType parameter(int i) {
    return single(A);
  }

  @Override
  public int parameters() {
    return 1;
  }

  @Override
  public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
    final var applierType =
        Type.getObjectType(
            String.format(
                "%s %s %d", output.owner().type().getInternalName(), output.name(), index));

    final var applier = output.owner().createClass();
    applier.visit(
        BC_VERSION,
        Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER,
        applierType.getInternalName(),
        null,
        JOBJECT_TYPE.getInternalName(),
        new String[] {ANY_FUNCTION_TYPE.getInternalName()});

    applier.visitSource(output.owner().sourceFile(), null);

    final var ctor =
        new GeneratorAdapter(Opcodes.ACC_SYNTHETIC, SimpleOpcode.DEFAULT_CTOR, null, null, applier);
    ctor.visitCode();
    ctor.loadThis();
    ctor.invokeConstructor(JOBJECT_TYPE, SimpleOpcode.DEFAULT_CTOR);
    ctor.visitInsn(Opcodes.RETURN);
    ctor.endMethod();

    for (final var type : FlabbergastType.values()) {
      final var applyMethod =
          new GeneratorAdapter(
              Opcodes.ACC_PUBLIC,
              new Method(
                  "apply",
                  JOBJECT_TYPE,
                  type.kwsType() == null
                      ? new Type[] {}
                      : new Type[] {LanguageType.of(type.kwsType())}),
              null,
              null,
              applier);
      applyMethod.visitCode();
      applyMethod.push(include.test(type));
      applyMethod.valueOf(BOOLEAN_TYPE);
      applyMethod.returnValue();
      applyMethod.endMethod();
    }
    applier.visitEnd();

    values.accept(0);

    output.generator().newInstance(applierType);

    output.generator().dup();

    output.generator().invokeConstructor(applierType, SimpleOpcode.DEFAULT_CTOR);
    ANY__APPLY.invoke(output.generator());

    output.generator().unbox(BOOLEAN_TYPE);
  }

  private static final Invokable ANY__APPLY =
      virtualMethod(ANY_TYPE, JOBJECT_TYPE, "apply", ANY_FUNCTION_TYPE);

  @Override
  public KwsType returnType() {
    return Z;
  }
}
