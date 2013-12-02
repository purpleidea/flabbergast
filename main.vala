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

public Expression? do_parsing<T> (Rules rules, GTeonoma.Parser parser) {
	if (debug_parsing) {
		GTeonoma.log_to_console (parser);
	}
	Value result;
	if (parser.parse (typeof (T), out result) != GTeonoma.Result.OK) {
		parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
		return null;
	}
	if (!parser.is_finished ()) {
		var end = parser.get_location ();
		stderr.printf ("%s:%d:%d: Junk at end of file.\n", end.source, end.line, end.offset);
		return null;
	}
	return (Expression) result.get_object ();
}

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
		var root = do_parsing<Expressions.File> (rules, parser);
		if (root == null) {
			return 1;
		}
		try {
			root_tuple = engine.start_from (root);
		} catch (EvaluationError e) {
			stderr.printf ("%s: %s\n", filename ?? "stdin", e.message);
			return 1;
		}
	}
	if (args.length == 1 && !interactive) {
		print_expression (engine, new Expressions.This ());
	} else {
		for (var it = 1; it < args.length; it++) {
			var arg_parser = new GTeonoma.StringParser (rules, args[it], "argument %d".printf (it));
			var expression = do_parsing<Expression> (rules, arg_parser);
			if (expression == null) {
				return 1;
			}
			print_expression (engine, expression);
		}
	}
	if (interactive) {
		string line;
		var it = 0;
		while ((line = Readline.readline ("‽ >")) != null) {
			Readline.History.add (line);
			var arg_parser = new GTeonoma.StringParser (rules, line, "input %d".printf (it++));
			var expression = do_parsing<Expression> (rules, arg_parser);
			if (expression != null) {
				print_expression (engine, expression);
			}
		}
		stderr.printf ("\nExiting.\n");
	}
	return 0;
}