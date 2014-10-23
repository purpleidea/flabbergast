using System;
using System.Collections.Generic;
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
			Console.WriteLine ("Try `" + System.AppDomain.CurrentDomain.FriendlyName + " --help' for more information.");
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
		foreach (var filename in files) {
			var parser = Parser.Open(filename);
			parser.Trace = trace;
			if (AstNode.ParseFile(parser) == null) {
				System.Console.WriteLine(parser.Message);
			}
		}
	}
}
}