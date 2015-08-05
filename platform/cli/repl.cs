using System;
using System.CodeDom;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;
using Mono.Terminal;
using NDesk.Options;

namespace Flabbergast {
	public class Completables {
		private readonly string[] keywords = {"args", "value", "Append", "Bool", "By", "Container", "Drop", "Each", "Else", "Enforce", "Error", "False", "Finite", "Float", "FloatMax", "FloatMin", "For", "Frame", "From", "GenerateId", "Id", "If", "In", "Infinity", "Int", "IntMax", "IntMin", "Is", "Length", "Let", "Lookup", "NaN", "Name", "Now", "Null", "Order", "Ordinal", "Reduce", "Required", "Reverse", "Select", "Str", "Template", "Then", "This", "Through", "To", "True", "Used", "Where", "With"};

		public LineEditor.Completion Handler(string text, int pos) {
			var start = pos;
			while (text.Length > 0 && start > 0 && Char.IsLetter(text[start - 1])) {
				start--;
			}
			var candidates = new List<string>();
			foreach (var word in keywords) {
				if (String.Compare(word, 0, text, start, pos - start) == 0) {
					candidates.Add(word.Substring(pos - start));
				}
			}
			return new LineEditor.Completion(text.Substring(start, pos - start), candidates.ToArray());
		}
	}

	public class REPL {
		private static void HandleResult(object result) {
			var seen = new Dictionary<Frame, int>();
			var seen_srcref = new Dictionary<SourceReference, bool>();
			var counter = 1;
			Print(result, "", seen, seen_srcref, ref counter);
		}

		public static int Main(string[] args) {
			var show_help = false;
			var use_precompiled = true;
			var options = new OptionSet {
				{"p|no-precomp", "do not use precompiled libraries", v => use_precompiled = v == null},
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
				Console.WriteLine("Usage: " + AppDomain.CurrentDomain.FriendlyName + " input.o_0");
				Console.WriteLine("Run Flabbergast interactively.");
				Console.WriteLine();
				Console.WriteLine("Options:");
				options.WriteOptionDescriptions(Console.Out);
				return 1;
			}

			if (files.Count > 1) {
				Console.Error.WriteLine("No more than one Flabbergast script may be given.");
				return 1;
			}

			Frame original = null;

			var assembly_builder = AppDomain.CurrentDomain.DefineDynamicAssembly(new AssemblyName("Repl"), AssemblyBuilderAccess.Run);
			var module_builder = assembly_builder.DefineDynamicModule("ReplModule");
			var unit = new CompilationUnit(module_builder, false);
			var collector = new ConsoleCollector();
			var task_master = new ConsoleTaskMaster();
			task_master.AddUriHandler(BuiltInLibraries.INSTANCE);
			if (use_precompiled) {
				task_master.AddUriHandler(new LoadPrecompiledLibraries());
			}
			task_master.AddUriHandler(new DynamicallyCompiledLibraries(collector));

			if (files.Count == 1) {
				var parser = Parser.Open(files[0]);
				var root_type = parser.ParseFile(collector, unit, "REPLRoot");
				if (root_type != null) {
					var computation = (Computation) Activator.CreateInstance(root_type, task_master);
					computation.Notify(r => original = r as Frame);
					task_master.Run();
					task_master.ReportCircularEvaluation();
				}
			}
			if (original == null) {
				original = new MutableFrame(task_master, new NativeSourceReference("REPL"), null, null);
			}

			var id = 0;
			Frame current = original;
			bool run = true;
			ConsumeResult update_current = (x) => current = (x as Frame) ?? current;

			var line_editor = new LineEditor("flabbergast");
			var completables = new Completables();
			line_editor.AutoCompleteEvent = completables.Handler;
			string s;

			while (run && (s = line_editor.Edit(id + "‽ ", "")) != null) {
				var parser = new Parser("line" + id, s);
				var run_type = parser.ParseRepl(collector, unit, "REPL" + id++);
				if (run_type != null) {
					object result = null;
					var computation = (Computation) Activator.CreateInstance(run_type, new object[] { task_master, original, current, update_current, (ConsumeResult) (output => result = output), (ConsumeResult) Console.WriteLine });
					computation.Notify(r => run = (r as bool?) ?? true);
					task_master.Run();
					if (result != null) {
						HandleResult(result);
					}
					task_master.ReportCircularEvaluation();
				}
			}
			line_editor.SaveHistory();
			return 0;
		}

		private static void Print(object result, string prefix, Dictionary<Frame, int> seen, Dictionary<SourceReference, bool> seen_srcref, ref int counter) {
			if (result == null) {
				Console.WriteLine("∅");
			} else if (result is Frame) {
				var f = result as Frame;
				if (seen.ContainsKey(f)) {
					Console.WriteLine("{0} # Frame {1}", f.Id, seen[f]);
				} else {
					Console.Write("{ # Frame ");
					Console.WriteLine(counter);
					seen[f] = counter++;
					foreach (var name in f.GetAttributeNames()) {
						Console.Write("{0}{1} : ", prefix, name);
						Print(f[name], prefix + "  ", seen, seen_srcref, ref counter);
					}
					Console.Write(prefix);
					Console.WriteLine("}");
				}
			} else if (result is bool) {
				Console.WriteLine(((bool) result) ? "True" : "False");
			} else if (result is Template) {
				var t = result as Template;
				Console.WriteLine("Template");
				foreach (var name in t.GetAttributeNames()) {
					Console.Write(" ");
					Console.Write(name);
				}
				Console.WriteLine();
				t.SourceReference.Write(Console.Out, prefix, seen_srcref);
			} else if (result is Computation) {
				Console.WriteLine("<unfinished>");
			} else if (result is Stringish) {
				var provider = CodeDomProvider.CreateProvider("CSharp");
				provider.GenerateCodeFromExpression(new CodePrimitiveExpression(result.ToString()), Console.Out, null);
				Console.Out.Flush();
				Console.WriteLine();
			} else {
				Console.WriteLine(result);
			}
		}
	}
}
