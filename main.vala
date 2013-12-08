using Flabbergast;
namespace Debug {
	abstract class Statement : Object {
		public abstract bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame);
	}
	class Switch : Statement {
		public int frame {
			get;
			set;
		}
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			if (frame >= 0 && frame < engine.call_depth) {
				stack_frame = frame;
			} else {
				stdout.printf ("Stack frame %d out of range.\n", frame);
			}
			return false;
		}
	}
	class Trace : Statement {
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			back_trace (engine, stack_frame);
			return false;
		}
	}
	class Up : Statement {
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			if (stack_frame > 0) {
				stack_frame--;
			} else {
				stdout.printf ("End of stack.\n");
			}
			return false;
		}
	}
	class Down : Statement {
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			if (stack_frame < engine.call_depth - 1) {
				stack_frame++;
			} else {
				stdout.printf ("End of stack.\n");
			}
			return false;
		}
	}
	class Quit : Statement {
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			return true;
		}
	}
	class Lookup : Statement {
		public Gee.List<Nameish> names {
			get;
			set;
		}
		public override bool execute (Rules rules, ExecutionEngine engine, ref int stack_frame) {
			try {
				var result = engine.debug_lookup (stack_frame, names);
				print_datum (result, rules, engine, false, 0);
			} catch (EvaluationError e) {
				stdout.printf ("Error: %s\n", e.message);
			}
			return false;
		}
	}
	public void back_trace (ExecutionEngine engine, int current_frame = 0) {
		var num_frames = engine.call_depth;
		for (var it = 0; it < num_frames; it++) {
			var source = engine.get_call_source (it).source;
			stdout.printf ("%s #%d %s:%d:%d\n", it == current_frame ? "->" : "  ",  it, source.source, source.line, source.offset);
		}
	}
}

void print_tabs (uint depth) {
	for (var it = 0; it < depth; it++) {
		stdout.putc (' ');
	}
}

void print_datum (Data.Datum result, Rules rules, ExecutionEngine engine, bool debug, uint depth) {
	if (result is Data.Tuple) {
		stdout.printf ("{\n");
		foreach (var entry in (Data.Tuple)result) {
			if (entry.key == "Container") {
				continue;
			}
			print_tabs (depth);
			stdout.printf (" %s: ", entry.key);
			print_expression (rules, engine, entry.value, debug && debug_inner, depth + 1);
		}
		print_tabs (depth);
		stdout.printf ("}\n");
	} else {
		string type_name;
		if (result is Data.Boolean) {
			type_name = "bool";
		} else if (result is Data.Float) {
			type_name = "float";
		} else if (result is Data.Null) {
			stdout.printf ("Null\n");
			return;
		} else if (result is Data.Integer) {
			type_name = "int";
		} else if (result is Data.String) {
			stdout.printf ("\"%s\" As str\n", result.to_string ().escape ("'"));
			return;
		} else if (result is Data.Template) {
			stdout.printf ("Template\n");
			return;
		} else {
			type_name = "unknown";
		}
		stdout.printf ("%s As %s\n", result.to_string (), type_name);
	}
}

void print_expression (Rules rules, ExecutionEngine engine, Expression expression, bool debug, uint depth = 0) {
	try {
		var result = engine.run (expression, !debug);
		print_datum (result, rules, engine, debug, depth);
	} catch (EvaluationError e) {
		stdout.printf ("Error: %s\n", e.message);
		if (debug) {
			Debug.back_trace (engine);
			var stack_frame = 0;
			string? line;
			while ((line = sane_readline ("Debug:%d> ".printf (stack_frame))) != null) {
				line = line.strip ();
				if (line.length == 0) {
					break;
				}
				Value debug_stmt;
				var parser = new GTeonoma.StringParser (rules, line, "debug command");
				if (parser.parse (typeof (Debug.Statement), out debug_stmt) == GTeonoma.Result.OK) {
					if (((Debug.Statement)debug_stmt.get_object ()).execute (rules, engine, ref stack_frame)) {
						break;
					}
				} else {
					parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
				}
			}
		}
		engine.clear_call_stack ();
	}
}

public Expression? do_parsing (Rules rules, GTeonoma.Parser parser, bool can_try_more = false, out bool try_more = null) {
	if (debug_parsing) {
		GTeonoma.log_to_console (parser);
	}
	Value result;
	GTeonoma.Result r;
	if ((r = parser.parse (typeof (Expression), out result)) != GTeonoma.Result.OK) {
		try_more = r == GTeonoma.Result.EOI;
		if (!can_try_more) {
			parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
		}
		return null;
	}
	if (!parser.is_finished ()) {
		try_more = true;
		if (!can_try_more) {
			var end = parser.get_location ();
			stderr.printf ("%s:%d:%d: Junk at end of input.\n", end.source, end.line, end.offset);
		}
		return null;
	}
	try_more = false;
	return ((Expression) result.get_object ()).transform ();
}

private extern string? sane_readline (string prompt);

bool interactive;
bool debug_parsing;
bool debug_inner;
string? filename;
const OptionEntry[] options = {
	{ "debug-inner", 'g', 0, OptionArg.NONE, ref debug_inner, "Interactively debug inside tuple printing.", null },
	{ "debug-parsing", 0, 0, OptionArg.NONE, ref debug_parsing, "Turn on GTeonoma parsing output.", null },
	{ "file", 'f', 0, OptionArg.FILENAME, ref filename, "Input file to read.", "FILE" },
	{ "interactive", 'i', 0, OptionArg.NONE, ref interactive, "Enter interactive shell.", null },
	{ null }
};

int main (string[] args) {
	try {
		var opt_context = new OptionContext ("- Flabbergast");
		opt_context.set_help_enabled (true);
		opt_context.add_main_entries (options, null);
		opt_context.parse (ref args);
	} catch (OptionError e) {
		stdout.printf ("%s\n", e.message);
		stdout.printf ("Run '%s --help' to see a full list of available command line options.\n", args[0]);
		return 1;
	}
	if (interactive) {
		stderr.printf ("Flabbergast – %s %s\n", Package.STRING, Package.URL);
	}
	Rules rules;
	try {
		rules = new Rules ();
		rules.register<Debug.Down> ("up one call frame", 0, "% Down% ");
		rules.register<Debug.Lookup> ("lookup name", 0, "% %L{names}{% .% }% ", new Type[] { typeof (Nameish) });
		rules.register<Debug.Quit> ("exit debugger", 0, "% Quit% ");
		rules.register<Debug.Switch> ("switch call frame", 0, "% Switch %P{frame}% ");
		rules.register<Debug.Trace> ("backtrace", 0, "% Trace% ");
		rules.register<Debug.Up> ("up one call frame", 0, "% Up% ");
	} catch (GTeonoma.RegisterError e) {
		stderr.printf ("Grammar error: %s\n", e.message);
		return 1;
	}
	var engine = new ExecutionEngine ();
	Data.Tuple? root_tuple = null;
	if (!(interactive && filename == null)) {
		var parser = GTeonoma.FileParser.open (rules, filename?? "/dev/stdin");
		if (parser == null) {
			stderr.printf ("Failed to open input file.\n");
			return 1;
		}
		if (debug_parsing) {
			GTeonoma.log_to_console (parser);
		}
		try {
			Value result;
			if ((parser.parse (typeof (File), out result)) != GTeonoma.Result.OK) {
				parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
				return 1;
			}
			if (!parser.is_finished ()) {
				var end = parser.get_location ();
				stderr.printf ("%s:%d:%d: Junk at end of input.\n", end.source, end.line, end.offset);
				return 1;
			}
			var file = (File) result.get_object ();
			file.transform ();
			root_tuple = engine.start_from (file);
		} catch (EvaluationError e) {
			stderr.printf ("%s: %s\n", filename ?? "stdin", e.message);
			return 1;
		}
	}
	if (args.length == 1 && !interactive) {
		print_expression (rules, engine, new Expressions.This (), interactive);
	}
	for (var it = 1; it < args.length; it++) {
		var arg_parser = new GTeonoma.StringParser (rules, args[it].strip (), "argument %d".printf (it));
		var expression = do_parsing (rules, arg_parser);
		if (expression == null) {
			return 1;
		}
		print_expression (rules, engine, expression, interactive);
	}
	if (interactive) {
		Readline.initialize ();
		Readline.readline_name = "Flabbergast";
		string line;
		var it = 1;
		while ((line = sane_readline ("%d‽ ".printf (it))) != null) {
			line = line.strip ();
			if (line.length == 0) {
				continue;
			}
			it++;
			bool try_more = false;
			Expression? expression = null;
			do {
				var arg_parser = new GTeonoma.StringParser (rules, line, "input %d".printf (it));
				expression = do_parsing (rules, arg_parser, true, out try_more);
				if (expression == null && try_more) {
					var next_line = sane_readline ("> ");
					if (next_line == null) {
						break;
					}
					line = "%s\n%s".printf (line, next_line.strip ());
				}
			} while (try_more);

			if (expression != null) {
				print_expression (rules, engine, expression, true);
			}
			Readline.History.add (line);
		}
		stdout.putc ('\n');
	}
	return 0;
}
