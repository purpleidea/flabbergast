using System;
using System.Collections.Generic;
namespace Flabbergast {

public delegate bool ParseRule<T>(ref Parser parser, out T result);

public abstract class AstNode {
	public int StartRow;
	public int StartColumn;
	public int EndRow;
	public int EndColumn;
	public string FileName;
}

public class Parser {
	private string Input;
	private int Index;
	public int Row { get; private set; }
	public int Column { get; private set; }
	public string FileName { get; private set; }
	public Parser(string filename, string input) {
		FileName = filename;
		Input = input;
		Index = 0;
	}
	public Parser Clone() {
		var child = new Parser(FileName, Input);
		child.Index = Index;
		child.Row = Row;
		child.Column = Column;
		return child;
	}
	public Char Next() {
		if (Index < Input.Length) {
			var c = Input[Index++];
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
		if (Index > 0 && Index <= Input.Length) {
			return Input[Index - 1];
		} else {
			return '\0';
		}		
	}
	public bool Match(string word) {
		bool result =  String.Compare(Input, Index, word, 0, Int32.MaxValue) == 0;
		Index += word.Length;
		return result;
	}
	public static bool ParseIntoList<T, U>(ref Parser parser, List<T> result, ParseRule<U> rule) where U : T {
		U obj;
		if (rule(ref parser, out obj)) {
			result.Add(obj);
			return true;
		} else {
			return false;
		}
	}
}
internal class ParseOptionalFailure : Exception { }
internal class ParseAlternateComplete : Exception { }
}
