package flabbergast;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class UpgradeValue extends LoadableValue {
    private final LoadableValue original;

    public UpgradeValue(LoadableValue original) {
        this.original = original;
    }

    @Override
    public Class<?> getBackingType() {
        return double.class;
    }

    @Override
    public void load(MethodVisitor generator) throws Exception {
        original.load(generator);
        if (original.getBackingType() == long.class) {
            generator.visitInsn(Opcodes.L2D);
        }
    }
}
