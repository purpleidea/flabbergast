package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import flabbergast.TaskMaster.LibraryFailure;

public class DynamicCompiler extends LoadLibraries {

	protected String[] extensions = new String[]{".jo_0", ".o_0"};

	class AutoLoaderClassVisitor extends ClassVisitor {
		private String class_name;

		private ClassWriter writer;

		public AutoLoaderClassVisitor(ClassWriter writer) {
			super(Opcodes.ASM4, writer);
			this.writer = writer;
		}

		@Override
		public void visit(int version, int access, String class_name,
				String signature, String super_name, String[] interfaces) {
			this.class_name = class_name;
			super.visit(version, access, class_name, signature, super_name,
					interfaces);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void visitEnd() {
			super.visitEnd();
			byte[] x = writer.toByteArray();
			try {
				FileOutputStream output = new FileOutputStream("fail.class");
				output.write(writer.toByteArray());
				output.close();
			} catch (IOException f) {
			}
			try {
				Class<?> loaded = class_loader.hotload(class_name, x);
				if (Computation.class.isAssignableFrom(loaded)) {
					cache.put(class_name, (Class<? extends Computation>) loaded);
				} else {
					other_cache.put(class_name, loaded);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	static class ClassLoader extends URLClassLoader {

		public ClassLoader() {
			super(new URL[0]);
		}

		public Class<?> hotload(String name, byte[] class_file) {
			return defineClass(name.replace('/', '.'), class_file, 0,
					class_file.length);
		}
	}

	private Map<String, Class<? extends Computation>> cache = new HashMap<String, Class<? extends Computation>>();
	private final ClassLoader class_loader = new ClassLoader();

	private final ErrorCollector collector;

	private Map<String, Class<?>> other_cache = new HashMap<String, Class<?>>();

	private CompilationUnit<Class<? extends Computation>> unit = new CompilationUnit<Class<? extends Computation>>() {

		@Override
		public ClassVisitor defineClass(int access, String class_name,
				Class<?> superclass, Class<?>... interfaces) {
			ClassVisitor visitor = new AutoLoaderClassVisitor(new ClassWriter(
					ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES));
			String[] interface_names = new String[interfaces.length];
			for (int it = 0; it < interfaces.length; it++) {
				interface_names[it] = getInternalName(interfaces[it]);
			}
			visitor.visit(Opcodes.V1_5, access, class_name, null,
					getInternalName(superclass), interface_names);
			return visitor;
		}

		@Override
		protected Class<? extends Computation> doMagic(String name) {
			return cache.get(name);
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
	public Class<? extends Computation> resolveUri(String uri,
			Ptr<LibraryFailure> reason) {
		if (cache.containsKey(uri)) {
			return cache.get(uri);
		}
		if (!uri.startsWith("lib:"))
			return null;
		String type_name = "flabbergast/library/" + uri.substring(4);
		for (String path : paths) {
			for (String extension : extensions) {
				try {
					File f = new File(path + File.separator + uri.substring(4)
							+ extension);
					if (!f.exists())
						continue;
					Parser parser = Parser.open(f.getAbsolutePath());
					Class<? extends Computation> result = parser.parseFile(
							collector, unit, type_name);
					reason.set(result == null ? LibraryFailure.CORRUPT : null);
					cache.put(uri, result);
					parser = null;
					System.gc();
					return (Class<? extends Computation>) result;
				} catch (Exception e) {
					System.err.println(e.getMessage());
					reason.set(LibraryFailure.CORRUPT);
					return null;
				}
			}
		}
		reason.set(LibraryFailure.MISSING);
		return null;
	}

}
