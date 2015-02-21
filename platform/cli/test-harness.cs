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
public class Compiler {
	public static void Main(string[] args) {
		var uri = new Uri(System.Reflection.Assembly.GetExecutingAssembly().GetName().CodeBase);
		var directory = Path.GetDirectoryName(uri.LocalPath);
		DoTests(Path.Combine(directory, "..", "..", "..", "..", "tests"), "*");
		DoTests(Path.Combine(directory, "..", "..", "tests"), "I");
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
	public static void DoTests(string root, string type) {
		if (!Directory.Exists(root)) {
			System.Console.WriteLine("Skipping non-existent directory: " + root);
			return;
		}
		var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Test"), AssemblyBuilderAccess.Run);
		var module_builder = assembly_builder.DefineDynamicModule("TestModule");
		var id = 0;
		foreach (var file in GetFiles(root, "malformed")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			parser.ParseFile(collector, module_builder, "Test" + id++, false);
			System.Console.WriteLine("{0} {1} {2} {3}", collector.ParseDirty ? "----" : "FAIL", "M", type, Path.GetFileNameWithoutExtension(file));
		}
		foreach (var file in GetFiles(root, "errors")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			parser.ParseFile(collector, module_builder, "Test" + id++, false);
			System.Console.WriteLine("{0} {1} {2} {3}", collector.AnalyseDirty ? "----" : "FAIL", "E", type, Path.GetFileNameWithoutExtension(file));
		}
		foreach (var file in GetFiles(root, "working")) {
			var collector = new DirtyCollector();
			var parser = Parser.Open(file);
 			var test = parser.ParseFile(collector, module_builder, "Test" + id++, false);
			var success = !collector.AnalyseDirty && !collector.ParseDirty;
			// TODO: Run and get the results.
			System.Console.WriteLine("{0} {1} {2} {3}", success ? "----" : "FAIL", "W", type, Path.GetFileNameWithoutExtension(file));
		}
	}
}
