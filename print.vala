using Flabbergast;

public Expression? do_parsing (Rules rules, GTeonoma.Parser parser) {
	Value result;
	GTeonoma.Result r;
	if ((r = parser.parse (typeof (Expression), out result)) != GTeonoma.Result.OK) {
		parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
		return null;
	}
	if (!parser.is_finished ()) {
		var end = parser.get_location ();
		stderr.printf ("%s:%d:%d: Junk at end of input.\n", end.source, end.line, end.offset);
		return null;
	}
	return ((Expression) result.get_object ()).transform ();
}

string? filename;
string? output;
const OptionEntry[] options = {
	{ "file", 'f', 0, OptionArg.FILENAME, ref filename, "Input file to read.", "FILE" },
	{ "output", 'o', 0, OptionArg.FILENAME, ref output, "Write output to file.", "FILE" },
	{ null }
};

int main (string[] args) {
	try {
		var opt_context = new OptionContext ("- Flabbergast");
		opt_context.set_help_enabled (true);
		opt_context.add_main_entries (options, null);
		opt_context.parse (ref args);
	} catch (OptionError e) {
		stderr.printf ("%s\n", e.message);
		stderr.printf ("Run '%s --help' to see a full list of available command line options.\n", args[0]);
		return 1;
	}
	if (args.length < 2) {
		stderr.printf ("No expression specified.\n");
		return 1;
	}
	Rules rules;
	try {
		rules = new Rules ();
	} catch (GTeonoma.RegisterError e) {
		stderr.printf ("Grammar error: %s\n", e.message);
		return 1;
	}
	FileStream? output_file = null;
	if (output != null) {
		output_file = FileStream.open (output, "w");
		if (output_file == null) {
			stderr.printf ("Failed to open: %s\n", output);
			return 1;
		}
	}
	var engine = new ExecutionEngine ();
	Data.Tuple? root_tuple = null;
	var parser = GTeonoma.StreamParser.open (rules, filename?? "/dev/stdin");
	if (parser == null) {
		stderr.printf ("Failed to open input file.\n");
		return 1;
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
	for (var it = 1; it < args.length; it++) {
		var arg_parser = new GTeonoma.StringParser (rules, args[it].strip (), "argument %d".printf (it));
		var expression = do_parsing (rules, arg_parser);
		if (expression == null) {
			return 1;
		}
		try {
			var result = engine.run (expression);
			(output_file?? stdout).puts (result.to_string ());
		} catch (EvaluationError e) {
			stderr.printf ("argument %d: %s\n", it, e.message);
			return 1;
		}
	}
	return 0;
}
