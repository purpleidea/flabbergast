using Flabbergast;
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Reflection.Emit;
using NDesk.Options;

public class PrintResult : Computation {
	private TaskMaster task_master;
	private System.Type test_target;
	private string output_filename;
	public bool Success { get; private set; }
	public PrintResult(TaskMaster task_master, System.Type test_target, string output_filename) {
		this.task_master = task_master;
		this.test_target = test_target;
		this.output_filename = output_filename;
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
		if (result is Stringish || result is long || result is bool || result is double) {
			Success = true;
			if (output_filename == null) {
				Console.WriteLine(result);
			} else {
				System.IO.File.WriteAllText(output_filename, result.ToString(), System.Text.Encoding.UTF8);
			}
		}
	}
}

public class ConsoleTaskMaster : TaskMaster {
	public override void ReportOtherError(SourceReference reference, string message) {
		Console.WriteLine(message);
		reference.Write(Console.Out, 0, "\t");
	}
}

public class Printer {
	public static int Main(string[] args) {
		bool trace = false;
		string output_filename = null;
		bool show_help = false;
		var options = new OptionSet () {
			{ "o:|output", "Write output to file instead of standard output.", v => output_filename = v },
			{ "t|trace-parsing", "Produce a trace of the parse process.", v => trace = v != null },
			{ "h|help",  "show this message and exit", v => show_help = v != null },
		};

		List<string> files;
		try {
			files = options.Parse (args);
		} catch (OptionException e) {
			Console.Error.Write (System.AppDomain.CurrentDomain.FriendlyName + ": ");
			Console.Error.WriteLine (e.Message);
			Console.Error.WriteLine ("Try “" + System.AppDomain.CurrentDomain.FriendlyName + " --help” for more information.");
			return 1;
		}

		if (show_help) {
			Console.WriteLine("Usage: " + System.AppDomain.CurrentDomain.FriendlyName + " input.flbgst");
			Console.WriteLine("Compile a Flabbergast file to native CLR.");
			Console.WriteLine();
			Console.WriteLine("Options:");
			options.WriteOptionDescriptions (Console.Out);
			return 1;
		}

		if (files.Count != 1) {
			Console.Error.WriteLine ("Exactly one Flabbergast script must be given.");
			return 1;
		}

		var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Print"), AssemblyBuilderAccess.Run);
		var module_builder = assembly_builder.DefineDynamicModule("PrintModule");
		var unit = new CompilationUnit("<input>", module_builder, false);
		var task_master = new ConsoleTaskMaster();
		var collector = new ConsoleCollector();
		var parser = Parser.Open(files[0]);
		parser.Trace = trace;
 		var run_type = parser.ParseFile(collector, unit, "Printer");
		if (run_type != null) {
			var filewriter = new PrintResult(task_master, run_type, output_filename);
			task_master.Slot(filewriter);
			task_master.Run();
			return filewriter.Success ? 0 : 1;
		}
		return 1;
	}
}
