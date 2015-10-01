package flabbergast;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class FunctionGenerator extends Generator {
    private final FieldValue initial_container;
    private final FieldValue initial_context;
    private final FieldValue initial_original;
    private final FieldValue initial_self;
    private final FieldValue initial_source_reference;

    FunctionGenerator(AstNode node, CompilationUnit<?> owner,
                      ClassVisitor type_builder, boolean has_original, String class_name,
                      String root_prefix, Set<String> owner_externals)
    throws NoSuchMethodException, NoSuchFieldException,
        SecurityException {
        super(node, owner, type_builder, class_name, root_prefix,
              owner_externals);

        initial_source_reference = makeField("source_reference",
                                             SourceReference.class);
        initial_context = makeField("context", Context.class);
        initial_self = makeField("self", Frame.class);
        initial_container = makeField("container", Frame.class);
        FieldValue original = has_original ? makeField("original",
                              Computation.class) : null;

        Class<?>[] construct_params = new Class<?>[] {TaskMaster.class,
                SourceReference.class, Context.class, Frame.class, Frame.class,
                has_original ? Computation.class : null
                                                     };
        FieldValue[] initial_information = new FieldValue[] {
            initial_source_reference, initial_context, initial_self,
            initial_container, original
        };

        // Create a constructor the takes all the state information provided by
        // the
        // caller and stores it in appropriate fields.
        MethodVisitor ctor_builder = type_builder.visitMethod(
                                         Opcodes.ACC_PUBLIC, "<init>",
                                         makeSignature(null, construct_params), null, null);
        ctor_builder.visitCode();
        ctor_builder.visitVarInsn(Opcodes.ALOAD, 0);
        ctor_builder.visitVarInsn(Opcodes.ALOAD, 1);
        ctor_builder.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                     getInternalName(Computation.class), "<init>", Type
                                     .getConstructorDescriptor(Computation.class
                                             .getConstructors()[0]));
        for (int it = 0; it < initial_information.length; it++) {
            if (initial_information[it] == null)
                continue;
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

        String init_name = class_name + "$Initialiser";
        ClassVisitor init_class = owner.defineClass(Opcodes.ACC_PUBLIC,
                                  init_name, Object.class, has_original
                                  ? ComputeOverride.class
                                  : ComputeValue.class);
        MethodVisitor init_ctor = init_class.visitMethod(Opcodes.ACC_PUBLIC,
                                  "<init>", "()V", null, null);
        init_ctor.visitCode();
        init_ctor.visitVarInsn(Opcodes.ALOAD, 0);
        init_ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                  getInternalName(Object.class), "<init>", "()V");
        init_ctor.visitInsn(Opcodes.RETURN);
        init_ctor.visitMaxs(0, 0);
        init_ctor.visitEnd();

        // Create a static method that wraps the constructor. This is needed to
        // create a delegate.
        MethodVisitor init_builder = init_class.visitMethod(Opcodes.ACC_PUBLIC,
                                     "invoke", makeSignature(Computation.class, construct_params),
                                     null, null);
        init_builder.visitCode();
        if (has_original) {
            // If the thing we are overriding is null, create an error and give
            // up.
            Label has_instance = new Label();
            init_builder.visitVarInsn(Opcodes.ALOAD, construct_params.length);
            init_builder.visitJumpInsn(Opcodes.IFNONNULL, has_instance);
            init_builder.visitVarInsn(Opcodes.ALOAD, 1);
            init_builder.visitVarInsn(Opcodes.ALOAD, 2);
            init_builder
            .visitLdcInsn("Cannot perform override. No value in source frame to override!");
            init_builder.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                         getInternalName(TaskMaster.class), "reportOtherError", Type
                                         .getMethodDescriptor(TaskMaster.class.getMethod(
                                                 "reportOtherError", SourceReference.class,
                                                 String.class)));
            init_builder.visitInsn(Opcodes.ACONST_NULL);
            init_builder.visitInsn(Opcodes.ARETURN);
            init_builder.visitLabel(has_instance);
        }
        init_builder.visitTypeInsn(Opcodes.NEW, class_name);
        init_builder.visitInsn(Opcodes.DUP);
        for (int it = 0; it < construct_params.length; it++) {
            if (construct_params[it] == null)
                continue;
            init_builder.visitVarInsn(Opcodes.ALOAD, it + 1);
        }
        init_builder.visitMethodInsn(Opcodes.INVOKESPECIAL, class_name,
                                     "<init>", makeSignature(null, construct_params));

        init_builder.visitInsn(Opcodes.ARETURN);
        init_builder.visitMaxs(0, 0);
        init_builder.visitEnd();
        init_class.visitEnd();

        if (has_original) {
            startInterlock(1);
            original.load(builder);
            initial_original = makeField("initial_original", Object.class);
            generateConsumeResult(initial_original);
            visitMethod(Computation.class.getMethod("listen",
                                                    ConsumeResult.class));
            stopInterlock();
        } else {
            initial_original = null;
        }
    }

    /**
     * The “Container” provided by from the caller.
     */
    public FieldValue getInitialContainerFrame() {
        return initial_container;
    }

    /**
     * The lookup context provided by the caller.
     */
    public FieldValue getInitialContext() {
        return initial_context;
    }

    /**
     * A static method capable of creating a new instance of the class.
     */
    public DelegateValue getInitialiser() {
        return new DelegateValue(class_name + "$Initialiser",
                                 initial_original == null
                                 ? ComputeValue.class
                                 : ComputeOverride.class);
    }

    /**
     * The original value to an function, null otherwise.
     */
    public FieldValue getInitialOriginal() {
        return initial_original;
    }

    /**
     * The “This” frame provided by the caller.
     */
    public FieldValue getInitialSelfFrame() {
        return initial_self;
    }

    /**
     * The source reference of the caller of this function.
     */
    public FieldValue getInitialSourceReference() {
        return initial_source_reference;
    }
}
