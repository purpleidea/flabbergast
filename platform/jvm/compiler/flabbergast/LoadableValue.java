package flabbergast;

import org.objectweb.asm.MethodVisitor;

abstract class LoadableValue {
    public static LoadableValue NULL_FRAME = new NullValue(Frame.class);
    public static LoadableValue NULL_LIST = new NullValue(Context.class);

    public abstract Class<?> getBackingType();

    public void load(Generator generator) throws Exception {
        load(generator.getBuilder());
    }

    public abstract void load(MethodVisitor generator) throws Exception;
}
