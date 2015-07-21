package flabbergast;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class RootGenerator extends Generator {

	RootGenerator(AstNode node, CompilationUnit<?> owner,
			ClassVisitor type_builder, String class_name)
			throws NoSuchMethodException, NoSuchFieldException {
		super(node, owner, type_builder, class_name, class_name,
				new HashSet<String>());

		MethodVisitor ctor_builder = type_builder.visitMethod(
				Opcodes.ACC_PUBLIC, "<init>",
				makeSignature(null, TaskMaster.class), null, null);
		ctor_builder.visitCode();
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 1);
		ctor_builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
				getInternalName(Computation.class), "<init>",
				makeSignature(null, TaskMaster.class));
		ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
		ctor_builder.visitInsn(Opcodes.ICONST_0);
		ctor_builder.visitFieldInsn(Opcodes.PUTFIELD, class_name, "state",
				getDescriptor(int.class));
		createInterlock(ctor_builder);
		ctor_builder.visitInsn(Opcodes.RETURN);
		ctor_builder.visitMaxs(0, 0);
		ctor_builder.visitEnd();
	}
}
