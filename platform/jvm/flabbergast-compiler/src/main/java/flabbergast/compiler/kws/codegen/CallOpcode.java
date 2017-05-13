package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.kws.KwsType;
import java.util.function.IntConsumer;

final class CallOpcode implements GeneralOpcode {

  private final JvmFunction function;

  public CallOpcode(JvmFunction function) {
    this.function = function;
  }

  @Override
  public boolean isCallback() {
    return true;
  }

  @Override
  public String opcode() {
    return function.name();
  }

  @Override
  public InputParameterType parameter(int i) {
    return InputParameterType.single(function.capture(i).type());
  }

  @Override
  public int parameters() {
    return function.captures();
  }

  @Override
  public void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values) {
    output.generator().newInstance(function.type());
    output.generator().dup();
    for (var i = 0; i < function.captures(); i++) {
      values.accept(i);
    }
    output.generator().invokeConstructor(function.type(), function.creatorMethod());
  }

  @Override
  public KwsType returnType() {
    return function.kwsType();
  }
}
