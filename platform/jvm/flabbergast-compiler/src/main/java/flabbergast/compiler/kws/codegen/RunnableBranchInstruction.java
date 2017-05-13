package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.JvmBuildCollector.BC_VERSION;
import static flabbergast.compiler.kws.codegen.LanguageType.JOBJECT_TYPE;
import static flabbergast.compiler.kws.codegen.LanguageType.RUNNABLE_TYPE;
import static flabbergast.compiler.kws.codegen.SimpleOpcode.DEFAULT_CTOR;
import static org.objectweb.asm.Type.VOID_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

abstract class RunnableBranchInstruction implements Instruction {
  static class BranchPath {
    private final Streamable<Value> arguments;
    private final JvmBlock target;

    BranchPath(JvmBlock target, Streamable<Value> arguments) {
      this.target = target;
      this.arguments = arguments;
    }

    public void accessCheck(JvmBlock block, int index, ErrorCollector errorCollector) {
      arguments.stream().forEach(argument -> argument.access(block, index, errorCollector));
    }

    void makeRunnable(JvmBlock current) {

      final var callbackType =
          Type.getObjectType(
              current.owner().type().getInternalName()
                  + " "
                  + current.name()
                  + " → "
                  + target.name());
      final var callbackClass = current.owner().createClass();
      callbackClass.visit(
          BC_VERSION,
          Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
          callbackType.getInternalName(),
          null,
          JOBJECT_TYPE.getInternalName(),
          new String[] {RUNNABLE_TYPE.getInternalName()});
      callbackClass.visitSource(current.owner().sourceFile(), null);
      callbackClass.visitNestHost(current.owner().type().getInternalName());
      current.owner().visitNestMember(callbackType);

      callbackClass
          .visitField(
              Opcodes.ACC_PRIVATE, "owner", current.owner().type().getDescriptor(), null, null)
          .visitEnd();
      final var ctorMethod =
          new Method(
              "<init>",
              VOID_TYPE,
              Stream.concat(
                      Stream.of(current.owner().type()),
                      arguments.stream().map(Value::type).map(LanguageType::of))
                  .toArray(Type[]::new));
      final var ctor =
          new GeneratorAdapter(
              Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, ctorMethod, null, null, callbackClass);
      ctor.visitCode();
      ctor.loadThis();
      ctor.invokeConstructor(JOBJECT_TYPE, DEFAULT_CTOR);
      ctor.loadThis();
      ctor.loadArg(0);
      ctor.putField(callbackType, "owner", current.owner().type());

      final var handler =
          new GeneratorAdapter(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
              new Method("run", VOID_TYPE, new Type[0]),
              null,
              null,
              callbackClass);
      handler.loadThis();
      handler.getField(callbackType, "owner", current.owner().type());

      current.generator().newInstance(callbackType);

      current.generator().dup();

      current.generator().loadThis();

      arguments
          .stream()
          .forEachOrdered(
              new Consumer<>() {
                private int count;

                public void accept(Value arg) {
                  final var index = count++;
                  final var jvmType = LanguageType.of(arg.type());
                  final var fieldName = "arg " + count;
                  callbackClass
                      .visitField(
                          Opcodes.ACC_PRIVATE, fieldName, jvmType.getDescriptor(), null, null)
                      .visitEnd();
                  ctor.loadThis();
                  ctor.loadArg(index + 1);
                  ctor.putField(callbackType, fieldName, jvmType);
                  handler.loadThis();
                  handler.getField(callbackType, fieldName, jvmType);
                  arg.load(current, InputParameterType.single(current.parameter(index).type()));
                }
              });

      ctor.visitInsn(Opcodes.RETURN);
      ctor.endMethod();
      handler.invokeVirtual(
          current.owner().type(),
          new Method(
              target.name(),
              VOID_TYPE,
              arguments.stream().map(Value::type).map(LanguageType::of).toArray(Type[]::new)));
      handler.visitInsn(Opcodes.RETURN);
      handler.endMethod();

      callbackClass.visitEnd();

      current.generator().invokeConstructor(callbackType, ctorMethod);
    }

    public void typeCheck(SourceLocation location, ErrorCollector errorCollector) {
      if (target.parameters() != arguments.stream().count()) {
        errorCollector.emitError(
            location,
            String.format(
                "Call to block %s requires %d arguments, but %d given.",
                target.name(), target.parameters(), arguments.stream().count()));
      } else {
        arguments
            .stream()
            .forEach(
                new Consumer<>() {
                  private int index;

                  @Override
                  public void accept(Value value) {
                    value.typeCheck(
                        InputParameterType.single(target.parameter(index++).type()),
                        location,
                        errorCollector);
                  }
                });
      }
    }
  }

  private final int index;
  private final SourceLocation location;

  protected RunnableBranchInstruction(int index, SourceLocation location) {
    this.index = index;
    this.location = location;
  }

  @Override
  public final void accessCheck(ErrorCollector errorCollector) {
    accessCheckExtra(errorCollector);
    paths().forEach(path -> path.accessCheck(block(), index, errorCollector));
  }

  protected abstract void accessCheckExtra(ErrorCollector errorCollector);

  protected abstract JvmBlock block();

  @Override
  public final Stream<JvmBlock> dominators() {
    return paths().map(p -> p.target);
  }

  public final int index() {
    return index;
  }

  public SourceLocation location() {
    return location;
  }

  protected abstract String opcode();

  protected abstract Stream<BranchPath> paths();

  @Override
  public String toString() {
    return String.format("%s:%s:%d %s", block().owner().name(), block().name(), index, opcode());
  }

  @Override
  public final void typeCheck(ErrorCollector errorCollector) {
    paths().forEachOrdered(s -> s.typeCheck(location, errorCollector));
    typeCheckExtra(errorCollector);
  }

  protected abstract void typeCheckExtra(ErrorCollector errorCollector);
}
