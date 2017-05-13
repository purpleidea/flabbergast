package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.LanguageType.FUTURE_TYPE;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.compiler.kws.KwsFactory;
import flabbergast.compiler.kws.KwsType;
import flabbergast.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public abstract class JvmBuildCollector
    implements ErrorCollector,
        AutoCloseable,
        KwsFactory<Value, JvmBlock, JvmDispatch, JvmFunction> {
  public static final int BC_VERSION = Opcodes.V11;
  private static final Method EXPORT_METHOD =
      new Method("Export to TaskMaster", Type.VOID_TYPE, new Type[] {FUTURE_TYPE});
  private final ErrorCollector errorCollector;
  private GeneratorAdapter exporter;
  private final FunctionKind rootFunctionKind;
  private final List<JvmFunction> functions = new ArrayList<>();
  private boolean ok = true;
  private final String packageName;
  private Type rootType;
  private final String sourceFile;

  public JvmBuildCollector(
      ErrorCollector collector,
      String packageName,
      String sourceFile,
      FunctionKind rootFunctionKind) {
    errorCollector =
        new ErrorCollector() {

          @Override
          public void emitConflict(String error, Stream<Pair<SourceLocation, String>> locations) {
            collector.emitConflict(error, locations);
            ok = false;
          }

          @Override
          public void emitError(SourceLocation location, String error) {
            collector.emitError(location, error);
            ok = false;
          }
        };
    this.packageName = packageName;
    this.rootFunctionKind = rootFunctionKind;
    this.sourceFile = sourceFile;
  }

  @Override
  public final void close() throws Exception {
    for (final var function : functions) {
      function.dominatorAnalysis();
    }
    if (!ok) return;
    for (final var function : functions) {
      function.accessCheck(errorCollector);
    }
    if (!ok) return;
    for (final var function : functions) {
      function.typeCheck(errorCollector);
    }
    if (!ok) return;
    for (final var function : functions) {
      function.render();
    }
    finishOutput();
  }

  @Override
  public final JvmFunction createAccumulator(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    final var function =
        new JvmFunction(
            this,
            export,
            StandardFunctionKind.ACCUMULATOR,
            name,
            Type.getObjectType(packageName + name),
            entryBlockName,
            captures.collect(Collectors.toList()));
    functions.add(function);
    return function;
  }

  public abstract ClassVisitor createClass();

  @Override
  public final JvmFunction createCollector(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    final var function =
        new JvmFunction(
            this,
            export,
            StandardFunctionKind.COLLECTOR,
            name,
            Type.getObjectType(packageName + name),
            entryBlockName,
            captures.collect(Collectors.toList()));
    functions.add(function);
    return function;
  }

  @Override
  public final JvmFunction createDefinition(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    final var function =
        new JvmFunction(
            this,
            export,
            StandardFunctionKind.DEFINITION,
            name,
            Type.getObjectType(packageName + name),
            entryBlockName,
            captures.collect(Collectors.toList()));
    functions.add(function);
    return function;
  }

  @Override
  public final JvmFunction createDistributor(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    final var function =
        new JvmFunction(
            this,
            export,
            StandardFunctionKind.DISTRIBUTOR,
            name,
            Type.getObjectType(packageName + name),
            entryBlockName,
            captures.collect(Collectors.toList()));
    functions.add(function);
    return function;
  }

  @Override
  public final JvmFunction createFile(SourceLocation location, String name, String entryBlockName) {
    if (rootType != null) {
      throw new IllegalStateException("Cannot create two root functions for one library.");
    }
    rootType = Type.getObjectType(packageName + name);
    final var function =
        new JvmFunction(this, false, rootFunctionKind, null, rootType, entryBlockName, List.of()) {

          @Override
          public void finish() {
            exporter.visitMaxs(0, 0);
            exporter.visitEnd();
            super.finish();
          }

          @Override
          protected void prepareExports(Type creatorType, GeneratorAdapter methodGen) {
            exporter =
                new GeneratorAdapter(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                    EXPORT_METHOD,
                    null,
                    null,
                    classVisitor);
            exporter.visitCode();
            methodGen.loadThis();
            methodGen.loadArg(0);
            methodGen.invokeVirtual(creatorType, EXPORT_METHOD);
          }
        };
    functions.add(function);
    return function;
  }

  @Override
  public final JvmFunction createOverride(
      SourceLocation location,
      String name,
      boolean export,
      String entryBlockName,
      Stream<KwsType> captures) {
    final var function =
        new JvmFunction(
            this,
            export,
            StandardFunctionKind.OVERRIDE,
            name,
            Type.getObjectType(packageName + name),
            entryBlockName,
            captures.collect(Collectors.toList()));
    functions.add(function);
    return function;
  }

  @Override
  public final void emitConflict(String error, Stream<Pair<SourceLocation, String>> locations) {
    errorCollector.emitConflict(error, locations);
  }

  @Override
  public final void emitError(SourceLocation location, String error) {
    errorCollector.emitError(location, error);
  }

  final GeneratorAdapter export() {
    return exporter;
  }

  protected abstract void finishOutput() throws Exception;

  protected Type rootType() {
    return rootType;
  }

  public String sourceFile() {
    return sourceFile;
  }
}
