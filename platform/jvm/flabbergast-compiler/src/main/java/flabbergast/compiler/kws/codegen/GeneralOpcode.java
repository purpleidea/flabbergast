package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.kws.KwsType;
import java.util.function.IntConsumer;

interface GeneralOpcode {

  boolean isCallback();

  String opcode();

  InputParameterType parameter(int i);

  int parameters();

  void render(JvmBlock output, int index, Runnable loadCallback, IntConsumer values);

  KwsType returnType();
}
