using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using Flabbergast;
using NDesk.Options;
namespace Flabbergast {
public class ConsoleCollector : ErrorCollector {
	public void ReportExpressionTypeError(CodeRegion where, Type new_type, Type existing_type) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Expression has conflicting types: {5} versus {6}.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, new_type, existing_type);
	}
	public void ReportLookupTypeError(CodeRegion environment, string name, Type new_type, Type existing_type) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” has conflicting types: {6} versus {7}.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name, new_type, existing_type);
	}
	public void ReportForbiddenNameAccess(CodeRegion environment, string name) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” is forbidden.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name);
	}
	public void ReportParseError(string filename, int index, int row, int column, string message) {
		Console.WriteLine("{0}:{1}:{2}: Lookup for “{3}” is forbidden.", filename, row, column, message);
	}
	public void ReportRawError(CodeRegion where, string message) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: {5}", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, message);
	}
}
public class Compiler {
	public static void Main(string[] args) {
		bool trace = false;
		bool show_help = false;
		var options = new OptionSet () {
			{ "t|trace-parsing", "Produce a trace of the parse process.", v => trace = v != null },
			{ "h|help",  "show this message and exit", v => show_help = v != null },
		};

		List<string> files;
		try {
			files = options.Parse (args);
		} catch (OptionException e) {
			Console.Write (System.AppDomain.CurrentDomain.FriendlyName + ": ");
			Console.WriteLine (e.Message);
			Console.WriteLine ("Try “" + System.AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
			return;
		}

		if (show_help) {
			Console.WriteLine ("Usage: " + System.AppDomain.CurrentDomain.FriendlyName + " files ...");
			Console.WriteLine ("Compile a Flabbergast file to native CLR.");
			Console.WriteLine ();
			Console.WriteLine ("Options:");
			options.WriteOptionDescriptions (Console.Out);
			return;
		}
		if (files.Count == 0) {
			Console.WriteLine ("Perhaps you wish to compile some source files?");
			return;
		}
		var collector = new ConsoleCollector();
		foreach (var filename in files) {
			var parser = Parser.Open(filename);
			parser.Trace = trace;

			var dll_name = Path.ChangeExtension(filename, ".dll");
			var type_name = "Flabbergast.Library." + Path.GetDirectoryName(filename).Replace(Path.DirectorySeparatorChar, '.') + Path.GetFileNameWithoutExtension(filename);
			var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName(type_name), AssemblyBuilderAccess.Save);
			var module_builder = assembly_builder.DefineDynamicModule(type_name, dll_name);
			var type = parser.ParseFile(collector, module_builder, type_name, true);
			if (type != null) {
				assembly_builder.Save(dll_name);
			}
		}
	}
}
}
