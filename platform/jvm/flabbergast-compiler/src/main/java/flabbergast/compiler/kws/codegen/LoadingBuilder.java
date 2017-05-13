package flabbergast.compiler.kws.codegen;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.objectweb.asm.ClassVisitor;

public final class LoadingBuilder<T> extends JvmBuildCollector {
  private final class DynamicClassLoader extends ClassLoader implements BiConsumer<String, byte[]> {
    private final Map<String, byte[]> byteCode = new HashMap<>();

    public DynamicClassLoader() {
      super(clazz.getClassLoader());
    }

    @Override
    public void accept(String fileName, byte[] contents) {
      byteCode.put(fileName.replace('/', '.'), contents);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
      final var code = byteCode.get(className);
      if (code == null) {
        return super.findClass(className);
      }
      return defineClass(className, code, 0, code.length);
    }
  }

  public static <T> JvmBuildCollector instantiating(
      Class<T> targetClass,
      ErrorCollector collector,
      String sourceFile,
      Consumer<? super T> consumer,
      FunctionKind rootFunctionKind) {
    return new LoadingBuilder<>(
        targetClass,
        clazz -> {
          try {
            consumer.accept(clazz.getConstructor().newInstance());
          } catch (InstantiationException
              | IllegalAccessException
              | IllegalArgumentException
              | InvocationTargetException
              | NoSuchMethodException
              | SecurityException e) {
            collector.emitError(
                new SourceLocation(sourceFile, 0, 0, 0, 0),
                "Internal error: generated output cannot be instantiated: " + e.getMessage());
          }
        },
        sourceFile,
        collector,
        rootFunctionKind);
  }

  private final DynamicClassLoader classLoader;
  private final Class<T> clazz;
  private final Consumer<Class<? extends T>> consumer;

  public LoadingBuilder(
      Class<T> clazz,
      Consumer<Class<? extends T>> consumer,
      String sourceFile,
      ErrorCollector errorCollector,
      FunctionKind rootFunctionKind) {
    super(errorCollector, "flabbergast/dyn/", sourceFile, rootFunctionKind);
    this.clazz = clazz;
    this.consumer = consumer;
    classLoader = new DynamicClassLoader();
  }

  @Override
  protected void finishOutput() throws Exception {
    consumer.accept(classLoader.findClass(rootType().getClassName()).asSubclass(clazz));
  }

  @Override
  public ClassVisitor createClass() {
    return WritingClassVisitor.create(classLoader);
  }
}
