package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static org.objectweb.asm.Type.VOID_TYPE;

import flabbergast.compiler.kws.KwsType;
import flabbergast.lang.Scheduler;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class ImportOpcode implements GeneralOpcode {
  private final KwsType returnType;
  private final String kwsName;
  private final KwsType[] parameters;

  public ImportOpcode(KwsType returnType, String kwsName, KwsType... parameters) {
    this.returnType = returnType;
    this.kwsName = kwsName;
    this.parameters = parameters;
  }

  @Override
  public boolean isCallback() {
    return true;
  }

  @Override
  public String opcode() {
    return "import " + kwsName;
  }

  @Override
  public InputParameterType parameter(int i) {
    return InputParameterType.single(parameters[i]);
  }

  @Override
  public int parameters() {
    return parameters.length;
  }

  @Override
  public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
    output.loadFuture();
    output.loadSource();
    loadCallback.run();
    for (var i = 0; i < parameters(); i++) {
      values.accept(i);
    }

    output
        .generator()
        .invokeDynamic(
            kwsName,
            Type.getMethodDescriptor(
                VOID_TYPE,
                Stream.concat(
                        Stream.of(FUTURE_TYPE, SOURCE_REFERENCE_TYPE, CONSUMER_TYPE),
                        Stream.of(parameters).map(LanguageType::of))
                    .toArray(Type[]::new)),
            TASK_MASTER_BOOTSTRAP,
            LanguageType.of(returnType));
  }

  private static final Handle TASK_MASTER_BOOTSTRAP =
      new Handle(
          Opcodes.H_INVOKESTATIC,
          Type.getInternalName(Scheduler.class),
          "bootstrap",
          Type.getMethodDescriptor(
              VOID_TYPE,
              Type.getType(MethodHandles.Lookup.class),
              JSTRING_TYPE,
              Type.getType(MethodType.class),
              Type.getType(Class.class)),
          false);

  @Override
  public KwsType returnType() {
    return returnType;
  }
}
