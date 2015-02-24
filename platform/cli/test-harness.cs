using Flabbergast;
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;

public class DirtyCollector : ErrorCollector {
	public bool AnalyseDirty { get; set; }
	public bool ParseDirty { get; set; }
	public void ReportExpressionTypeError(CodeRegion where, Flabbergast.Type new_type, Flabbergast.Type existing_type) {
		AnalyseDirty = true;
	}
	public void ReportLookupTypeError(CodeRegion environment, string name, Flabbergast.Type new_type, Flabbergast.Type existing_type) {
		AnalyseDirty = true;
	}
	public void ReportForbiddenNameAccess(CodeRegion environment, string name) {
		AnalyseDirty = true;
	}
	public void ReportRawError(CodeRegion where, string message) {
		AnalyseDirty = true;
	}
	public void ReportParseError(string filename, int index, int row, int column, string message) {
		ParseDirty = true;
	}
}

public class CheckResult : Computation {
	private TaskMaster task_master;
	private System.Type test_target;
	public bool Success { get; private set; }
	public CheckResult(TaskMaster task_master, System.Type test_target) {
		this.task_master = task_master;
		this.test_target = test_target;
	}

	protected override bool Run() {
		var computation = (Computation) Activator.CreateInstance(test_target, task_master);
		computation.Notify(HandleFrameResult);
		task_master.Slot(computation);
		return false;
	}

	private void HandleFrameResult(object result) {
		if (result is Frame) {
			var lookup = new Lookup(task_master, null, new string[] { "value" }, (result as Frame).Context);
			lookup.Notify(HandleFinalResult);
			task_master.Slot(lookup);
		}
	}
	private void HandleFinalResult(object result) {
		if (result == null) {
			return;
		}
		if (result is bool) {
			Success = (bool) result;
		}
	}
}

public class TestTaskMaster : TaskMaster {
	public override void ReportOtherError(SourceReference reference, string message) {
	}
}

public class TestHarness {
	public static int Main(string[] args) {
		var uri = new Uri(System.Reflection.Assembly.GetExecutingAssembly().GetName().CodeBase);
		var directory = Path.GetDirectoryName(uri.LocalPath);
		var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Test"), AssemblyBuilderAccess.Run);
		var module_builder = assembly_builder.DefineDynamicModule("TestModule");
		var unit = new CompilationUnit("<tests>", module_builder, false);
		var id = 0;
		var success = true;
		success &= DoTests(Path.Combine(directory, "..", "..", "..", "..", "tests"), "*", unit, ref id);
		success &= DoTests(Path.Combine(directory, "..", "..", "tests"), "I", unit, ref id);
		return success ? 0 : 1;
	}
	public static List<string> GetFiles(string root, string child) {
		var files = new List<string>();
		var path = Path.Combine(root, child);
		if (Directory.Exists(path)) {
			foreach (var file in Directory.GetFiles(path)) {
				if (file.EndsWith(".flbgst")) {
					files.Add(file);
				}
			}
		}
		return files;
	}
	public static bool DoTests(string root, string type, CompilationUnit unit, ref int id) {
		var all_succeeded = true;
		if (!Directory.Exists(root)) {
			System.Console.WriteLine("Skipping non-existent directory: " + root);
			return all_succeeded;
		}
		foreach (var file in GetFiles(root, "malformed")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			parser.ParseFile(collector, unit, "Test" + id++);
			System.Console.WriteLine("{0} {1} {2} {3}", collector.ParseDirty ? "----" : "FAIL", "M", type, Path.GetFileNameWithoutExtension(file));
			all_succeeded &= collector.ParseDirty;
		}
		var task_master = new TestTaskMaster();
		foreach (var file in GetFiles(root, "errors")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			var test_type = parser.ParseFile(collector, unit, "Test" + id++);
			var success = collector.AnalyseDirty;
			if (!success && test_type != null) {
				var tester = new CheckResult(task_master, test_type);
				task_master.Slot(tester);
				task_master.Run();
				success = !tester.Success;
			}
			System.Console.WriteLine("{0} {1} {2} {3}", success ? "----" : "FAIL", "E", type, Path.GetFileNameWithoutExtension(file));
			all_succeeded &= success;
		}
		foreach (var file in GetFiles(root, "working")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			var test_type = parser.ParseFile(collector, unit, "Test" + id++);
			var success = !collector.AnalyseDirty && !collector.ParseDirty;
			if (success && test_type != null) {
				var tester = new CheckResult(task_master, test_type);
				task_master.Slot(tester);
				task_master.Run();
				success = tester.Success;
			}
			System.Console.WriteLine("{0} {1} {2} {3}", success ? "----" : "FAIL", "W", type, Path.GetFileNameWithoutExtension(file));
			all_succeeded &= success;
		}
		return all_succeeded;
	}
}
