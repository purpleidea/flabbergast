package flabbergast;

import java.io.IOException;

import jline.ConsoleReader;
import jline.SimpleCompletor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MainREPL {

	public static class PrintToConsole extends ElaboratePrinter {
		private ConsoleReader reader;

		public PrintToConsole(ConsoleReader reader) {
			this.reader = reader;
		}

		@Override
		protected void write(String string) throws IOException {
			reader.printString(string);
		}

	}

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("h", "help", false, "Show this message and exit");
		CommandLineParser cl_parser = new GnuParser();
		CommandLine result;

		try {
			result = cl_parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}

		if (result.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			System.err
					.println("Run a Flabbergast file and browse the results or just enter expessions to see what happens.");
			formatter.printHelp("gnu", options);
			System.exit(1);
		}

		String[] files = result.getArgs();
		if (files.length > 1) {
			System.err.println("Only one Flabbergast script may be given.");
			System.exit(1);
		}

		ErrorCollector collector = new ConsoleCollector();
		DynamicCompiler compiler = new DynamicCompiler(collector);
		TaskMaster task_master = new ConsoleTaskMaster();
		task_master.addUriHandler(BuiltInLibraries.INSTANCE);
		task_master.addUriHandler(new LoadPrecompiledLibraries());
		task_master.addUriHandler(compiler);
		final Ptr<Frame> root = new Ptr<Frame>();
		try {
			if (files.length == 1) {
				Parser parser = Parser.open(files[0]);
				Class<? extends Computation> run_type = parser.parseFile(
						collector, compiler.getCompilationUnit(), "Printer");
				if (run_type != null) {
					Computation computation = run_type.getConstructor(
							TaskMaster.class).newInstance(task_master);
					computation.listen(new ConsumeResult() {

						@Override
						public void consume(Object result) {
							root.set((Frame) result);

						}
					});
					task_master.slot(computation);
					task_master.run();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		try {
			ConsoleReader reader = new ConsoleReader();
			reader.addCompletor(new SimpleCompletor(new String[] { "args",
					"value", "Bool", "By", "Container", "Each", "Else", "Enforce",
					"Error", "False", "Finite", "Float", "FloatMax",
					"FloatMin", "For", "Frame", "From", "GenerateId", "Id",
					"If", "In", "Infinity", "Int", "IntMax", "IntMin", "Is",
					"Length", "Let", "Lookup", "NaN", "Name", "Null", "Order",
					"Ordinal", "Reduce", "Reverse", "Select", "Str",
					"Template", "Then", "This", "Through", "To", "True",
					"Where", "With" }));
			reader.setDefaultPrompt("‽ ");
			reader.setUseHistory(true);
			reader.setUsePagination(true);
			String line;
			int id = 0;
			ElaboratePrinter printer = new PrintToConsole(reader);
			while ((line = reader.readLine()) != null) {
				Parser parser = new Parser("<console>", line);
				Class<? extends Computation> run_type = parser.parseFile(
						collector, compiler.getCompilationUnit(),
						"flabbergast.interactive.Line" + (id++));
				if (run_type != null) {
					Computation computation = run_type.getConstructor(
							TaskMaster.class).newInstance(task_master);
					computation.listen(printer);
					task_master.slot(computation);
					task_master.run();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
