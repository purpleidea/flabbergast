using System;
using System.Collections.Generic;
namespace Flabbergast {

public delegate bool ParseRule<T>(ref ParserPosition position, out T result);

public abstract class AstNode {
	public int StartRow;
	public int StartColumn;
	public int EndRow;
	public int EndColumn;
	public string FileName;
	public static AstNode ParseFile(Parser parser) {
		file result;
		var position = new ParserPosition(parser);
		if (Flabbergast.file.ParseRule_Base(ref position, out result) && position.Finished) {
			return result;
		} else {
			return null;
		}
	}
}
public class Parser {
	internal struct Memory {
		internal Object Result;
		internal int Index;
	}
	internal string Input;
	internal int Index = -1;
	internal Dictionary<int, Dictionary<Type, Memory>> Cache = new Dictionary<int, Dictionary<Type, Memory>>();
	internal Dictionary<int, Dictionary<string, Memory>> AlternateCache = new Dictionary<int, Dictionary<string, Memory>>();
	public string Message { get; internal set; }
	public string FileName { get; private set; }
	public bool Trace { get; set; }

	public static Parser Open(string filename) {
		var file = new System.IO.StreamReader(filename);
		var parser = new Parser(filename, file.ReadToEnd());
		file.Close();
		return parser;
	}

	public static string ToLiteral(string input) {
		if (String.IsNullOrWhiteSpace(input)) {
			return "whitespace";
		} else {
			return "one of \"" + input + "\"";
		}
	}

	public Parser(string filename, string input) {
		FileName = filename;
		Input = input;
	}
}
public class ParserPosition {
	public Parser Parser { get; private set; }
	public int Index { get; private set; }
	public int Row { get; private set; }
	public int Column { get; private set; }
	public bool Finished { get { return Index >= Parser.Input.Length; } }
	private int TraceDepth;
	public ParserPosition(Parser parser) {
		Parser = parser;
		Index = 0;
		Row = 1;
		Column = 0;
		TraceDepth = 0;
	}
	public bool CheckCache<T, U>(out U result) where T: U {
		if (Parser.Cache.ContainsKey(Index) && Parser.Cache[Index].ContainsKey(typeof(T))) {
			var memory = Parser.Cache[Index][typeof(T)];
			result = (U) memory.Result;
			Index = memory.Index;
			if (Parser.Trace) {
				for (var it = 1; it < TraceDepth; it++) {
					System.Console.Write(" ");
				}
				System.Console.WriteLine(Row + ":" + Column + (result == null ? " M " : " H ") + typeof(T));
				TraceDepth++;
			}
			return true;
		}
		result = default(T);
		return false;
	}
	public bool CheckCache<T>(string name, out T result) {
		if (Parser.AlternateCache.ContainsKey(Index) && Parser.AlternateCache[Index].ContainsKey(name)) {
			var memory = Parser.AlternateCache[Index][name];
			result = (T) memory.Result;
			Index = memory.Index;
			if (Parser.Trace) {
				for (var it = 1; it < TraceDepth; it++) {
					System.Console.Write(" ");
				}
				System.Console.WriteLine(Row + ":" + Column + (result == null ? " M " : " H ") + name);
				TraceDepth++;
			}
			return true;
		}
		result = default(T);
		return false;
	}
	public void Cache<T>(int start_index, T result) {
		if (!Parser.Cache.ContainsKey(start_index)) {
			Parser.Cache[start_index] = new Dictionary<Type, Parser.Memory>();
		}
		Parser.Cache[start_index][typeof(T)] = new Parser.Memory() { Result = result, Index = Index };
	}
	public void Cache<T>(string name, int start_index, T result) {
		if (!Parser.AlternateCache.ContainsKey(start_index)) {
			Parser.AlternateCache[start_index] = new Dictionary<string, Parser.Memory>();
		}
		Parser.AlternateCache[start_index][name] = new Parser.Memory() { Result = result, Index = Index };
	}
	public ParserPosition Clone() {
		var child = new ParserPosition(Parser);
		child.Index = Index;
		child.Row = Row;
		child.Column = Column;
		child.TraceDepth = TraceDepth;
		return child;
	}
	public Char Next() {
		if (Index < Parser.Input.Length) {
			var c = Parser.Input[Index++];
			if (c == '\n') {
				Row++;
				Column = 0;
			} else {
				Column++;
			}
			return c;
		} else {
			return '\0';
		}
	}
	public Char PeekLast() {
		if (Index > 0 && Index <= Parser.Input.Length) {
			return Parser.Input[Index - 1];
		} else {
			return '\0';
		}		
	}
	public bool Match(string word) {
		bool result = String.Compare(Parser.Input, Index, word, 0, word.Length) == 0;
		Index += word.Length;
		return result;
	}
	public static bool ParseIntoList<T>(ref ParserPosition position, List<T> result, ParseRule<T> rule) {
		T obj;
		if (rule(ref position, out obj)) {
			result.Add(obj);
			return true;
		} else {
			return false;
		}
	}
	public void TraceEnter(string rule) {
		if (Parser.Trace) {
			TraceDepth++;
			for (var it = 1; it < TraceDepth; it++) {
				System.Console.Write(" ");
			}
			System.Console.WriteLine(Row + ":" + Column + " > " + rule);
		}
	}

	public void TraceExit(string rule, bool success) {
		if (Parser.Trace) {
			TraceDepth--;
			for (var it = 1; it < TraceDepth; it++) {
				System.Console.Write(" ");
			}
			System.Console.WriteLine(Row + ":" + Column + " <" + (success ? "+" : "-") + " " + rule);
		}
	}

	public void Update(string message, string syntax_name) {
		if (Index > Parser.Index + 1) {
			Parser.Message = Parser.FileName + ":" + Row + ":" + Column + ": Expected " + message + " while parsing " + syntax_name + " but got `" + PeekLast() + "' instead.";
			Parser.Index = Index;
		}
	}
}
}
