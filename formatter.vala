using Flabbergast;

[CCode (array_null_terminated = true)]
string[]? filenames;
const OptionEntry[] options = {
	{ "", 0, 0, OptionArg.FILENAME_ARRAY, ref filenames, null, "FILE..." },
	{ null }
};
int main(string[] args) {
	try {
		var opt_context = new OptionContext ("- Flabbergast Formatter");
		opt_context.set_help_enabled (true);
		opt_context.add_main_entries (options, null);
		opt_context.parse (ref args);
	} catch (OptionError e) {
		stdout.printf ("%s\n", e.message);
		stdout.printf ("Run '%s --help' to see a full list of available command line options.\n", args[0]);
		return 1;
	}
	stderr.printf ("Flabbergast – %s %s\n", Package.STRING, Package.URL);
	var rules = new Rules ();
	foreach (var filename in filenames) {
		stderr.printf ("Reformatting %s...\n", filename);
		var parser = GTeonoma.FileParser.open (rules, filename);
		if (parser == null) {
			stderr.printf ("Failed to open input file %s.\n", filename);
			continue;
		}
		Value result;
		if ((parser.parse (typeof (File), out result)) != GTeonoma.Result.OK) {
			parser.visit_errors ((source, error) => stderr.printf ("%s:%d:%d: %s\n", source.source, source.line, source.offset, error));
			continue;
		}
		if (!parser.is_finished ()) {
			var end = parser.get_location ();
			stderr.printf ("%s:%d:%d: Junk at end of input.\n", end.source, end.line, end.offset);
			continue;
		}
		parser = null;
		var printer = GTeonoma.FilePrinter.open (rules, filename);
		printer.print (result);
	}
	return 0;
}
