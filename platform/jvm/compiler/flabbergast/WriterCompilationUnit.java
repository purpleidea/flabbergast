package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.io.File;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class WriterCompilationUnit extends CompilationUnit<Boolean> {
	private boolean compute_frames;

	WriterCompilationUnit(boolean compute_frames) {
		this.compute_frames = compute_frames;
	}

	@Override
	public ClassVisitor defineClass(int access, String class_name,
			Class<?> superclass, Class<?>... interfaces) {
		AutoWriteClassVisitor visitor = new AutoWriteClassVisitor(
				new ClassWriter(ClassWriter.COMPUTE_MAXS
						| (compute_frames ? ClassWriter.COMPUTE_FRAMES : 0))) {
			@Override
			public void visit(int version, int access, String class_name,
					String signature, String super_name, String[] interfaces) {
				super.visit(version, access, class_name, signature, super_name,
						interfaces);
				fileEvent(target);
			}

		};
		String[] interface_names = new String[interfaces.length];
		for (int it = 0; it < interfaces.length; it++) {
			interface_names[it] = getInternalName(interfaces[it]);
		}
		visitor.visit(Opcodes.V1_5, access, class_name, null,
				getInternalName(superclass), interface_names);
		return visitor;
	}

	@Override
	protected Boolean doMagic(String name) {
		return true;
	}

	protected void fileEvent(File file) {

	}
}
