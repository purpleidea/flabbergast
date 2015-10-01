package flabbergast;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ReplGenerator extends Generator {
    public interface Block {
        void invoke(Generator generator, LoadableValue root_frame,
                    LoadableValue current_frame, LoadableValue update_current,
                    LoadableValue escape_value, LoadableValue print_value)
        throws Exception;
    }
    private final FieldValue current_frame;
    private final FieldValue escape_value;
    private final FieldValue print_value;
    private final FieldValue root_frame;
    private final FieldValue update_current;

    ReplGenerator(AstNode node, CompilationUnit<?> owner,
                  ClassVisitor type_builder, String class_name)
    throws NoSuchMethodException, NoSuchFieldException {
        super(node, owner, type_builder, class_name, class_name,
              new HashSet<String>());

        // Create fields for all information provided by the caller.
        root_frame = makeField("root", Frame.class);
        current_frame = makeField("current", Frame.class);
        update_current = makeField("update_current", ConsumeResult.class);
        escape_value = makeField("escape_value", ConsumeResult.class);
        print_value = makeField("print_value", ConsumeResult.class);
        Class<?>[] construct_params = new Class<?>[] {TaskMaster.class,
                Frame.class, Frame.class, ConsumeResult.class,
                ConsumeResult.class, ConsumeResult.class
                                                     };
        FieldValue[] initial_information = new FieldValue[] {root_frame,
                current_frame, update_current, escape_value, print_value
                                                            };

        MethodVisitor ctor_builder = type_builder.visitMethod(
                                         Opcodes.ACC_PUBLIC, "<init>",
                                         makeSignature(null, construct_params), null, null);
        ctor_builder.visitCode();
        ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
        ctor_builder.visitVarInsn(Opcodes.ALOAD, 1);
        ctor_builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                     getInternalName(Computation.class), "<init>",
                                     makeSignature(null, TaskMaster.class));
        for (int it = 0; it < initial_information.length; it++) {
            ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
            ctor_builder.visitVarInsn(Opcodes.ALOAD, it + 2);
            initial_information[it].store(ctor_builder);
        }
        ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
        ctor_builder.visitInsn(Opcodes.ICONST_0);
        ctor_builder.visitFieldInsn(Opcodes.PUTFIELD, class_name, "state",
                                    getDescriptor(int.class));
        createInterlock(ctor_builder);
        ctor_builder.visitInsn(Opcodes.RETURN);
        ctor_builder.visitMaxs(0, 0);
        ctor_builder.visitEnd();
    }

    LoadableValue getCurrentFrame() {
        return current_frame;
    }

    LoadableValue getEscapeValue() {
        return escape_value;
    }

    LoadableValue getPrintValue() {
        return print_value;
    }

    LoadableValue getRootFrame() {
        return root_frame;
    }

    LoadableValue getUpdateCurrent() {
        return update_current;
    }
}
