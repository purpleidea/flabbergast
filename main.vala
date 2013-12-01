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
			stdout.printf ("%s : %s\n", result.to_string (), result.g_type.name ());
		}
	} catch (EvaluationError e) {
		stdout.printf ("Error: %s\n", e.message);
	}
}
static bool debug_parsing;
const OptionEntry[] options = {
	{ "debug-parsing", 0, 0, OptionArg.NONE, ref debug_parsing, "Turn on GTeonoma parsing output ", null },
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
	var rules = new Rules ();
	var engine = new ExecutionEngine ();
	var parser = GTeonoma.FileParser.open (rules, args[1]);
	if (parser == null) {
		stderr.printf ("Failed to open input file.\n");
		return 1;
	}
	if (debug_parsing) {
		GTeonoma.log_to_console (parser);
	}
	Gee.List<Expression> list;
	parser.parse_all<Expression> (out list, "%-;%n");
	foreach (var expression in list) {
		print_expression (engine, expression);
	}
	return 0;
}