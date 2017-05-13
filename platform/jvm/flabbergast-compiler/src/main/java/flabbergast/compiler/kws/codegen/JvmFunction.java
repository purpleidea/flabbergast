package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.JvmBuildCollector.BC_VERSION;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static org.objectweb.asm.Type.VOID_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsFunction;
import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import flabbergast.lang.Scheduler;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

class JvmFunction implements KwsFunction<Value, JvmBlock, JvmDispatch> {
  private static final Method JOBJECT__CTOR = new Method("<init>", VOID_TYPE, new Type[] {});
  private static final Method RUNNABLE__RUN = new Method("run", VOID_TYPE, new Type[] {});
  private static final Handle TASK_MASTER_BINDER =
      new Handle(
          Opcodes.H_INVOKESTATIC,
          Type.getInternalName(Scheduler.class),
          "binder",
          Type.getMethodDescriptor(
              Type.getType(CallSite.class),
              Type.getType(MethodHandles.Lookup.class),
              Type.getType(String.class),
              Type.getType(MethodType.class),
              Type.getType(Class.class),
              Type.getType(MethodHandle.class)),
          false);
  private final List<JvmBlock> blocks = new ArrayList<>();
  private final List<KwsType> captures;
  protected ClassVisitor classVisitor;
  private final Method creatorCtorMethod;
  private final Type creatorType;
  private final JvmBlock entryBlock;
  private final boolean export;
  private int field;
  private final FunctionKind kind;
  private final String name;
  private final JvmBuildCollector owner;
  private final Type selfType;

  public JvmFunction(
      JvmBuildCollector owner,
      boolean export,
      FunctionKind kind,
      String name,
      Type selfType,
      String entryName,
      List<KwsType> captures) {
    this.owner = owner;
    this.export = export;
    this.kind = kind;
    this.name = name;
    this.selfType = selfType;
    creatorType = Type.getObjectType(selfType.getInternalName() + " Creator");
    this.captures = captures;
    entryBlock = new JvmBlock(this, entryName, kind.parameters());
    creatorCtorMethod =
        new Method(
            "<init>", VOID_TYPE, captures.stream().map(LanguageType::of).toArray(Type[]::new));
  }

  @Override
  public final Value access(JvmBlock block, Stream<Value> parameters) {
    return block.call(new CallOpcode(this), parameters.toArray(InputArgument[]::new));
  }

  @Override
  public final Value capture(int i) {
    return new Value() {
      @Override
      public void access(JvmBlock block, int index, ErrorCollector errorCollector) {
        // Do nothing.
      }

      @Override
      public void load(GeneratorAdapter methodGen) {
        methodGen.loadThis();
        methodGen.getField(selfType, "Creator", selfType);
        methodGen.getField(creatorType, "Capture " + i, LanguageType.of(captures.get(i)));
      }

      @Override
      public void load(JvmBlock block, InputParameterType parameterType) {}

      @Override
      public KwsType type() {
        return captures.get(i);
      }

      @Override
      public void typeCheck(
          InputParameterType input, SourceLocation location, ErrorCollector collector) {}
    };
  }

  @Override
  public final int captures() {
    return captures.size();
  }

  public final ClassVisitor classVisitor() {
    return classVisitor;
  }

  @Override
  public JvmBlock createBlock(String name, Stream<KwsType> parameterTypes) {
    final var block = new JvmBlock(this, name, parameterTypes);
    blocks.add(block);
    return block;
  }

  public final ClassVisitor createClass() {
    return owner.createClass();
  }

  public String createField(Type jvmType) {
    final var name = "f" + (field++);
    classVisitor
        .visitField(Opcodes.ACC_PRIVATE, name, jvmType.getDescriptor(), null, null)
        .visitEnd();
    return name;
  }

  public final Method creatorMethod() {
    return creatorCtorMethod;
  }

  void dominatorAnalysis() {
    // This is derived from
    // https://android.googlesource.com/platform/dalvik2/+/master/dx/src/com/android/dx/ssa/Dominators.java
    // I'm not convinced I understand it, but I'm pretty certain the implementers there don't either
    final var dominatorCounter = new AtomicInteger();
    for (final var block : blocks) {
      block.setDominatorId(dominatorCounter);
    }

    for (var i = dominatorCounter.get() - 1; i > 1; i--) {
      final var predecessors = blocks.get(i).predecessors;
      for (int j = predecessors.nextSetBit(0); j >= 0; j = predecessors.nextSetBit(j + 1)) {
        final var predBlock = blocks.get(j);
        if (predBlock.dominatorId == -1) {
          // Block is not reachable
          continue;
        }

        int predSemidom = eval(predBlock).semidom;
        if (predSemidom < blocks.get(i).semidom) {
          blocks.get(i).semidom = predSemidom;
        }
      }
      blocks.get(blocks.get(i).semidom).bucket.add(blocks.get(i));
      blocks.get(i).ancestor = blocks.get(i).parent;
      final var wParentBucket = blocks.get(i).parent.bucket;
      while (!wParentBucket.isEmpty()) {
        final var last = wParentBucket.removeLast();
        final var U = eval(last);
        if (U.semidom < last.semidom) {
          last.idom = U.dominatorId;
        } else {
          last.idom = blocks.get(i).parent.dominatorId;
        }
      }
    }
    // Now explicitly define the immediate dominator of each vertex
    for (int i = 2; i <= dominatorCounter.get(); i++) {
      final var w = blocks.get(i);
      if (w.idom != blocks.get(w.semidom).dominatorId) {
        w.idom = blocks.get(w.idom).idom;
      }
    }
  }

  private void compress(JvmBlock in) {
    if (in.ancestor.ancestor != null) {
      final var worklist = new ArrayList<JvmBlock>();
      final var visited = new HashSet<JvmBlock>();
      worklist.add(in);
      while (!worklist.isEmpty()) {
        int wsize = worklist.size();
        final var v = worklist.get(wsize - 1);
        if (visited.add(v.ancestor) && v.ancestor.ancestor != null) {
          worklist.add(v.ancestor);
          continue;
        }
        worklist.remove(wsize - 1);
        if (v.ancestor.ancestor == null) {
          continue;
        }
        if (v.ancestor.rep.semidom < v.rep.semidom) {
          v.rep = v.ancestor.rep;
        }
        v.ancestor.ancestor = v.ancestor.ancestor.ancestor;
      }
    }
  }

  private JvmBlock eval(JvmBlock v) {
    if (v.ancestor == null) {
      return v;
    }
    compress(v);
    return v.rep;
  }

  @Override
  public final JvmBlock entryBlock() {
    return entryBlock;
  }

  @Override
  public void finish() {}

  public FunctionKind kind() {
    return kind;
  }

  public KwsType kwsType() {
    return kind.type();
  }

  public String name() {
    return name;
  }

  protected void prepareExports(Type creatorType, GeneratorAdapter methodGen) {
    // Do nothing.
  }

  void render() {
    final var rawParameterTypes =
        Stream.concat(Stream.of(selfType), kind.parameters().map(LanguageType::of))
            .toArray(Type[]::new);
    classVisitor = owner.createClass();

    classVisitor.visit(
        BC_VERSION,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
        selfType.getInternalName(),
        null,
        JOBJECT_TYPE.getInternalName(),
        null);
    classVisitor.visitSource(sourceFile(), null);
    classVisitor
        .visitField(Opcodes.ACC_PRIVATE, "Creator", creatorType.getInternalName(), null, null)
        .visitEnd();
    classVisitor
        .visitField(Opcodes.ACC_PRIVATE, "future", FUTURE_TYPE.getInternalName(), null, null)
        .visitEnd();
    if (!kind.needsSourceReference()) {
      classVisitor
          .visitField(
              Opcodes.ACC_PRIVATE,
              "sourceReference",
              SOURCE_REFERENCE_TYPE.getInternalName(),
              null,
              null)
          .visitEnd();
    }
    blocks.add(entryBlock);

    final var runType = Type.getObjectType(selfType.getInternalName() + " Bridge");
    final var runVisitor = owner.createClass();
    runVisitor.visit(
        BC_VERSION,
        Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER,
        runType.getInternalName(),
        null,
        JOBJECT_TYPE.getInternalName(),
        null);
    runVisitor.visitSource(sourceFile(), null);
    runVisitor.visitNestHost(selfType.getInternalName());
    classVisitor.visitNestMember(runType.getInternalName());

    final var run = new GeneratorAdapter(Opcodes.ACC_PUBLIC, RUNNABLE__RUN, null, null, runVisitor);
    final var runCtorMethod = new Method("<init>", VOID_TYPE, rawParameterTypes);
    final var runCtor =
        new GeneratorAdapter(Opcodes.ACC_PUBLIC, runCtorMethod, null, null, runVisitor);
    run.visitCode();
    for (var i = 0; i < rawParameterTypes.length; i++) {
      final var fieldName = "Initial " + i;
      runVisitor
          .visitField(
              Opcodes.ACC_PRIVATE, fieldName, rawParameterTypes[i].getDescriptor(), null, null)
          .visitEnd();
      runCtor.loadThis();
      runCtor.loadArg(i + 1);
      runCtor.putField(runType, fieldName, rawParameterTypes[i]);
      run.loadThis();
      run.getField(runType, fieldName, rawParameterTypes[i]);
    }
    run.invokeVirtual(selfType, entryBlock.method());
    run.visitInsn(Opcodes.RETURN);
    run.endMethod();
    runCtor.visitInsn(Opcodes.RETURN);
    runCtor.endMethod();
    runVisitor.visitEnd();

    final var ctorMethod =
        new Method(
            "<init>",
            VOID_TYPE,
            kind.needsSourceReference()
                ? new Type[] {creatorType, FUTURE_TYPE}
                : new Type[] {creatorType, FUTURE_TYPE, SOURCE_REFERENCE_TYPE});
    final var ctor =
        new GeneratorAdapter(Opcodes.ACC_PRIVATE, ctorMethod, null, null, classVisitor);
    ctor.visitCode();
    ctor.loadThis();
    ctor.invokeConstructor(JOBJECT_TYPE, JOBJECT__CTOR);
    ctor.loadThis();
    ctor.loadArg(0);
    ctor.putField(selfType, "Creator", creatorType);
    ctor.loadThis();
    ctor.loadArg(1);
    ctor.putField(selfType, "future", FUTURE_TYPE);
    if (!kind.needsSourceReference()) {
      ctor.loadThis();
      ctor.loadArg(2);
      ctor.putField(selfType, "sourceReference", SOURCE_REFERENCE_TYPE);
    }
    ctor.visitInsn(Opcodes.RETURN);
    ctor.endMethod();

    final var createParameters =
        Stream.concat(
                Stream.of(FUTURE_TYPE, kind.needsSourceReference() ? null : SOURCE_REFERENCE_TYPE),
                kind.parameters().map(LanguageType::of))
            .filter(Objects::nonNull)
            .toArray(Type[]::new);

    final var creatorVisitor = owner.createClass();
    creatorVisitor.visit(
        BC_VERSION,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER,
        creatorType.getInternalName(),
        null,
        (kind.isInterface() ? JOBJECT_TYPE : kind.jvmType()).getInternalName(),
        kind.isInterface() ? new String[] {kind.jvmType().getInternalName()} : null);
    creatorVisitor.visitSource(sourceFile(), null);
    creatorVisitor.visitNestHost(selfType.getInternalName());
    classVisitor.visitNestMember(creatorType.getInternalName());
    final var creatorCtor =
        new GeneratorAdapter(Opcodes.ACC_PRIVATE, creatorCtorMethod, null, null, classVisitor);
    creatorCtor.visitCode();
    creatorCtor.loadThis();
    kind.callSuperConstructor(creatorCtor);
    for (var i = 0; i < captures.size(); i++) {
      final var captureId = "Capture " + i;
      creatorVisitor
          .visitField(
              Opcodes.ACC_PRIVATE,
              captureId,
              LanguageType.of(captures.get(i)).getDescriptor(),
              null,
              null)
          .visitEnd();
      creatorCtor.loadThis();
      creatorCtor.loadArg(i);
      creatorCtor.putField(creatorType, captureId, LanguageType.of(captures.get(i)));
    }
    creatorCtor.visitInsn(Opcodes.RETURN);
    creatorCtor.endMethod();

    final var create =
        new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method(kind.methodName(), RUNNABLE_TYPE, createParameters),
            null,
            null,
            creatorVisitor);
    create.visitCode();
    prepareExports(creatorType, create);
    create.newInstance(runType);
    create.dup();
    create.newInstance(selfType);
    create.dup();
    create.loadThis();
    create.loadArg(0);
    if (!kind.needsSourceReference()) {
      create.loadArg(1);
    }
    create.invokeConstructor(selfType, ctorMethod);
    for (var i = 0; i < kind.parameterCount(); i++) {
      create.loadArg(ctorMethod.getArgumentTypes().length + i);
    }
    create.invokeConstructor(runType, runCtorMethod);
    create.returnValue();
    create.endMethod();
    creatorVisitor.visitEnd();

    if (export) {
      owner.export().loadArg(0);
      owner
          .export()
          .invokeDynamic(
              name,
              Type.getMethodDescriptor(VOID_TYPE, FUTURE_TYPE),
              TASK_MASTER_BINDER,
              LanguageType.of(kind.type()),
              new Handle(
                  Opcodes.H_NEWINVOKESPECIAL,
                  creatorType.getInternalName(),
                  creatorCtorMethod.getName(),
                  creatorCtorMethod.getDescriptor(),
                  false));
    }
    for (final var block : blocks) {
      block.render();
    }
    classVisitor.visitEnd();
  }

  @Override
  public final ResultType result() {
    return kind.resultType();
  }

  public final String sourceFile() {
    return owner.sourceFile();
  }

  public final Type type() {
    return selfType;
  }

  void typeCheck(ErrorCollector errorCollector) {
    for (final var block : blocks) {
      block.typeCheck(errorCollector);
    }
  }

  void accessCheck(ErrorCollector errorCollector) {
    for (final var block : blocks) {
      block.accessChecks(errorCollector);
    }
  }

  final void visitNestMember(Type nestType) {
    classVisitor.visitNestMember(nestType.getInternalName());
  }
}
