package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import flabbergast.lang.RootDefinition;
import java.util.stream.Stream;
import org.objectweb.asm.Type;

abstract class RootFunctionKind implements FunctionKind {
  private final Class<?> clazz;

  public RootFunctionKind(Class<? extends RootDefinition> clazz) {
    this.clazz = clazz;
  }

  @Override
  public final boolean isInterface() {
    return clazz.isInterface();
  }

  @Override
  public final Type jvmType() {
    return Type.getType(clazz);
  }

  @Override
  public final String methodName() {
    return "launch";
  }

  @Override
  public final boolean needsSourceReference() {
    return false;
  }

  @Override
  public final KwsType parameter(int index) {
    return null;
  }

  @Override
  public final int parameterCount() {
    return 0;
  }

  @Override
  public final Stream<KwsType> parameters() {
    return Stream.empty();
  }

  @Override
  public final ResultType resultType() {
    return ResultType.ANY;
  }

  @Override
  public final KwsType type() {
    return null;
  }
}
