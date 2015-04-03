package flabbergast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class TestHarness {
	private static final class OnlySourceFiles implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".flbgst");
		}
	}

	private static File combine(String... parts) {
		StringBuilder buffer = new StringBuilder();
		for (int it = 0; it < parts.length; it++) {
			if (it > 0)
				buffer.append(File.separator);
			buffer.append(parts[it]);
		}
		return new File(buffer.toString());
	}

	public static boolean doTests(File root, String type, Ptr<Integer> id)
			throws IOException {
		boolean all_succeeded = true;
		if (!root.exists()) {
			System.err.printf("Skipping non-existent directory: %s\n", root);
			return all_succeeded;
		}
		for (File file : new File(root, "malformed")
				.listFiles(new OnlySourceFiles())) {
			DirtyCollector collector = new DirtyCollector();
			DynamicCompiler compiler = new DynamicCompiler(collector);

			Parser parser = Parser.open(file.getAbsolutePath());
			int test_id = id.get();
			id.set(test_id + 1);
			try {
				parser.parseFile(collector, compiler.getCompilationUnit(),
						"Test" + test_id);
			} catch (Exception e) {
			}
			System.err.printf("%s %s %s %s\n", collector.isParseDirty()
					? "----"
					: "FAIL", "M", type, file.getName());
			all_succeeded &= collector.isParseDirty();
		}
		TaskMaster task_master = new TestTaskMaster();
		for (File file : new File(root, "errors")
				.listFiles(new OnlySourceFiles())) {
			boolean success;
			try {
				DirtyCollector collector = new DirtyCollector();
				DynamicCompiler compiler = new DynamicCompiler(collector);
				Parser parser = Parser.open(file.getAbsolutePath());
				int test_id = id.get();
				id.set(test_id + 1);
				Class<? extends Computation> test_type = parser.parseFile(
						collector, compiler.getCompilationUnit(), "Test"
								+ test_id);
				success = collector.isAnalyseDirty();
				if (!success && test_type != null) {
					CheckResult tester = new CheckResult(task_master, test_type);
					task_master.slot(tester);
					task_master.run();
					success = !tester.getSuccess();
				}
			} catch (Exception e) {
				success = false;
			}
			System.err.printf("%s %s %s %s\n", success ? "----" : "FAIL", "E",
					type, file.getName());
			all_succeeded &= success;
		}
		for (File file : new File(root, "working")
				.listFiles(new OnlySourceFiles())) {
			boolean success;
			try {
				DirtyCollector collector = new DirtyCollector();
				DynamicCompiler compiler = new DynamicCompiler(collector);
				Parser parser = Parser.open(file.getAbsolutePath());
				int test_id = id.get();
				id.set(test_id + 1);
				Class<? extends Computation> test_type = parser.parseFile(
						collector, compiler.getCompilationUnit(), "Test"
								+ test_id);
				success = !collector.isAnalyseDirty()
						&& !collector.isParseDirty();
				if (success && test_type != null) {
					CheckResult tester = new CheckResult(task_master, test_type);
					task_master.slot(tester);
					task_master.run();
					success = tester.getSuccess();
				}
			} catch (Exception e) {
				success = false;
			}
			System.err.printf("%s %s %s %s\n", success ? "----" : "FAIL", "W",
					type, file.getName());
			all_succeeded &= success;
		}
		return all_succeeded;
	}

	public static void main(String[] args) {
		try {
			String path;
			path = TestHarness.class.getProtectionDomain().getCodeSource()
					.getLocation().toURI().getPath();

			Ptr<Integer> id = new Ptr<Integer>(0);
			boolean success = true;
			success &= doTests(combine(path, "..", "..", "..", "tests"), "*",
					id);
			success &= doTests(combine(path, "..", "tests"), "I", id);
			System.exit(success ? 0 : 1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
