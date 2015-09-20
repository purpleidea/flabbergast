package flabbergast;

import java.io.File;
import java.io.IOException;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class MainREPL {

	public static class PrintToConsole extends ElaboratePrinter {
		private ConsoleReader reader;
		private Object value;

		public PrintToConsole(ConsoleReader reader) {
			this.reader = reader;
		}

		@Override
		public void consume(Object result) {
			value = result;
		}

		public void print() {
			if (value != null)
				print(value);
		}

		@Override
		protected void write(String string) throws IOException {
			reader.print(string);
		}

	}

	static class RawPrint implements ConsumeResult {
		private ConsoleReader reader;

		public RawPrint(ConsoleReader reader) {
			this.reader = reader;
		}

		@Override
		public void consume(Object result) {
			try {
				reader.println(result.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static class CurrentFrame implements ConsumeResult {
		private Frame current;
		public CurrentFrame(Frame initial) {
			current = initial;
		}
		public Frame get() {
			return current;
		}
		@Override
		public void consume(Object result) {
			if (result != null && result instanceof Frame) {
				current = (Frame) result;
			}
		}
	}

	static class KeepRunning implements ConsumeResult {
		private boolean keep_running = true;

		boolean allowed() {
			return keep_running;
		}
		@Override
		public void consume(Object result) {
			if (result != null && result instanceof Boolean) {
				keep_running = (Boolean) result;
			}
		}
	}

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("p", "no-precomp", false,
				"Do not use precompiled libraries");
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

		String accessory_lib_path = null;
		try {
			accessory_lib_path = new File(files.length == 1
					? new File(files[0]).getParentFile()
					: new File("."), "lib").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ErrorCollector collector = new ConsoleCollector();
		DynamicCompiler compiler = new DynamicCompiler(collector);
		if (accessory_lib_path != null)
			compiler.prependPath(accessory_lib_path);
		ConsoleTaskMaster task_master = new ConsoleTaskMaster();
		task_master.addUriHandler(BuiltInLibraries.INSTANCE);
		task_master.addUriHandler(JdbcUriHandler.INSTANCE);
		task_master.addUriHandler(EnvironmentUriHandler.INSTANCE);
		if (!result.hasOption('p')) {
			LoadPrecompiledLibraries precomp = new LoadPrecompiledLibraries();
			task_master.addUriHandler(precomp);
			if (accessory_lib_path != null)
				precomp.prependPath(accessory_lib_path);
		}
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
					computation.slot();
					task_master.run();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		if (root.get() == null) {
			root.set(new MutableFrame(task_master, new NativeSourceReference(
					"REPL"), null, null));
		}
		CurrentFrame current = new CurrentFrame(root.get());

		try {
			ConsoleReader reader = new ConsoleReader();
			reader.addCompleter(new StringsCompleter("args", "value", "Append",
					"Bool", "By", "Container", "Drop", "Each", "Else",
					"Enforce", "Error", "False", "Finite", "Float", "FloatMax",
					"FloatMin", "For", "Frame", "From", "GenerateId", "Id",
					"If", "In", "Infinity", "Int", "IntMax", "IntMin", "Is",
					"Length", "Let", "Lookup", "NaN", "Name", "Now", "Null",
					"Order", "Ordinal", "Reduce", "Required", "Reverse",
					"Select", "Str", "Template", "Then", "This", "Through",
					"To", "True", "Used", "Where", "With"));
			reader.setPrompt("‽ ");
			reader.setHistoryEnabled(true);
			reader.setPaginationEnabled(true);
			String line;
			int id = 0;
			KeepRunning keep_running = new KeepRunning();
			while (keep_running.allowed() && (line = reader.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;
				Parser parser = new Parser("<console>", line);
				PrintToConsole printer = new PrintToConsole(reader);
				RawPrint raw_printer = new RawPrint(reader);
				Class<? extends Computation> run_type = parser.parseRepl(
						collector, compiler.getCompilationUnit(),
						"flabbergast/interactive/Line" + (id++));
				if (run_type != null) {
					Computation computation = run_type.getConstructor(
							TaskMaster.class, Frame.class, Frame.class,
							ConsumeResult.class, ConsumeResult.class,
							ConsumeResult.class).newInstance(task_master,
							root.get(), current.get(), current, printer,
							raw_printer);
					computation.listen(keep_running);
					computation.slot();
					task_master.run();
					printer.print();
					task_master.reportCircularEvaluation();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
