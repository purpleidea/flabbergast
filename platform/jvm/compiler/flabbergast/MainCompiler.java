package flabbergast;

import static org.objectweb.asm.Type.getInternalName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class AutoWriteClassVisitor extends ClassVisitor {
	private String class_name;

	private ClassWriter writer;

	public AutoWriteClassVisitor(ClassWriter writer) {
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

	@Override
	public void visitEnd() {
		super.visitEnd();
		try {
			String path = class_name.replace('/', File.separatorChar)
					+ ".class";
			new File(path).getParentFile().mkdirs();
			Files.write(Paths.get(path), writer.toByteArray(),
					StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

public class MainCompiler {
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("t", "trace-parsing", false,
				"Produce a trace of the parse process.");
		options.addOption("F", "no-frames", false,
				"Do not compute frames. This class will not work. For debugging the compiler.");
		options.addOption("h", "help", false, "Show this message and exit");
		CommandLineParser cl_parser = new GnuParser();
		final CommandLine result;

		try {
			result = cl_parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}

		if (result.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			System.err
					.println("Compile a Flabbergast file for use as a library.");
			formatter.printHelp("gnu", options);
			System.exit(1);
		}

		String[] files = result.getArgs();
		if (files.length == 0) {
			System.err
					.println("Perhaps you wish to compile some source files?");
			System.exit(1);
			return;
		}
		ErrorCollector collector = new ConsoleCollector();
		CompilationUnit<Boolean> unit = new CompilationUnit<Boolean>() {

			@Override
			public ClassVisitor defineClass(int access, String class_name,
					Class<?> superclass, Class<?>... interfaces) {
				ClassVisitor visitor = new AutoWriteClassVisitor(
						new ClassWriter(ClassWriter.COMPUTE_MAXS
								| (result.hasOption('F') ? 0
										: ClassWriter.COMPUTE_FRAMES)));
				String[] interface_names = new String[interfaces.length];
				for (int it = 0; it < interfaces.length; it++) {
					interface_names[it] = getInternalName(interfaces[it]);
				}
				visitor.visit(Opcodes.V1_7, access, class_name, null,
						getInternalName(superclass), interface_names);
				return visitor;
			}

			@Override
			protected Boolean doMagic(String name) {
				return true;
			}
		};
		for (String filename : files) {
			try {
				Parser parser = Parser.open(filename);
				parser.setTrace(result.hasOption('t'));
				String file_root = ("flabbergast/library." + (filename
						.endsWith(".flbgst") ? filename.substring(0,
						filename.length() - ".flbgst".length()) : filename))
						.replace(File.separatorChar, '/')
						.replaceAll("[/.]+", "/").replace('-', '_');
				parser.parseFile(collector, unit, file_root);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
