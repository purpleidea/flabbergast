package flabbergast;

import org.objectweb.asm.MethodVisitor;

class IntConstant extends LoadableValue {
  private final long number;

  public IntConstant(long number) {
    this.number = number;
  }

  @Override
  public Class<?> getBackingType() {
    return long.class;
  }

  @Override
  public void load(MethodVisitor generator) {
    generator.visitLdcInsn(number);
  }
}
