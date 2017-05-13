package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.ErrorCollector;
import java.util.stream.Stream;

interface Instruction {
  void accessCheck(ErrorCollector errorCollector);

  default Stream<JvmBlock> dominators() {
    return Stream.empty();
  }

  void render();

  void typeCheck(ErrorCollector errorCollector);
}
