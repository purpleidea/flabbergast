package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.LanguageType.JOBJECT_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import java.util.stream.Stream;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public enum StandardFunctionKind implements FunctionKind {
  ACCUMULATOR(KwsType.M, ResultType.ACCUMULATOR, KwsType.C, KwsType.A),
  DEFINITION(KwsType.D, ResultType.ANY, KwsType.C),
  DISTRIBUTOR(KwsType.U, ResultType.FRICASSEE, KwsType.C),
  COLLECTOR(KwsType.K, ResultType.ANY, KwsType.C, KwsType.E),
  OVERRIDE(KwsType.O, ResultType.ANY, KwsType.C, KwsType.A);
  private static final Method JOBJECT__CTOR = new Method("<init>", VOID_TYPE, new Type[] {});
  private final KwsType[] entryParameters;
  private final ResultType resultType;
  private final KwsType type;

  StandardFunctionKind(KwsType type, ResultType resultType, KwsType... entryParameters) {
    this.resultType = resultType;
    this.type = type;
    this.entryParameters = entryParameters;
  }

  @Override
  public void callSuperConstructor(GeneratorAdapter constructor) {
    constructor.invokeConstructor(JOBJECT_TYPE, JOBJECT__CTOR);
  }

  @Override
  public boolean isInterface() {
    return true;
  }

  @Override
  public boolean needsSourceReference() {
    return false;
  }

  @Override
  public Type jvmType() {
    return LanguageType.of(type);
  }

  @Override
  public String methodName() {
    return "invoke";
  }

  @Override
  public KwsType parameter(int index) {
    return entryParameters[index];
  }

  @Override
  public int parameterCount() {
    return entryParameters.length;
  }

  @Override
  public Stream<KwsType> parameters() {
    return Stream.of(entryParameters);
  }

  @Override
  public ResultType resultType() {
    return resultType;
  }

  @Override
  public KwsType type() {
    return type;
  }
}
