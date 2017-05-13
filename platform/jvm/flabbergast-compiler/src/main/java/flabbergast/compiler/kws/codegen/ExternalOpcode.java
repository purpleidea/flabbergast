package flabbergast.compiler.kws.codegen;

import static flabbergast.compiler.kws.codegen.Invokable.virtualMethod;
import static flabbergast.compiler.kws.codegen.LanguageType.*;
import static org.objectweb.asm.Type.*;

import flabbergast.compiler.kws.KwsType;
import java.util.function.IntConsumer;

final class ExternalOpcode implements GeneralOpcode {

  public ExternalOpcode(String uri) {
    this.uri = uri;
  }

  @Override
  public boolean isCallback() {
    return true;
  }

  @Override
  public String opcode() {
    return "ext " + uri;
  }

  private final String uri;

  @Override
  public InputParameterType parameter(int i) {
    return null;
  }

  @Override
  public int parameters() {
    return 0;
  }

  private static final Invokable FUTURE__EXTERNAL =
      virtualMethod(
          FUTURE_TYPE, VOID_TYPE, "external", JSTRING_TYPE, SOURCE_REFERENCE_TYPE, CONSUMER_TYPE);

  @Override
  public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
    output.loadFuture();
    output.loadSource();

    output.generator().push(uri);
    loadCallback.run();
    output.loadSource();
    FUTURE__EXTERNAL.invoke(output.generator());
  }

  @Override
  public KwsType returnType() {
    return KwsType.A;
  }
}
