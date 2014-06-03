using Flabbergast;

[CCode (array_null_terminated = true)]
string[]? filenames;
const OptionEntry[] options = {
	{ "", 0, 0, OptionArg.FILENAME_ARRAY, ref filenames, null, "FILE..." },
	{ null }
};
int main (string[] args) {
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
	if (filenames.length == 0) {
		stderr.printf ("No files specified.\n");
		return 1;
	}
	Rules rules;
	try {
		rules = new Rules ();
	} catch (GTeonoma.RegisterError e) {
		stderr.printf ("Grammar error: %s\n", e.message);
		return 1;
	}
	var exit_code = 0;
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
			exit_code = 1;
			continue;
		}
		if (!parser.is_finished ()) {
			var end = parser.get_location ();
			stderr.printf ("%s:%d:%d: Junk at end of input.\n", end.source, end.line, end.offset);
			exit_code = 1;
			continue;
		}
		parser = null;
		var printer = new GTeonoma.Printer (rules);
		printer.print (result);
		try {
			FileUtils.set_contents (filename, printer.str);
		} catch (FileError e) {
			stderr.printf ("%s: %s\n", filename, e.message);
			exit_code = 1;
		}
	}
	return exit_code;
}
