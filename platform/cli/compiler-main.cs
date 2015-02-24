using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using Flabbergast;
using NDesk.Options;

namespace Flabbergast {
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

			var dll_name = Path.ChangeExtension(Path.GetFileNameWithoutExtension(filename), ".dll");
			var type_name = "Flabbergast.Library." + Path.GetDirectoryName(filename).Replace(Path.DirectorySeparatorChar, '.') + Path.GetFileNameWithoutExtension(filename);
			var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName(type_name), AssemblyBuilderAccess.RunAndSave);
			var module_builder = assembly_builder.DefineDynamicModule(type_name, dll_name);
			var unit = new CompilationUnit(filename, module_builder, true);
			var type = parser.ParseFile(collector, unit, type_name);
			if (type != null) {
				assembly_builder.Save(dll_name);
			}
		}
	}
}
}
