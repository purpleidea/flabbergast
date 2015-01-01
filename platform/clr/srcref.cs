using System;
using System.IO;

namespace Flabbergast {

public abstract class SourceReference {
	public abstract void Write(TextWriter writer, int indentation = 0, string prefix = "");
}

public class SimpleReference : SourceReference {
	public string Message { get; private set; }
	public string FileName { get; private set; }
	public int StartLine { get; private set; }
	public int StartColumn { get; private set; }
	public int EndLine { get; private set; }
	public int EndColumn { get; private set; }
	public SourceReference Caller { get; private set; }
	public SimpleReference(string message, string filename, int start_line, int start_column, int end_line, int end_column, SourceReference caller) {
		Message = message;
		FileName = filename;
		StartLine = start_line;
		StartColumn = start_column;
		EndLine = end_line;
		EndColumn = end_column;
		Caller = caller;
	}
	public override void Write(TextWriter writer, int indentation, string prefix) {
		writer.Write(prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write('\t');
		writer.Write(FileName);
		writer.Write(": ");
		writer.Write(StartLine);
		writer.Write(":");
		writer.Write(StartColumn);
		writer.Write("-");
		writer.Write(EndLine);
		writer.Write(":");
		writer.Write(EndColumn);
		writer.Write(": ");
		writer.Write(Message);
		writer.Write("\n");
		if (Caller != null)
			Caller.Write(writer, indentation, prefix);
	}
}

public class JunctionReference : SourceReference {
	public string Message { get; private set; }
	public string FileName { get; private set; }
	public int StartLine { get; private set; }
	public int StartColumn { get; private set; }
	public int EndLine { get; private set; }
	public int EndColumn { get; private set; }
	public SourceReference Caller { get; private set; }
	public SourceReference Junction { get; private set; }
	public JunctionReference(string message, string filename, int start_line, int start_column, int end_line, int end_column, SourceReference caller, SourceReference junction) {
		Message = message;
		FileName = filename;
		StartLine = start_line;
		StartColumn = start_column;
		EndLine = end_line;
		EndColumn = end_column;
		Caller = caller;
		Junction = junction;
	}
	public override void Write(TextWriter writer, int indentation, string prefix) {
		writer.Write(prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write('\t');
		writer.Write(FileName);
		writer.Write(": ");
		writer.Write(StartLine);
		writer.Write(":");
		writer.Write(StartColumn);
		writer.Write("-");
		writer.Write(EndLine);
		writer.Write(":");
		writer.Write(EndColumn);
		writer.Write(": ");
		writer.Write(Message);
		writer.Write("\n");
		Junction.Write(writer, indentation + 1, prefix);
		if (Caller != null)
			Caller.Write(writer, indentation, prefix);
	}
}
public class IterationReference : SourceReference {
	public string Name { get; private set; }
	public int Index { get; private set; }
	public string FileName { get; private set; }
	public int StartLine { get; private set; }
	public int StartColumn { get; private set; }
	public int EndLine { get; private set; }
	public int EndColumn { get; private set; }
	public SourceReference Caller { get; private set; }
	public IterationReference(string name, int index, string filename, int start_line, int start_column, int end_line, int end_column, SourceReference caller) {
		Name = name;
		Index = index;
		FileName = filename;
		StartLine = start_line;
		StartColumn = start_column;
		EndLine = end_line;
		EndColumn = end_column;
		Caller = caller;
	}
	public override void Write(TextWriter writer, int indentation, string prefix) {
		writer.Write(prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write('\t');
		writer.Write(FileName);
		writer.Write(": ");
		writer.Write(StartLine);
		writer.Write(":");
		writer.Write(StartColumn);
		writer.Write("-");
		writer.Write(EndLine);
		writer.Write(":");
		writer.Write(EndColumn);
		writer.Write(": Fricassée expression with attribute {0} and index {1}.\n", Name, Index);
		if (Caller != null)
			Caller.Write(writer, indentation, prefix);
	}
}
}
