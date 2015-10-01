package flabbergast;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class UnitConstant extends LoadableValue {
    public final static UnitConstant NULL = new UnitConstant();

    private UnitConstant() {
    }

    @Override
    public Class<?> getBackingType() {
        return Unit.class;
    }

    @Override
    public void load(MethodVisitor generator) {
        generator.visitFieldInsn(Opcodes.GETSTATIC,
                                 getInternalName(Unit.class), "NULL", getDescriptor(Unit.class));
    }
}
