package flabbergast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainBuildCache {

	private static void discover(File root, List<File> sources,
			Set<String> known_classes) {
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				discover(f, sources, known_classes);
			} else if (f.isFile()) {
				if (f.getName().endsWith(".flbgst")
						|| f.getName().endsWith(".jflbgst")) {
					sources.add(f);
				} else if (f.getName().endsWith(".class")) {
					try {
						known_classes.add(f.getCanonicalPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 0) {
			System.exit(1);
			return;
		}
		ErrorCollector collector = new ConsoleCollector();
		final Set<String> known_classes = new HashSet<String>();
		List<File> sources = new ArrayList<File>();
		File root = new File(".");
		discover(root, sources, known_classes);

		CompilationUnit<Boolean> unit = new WriterCompilationUnit(true) {
			@Override
			protected void fileEvent(File file) {
				try {
					known_classes.remove(file.getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		boolean success = true;
		for (File source : sources) {
			try {
				String filename = source.getPath();
				Parser parser = Parser.open(source.getCanonicalPath());
				String file_root = ("flabbergast/library/" + removeSuffix(filename))
						.replace(File.separatorChar, '/')
						.replaceAll("[/.]+", "/").replace('-', '_');
				parser.parseFile(collector, unit, file_root);
			} catch (Exception e) {
				success = false;
				e.printStackTrace();
			}
		}
		for (String dead : known_classes) {
			new File(dead).delete();
		}
		System.exit(success ? 0 : 1);
	}

	public static String removeSuffix(String filename) {
		for (String suffix : new String[]{".flbgst", ".jflbgst"}) {
			if (filename.endsWith(suffix)) {
				return filename.substring(0,
						filename.length() - suffix.length());
			}
		}
		return filename;
	}
}
