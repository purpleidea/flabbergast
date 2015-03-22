package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.io.File;
import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class DynamicCompiler extends LoadLibraries {

	class AutoLoaderClassVisitor extends ClassVisitor {
		private String class_name;

		private ClassWriter writer;

		public AutoLoaderClassVisitor(ClassWriter writer) {
			super(Opcodes.ASM5, writer);
			this.writer = writer;
		}

		@Override
		public void visit(int version, int access, String class_name,
				String signature, String super_name, String[] interfaces) {
			this.class_name = class_name;
			super.visit(version, access, class_name, signature, super_name,
					interfaces);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			class_loader.load(class_name, writer);
		}

	}

	class DynamicClassLoader extends SecureClassLoader {
		@SuppressWarnings("unchecked")
		public <T> Class<? extends T> load(String name, ClassWriter writer) {
			byte[] bytecode = writer.toByteArray();
			try {
				Class<? extends T> clazz = (Class<? extends T>) defineClass(
						name, bytecode, 0, bytecode.length);
				return clazz;
			} catch (ClassFormatError e) {
				System.err.println(e.getMessage());
			} catch (VerifyError e) {
				System.err.println(e.getMessage());
			}
			return null;
		}

	}

	private Map<String, Class<? extends Computation>> cache = new HashMap<String, Class<? extends Computation>>();

	private DynamicClassLoader class_loader = new DynamicClassLoader();

	private final ErrorCollector collector;;

	private CompilationUnit<Class<? extends Computation>> unit = new CompilationUnit<Class<? extends Computation>>() {

		@Override
		public ClassVisitor defineClass(int access, String class_name,
				Class<?> superclass, Class<?>... interfaces) {
			ClassVisitor visitor = new AutoLoaderClassVisitor(new ClassWriter(
					ClassWriter.COMPUTE_MAXS));
			String[] interface_names = new String[interfaces.length];
			for (int it = 0; it < interfaces.length; it++) {
				interface_names[it] = getInternalName(interfaces[it]);
			}
			visitor.visit(Opcodes.V1_6, access, class_name,
					getInternalName(superclass), null, interface_names);
			return visitor;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<? extends Computation> doMagic(String name) {
			try {
				return (Class<? extends Computation>) class_loader
						.loadClass(name);
			} catch (ClassNotFoundException e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
	};

	public DynamicCompiler(ErrorCollector collector) {
		this.collector = collector;
	}

	public CompilationUnit<Class<? extends Computation>> getCompilationUnit() {
		return unit;
	};

	@Override
	public String getUriName() {
		return "dynamically-compiled";
	}

	@Override
	public Class<? extends Computation> resolveUri(String uri, Ptr<Boolean> stop) {
		stop.set(false);
		if (cache.containsKey(uri))
			return cache.get(uri);
		if (!uri.startsWith("lib:"))
			return null;
		String type_name = "flabbergast.library."
				+ uri.substring(4).replace('/', '.');
		for (String path : paths) {
			try {
				File f = new File(path + File.separator + uri.substring(4)
						+ ".flbgst");
				if (!f.exists())
					continue;
				Parser parser = Parser.open(f.getAbsolutePath());
				Class<? extends Computation> result = parser.parseFile(
						collector, unit, type_name);
				stop.set(result == null);
				cache.put(uri, result);
				return result;
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
		return null;
	}

}
