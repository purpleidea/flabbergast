package flabbergast;

import org.objectweb.asm.MethodVisitor;

abstract class LoadableValue {
	public static LoadableValue NULL_LIST = new NullValue(Context.class);
	public static LoadableValue NULL_FRAME = new NullValue(Frame.class);
	public abstract Class <?> getBackingType();
	public abstract void load(MethodVisitor generator);
	public void load(Generator generator) {
		load(generator.getBuilder());
	}
}
