package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.KwsType.*;
import static flabbergast.compiler.kws.codegen.Invokable.virtualMethod;
import static flabbergast.compiler.kws.codegen.JvmBuildCollector.BC_VERSION;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static flabbergast.compiler.kws.codegen.SimpleOpcode.DEFAULT_CTOR;
import static org.objectweb.asm.Type.*;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.FlabbergastType;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.Streamable;
import flabbergast.compiler.kws.KwsBlock;
import flabbergast.compiler.kws.KwsType;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

class JvmBlock implements KwsBlock<Value, JvmBlock, JvmDispatch> {
  private class DisperseInstruction implements Instruction {
    private final Value frame;
    private final int index = instructions.size();
    private final SourceLocation location = currentLocation;
    private final Invokable method;
    private final Value name;
    private final KwsType nameType;
    private final Value value;

    private DisperseInstruction(
        Invokable method, Value frame, KwsType nameType, Value name, Value value) {
      this.method = method;
      this.frame = frame;
      this.nameType = nameType;
      this.name = name;
      this.value = value;
    }

    @Override
    public void accessCheck(ErrorCollector errorCollector) {
      frame.access(JvmBlock.this, index, errorCollector);
      name.access(JvmBlock.this, index, errorCollector);
      value.access(JvmBlock.this, index, errorCollector);
    }

    @Override
    public void render() {
      frame.load(JvmBlock.this, InputParameterType.single(R));
      loadFuture();
      loadSource();
      name.load(JvmBlock.this, InputParameterType.single(N));
      value.load(JvmBlock.this, InputParameterType.single(A));
      method.invoke(output);
      final var end = output.newLabel();
      output.ifZCmp(GeneratorAdapter.NE, end);
      output.visitInsn(Opcodes.RETURN);
      output.visitLabel(end);
    }

    @Override
    public String toString() {
      return String.format("%s:%s:%d disperse instruction", owner.name(), name(), index);
    }

    @Override
    public void typeCheck(ErrorCollector errorCollector) {
      frame.typeCheck(InputParameterType.single(R), location, errorCollector);
      name.typeCheck(InputParameterType.single(nameType), location, errorCollector);
      value.typeCheck(InputParameterType.single(A), location, errorCollector);
    }
  }

  private static class Fixed extends Value {
    private final LoadableValue value;

    public Fixed(LoadableValue value) {
      super();
      this.value = value;
    }

    @Override
    public void access(JvmBlock block, int index, ErrorCollector errorCollector) {
      // Do not care.
    }

    @Override
    public void load(JvmBlock block, InputParameterType parameterType) {
      parameterType.load(block, value);
    }

    @Override
    public void load(GeneratorAdapter method) {
      value.load(method);
    }

    @Override
    public KwsType type() {
      return value.type();
    }

    @Override
    public void typeCheck(
        InputParameterType input, SourceLocation location, ErrorCollector collector) {
      input.typeCheck(value.type(), location, collector);
    }
  }

  private class Parameter extends RootValue {
    private Consumer<GeneratorAdapter> loader;
    private final KwsType type;

    private Parameter(KwsType type) {
      this.type = type;
    }

    @Override
    public void accessCheck(ErrorCollector errorCollector) {
      // Do nothing.
    }

    @Override
    public void load(JvmBlock block, InputParameterType parameterType) {
      parameterType.load(
          block,
          new LoadableValue() {
            @Override
            public void load(GeneratorAdapter method) {
              loader.accept(method);
            }

            @Override
            public KwsType type() {
              return type;
            }
          });
    }

    @Override
    public void load(GeneratorAdapter method) {
      loader.accept(method);
    }

    @Override
    public void render() {
      if (isHoisted()) {
        final var jvmType = LanguageType.of(type);
        final var field = owner.createField(jvmType);
        output.loadThis();
        output.loadArg(index());
        output.putField(owner.type(), field, jvmType);
        loader =
            r -> {
              r.loadThis();
              r.getField(owner.type(), field, jvmType);
            };
      } else {
        loader = g -> g.loadArg(index());
      }
    }

    @Override
    public String toString() {
      return String.format("%s:%s parameter %s", owner.name(), name(), type);
    }

    @Override
    public KwsType type() {
      return type;
    }

    @Override
    public void typeCheck(ErrorCollector errorCollector) {
      // Do nothing.
    }

    @Override
    public void typeCheck(
        InputParameterType input, SourceLocation location, ErrorCollector collector) {
      input.typeCheck(type, firstLocation, collector);
    }
  }

  private abstract class RootValue extends Value implements Instruction {
    private int end;
    private boolean hoisted;
    protected final int start;

    public RootValue() {
      start = instructions.size();
      instructions.add(this);
    }

    @Override
    public final void access(JvmBlock block, int index, ErrorCollector errorCollector) {
      if (block == JvmBlock.this) {
        end = Math.max(end, index);
      } else if (block.predecessors.get(dominatorId)) {
        hoisted = true;
      } else {
        errorCollector.emitError(
            block.firstLocation,
            String.format(
                "Block %s:%d accesses value %s which is not a dominator of this block.",
                block.name(), index, this));
      }
    }

    final int index() {
      return start;
    }

    final boolean isHoisted() {
      if (hoisted) {
        return true;
      }
      return breaks.stream().anyMatch(b -> b > start && b <= end);
    }
  }

  class StandardLocal extends RootValue {
    private final InputArgument[] arguments;
    private final GeneralOpcode instruction;
    private Consumer<GeneratorAdapter> loader;
    private final SourceLocation location = currentLocation;

    StandardLocal(GeneralOpcode instruction, InputArgument... arguments) {
      this.instruction = instruction;
      this.arguments = arguments;
      if (instruction.isCallback()) {
        breaks.add(index());
      }
    }

    @Override
    public void accessCheck(ErrorCollector errorCollector) {
      for (final var argument : arguments) {
        argument.access(JvmBlock.this, start, errorCollector);
      }
    }

    private void argument(int i) {
      arguments[i].load(JvmBlock.this, instruction.parameter(i));
    }

    @Override
    public void load(GeneratorAdapter method) {
      loader.accept(method);
    }

    @Override
    public void load(JvmBlock block, InputParameterType parameterType) {
      parameterType.load(
          block,
          new LoadableValue() {
            @Override
            public void load(GeneratorAdapter method) {
              loader.accept(method);
            }

            @Override
            public KwsType type() {
              return instruction.returnType();
            }
          });
    }

    @Override
    public void render() {
      final var jvmType = LanguageType.of(instruction.returnType());
      if (instruction.isCallback() && isHoisted()) {
        final var field = owner.createField(jvmType);
        final var id = ++callback;
        final var nextMethodName = String.format("%s %d", JvmBlock.this.id, id);
        final var nextMethod = new Method(nextMethodName, VOID_TYPE, new Type[0]);
        final var next =
            new GeneratorAdapter(Opcodes.ACC_PRIVATE, nextMethod, null, null, owner.classVisitor());

        final var callbackType =
            Type.getObjectType(owner.type().getInternalName() + " " + nextMethodName);
        final var callbackClass = owner.createClass();
        callbackClass.visit(
            BC_VERSION,
            Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
            callbackType.getInternalName(),
            null,
            JOBJECT_TYPE.getInternalName(),
            new String[] {CONSUMER_TYPE.getInternalName()});
        callbackClass.visitSource(owner.sourceFile(), null);
        callbackClass.visitNestHost(owner.type().getInternalName());
        owner.visitNestMember(callbackType);

        callbackClass
            .visitField(Opcodes.ACC_PRIVATE, "owner", owner.type().getDescriptor(), null, null)
            .visitEnd();
        final var ctorMethod = new Method("<init>", VOID_TYPE, new Type[] {owner.type()});
        final var ctor =
            new GeneratorAdapter(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, ctorMethod, null, null, callbackClass);
        ctor.visitCode();
        ctor.loadThis();
        ctor.invokeConstructor(JOBJECT_TYPE, DEFAULT_CTOR);
        ctor.loadThis();
        ctor.loadArg(0);
        ctor.putField(callbackType, "owner", owner.type());
        ctor.visitInsn(Opcodes.RETURN);
        ctor.endMethod();

        final var handler =
            new GeneratorAdapter(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                new Method("accept", VOID_TYPE, new Type[] {JOBJECT_TYPE}),
                null,
                null,
                callbackClass);
        handler.loadThis();
        handler.getField(callbackType, "owner", owner.type());
        handler.dup();
        handler.loadArg(0);
        handler.unbox(jvmType);
        handler.putField(owner.type(), field, jvmType);
        handler.invokeVirtual(owner.type(), nextMethod);
        handler.visitInsn(Opcodes.RETURN);
        handler.endMethod();

        callbackClass.visitEnd();
        instruction.render(
            JvmBlock.this,
            index(),
            () -> {
              output.newInstance(callbackType);
              output.dup();
              output.loadThis();
              output.invokeConstructor(callbackType, ctorMethod);
            },
            this::argument);
        output.visitInsn(Opcodes.RETURN);
        output.endMethod();
        output = next;
        output.visitCode();
        loader =
            r -> {
              r.loadThis();
              r.getField(owner.type(), field, jvmType);
            };
      } else if (instruction.isCallback()) {
        final var id = ++callback;
        final var nextMethodName = String.format("%s %d", JvmBlock.this.id, id);
        final var nextMethod = new Method(nextMethodName, VOID_TYPE, new Type[] {jvmType});
        final var next =
            new GeneratorAdapter(Opcodes.ACC_PRIVATE, nextMethod, null, null, owner.classVisitor());

        final var callbackType =
            Type.getObjectType(owner.type().getInternalName() + " " + nextMethodName);
        final var callbackClass = owner.createClass();
        callbackClass.visit(
            BC_VERSION,
            Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
            callbackType.getInternalName(),
            null,
            JOBJECT_TYPE.getInternalName(),
            new String[] {CONSUMER_TYPE.getInternalName()});
        callbackClass.visitSource(owner.sourceFile(), null);
        callbackClass.visitNestHost(owner.type().getInternalName());
        owner.visitNestMember(callbackType);

        callbackClass
            .visitField(Opcodes.ACC_PRIVATE, "owner", owner.type().getDescriptor(), null, null)
            .visitEnd();
        final var ctorMethod = new Method("<init>", VOID_TYPE, new Type[] {owner.type()});
        final var ctor =
            new GeneratorAdapter(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, ctorMethod, null, null, callbackClass);
        ctor.visitCode();
        ctor.loadThis();
        ctor.invokeConstructor(JOBJECT_TYPE, DEFAULT_CTOR);
        ctor.loadThis();
        ctor.loadArg(0);
        ctor.putField(callbackType, "owner", owner.type());
        ctor.visitInsn(Opcodes.RETURN);
        ctor.endMethod();

        final var handler =
            new GeneratorAdapter(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                new Method("accept", VOID_TYPE, new Type[] {JOBJECT_TYPE}),
                null,
                null,
                callbackClass);
        handler.loadThis();
        handler.getField(callbackType, "owner", owner.type());
        handler.loadArg(0);
        handler.unbox(jvmType);
        handler.invokeVirtual(owner.type(), nextMethod);
        handler.visitInsn(Opcodes.RETURN);
        handler.endMethod();

        callbackClass.visitEnd();
        instruction.render(
            JvmBlock.this,
            index(),
            () -> {
              output.newInstance(callbackType);
              output.dup();
              output.loadThis();
              output.invokeConstructor(callbackType, ctorMethod);
            },
            this::argument);
        output.visitInsn(Opcodes.RETURN);
        output.endMethod();
        output = next;
        output.visitCode();
        loader = r -> r.loadArg(0);
      } else if (isHoisted()) {
        final var field = owner.createField(jvmType);
        output.loadThis();
        instruction.render(JvmBlock.this, index(), null, this::argument);
        output.putField(owner.type(), field, jvmType);
        loader =
            r -> {
              r.loadThis();
              r.getField(owner.type(), field, jvmType);
            };
      } else {
        instruction.render(JvmBlock.this, index(), null, this::argument);
        final var slot = output.newLocal(jvmType);
        output.storeLocal(slot, jvmType);
        loader = r -> r.loadLocal(slot);
      }
    }

    @Override
    public String toString() {
      return String.format("%s:%s:%d %s", owner.name(), name(), start, instruction.opcode());
    }

    @Override
    public KwsType type() {
      return instruction.returnType();
    }

    @Override
    public void typeCheck(
        InputParameterType input, SourceLocation location, ErrorCollector collector) {
      input.typeCheck(instruction.returnType(), location, collector);
    }

    @Override
    public void typeCheck(ErrorCollector errorCollector) {
      if (arguments.length != instruction.parameters()) {
        errorCollector.emitError(
            location,
            String.format(
                "Instruction %s expected %d arguments but got %d.",
                instruction.opcode(), instruction.parameters(), arguments.length));
      } else {
        for (var i = 0; i < arguments.length; i++) {
          arguments[i].typeCheck(instruction.parameter(i), location, errorCollector);
        }
      }
    }
  }

  private static final Invokable ACCUMULATOR__OF =
      Invokable.staticMethod(
          ACCUMULATOR_TYPE, ACCUMULATOR_TYPE, "of", ANY_TYPE, ATTRIBUTE_SOURCE_ARRAY_TYPE);
  private static final Invokable DOUBLE_BI_CONSUMER__DISPATCH_FA =
      virtualMethod(
          DOUBLE_BI_CONSUMER_TYPE,
          VOID_TYPE,
          "dispatch",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          DOUBLE_TYPE,
          ANY_TYPE);
  private static final Invokable FRAME__DISPERSE_I =
      virtualMethod(
          FRAME_TYPE,
          BOOLEAN_TYPE,
          "disperse",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          LONG_TYPE,
          ANY_TYPE);
  private static final Invokable FRAME__DISPERSE_S =
      virtualMethod(
          FRAME_TYPE,
          BOOLEAN_TYPE,
          "disperse",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          STR_TYPE,
          ANY_TYPE);
  private static final Invokable FUTURE__COMPLETE =
      virtualMethod(FUTURE_TYPE, VOID_TYPE, "complete", JOBJECT_TYPE);
  private static final Invokable FUTURE__ERROR =
      virtualMethod(FUTURE_TYPE, VOID_TYPE, "error", SOURCE_REFERENCE_TYPE, JSTRING_TYPE);
  private static final Invokable FUTURE__RESCHEDULE =
      virtualMethod(FUTURE_TYPE, VOID_TYPE, "reschedule", RUNNABLE_TYPE);
  private static final Invokable FUTURE__RESCHEDULE_ANY =
      virtualMethod(FUTURE_TYPE, VOID_TYPE, "reschedule", ANY_TYPE, ANY_CONSUMER_TYPE);
  private static final Invokable JSTRING__CONCAT =
      Invokable.staticMethod(
          LanguageType.JSTRING_TYPE,
          LanguageType.JSTRING_TYPE,
          "concat",
          JSTRING_ARRAY_TYPE,
          LanguageType.JSTRING_TYPE);
  private static final Invokable LONG_BI_CONSUMER__DISPATCH_AA =
      virtualMethod(
          LONG_BI_CONSUMER_TYPE,
          VOID_TYPE,
          "dispatch",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          ANY_TYPE,
          ANY_TYPE);
  private static final Invokable LONG_BI_CONSUMER__DISPATCH_IA =
      Invokable.staticMethod(
          LONG_BI_CONSUMER_TYPE,
          VOID_TYPE,
          "dispatch",
          FUTURE_TYPE,
          SOURCE_REFERENCE_TYPE,
          LONG_TYPE,
          ANY_TYPE);
  private static final Invokable OBJECT__TO_STRING =
      virtualMethod(Type.getType(Object.class), JSTRING_TYPE, "toString");
  private static final Invokable SOURCE_REFERENCE__BASIC =
      virtualMethod(
          SOURCE_REFERENCE_TYPE,
          SOURCE_REFERENCE_TYPE,
          "basic",
          JSTRING_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          INT_TYPE,
          JSTRING_TYPE);
  JvmBlock ancestor;
  private final List<Integer> breaks = new ArrayList<>();
  final Deque<JvmBlock> bucket = new ArrayDeque<>();
  private int callback;
  private SourceLocation currentLocation = SourceLocation.EMPTY;
  private String currentMessage;
  int dominatorId = -1;
  private SourceLocation firstLocation;
  private final String id;
  int idom;
  private List<Instruction> instructions = new ArrayList<>();
  private final Map<SourceLocation, String> locations = new HashMap<>();
  private GeneratorAdapter output;
  private final JvmFunction owner;
  private final Value[] parameters;
  JvmBlock parent;
  final BitSet predecessors = new BitSet();
  JvmBlock rep;
  int semidom;

  public JvmBlock(JvmFunction owner, String id, Stream<KwsType> parameters) {
    this.id = id;
    this.owner = owner;
    this.parameters = parameters.map(Parameter::new).toArray(Value[]::new);
  }

  public void accessChecks(ErrorCollector errorCollector) {
    for (final var instruction : instructions) {
      instruction.accessCheck(errorCollector);
    }
  }

  @Override
  public Value add_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.ADD_F, left, right);
  }

  @Override
  public Value add_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.ADD_I, left, right);
  }

  @Override
  public Value add_n(Value source, Streamable<String> names) {
    return new StandardLocal(
        SimpleOpcode.ADD_N, source, InputArgument.of(names, LoadableValue::of));
  }

  @Override
  public Value add_n_a(Value source, Value value) {
    return new StandardLocal(SimpleOpcode.ADD_N_A, source, value);
  }

  @Override
  public Value add_n_i(Value source, Value ordinal) {
    return new StandardLocal(SimpleOpcode.ADD_N_I, source, ordinal);
  }

  @Override
  public Value add_n_r(Value source, Value frame) {
    return new StandardLocal(SimpleOpcode.ADD_N_R, source, frame);
  }

  @Override
  public Value add_n_s(Value source, Value name) {
    return new StandardLocal(SimpleOpcode.ADD_N_S, source, name);
  }

  @Override
  public Value adjacent_f(Value name, Value definition) {
    return new StandardLocal(SimpleOpcode.ADJACENT_F, name, definition);
  }

  @Override
  public Value adjacent_i(Value name, Value definition) {
    return new StandardLocal(SimpleOpcode.ADJACENT_I, name, definition);
  }

  @Override
  public Value adjacent_s(Value name, Value definition) {
    return new StandardLocal(SimpleOpcode.ADJACENT_S, name, definition);
  }

  @Override
  public Value adjacent_z(Value name, Value definition) {
    return new StandardLocal(SimpleOpcode.ADJACENT_Z, name, definition);
  }

  @Override
  public Value alwaysinclude_f(Value name, Value key) {
    return new StandardLocal(SimpleOpcode.ALWAYSINCLUDE_F, name, key);
  }

  @Override
  public Value alwaysinclude_i(Value name, Value key) {
    return new StandardLocal(SimpleOpcode.ALWAYSINCLUDE_I, name, key);
  }

  @Override
  public Value alwaysinclude_s(Value name, Value key) {
    return new StandardLocal(SimpleOpcode.ALWAYSINCLUDE_S, name, key);
  }

  @Override
  public Value alwaysinclude_z(Value name, Value key) {
    return new StandardLocal(SimpleOpcode.ALWAYSINCLUDE_Z, name, key);
  }

  @Override
  public Value and_g(Streamable<Value> groupers) {
    return new StandardLocal(SimpleOpcode.AND_G, InputArgument.of(groupers));
  }

  @Override
  public Value and_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.AND_I, left, right);
  }

  @Override
  public Value atos(Value value) {
    return new StandardLocal(SimpleOpcode.ATOS, value);
  }

  @Override
  public Value atoz(Value value, Predicate<FlabbergastType> include) {
    return new StandardLocal(new TestTypeOpcode(include), value);
  }

  @Override
  public Value boundary(Value definition, Value trailing) {
    return new StandardLocal(SimpleOpcode.BOUNDARY, definition, trailing);
  }

  @Override
  public void br(JvmBlock target, Streamable<Value> arguments) {
    instructions.add(
        new RunnableBranchInstruction(instructions.size(), currentLocation) {
          private final BranchPath targetPoint = new BranchPath(target, arguments);

          @Override
          protected void accessCheckExtra(ErrorCollector errorCollector) {
            // Do nothing.
          }

          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected String opcode() {
            return "br";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return Stream.of(targetPoint);
          }

          @Override
          public void render() {
            loadFuture();
            targetPoint.makeRunnable(JvmBlock.this);
            FUTURE__RESCHEDULE.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            // Do nothing.
          }
        });
  }

  @Override
  public void br_a(Value value, JvmDispatch dispatch, Optional<String> error) {
    instructions.add(
        new MethodBranchInstruction(instructions.size(), currentLocation) {
          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected void extraMethods(ClassVisitor callbackClass, Type callbackType) {
            final var fail =
                new GeneratorAdapter(
                    Opcodes.ACC_PROTECTED,
                    new Method("fail", VOID_TYPE, new Type[] {JSTRING_TYPE}),
                    null,
                    null,
                    callbackClass);
            fail.visitCode();
            fail.loadThis();
            fail.getField(callbackType, "owner", owner.type());
            fail.getField(owner.type(), "future", FUTURE_TYPE);
            fail.loadThis();
            fail.getField(callbackType, "owner", owner.type());
            prepareSource();
            fail.getField(owner.type(), locations.get(location()), SOURCE_REFERENCE_TYPE);
            fail.push(
                dispatch
                    .names()
                    .map(Object::toString)
                    .sorted()
                    .collect(
                        Collectors.joining(
                            " or ",
                            "Expected ",
                            error.map(" "::concat).orElse("") + ", but got ")));
            fail.loadArg(0);
            JSTRING__CONCAT.invoke(fail);
            fail.push(".");
            JSTRING__CONCAT.invoke(fail);
            FUTURE__ERROR.invoke(fail);
            fail.visitInsn(Opcodes.RETURN);
            fail.endMethod();
          }

          @Override
          protected String opcode() {
            return "br.a";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return dispatch.paths();
          }

          @Override
          public void render() {
            loadFuture();
            value.load(JvmBlock.this, InputParameterType.single(A));
            push(JvmBlock.this, WHINY_ANY_CONSUMER_TYPE);
            FUTURE__RESCHEDULE_ANY.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            value.typeCheck(InputParameterType.single(A), location(), errorCollector);
          }
        });
  }

  @Override
  public void br_aa(
      Value left,
      Value right,
      JvmBlock intTarget,
      Streamable<Value> intArguments,
      JvmBlock floatTarget,
      Streamable<Value> floatArguments) {
    instructions.add(
        new MethodBranchInstruction(instructions.size(), currentLocation) {
          private final BranchPath floatBranchPath =
              new BranchPath(floatTarget, floatArguments, F, F);
          private final BranchPath intBranchPath = new BranchPath(intTarget, intArguments, I, I);

          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected void extraMethods(ClassVisitor callbackClass, Type callbackType) {
            // Do nothing
          }

          @Override
          protected String opcode() {
            return "br.aa";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return Stream.of(intBranchPath, floatBranchPath);
          }

          @Override
          public void render() {
            push(JvmBlock.this, LONG_BI_CONSUMER_TYPE);
            loadFuture();
            loadSource();
            left.load(JvmBlock.this, InputParameterType.single(A));
            right.load(JvmBlock.this, InputParameterType.single(A));
            LONG_BI_CONSUMER__DISPATCH_AA.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            left.typeCheck(InputParameterType.single(A), location(), errorCollector);
            right.typeCheck(InputParameterType.single(A), location(), errorCollector);
          }
        });
  }

  @Override
  public void br_fa(Value left, Value right, JvmBlock target, Streamable<Value> arguments) {
    instructions.add(
        new MethodBranchInstruction(instructions.size(), currentLocation) {
          private final BranchPath floatBranchPath = new BranchPath(target, arguments, F);

          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected void extraMethods(ClassVisitor callbackClass, Type callbackType) {
            // Do nothing
          }

          @Override
          protected String opcode() {
            return "br.fa";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return Stream.of(floatBranchPath);
          }

          @Override
          public void render() {
            push(JvmBlock.this, DOUBLE_BI_CONSUMER_TYPE);
            loadFuture();
            loadSource();
            left.load(JvmBlock.this, InputParameterType.single(F));
            right.load(JvmBlock.this, InputParameterType.single(A));
            DOUBLE_BI_CONSUMER__DISPATCH_FA.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            left.typeCheck(InputParameterType.single(F), location(), errorCollector);
            right.typeCheck(InputParameterType.single(A), location(), errorCollector);
          }
        });
  }

  @Override
  public void br_ia(
      Value left,
      Value right,
      JvmBlock intTarget,
      Streamable<Value> intArguments,
      JvmBlock floatTarget,
      Streamable<Value> floatArguments) {
    instructions.add(
        new MethodBranchInstruction(instructions.size(), currentLocation) {
          private final BranchPath floatBranchPath =
              new BranchPath(floatTarget, floatArguments, F, F);
          private final BranchPath intBranchPath = new BranchPath(intTarget, intArguments, I, I);

          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected void extraMethods(ClassVisitor callbackClass, Type callbackType) {
            // Do nothing
          }

          @Override
          protected String opcode() {
            return "br.ia";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return Stream.of(intBranchPath, floatBranchPath);
          }

          @Override
          public void render() {
            push(JvmBlock.this, LONG_BI_CONSUMER_TYPE);
            loadFuture();
            loadSource();
            left.load(JvmBlock.this, InputParameterType.single(I));
            right.load(JvmBlock.this, InputParameterType.single(A));
            LONG_BI_CONSUMER__DISPATCH_IA.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            left.typeCheck(InputParameterType.single(I), location(), errorCollector);
            right.typeCheck(InputParameterType.single(A), location(), errorCollector);
          }
        });
  }

  @Override
  public void br_z(
      Value condition,
      JvmBlock trueTarget,
      Streamable<Value> trueArguments,
      JvmBlock falseTarget,
      Streamable<Value> falseArguments) {
    instructions.add(
        new RunnableBranchInstruction(instructions.size(), currentLocation) {
          private final BranchPath falsePoint = new BranchPath(falseTarget, falseArguments);
          private final BranchPath truePoint = new BranchPath(trueTarget, trueArguments);

          @Override
          protected void accessCheckExtra(ErrorCollector errorCollector) {
            condition.access(block(), index(), errorCollector);
          }

          @Override
          protected JvmBlock block() {
            return JvmBlock.this;
          }

          @Override
          protected String opcode() {
            return "br.z";
          }

          @Override
          protected Stream<BranchPath> paths() {
            return Stream.of(truePoint, falsePoint);
          }

          @Override
          public void render() {
            loadFuture();
            condition.load(JvmBlock.this, InputParameterType.single(Z));
            final var end = output.newLabel();
            final var falsePath = output.newLabel();
            output.ifZCmp(GeneratorAdapter.EQ, falsePath);
            truePoint.makeRunnable(JvmBlock.this);
            output.goTo(end);
            output.mark(falsePath);
            falsePoint.makeRunnable(JvmBlock.this);
            output.mark(end);
            FUTURE__RESCHEDULE.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          protected void typeCheckExtra(ErrorCollector errorCollector) {
            condition.typeCheck(InputParameterType.single(Z), location(), errorCollector);
          }
        });
  }

  @Override
  public Value btoa(Value value) {
    return new StandardLocal(SimpleOpcode.BTOA, value);
  }

  @Override
  public Value buckets_f(Value definition, Value count) {
    return new StandardLocal(SimpleOpcode.BUCKETS_F, definition, count);
  }

  @Override
  public Value buckets_i(Value definition, Value count) {
    return new StandardLocal(SimpleOpcode.BUCKETS_I, definition, count);
  }

  @Override
  public Value buckets_s(Value definition, Value count) {
    return new StandardLocal(SimpleOpcode.BUCKETS_S, definition, count);
  }

  public Value call(CallOpcode callOpcode, InputArgument[] arguments) {
    return new StandardLocal(callOpcode, arguments);
  }

  @Override
  public Value call_d(Value definition, Value context) {
    return new StandardLocal(SimpleOpcode.CALL_D, definition, context);
  }

  @Override
  public Value call_o(Value override, Value context, Value original) {
    return new StandardLocal(SimpleOpcode.CALL_O, override, context, original);
  }

  @Override
  public Value cat_e(Value context, Streamable<Value> chains) {
    return new StandardLocal(SimpleOpcode.CAT_E, context, InputArgument.of(chains));
  }

  @Override
  public Value cat_ke(Value definition, Value chain) {
    return new StandardLocal(SimpleOpcode.CAT_KE, definition, chain);
  }

  @Override
  public Value cat_r(Value context, Value first, Value second) {
    return new StandardLocal(SimpleOpcode.CAT_R, context, first, second);
  }

  @Override
  public Value cat_rc(Value head, Value tail) {
    return new StandardLocal(SimpleOpcode.CAT_RC, head, tail);
  }

  @Override
  public Value cat_s(Value first, Value second) {
    return new StandardLocal(SimpleOpcode.CAT_S, first, second);
  }

  @Override
  public Value chunk_e(Value width) {
    return new StandardLocal(SimpleOpcode.CHUNK_E, width);
  }

  @Override
  public Value cmp_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.CMP_F, left, right);
  }

  @Override
  public Value cmp_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.CMP_I, left, right);
  }

  @Override
  public Value cmp_s(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.CMP_S, left, right);
  }

  @Override
  public Value cmp_z(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.CMP_Z, left, right);
  }

  @Override
  public Value contextual() {
    return new Fixed(LoadableValue.CONTEXTUAL);
  }

  @Override
  public Value count_w(Value count) {
    return new StandardLocal(SimpleOpcode.COUNT_W, count);
  }

  @Override
  public JvmDispatch createDispatch() {
    return new JvmDispatch(this);
  }

  @Override
  public Value crosstab_f(Value key) {
    return new StandardLocal(SimpleOpcode.CROSSTAB_F, key);
  }

  @Override
  public Value crosstab_i(Value key) {
    return new StandardLocal(SimpleOpcode.CROSSTAB_I, key);
  }

  @Override
  public Value crosstab_s(Value key) {
    return new StandardLocal(SimpleOpcode.CROSSTAB_S, key);
  }

  @Override
  public Value crosstab_z(Value key) {
    return new StandardLocal(SimpleOpcode.CROSSTAB_Z, key);
  }

  @Override
  public Value ctr_c(Value value) {
    return new StandardLocal(SimpleOpcode.CTR_C, value);
  }

  @Override
  public Value ctr_r(Value frame) {
    return new StandardLocal(SimpleOpcode.CTR_R, frame);
  }

  @Override
  public Value ctxt_r(Value context, Value frame) {
    return new StandardLocal(SimpleOpcode.CTXT_R, context, frame);
  }

  @Override
  public Value debug_d(Value definition, Value context) {
    return new StandardLocal(SimpleOpcode.DEBUG_D, definition, context);
  }

  @Override
  public Value disc_g_f(Value name, Value getter) {
    return new StandardLocal(SimpleOpcode.DISC_G_F, name, getter);
  }

  @Override
  public Value disc_g_i(Value name, Value getter) {
    return new StandardLocal(SimpleOpcode.DISC_G_I, name, getter);
  }

  @Override
  public Value disc_g_s(Value name, Value getter) {
    return new StandardLocal(SimpleOpcode.DISC_G_S, name, getter);
  }

  @Override
  public Value disc_g_z(Value name, Value getter) {
    return new StandardLocal(SimpleOpcode.DISC_G_Z, name, getter);
  }

  @Override
  public void disperse_i(Value frame, Value name, Value value) {
    instructions.add(new DisperseInstruction(FRAME__DISPERSE_I, frame, I, name, value));
  }

  @Override
  public void disperse_s(Value frame, Value name, Value value) {
    instructions.add(new DisperseInstruction(FRAME__DISPERSE_S, frame, S, name, value));
  }

  @Override
  public Value div_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.DIV_F, left, right);
  }

  @Override
  public Value div_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.DIV_I, left, right);
  }

  @Override
  public Value drop_ed(Value source, Value clause) {
    return new StandardLocal(SimpleOpcode.DROP_ED, source, clause);
  }

  @Override
  public Value drop_ei(Value source, Value count) {
    return new StandardLocal(SimpleOpcode.DROP_EI, source, count);
  }

  @Override
  public Value drop_x(Value name) {
    return new StandardLocal(SimpleOpcode.DROP_X, name);
  }

  @Override
  public Value dropl_ei(Value source, Value count) {
    return new StandardLocal(SimpleOpcode.DROPL_EI, source, count);
  }

  @Override
  public Value duration_f(Value definition, Value duration) {
    return new StandardLocal(SimpleOpcode.DURATION_F, definition, duration);
  }

  @Override
  public Value duration_i(Value definition, Value duration) {
    return new StandardLocal(SimpleOpcode.DURATION_I, definition, duration);
  }

  @Override
  public void error(Value message) {
    instructions.add(
        new Instruction() {
          private final int index = instructions.size();
          private final SourceLocation location = currentLocation;

          @Override
          public void accessCheck(ErrorCollector errorCollector) {
            message.access(JvmBlock.this, index, errorCollector);
          }

          @Override
          public void render() {
            loadFuture();
            loadSource();
            message.load(JvmBlock.this, InputParameterType.single(S));
            OBJECT__TO_STRING.invoke(output);
            FUTURE__ERROR.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          public String toString() {
            return String.format("%s:%s:%d error", owner.name(), name(), index);
          }

          @Override
          public void typeCheck(ErrorCollector errorCollector) {
            message.typeCheck(InputParameterType.single(S), location, errorCollector);
          }
        });
  }

  @Override
  public Value etoa_ao(Value source, Value initial, Value reducer) {
    return new StandardLocal(SimpleOpcode.ETOA_AO, source, initial, reducer);
  }

  @Override
  public Value etoa_d(Value source, Value extractor) {
    return new StandardLocal(SimpleOpcode.ETOA_D, source, extractor);
  }

  @Override
  public Value etoa_dd(Value source, Value extractor, Value alternate) {
    return new StandardLocal(SimpleOpcode.ETOA_DD, source, extractor, alternate);
  }

  @Override
  public Value etod(Value source, Value computeValue) {
    return new StandardLocal(SimpleOpcode.ETOD, source, computeValue);
  }

  @Override
  public Value etod_a(Value source, Value computeValue, Value empty) {
    return new StandardLocal(SimpleOpcode.ETOD_A, source, computeValue, empty);
  }

  @Override
  public Value etoe_g(Value source, Streamable<Value> groupers) {
    return new StandardLocal(SimpleOpcode.ETOE_G, source, InputArgument.of(groupers));
  }

  @Override
  public Value etoe_m(Value source, Value initial, Value reducer) {
    return new StandardLocal(SimpleOpcode.ETOE_M, source, initial, reducer);
  }

  @Override
  public Value etoe_u(Value source, Value flattener) {
    return new StandardLocal(SimpleOpcode.ETOE_U, source, flattener);
  }

  @Override
  public Value etoi(Value source) {
    return new StandardLocal(SimpleOpcode.ETOI, source);
  }

  @Override
  public Value etor_ao(Value source, Value initial, Value reducer) {
    return new StandardLocal(SimpleOpcode.ETOR_AO, source, initial, reducer);
  }

  @Override
  public Value etor_i(Value source, Value computeValue) {
    return new StandardLocal(SimpleOpcode.ETOR_I, source, computeValue);
  }

  @Override
  public Value etor_s(Value source, Value computeName, Value computeValue) {
    return new StandardLocal(SimpleOpcode.ETOR_S, source, computeName, computeValue);
  }

  @Override
  public Value ext(String uri) {
    return new StandardLocal(new ExternalOpcode(uri));
  }

  @Override
  public Value f(double value) {
    return new Fixed(LoadableValue.of(value));
  }

  @Override
  public Value filt_e(Value source, Value clause) {
    return new StandardLocal(SimpleOpcode.FILT_E, source, clause);
  }

  @Override
  public Value ftoa(Value value) {
    return new StandardLocal(SimpleOpcode.FTOA, value);
  }

  @Override
  public Value ftoi(Value value) {
    return new StandardLocal(SimpleOpcode.FTOI, value);
  }

  @Override
  public Value ftos(Value value) {
    return new StandardLocal(SimpleOpcode.FTOS, value);
  }

  @Override
  public Value gather_i(Value frame, Value name) {
    return new StandardLocal(SimpleOpcode.GATHER_I, frame, name);
  }

  @Override
  public Value gather_s(Value frame, Value name) {
    return new StandardLocal(SimpleOpcode.GATHER_S, frame, name);
  }

  public GeneratorAdapter generator() {
    return output;
  }

  @Override
  public Value i(long value) {
    return new Fixed(LoadableValue.of(value));
  }

  @Override
  public Value id(Value frame) {
    return new StandardLocal(SimpleOpcode.ID);
  }

  @Override
  public Value importFunction(String kwsName, KwsType returnType, Streamable<Value> arguments) {
    return new StandardLocal(
        new ImportOpcode(
            returnType, kwsName, arguments.stream().map(Value::type).toArray(KwsType[]::new)),
        InputArgument.of(arguments));
  }

  @Override
  public Value inf_f() {
    return new Fixed(LoadableValue.INF_F);
  }

  @Override
  public Value is_finite(Value value) {
    return new StandardLocal(SimpleOpcode.IS_FINITE, value);
  }

  @Override
  public Value is_nan(Value value) {
    return new StandardLocal(SimpleOpcode.IS_NAN, value);
  }

  @Override
  public Value itoa(Value value) {
    return new StandardLocal(SimpleOpcode.ITOA, value);
  }

  @Override
  public Value itof(Value value) {
    return new StandardLocal(SimpleOpcode.ITOF, value);
  }

  @Override
  public Value itos(Value value) {
    return new StandardLocal(SimpleOpcode.ITOS, value);
  }

  @Override
  public Value itoz(long reference, Value value) {
    return new StandardLocal(SimpleOpcode.ITOZ, InputArgument.of(reference), value);
  }

  @Override
  public Value ktol(Value name, Value context, Value definition) {
    return new StandardLocal(SimpleOpcode.KTOL, name, context, definition);
  }

  @Override
  public Value len_b(Value blob) {
    return new StandardLocal(SimpleOpcode.LEN_B, blob);
  }

  @Override
  public Value len_s(Value str) {
    return new StandardLocal(SimpleOpcode.LEN_S, str);
  }

  @Override
  public Value let_e(Value source, Streamable<Value> builder) {
    return new StandardLocal(SimpleOpcode.LET_E, source, InputArgument.of(builder));
  }

  public void loadFuture() {
    output.loadThis();
    output.getField(owner.type(), "future", FUTURE_TYPE);
  }

  void loadLocation() {
    currentLocation.pushToStack(output);
  }

  public void loadSource() {
    prepareSource();
    output.loadThis();
    output.getField(owner.type(), locations.get(currentLocation), SOURCE_REFERENCE_TYPE);
  }

  @Override
  public Value lookup(Value context, Streamable<String> names) {
    return new StandardLocal(
        SimpleOpcode.LOOKUP, context, InputArgument.of(names, LoadableValue::of));
  }

  @Override
  public Value lookup_l(Value handler, Value context, Value names) {
    return new StandardLocal(SimpleOpcode.LOOKUP_L, handler, context, names);
  }

  @Override
  public Value ltoa(Value value) {
    return new StandardLocal(SimpleOpcode.LTOA, value);
  }

  @Override
  public Value max_f() {
    return new Fixed(LoadableValue.MAX_F);
  }

  @Override
  public Value max_i() {
    return new Fixed(LoadableValue.MAX_I);
  }

  @Override
  public Value max_z() {
    return new Fixed(LoadableValue.MAX_Z);
  }

  public Method method() {
    return new Method(
        id,
        VOID_TYPE,
        Stream.of(parameters).map(Value::type).map(LanguageType::of).toArray(Type[]::new));
  }

  @Override
  public Value min_f() {
    return new Fixed(LoadableValue.MIN_F);
  }

  @Override
  public Value min_i() {
    return new Fixed(LoadableValue.MIN_I);
  }

  @Override
  public Value min_z() {
    return new Fixed(LoadableValue.MIN_Z);
  }

  @Override
  public Value mod_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.MOD_F, left, right);
  }

  @Override
  public Value mod_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.MOD_I, left, right);
  }

  @Override
  public Value mtoe(Value context, Value initial, Value definition) {
    return new StandardLocal(SimpleOpcode.MTOE, context, initial, definition);
  }

  @Override
  public Value mul_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.MUL_F, left, right);
  }

  @Override
  public Value mul_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.MUL_I, left, right);
  }

  public String name() {
    return id;
  }

  @Override
  public Value nan_f() {
    return new Fixed(LoadableValue.NAN_F);
  }

  @Override
  public Value neg_f(Value value) {
    return new StandardLocal(SimpleOpcode.NEG_F, value);
  }

  @Override
  public Value neg_i(Value value) {
    return new StandardLocal(SimpleOpcode.NEG_I, value);
  }

  @Override
  public Value new_g(Value name, Value collector) {
    return new StandardLocal(SimpleOpcode.NEW_G, name, collector);
  }

  @Override
  public Value new_g_a(Value name, Value value) {
    return new StandardLocal(SimpleOpcode.NEW_G_A, name, value);
  }

  @Override
  public Value new_p(Value context, Value intersect, Streamable<Value> zippers) {
    return new StandardLocal(SimpleOpcode.NEW_P, context, intersect, InputArgument.of(zippers));
  }

  @Override
  public Value new_p_i(Value name) {
    return new StandardLocal(SimpleOpcode.NEW_P_I, name);
  }

  @Override
  public Value new_p_r(Value name, Value frame) {
    return new StandardLocal(SimpleOpcode.NEW_P_R, name, frame);
  }

  @Override
  public Value new_p_s(Value name) {
    return new StandardLocal(SimpleOpcode.NEW_P_S, name);
  }

  @Override
  public Value new_r(
      Value selfIsThis, Value context, Streamable<Value> gatherers, Streamable<Value> builder) {
    return new StandardLocal(
        SimpleOpcode.NEW_R,
        selfIsThis,
        context,
        InputArgument.of(gatherers),
        InputArgument.of(builder));
  }

  @Override
  public Value new_r_i(Value context, Value start, Value end) {
    return new StandardLocal(SimpleOpcode.NEW_R_I, context, start, end);
  }

  @Override
  public Value new_t(Value context, Streamable<Value> gatherers, Streamable<Value> builder) {
    return new StandardLocal(
        SimpleOpcode.NEW_T, context, InputArgument.of(gatherers), InputArgument.of(builder));
  }

  @Override
  public Value new_x_ia(Value ordinal, Value value) {
    return new StandardLocal(SimpleOpcode.NEW_X_IA, ordinal, value);
  }

  @Override
  public Value new_x_sa(Value name, Value value) {
    return new StandardLocal(SimpleOpcode.NEW_X_SA, name, value);
  }

  @Override
  public Value new_x_sd(Value name, Value definition) {
    return new StandardLocal(SimpleOpcode.NEW_X_SD, name, definition);
  }

  @Override
  public Value new_x_so(Value name, Value override) {
    return new StandardLocal(SimpleOpcode.NEW_X_SO, name, override);
  }

  @Override
  public Value nil_a() {
    return new Fixed(LoadableValue.NIL_A);
  }

  @Override
  public Value nil_c() {
    return new Fixed(LoadableValue.NIL_C);
  }

  @Override
  public Value nil_n() {
    return new Fixed(LoadableValue.NIL_N);
  }

  @Override
  public Value nil_r() {
    return new Fixed(LoadableValue.NIL_R);
  }

  @Override
  public Value nil_w() {
    return new Fixed(LoadableValue.NIL_W);
  }

  @Override
  public Value not_g(Value value) {
    return new StandardLocal(SimpleOpcode.NOT_G, value);
  }

  @Override
  public Value not_i(Value value) {
    return new StandardLocal(SimpleOpcode.NOT_I, value);
  }

  @Override
  public Value not_z(Value value) {
    return new StandardLocal(SimpleOpcode.NOT_Z, value);
  }

  @Override
  public Value or_g(Streamable<Value> groupers) {
    return new StandardLocal(SimpleOpcode.OR_G, InputArgument.of(groupers));
  }

  @Override
  public Value or_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.OR_I, left, right);
  }

  @Override
  public Value ord_e_f(Value source, Value ascending, Value clause) {
    return new StandardLocal(SimpleOpcode.ORD_E_F, source, ascending, clause);
  }

  @Override
  public Value ord_e_i(Value source, Value ascending, Value clause) {
    return new StandardLocal(SimpleOpcode.ORD_E_I, source, ascending, clause);
  }

  @Override
  public Value ord_e_s(Value source, Value ascending, Value clause) {
    return new StandardLocal(SimpleOpcode.ORD_E_S, source, ascending, clause);
  }

  @Override
  public Value ord_e_z(Value source, Value ascending, Value clause) {
    return new StandardLocal(SimpleOpcode.ORD_E_Z, source, ascending, clause);
  }

  public JvmFunction owner() {
    return owner;
  }

  @Override
  public Value parameter(int index) {
    return parameters[index];
  }

  @Override
  public int parameters() {
    return parameters.length;
  }

  @Override
  public Value powerset(Streamable<Value> groupers) {
    return new StandardLocal(SimpleOpcode.POWERSET, InputArgument.of(groupers));
  }

  private void prepareSource() {
    if (!locations.containsKey(currentLocation)) {
      final var name = String.format("sourceReference_%s_%d", id, locations.size());
      owner
          .classVisitor()
          .visitField(Opcodes.ACC_PRIVATE, name, SOURCE_REFERENCE_TYPE.getDescriptor(), null, null)
          .visitEnd();
      output.loadThis();
      if (owner.kind().needsSourceReference()) {
        output.loadThis();
        output.getField(owner.type(), "sourceReference", SOURCE_REFERENCE_TYPE);
      } else {
        output.getStatic(SOURCE_REFERENCE_TYPE, "EMPTY", SOURCE_REFERENCE_TYPE);
      }
      currentLocation.pushToStack(output);
      output.push(currentMessage);

      SOURCE_REFERENCE__BASIC.invoke(output);
      output.putField(owner.type(), name, SOURCE_REFERENCE_TYPE);
      locations.put(currentLocation, name);
    }
  }

  @Override
  public Value priv_cr(Value context, Value frame) {
    return new StandardLocal(SimpleOpcode.PRIV_CR, context, frame);
  }

  @Override
  public Value priv_x(Value inner) {
    return new StandardLocal(SimpleOpcode.PRIV_X, inner);
  }

  public void render() {
    if (dominatorId == -1) {
      return;
    }
    final var blockMethod = new Method(id, VOID_TYPE, new Type[0]);
    output =
        new GeneratorAdapter(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
            blockMethod,
            null,
            null,
            owner.classVisitor());
    output.visitCode();
    for (final var instruction : instructions) {
      instruction.render();
    }
    output.visitInsn(Opcodes.RETURN);
    output.endMethod();
  }

  @Override
  public Value require_x(Value name) {
    return new StandardLocal(SimpleOpcode.REQUIRE_X, name);
  }

  @Override
  public void ret(Value value) {
    instructions.add(
        new Instruction() {
          private final int index = instructions.size();
          private final SourceLocation location = currentLocation;

          @Override
          public void accessCheck(ErrorCollector errorCollector) {
            value.access(JvmBlock.this, index, errorCollector);
          }

          @Override
          public void render() {
            loadFuture();
            value.load(JvmBlock.this, InputParameterType.single(A));
            FUTURE__COMPLETE.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          public String toString() {
            return String.format("%s:%s:%d ret", owner.name(), name(), index);
          }

          @Override
          public void typeCheck(ErrorCollector errorCollector) {
            value.typeCheck(InputParameterType.single(A), location, errorCollector);
          }
        });
  }

  @Override
  public void ret(Value value, Streamable<Value> builders) {
    instructions.add(
        new Instruction() {
          private final InputArgument builderValue = InputArgument.of(builders);
          private int index = instructions.size();
          private final SourceLocation location = currentLocation;

          @Override
          public void accessCheck(ErrorCollector errorCollector) {
            value.access(JvmBlock.this, index, errorCollector);
            builders
                .stream()
                .forEach(builder -> builder.access(JvmBlock.this, index, errorCollector));
          }

          @Override
          public void render() {
            loadFuture();
            value.load(JvmBlock.this, InputParameterType.single(A));
            builderValue.load(JvmBlock.this, InputParameterType.arrayOf(X));
            ACCUMULATOR__OF.invoke(output);
            FUTURE__COMPLETE.invoke(output);
            output.visitInsn(Opcodes.RETURN);
            output.endMethod();
          }

          @Override
          public String toString() {
            return String.format("%s:%s:%d ret (accumulator)", owner.name(), name(), index);
          }

          @Override
          public void typeCheck(ErrorCollector errorCollector) {
            value.typeCheck(InputParameterType.single(A), location, errorCollector);
            builderValue.typeCheck(InputParameterType.arrayOf(X), location, errorCollector);
          }
        });
  }

  @Override
  public Value rev_e(Value source) {
    return new StandardLocal(SimpleOpcode.REV_E, source);
  }

  @Override
  public Value ring_g(Value primitive, Value size) {
    return new StandardLocal(SimpleOpcode.RING_G, primitive, size);
  }

  @Override
  public Value rtoa(Value value) {
    return new StandardLocal(SimpleOpcode.RTOA, value);
  }

  @Override
  public Value rtoe(Value source, Value context) {
    return new StandardLocal(SimpleOpcode.RTOE, source, context);
  }

  @Override
  public Value s(String value) {
    return new Fixed(LoadableValue.of(value));
  }

  @Override
  public Value seal_d(Value definition, Value context) {
    return new StandardLocal(SimpleOpcode.SEAL_D, definition, context);
  }

  @Override
  public Value seal_o(Value definition, Value context) {
    return new StandardLocal(SimpleOpcode.SEAL_O, definition, context);
  }

  @Override
  public Value session_f(Value definition, Value adjacent, Value maximum) {
    return new StandardLocal(SimpleOpcode.SESSION_F, definition, adjacent, maximum);
  }

  @Override
  public Value session_i(Value definition, Value adjacent, Value maximum) {
    return new StandardLocal(SimpleOpcode.SESSION_I, definition, adjacent, maximum);
  }

  public void setDominatorId(AtomicInteger dominatorCounter) {
    if (dominatorId == -1) {
      dominatorId = dominatorCounter.getAndIncrement();
      instructions
          .get(instructions.size() - 1)
          .dominators()
          .forEach(b -> b.setDominatorId(dominatorCounter));
    }
    instructions
        .get(instructions.size() - 1)
        .dominators()
        .forEach(b -> b.predecessors.set(dominatorId));
  }

  @Override
  public Value sh_i(Value value, Value offset) {
    return new StandardLocal(SimpleOpcode.SH_I, value, offset);
  }

  @Override
  public Value shuf_e(Value source) {
    return new StandardLocal(SimpleOpcode.SHUF_E, source);
  }

  @Override
  public Value stoa(Value value) {
    return new StandardLocal(SimpleOpcode.STOA, value);
  }

  @Override
  public Value stripe_e(Value width) {
    return new StandardLocal(SimpleOpcode.STRIPE_E, width);
  }

  @Override
  public Value sub_f(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.SUB_F, left, right);
  }

  @Override
  public Value sub_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.SUB_I, left, right);
  }

  @Override
  public Value take_ed(Value source, Value clause) {
    return new StandardLocal(SimpleOpcode.TAKE_ED, source, clause);
  }

  @Override
  public Value take_ei(Value source, Value count) {
    return new StandardLocal(SimpleOpcode.TAKE_EI, source, count);
  }

  @Override
  public Value takel_ei(Value source, Value count) {
    return new StandardLocal(SimpleOpcode.TAKEL_EI, source, count);
  }

  @Override
  public Value ttoa(Value value) {
    return new StandardLocal(SimpleOpcode.TTOA, value);
  }

  public void typeCheck(ErrorCollector errorCollector) {
    for (final var instruction : instructions) {
      instruction.typeCheck(errorCollector);
    }
  }

  @Override
  public void update(SourceLocation location, String message) {
    if (firstLocation == null) {
      firstLocation = location;
    }
    currentLocation = location;
    currentMessage = message;
  }

  @Override
  public Value window_g(Value length, Value next) {
    return new StandardLocal(SimpleOpcode.WINDOW_G, length, next);
  }

  @Override
  public Value xor_i(Value left, Value right) {
    return new StandardLocal(SimpleOpcode.XOR_I, left, right);
  }

  @Override
  public Value ztoa(Value value) {
    return new StandardLocal(SimpleOpcode.ZTOA, value);
  }

  @Override
  public Value ztos(Value value) {
    return new StandardLocal(SimpleOpcode.ZTOS, value);
  }
}
