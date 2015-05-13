package flabbergast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flabbergast.Generator.ParameterisedBlock;

/**
 * The input being parsed along with all the memorised information from the
 * pack-rat parsing.
 */
public class Parser {
	/**
	 * A memory from previously-parsed passes and the final parse state.
	 */
	class Memory {
		int column;
		int index;
		AstNode result;
		int row;
	}

	/**
	 * The current position during parsing
	 */
	class Position {
		private int column;
		private ErrorCollector error_collector;
		private int index;
		private int row;

		private int trace_depth;

		Position(ErrorCollector error_collector) {
			index = 0;
			row = 1;
			column = 1;
			trace_depth = 0;
			this.error_collector = error_collector;
		}

		/**
		 * Create a new memory based on the type of the result.
		 * 
		 * @param result
		 *            The result to cache, or null if parsing failed.
		 */
		<T extends AstNode> void cache(int start_index, T result, Class<T> clazz) {
			if (!cache.containsKey(start_index)) {
				cache.put(start_index,
						new HashMap<Class<? extends AstNode>, Parser.Memory>());
			}
			Memory memory = new Memory();
			memory.result = result;
			memory.index = index;
			memory.row = row;
			memory.column = column;
			cache.get(start_index).put(clazz, memory);
		}

		/**
		 * Create a new memory based on the name of the result.
		 * 
		 * @param result
		 *            The result to cache, or null if parsing failed.
		 */
		public <T extends AstNode> void cache(String name, int start_index,
				T result) {
			if (!alternate_cache.containsKey(start_index)) {
				alternate_cache.put(start_index,
						new HashMap<String, Parser.Memory>());
			}
			Memory memory = new Memory();
			memory.result = result;
			memory.index = index;
			memory.row = row;
			memory.column = column;
			alternate_cache.get(start_index).put(name, memory);
		}

		/**
		 * Determine if the current position has been parsed based on the type
		 * of the rule.
		 */
		@SuppressWarnings("unchecked")
		<U extends AstNode, T extends U> boolean checkCache(Ptr<U> result,
				Class<T> clazz) {
			if (cache.containsKey(index) && cache.get(index).containsKey(clazz)) {
				Memory memory = cache.get(index).get(clazz);
				result.set((U) memory.result);
				index = memory.index;
				row = memory.row;
				column = memory.column;
				if (trace) {
					for (int it = 1; it < trace_depth; it++) {
						System.out.print(" ");
					}
					System.out.printf("%d:%d %s %s\n", row, column,
							(result.get() == null ? " M " : " H "),
							clazz.getName());
					trace_depth++;
				}
				return true;
			}
			return false;
		}

		/**
		 * Determine if the current position has been parsed based on the name
		 * of the rule.
		 */
		@SuppressWarnings("unchecked")
		<T extends AstNode> boolean checkCache(String name, Ptr<T> result) {
			if (alternate_cache.containsKey(index)
					&& alternate_cache.get(index).containsKey(name)) {
				Memory memory = alternate_cache.get(index).get(name);
				result.set((T) memory.result);
				index = memory.index;
				row = memory.row;
				column = memory.column;
				if (trace) {
					for (int it = 1; it < trace_depth; it++) {
						System.out.print(" ");
					}
					System.out.printf("%d:%d %s %s\n", row, column,
							(result.get() == null ? " M " : " H "), name);
					trace_depth++;
				}
				return true;
			}
			return false;
		}

		Position dup() {
			Position child = new Position(error_collector);
			child.index = index;
			child.row = row;
			child.column = column;
			return child;
		}

		int getColumn() {
			return column;
		}

		String getFileName() {
			return Parser.this.file_name;
		}

		int getIndex() {
			return index;
		}

		Parser getParser() {
			return Parser.this;
		}

		int getRow() {
			return row;
		}

		boolean isFinished() {
			return index >= input.length();
		}

		/**
		 * Match a fixed string in the input.
		 */
		boolean match(String word) {
			for (int it = 0; it < word.length(); it++) {
				if (word.charAt(it) != next()) {
					return false;
				}
			}
			return true;
		}

		void nameConstraint(String name) {
			error_collector.reportParseError(file_name, index, row, column,
					"The name " + name + " is already in use in this context.");
		}

		/**
		 * Consume a character from the input and return it.
		 */
		char next() {
			if (index < input.length()) {
				char c = input.charAt(index++);
				if (c == '\n') {
					row++;
					column = 0;
				} else {
					column++;
				}
				return c;
			} else {
				return '\0';
			}
		}

		/**
		 * Look back at the previously consumed character.
		 */
		char peekLast() {
			if (index > 0 && index <= input.length()) {
				return input.charAt(index - 1);
			} else {
				return '\0';
			}
		}

		/**
		 * Look the character to be consumed.
		 */
		char peekNext() {
			if (index > 0 && index < input.length()) {
				return input.charAt(index);
			} else {
				return '\0';
			}
		}

		/**
		 * Start a tracing block.
		 * 
		 * Must be matched to a finish.
		 */
		void traceEnter(String rule) {
			if (trace) {
				trace_depth++;
				for (int it = 1; it < trace_depth; it++) {
					System.out.print(" ");
				}
				System.out.printf("%d:%d > %s\n", row, column, rule);
			}
		}

		/**
		 * Finish a tracing block.
		 * 
		 * Must be matched to a start.
		 */
		void traceExit(String rule, boolean success) {
			if (trace) {
				trace_depth--;
				for (int it = 1; it < trace_depth; it++) {
					System.out.print(" ");
				}
				System.out.printf("%d:%s <%s %s\n", row, column, (success
						? "+"
						: "-"), rule);
			}
		}

		/**
		 * Mark an error at the current position.
		 */
		void update(String message, String syntax_name) {
			if (index > Parser.this.index + 1) {
				message = "Expected " + message + " while parsing "
						+ syntax_name + " but got "
						+ Parser.toLiteral("" + peekLast()) + " instead.";
				Parser.this.row = row;
				Parser.this.column = column;
				Parser.this.index = index;
			}
		}
	}

	private static boolean isWhiteSpace(String input) {
		for (int it = 0; it < input.length(); it++) {
			if (!Character.isWhitespace(input.charAt(it))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Open a file for parsing.
	 * 
	 * @throws IOException
	 */
	public static Parser open(String filename) throws IOException {
		return new Parser(filename, new String(Files.readAllBytes(Paths
				.get(filename))));
	}

	/**
	 * Format a string for public viewing.
	 */
	public static String toLiteral(String input) {
		if (input.length() == 0 || isWhiteSpace(input)) {
			return "whitespace";
		} else {
			return "one of \"" + input + "\"";
		}
	}

	Map<Integer, Map<String, Memory>> alternate_cache = new HashMap<Integer, Map<String, Memory>>();
	Map<Integer, Map<Class<? extends AstNode>, Memory>> cache = new HashMap<Integer, Map<Class<? extends AstNode>, Memory>>();
	int column;
	private String file_name;
	/**
	 * The position of the most helpful error yet produced by the parser.
	 */
	int index = -1;
	/**
	 * The characters to parse.
	 */
	String input;

	/**
	 * The most helpful error yet produced by the parser.
	 * 
	 * This may have a value even if the parse was successful.
	 */
	String message;

	int row;

	private boolean trace;

	public Parser(String filename, String input) {
		file_name = filename;
		this.input = input;
	}

	/**
	 * The name of the file being parsed for debugging and error information.
	 */
	public String getFileName() {
		return file_name;
	}

	/**
	 * Whether to produce copious junk on standard output.
	 */
	public boolean getTrace() {
		return trace;
	}

	/**
	 * Parse in the “file” context defined in the language specification.
	 */
	public <T> T parseFile(ErrorCollector collector, CompilationUnit<T> unit,
			String type_name) throws Exception {
		final Ptr<file> result = new Ptr<file>();
		Ptr<Position> position = new Ptr<Position>(new Position(collector));
		if (file.parseRule_Base(position, result)
				&& position.get().isFinished()) {
			if (result.get().analyse(collector)) {
				return unit.createRootGenerator(result.get(), type_name,
						new Generator.Block() {

							@Override
							public void invoke(final Generator generator)
									throws Exception {
								result.get()
										.generate(
												generator,
												new ParameterisedBlock<LoadableValue>() {

													@Override
													public void invoke(
															LoadableValue value)
															throws Exception {
														generator
																.doReturn(value);

													}

												});

							}
						});
			}
		} else {
			collector.reportParseError(file_name, index, row, column, message);
		}
		return null;
	}

	/**
	 * Parse a rule and, if successful, put the result into the provided list.
	 */
	<T> boolean parseIntoList(Ptr<Position> position, List<T> result,
			ParseRule<T> rule) {
		Ptr<T> obj = new Ptr<T>();
		if (rule.invoke(position, obj)) {
			result.add(obj.get());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Parse in the “repl” context defined in the language specification.
	 */
	public <T> T parseRepl(ErrorCollector collector, CompilationUnit<T> unit,
			String type_name) throws Exception {
		final Ptr<repl> result = new Ptr<repl>();
		Ptr<Position> position = new Ptr<Position>(new Position(collector));
		if (repl.parseRule_Base(position, result)
				&& position.get().isFinished()) {
			if (result.get().analyse(collector)) {
				return unit.createReplGenerator(result.get(), type_name,
						new ReplGenerator.Block() {

							@Override
							public void invoke(final Generator generator,
									LoadableValue root_frame,
									LoadableValue current_frame,
									LoadableValue update_current,
									LoadableValue escape_value,
									LoadableValue print_value

							) throws Exception {
								result.get()
										.generate(
												generator,
												root_frame,
												current_frame,
												update_current,
												escape_value,
												print_value,
												new ParameterisedBlock<LoadableValue>() {

													@Override
													public void invoke(
															LoadableValue value)
															throws Exception {
														generator
																.doReturn(value);

													}

												});

							}
						});
			}
		} else {
			collector.reportParseError(file_name, index, row, column, message);
		}
		return null;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}
}
