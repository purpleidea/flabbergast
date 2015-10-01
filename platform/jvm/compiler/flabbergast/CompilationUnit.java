package flabbergast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A handle for generating the needed components in an assembly.
 */
public abstract class CompilationUnit<T> {
    /**
     * A call back that will populate a function with generated code.
     */
    public interface FunctionBlock {
        void invoke(Generator generator, LoadableValue source_reference,
                    LoadableValue context, LoadableValue self,
                    LoadableValue container) throws Exception;
    }

    /**
     * A call back that will populate a function with generated code.
     */
    public interface FunctionOverrideBlock {
        void invoke(Generator generator, LoadableValue source_reference,
                    LoadableValue context, LoadableValue self,
                    LoadableValue container, LoadableValue original)
        throws Exception;
    }

    Set<String> externals = new HashSet<String>();

    /**
     * Functions and functions we have generated before.
     *
     * Since the surrounding syntax cannot affect a function, we cache the
     * functions to avoid regenerating them.
     */
    private final Map<String, DelegateValue> functions = new HashMap<String, DelegateValue>();

    /**
     * For generating unique class names.
     */
    private final Map<AstNode, Integer> id_gen = new HashMap<AstNode, Integer>();

    /**
     * Create a new function, and use the provided block to fill it with code.
     *
     * @throws Exception
     */
    DelegateValue createFunction(AstNode node, String syntax_id,
                                 FunctionBlock block, String root_prefix, Set<String> owner_externals)
    throws Exception {
        generateId(node);
        String name = root_prefix + "$Function" + id_gen.get(node) + syntax_id;
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        FunctionGenerator generator = createFunctionGenerator(node, name,
                                      false, root_prefix, owner_externals);
        block.invoke(generator, generator.getInitialContainerFrame(),
                     generator.getInitialContext(), generator.getInitialSelfFrame(),
                     generator.getInitialSourceReference());
        generator.generateSwitchBlock(false);
        DelegateValue initialiser = generator.getInitialiser();
        functions.put(name, initialiser);

        return initialiser;
    }

    private FunctionGenerator createFunctionGenerator(AstNode node,
            String name, boolean has_original, String root_prefix,
            Set<String> owner_externals) throws NoSuchMethodException,
        NoSuchFieldException, SecurityException {
        ClassVisitor type_builder = defineClass(Opcodes.ACC_FINAL, name,
                                                Computation.class);
        type_builder.visitSource(node.getFileName(), null);
        return new FunctionGenerator(node, this, type_builder, has_original,
                                     name, root_prefix, owner_externals);
    }

    DelegateValue createFunctionOverride(AstNode node, String syntax_id,
                                         FunctionOverrideBlock block, String root_prefix,
                                         Set<String> owner_externals) throws Exception {
        generateId(node);
        String name = root_prefix + "$Override" + id_gen.get(node) + syntax_id;
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        FunctionGenerator generator = createFunctionGenerator(node, name, true,
                                      root_prefix, owner_externals);
        block.invoke(generator, generator.getInitialContainerFrame(),
                     generator.getInitialContext(), generator.getInitialOriginal(),
                     generator.getInitialSelfFrame(),
                     generator.getInitialSourceReference());
        generator.generateSwitchBlock(false);
        DelegateValue initialiser = generator.getInitialiser();
        functions.put(name, initialiser);

        return initialiser;
    }

    T createReplGenerator(AstNode node, String name, ReplGenerator.Block block)
    throws Exception {
        ClassVisitor type_builder = defineClass(Opcodes.ACC_FINAL
                                                | Opcodes.ACC_PUBLIC, name, Computation.class);
        type_builder.visitSource(node.getFileName(), null);
        ReplGenerator generator = new ReplGenerator(node, this, type_builder,
                name);
        block.invoke(generator, generator.getRootFrame(),
                     generator.getCurrentFrame(), generator.getUpdateCurrent(),
                     generator.getEscapeValue(), generator.getPrintValue());
        generator.generateSwitchBlock(true);
        return doMagic(name);
    }

    T createRootGenerator(AstNode node, String name, Generator.Block block)
    throws Exception {
        ClassVisitor type_builder = defineClass(Opcodes.ACC_FINAL
                                                | Opcodes.ACC_PUBLIC, name, Computation.class);
        type_builder.visitSource(node.getFileName(), null);
        RootGenerator generator = new RootGenerator(node, this, type_builder,
                name);
        block.invoke(generator);
        generator.generateSwitchBlock(true);
        return doMagic(name);
    }

    public abstract ClassVisitor defineClass(int acccess, String class_name,
            Class<?> superclass, Class<?>... interfaces);

    protected abstract T doMagic(String name);

    /**
     * Create a new function, and use the provided block to fill it with code.
     */
    private void generateId(AstNode node) {
        if (!id_gen.containsKey(node)) {
            id_gen.put(node, id_gen.size());
        }
    }

    public Iterable<String> getExternalUris() {
        return externals;
    }
}
