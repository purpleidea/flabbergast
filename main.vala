using Flabbergast;

void print_tabs(uint depth) {
	for (var it = 0; it < depth; it++) {
		stdout.putc (' ');
	}
}

void print_expression(ExecutionEngine engine, Expression expression, uint depth = 0) {
	try {
		engine.call (expression);
		var result = engine.operands.pop ();
		if (result is Tuple) {
			stdout.printf ("{\n");
			foreach (var entry in (Tuple) result) {
				if (entry.key == "Container") {
					continue;
				}
				print_tabs (depth);
				stdout.printf (" %s: ", entry.key);
				print_expression (engine, entry.value, depth + 1);
			}
			print_tabs (depth);
			stdout.printf ("}\n");
		} else {
			string type_name;
			if (result is Boolean) {
				type_name = "bool";
			} else if (result is Float) {
				type_name = "float";
			} else if (result is Null) {
				stdout.printf ("Null\n");
				return;
			} else if (result is Integer) {
				type_name = "int";
			} else if (result is String) {
				type_name = "str";
			} else if (result is Template) {
				stdout.printf ("Template\n");
				return;
			} else {
				type_name = "unknown";
			}
			stdout.printf ("%s As %s\n", result.to_string (), type_name);
		}
	} catch (EvaluationError e) {
		stdout.printf ("Error: %s\n", e.message);
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
		if (!try_more) {
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
	return (Expression) result.get_object ();
}

private extern string? sane_readline (string prompt);

bool interactive;
bool debug_parsing;
string? filename;
const OptionEntry[] options = {
	{ "debug-parsing", 0, 0, OptionArg.NONE, ref debug_parsing, "Turn on GTeonoma parsing output.", null },
	{ "file", 'f', 0, OptionArg.FILENAME, ref filename, "Input file to read.", "FILE" },
	{ "interactive", 'i', 0, OptionArg.NONE, ref interactive, "Enter interactive shell.", null },
	{ null }
};
int main(string[] args) {
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
	var rules = new Rules ();
	var engine = new ExecutionEngine ();
	Tuple? root_tuple = null;
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
			root_tuple = engine.start_from ((File) result.get_object ());
		} catch (EvaluationError e) {
			stderr.printf ("%s: %s\n", filename ?? "stdin", e.message);
			return 1;
		}
	}
	if (args.length == 1 && !interactive) {
		print_expression (engine, new Expressions.This ());
	} else {
		for (var it = 1; it < args.length; it++) {
			var arg_parser = new GTeonoma.StringParser (rules, args[it].strip (), "argument %d".printf (it));
			var expression = do_parsing (rules, arg_parser);
			if (expression == null) {
				return 1;
			}
			print_expression (engine, expression);
		}
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
					if (next_line != null) {
						line = "%s\n%s".printf (line, next_line.strip ());
					}
				}
			} while (try_more);

			if (expression != null) {
				print_expression (engine, expression);
			}
			Readline.History.add (line);
		}
	}
	return 0;
}
