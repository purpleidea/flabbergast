package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.LanguageType.JOBJECT_TYPE;
import static flabbergast.compiler.kws.codegen.LanguageType.LIBRARY_TYPE;

import flabbergast.compiler.kws.KwsType;
import flabbergast.compiler.kws.ResultType;
import flabbergast.export.Library;
import flabbergast.lang.RootDefinition;
import java.time.Instant;
import java.util.stream.Stream;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public interface FunctionKind {
  FunctionKind ROOT_DEFINITION =
      new RootFunctionKind(RootDefinition.class) {
        @Override
        public void callSuperConstructor(GeneratorAdapter constructor) {
          constructor.invokeConstructor(
              JOBJECT_TYPE, new Method("<init>", Type.VOID_TYPE, new Type[] {}));
        }
      };

  static FunctionKind library(String libraryName, Instant libraryModificationTime) {
    return new RootFunctionKind(Library.class) {
      @Override
      public void callSuperConstructor(GeneratorAdapter constructor) {
        constructor.push(libraryModificationTime.toEpochMilli());
        constructor.push(libraryName);
        constructor.invokeConstructor(
            LIBRARY_TYPE,
            new Method(
                "<init>", Type.VOID_TYPE, new Type[] {Type.LONG_TYPE, Type.getType(String.class)}));
      }
    };
  }

  void callSuperConstructor(GeneratorAdapter constructor);

  boolean isInterface();

  Type jvmType();

  String methodName();

  boolean needsSourceReference();

  KwsType parameter(int index);

  int parameterCount();

  Stream<KwsType> parameters();

  ResultType resultType();

  KwsType type();
}
