package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.JvmBuildCollector.BC_VERSION;
import static flabbergast.compiler.kws.codegen.SimpleOpcode.DEFAULT_CTOR;
import static org.objectweb.asm.Type.VOID_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsType;
import flabbergast.util.Pair;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

abstract class MethodBranchInstruction implements Instruction {
  static class BranchPath {
    private final KwsType[] arguments;
    private final Streamable<Value> captures;
    private final String fieldPrefix;
    private final JvmBlock target;

    BranchPath(JvmBlock target, Streamable<Value> captures, KwsType... arguments) {
      this.target = target;
      this.captures = captures;
      this.arguments = arguments;
      fieldPrefix =
          Stream.of(arguments).map(KwsType::name).collect(Collectors.joining("", "", " "));
    }

    public void accessCheck(JvmBlock block, int index, ErrorCollector errorCollector) {
      captures.stream().forEach(capture -> capture.access(block, index, errorCollector));
    }

    Stream<Pair<Value, String>> captures() {
      return captures
          .stream()
          .map(
              new Function<>() {
                private int index;

                @Override
                public Pair<Value, String> apply(Value value) {
                  return Pair.of(value, fieldPrefix + (index++));
                }
              });
    }

    void makeMethod(ClassVisitor classVisitor, Type callbackType, JvmBlock current) {
      final var handler =
          new GeneratorAdapter(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
              new Method(
                  "accept",
                  VOID_TYPE,
                  Stream.of(arguments).map(LanguageType::of).toArray(Type[]::new)),
              null,
              null,
              classVisitor);
      handler.loadThis();
      handler.getField(callbackType, "owner", current.owner().type());

      captures
          .stream()
          .forEach(
              new Consumer<>() {
                private int index;

                @Override
                public void accept(Value value) {
                  handler.loadThis();
                  handler.getField(callbackType, fieldPrefix + (index++), current.owner().type());
                }
              });
      for (var i = 0; i < arguments.length; i++) {
        handler.loadArg(i);
      }

      handler.invokeVirtual(
          current.owner().type(),
          new Method(
              target.name(),
              VOID_TYPE,
              Stream.concat(captures.stream().map(Value::type), Stream.of(arguments))
                  .map(LanguageType::of)
                  .toArray(Type[]::new)));
      handler.visitInsn(Opcodes.RETURN);
      handler.endMethod();
    }

    public void typeCheck(SourceLocation location, ErrorCollector errorCollector) {
      if (target.parameters() != arguments.length + captures.stream().count()) {
        errorCollector.emitError(
            location,
            String.format(
                "Call to block %s requires %d arguments, but %d+%d given.",
                target.name(), target.parameters(), captures.stream().count(), arguments.length));
      } else {
        captures
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
        for (var i = 0; i < arguments.length; i++) {
          final var blockType = target.parameter(target.parameters() - arguments.length + i).type();
          if (blockType != arguments[i]) {
            errorCollector.emitError(
                location,
                String.format(
                    "Block %s has parameter type %s but argument is %s.",
                    target.name(), blockType, arguments[i]));
          }
        }
      }
    }
  }

  private final int index;
  private final SourceLocation location;

  protected MethodBranchInstruction(int index, SourceLocation location) {
    this.index = index;
    this.location = location;
  }

  @Override
  public void accessCheck(ErrorCollector errorCollector) {
    paths().forEach(path -> path.accessCheck(block(), index, errorCollector));
  }

  protected abstract JvmBlock block();

  @Override
  public final Stream<JvmBlock> dominators() {
    return paths().map(p -> p.target);
  }

  protected abstract void extraMethods(ClassVisitor callbackClass, Type callbackType);

  public final int index() {
    return index;
  }

  public final SourceLocation location() {
    return location;
  }

  protected abstract String opcode();

  protected abstract Stream<BranchPath> paths();

  protected final void push(JvmBlock current, Type superType) {
    final var callbackType =
        Type.getObjectType(
            current.owner().type().getInternalName() + " " + current.name() + " " + index);
    final var callbackClass = current.owner().createClass();
    callbackClass.visit(
        BC_VERSION,
        Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
        callbackType.getInternalName(),
        null,
        superType.getInternalName(),
        null);
    callbackClass.visitSource(current.owner().sourceFile(), null);
    callbackClass.visitNestHost(current.owner().type().getInternalName());
    current.owner().visitNestMember(callbackType);

    callbackClass
        .visitField(
            Opcodes.ACC_PRIVATE, "owner", current.owner().type().getDescriptor(), null, null)
        .visitEnd();

    final var captures = paths().flatMap(BranchPath::captures).collect(Collectors.toList());
    final var ctorMethod =
        new Method(
            "<init>",
            VOID_TYPE,
            Stream.concat(
                    Stream.of(current.owner().type()),
                    captures.stream().map(c -> LanguageType.of(c.first().type())))
                .toArray(Type[]::new));
    final var ctor =
        new GeneratorAdapter(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, ctorMethod, null, null, callbackClass);
    ctor.visitCode();
    ctor.loadThis();
    ctor.invokeConstructor(superType, DEFAULT_CTOR);
    ctor.loadThis();
    ctor.loadArg(0);
    ctor.putField(callbackType, "owner", current.owner().type());

    current.generator().newInstance(callbackType);

    current.generator().dup();

    current.generator().loadThis();

    for (var captureIndex = 0; captureIndex < captures.size(); captureIndex++) {
      final var kwsType = captures.get(captureIndex).first().type();
      final var jvmType = LanguageType.of(kwsType);
      final var fieldName = captures.get(captureIndex).second();
      callbackClass
          .visitField(
              Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
              fieldName,
              jvmType.getDescriptor(),
              null,
              null)
          .visitEnd();
      ctor.loadThis();
      ctor.loadArg(captureIndex + 1);
      ctor.putField(callbackType, fieldName, jvmType);
      captures.get(captureIndex).first().load(current, InputParameterType.single(kwsType));
    }

    ctor.visitInsn(Opcodes.RETURN);
    ctor.endMethod();
    paths().forEach(s -> s.makeMethod(callbackClass, callbackType, current));
    extraMethods(callbackClass, callbackType);
    callbackClass.visitEnd();

    current.generator().invokeConstructor(callbackType, ctorMethod);
  }

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
