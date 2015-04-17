using System;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;
using Flabbergast;
using NDesk.Options;

public class Printer {
	public static int Main(string[] args) {
		var trace = false;
		string output_filename = null;
		var show_help = false;
		var options = new OptionSet {
			{"o:|output", "Write output to file instead of standard output.", v => output_filename = v},
			{"t|trace-parsing", "Produce a trace of the parse process.", v => trace = v != null},
			{"h|help", "show this message and exit", v => show_help = v != null}
		};

		List<string> files;
		try {
			files = options.Parse(args);
		} catch (OptionException e) {
			Console.Error.Write(AppDomain.CurrentDomain.FriendlyName + ": ");
			Console.Error.WriteLine(e.Message);
			Console.Error.WriteLine("Try “" + AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
			return 1;
		}

		if (show_help) {
			Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " input.flbgst");
			Console.WriteLine("Run a Flabbergast file and display the “value” attribute.");
			Console.WriteLine();
			Console.WriteLine("Options:");
			options.WriteOptionDescriptions(Console.Out);
			return 1;
		}

		if (files.Count != 1) {
			Console.Error.WriteLine("Exactly one Flabbergast script must be given.");
			return 1;
		}

		var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Print"), AssemblyBuilderAccess.Run);
		CompilationUnit.MakeDebuggable(assembly_builder);
		var module_builder = assembly_builder.DefineDynamicModule("PrintModule", true);
		var unit = new CompilationUnit(module_builder, true);
		var collector = new ConsoleCollector();
		var task_master = new ConsoleTaskMaster();
		task_master.AddUriHandler(BuiltInLibraries.INSTANCE);
		task_master.AddUriHandler(new LoadPrecompiledLibraries());
		task_master.AddUriHandler(new DynamicallyCompiledLibraries(collector));
		var parser = Parser.Open(files[0]);
		parser.Trace = trace;
		var run_type = parser.ParseFile(collector, unit, "Printer");
		if (run_type != null) {
			var computation = (Computation) Activator.CreateInstance(run_type, task_master);
			var filewriter = new PrintResult(task_master, computation, output_filename);
			task_master.Slot(filewriter);
			task_master.Run();
			task_master.ReportCircularEvaluation();
			return filewriter.Success ? 0 : 1;
		}
		return 1;
	}
}
