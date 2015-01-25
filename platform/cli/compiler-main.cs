using System;
using System.Collections.Generic;
using Flabbergast;
using NDesk.Options;
namespace Flabbergast {
public class ConsoleCollector : ErrorCollector {
	public void ReportTypeError(AstNode where, Type new_type, Type existing_type) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Expression has conflicting types: {5} versus {6}.", where.FileName, where.StartRow, where.StartColumn, where.EndRow, where.EndColumn, new_type, existing_type);
	}
	public void ReportTypeError(Environment environment, string name, Type new_type, Type existing_type) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” has conflicting types: {6} versus {7}.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name, new_type, existing_type);
	}
	public void ReportForbiddenNameAccess(Environment environment, string name) {
		Console.WriteLine("{0}:{1}:{2}-{3}:{4}: Lookup for “{5}” is forbidden.", environment.FileName, environment.StartRow, environment.StartColumn, environment.EndRow, environment.EndColumn, name);
	}
	public void RawError(AstNode where, string message) {
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
			var ast_node = AstNode.ParseFile(parser);
			if (ast_node == null) {
				System.Console.WriteLine(parser.Message);
			} else {
				((AstTypeableNode)ast_node).Analyse (collector);
			}
		}
	}
}
}
