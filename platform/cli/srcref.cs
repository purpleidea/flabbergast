using System;
using System.IO;

namespace Flabbergast {

/**
 * Description of the current Flabbergast stack.
 *
 * Since the Flabbergast stack is utterly alien to the underlying VM (it can
 * bifurcate), this object records it such that it can be presented to the user
 * when needed.
 */
public class SourceReference {
	public string Message { get; private set; }
	public string FileName { get; private set; }
	public int StartLine { get; private set; }
	public int StartColumn { get; private set; }
	public int EndLine { get; private set; }
	public int EndColumn { get; private set; }
	public SourceReference Caller { get; private set; }

	public SourceReference(string message, string filename, int start_line, int start_column, int end_line, int end_column, SourceReference caller) {
		Message = message;
		FileName = filename;
		StartLine = start_line;
		StartColumn = start_column;
		EndLine = end_line;
		EndColumn = end_column;
		Caller = caller;
	}
	/**
	 * Write the current stack trace.
	 */
	public virtual void Write(TextWriter writer, int indentation, string prefix) {
		WriteMessage(writer, indentation, prefix);
		if (Caller != null)
			Caller.Write(writer, indentation, prefix);
	}
	protected void WriteMessage(TextWriter writer, int indentation, string prefix) {
		writer.Write(prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write("  ");
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
	}
}

/**
 * A stack element that bifurcates.
 *
 * These are typical of instantiation and tuple overriding that have both a
 * container and an ancestor.
 */
public class JunctionReference : SourceReference {
	/**
	 * The stack trace of the non-local component. (i.e., the ancestor's stack trace).
	 */
	public SourceReference Junction { get; private set; }
	public JunctionReference(string message, string filename, int start_line, int start_column, int end_line, int end_column, SourceReference caller, SourceReference junction) : base(message, filename, start_line, start_column, end_line, end_column, caller) {
		Junction = junction;
	}
	public override void Write(TextWriter writer, int indentation, string prefix) {
		WriteMessage(writer, indentation, prefix);
		writer.Write(prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write("  ");
		writer.WriteLine("↳");
		Junction.Write(writer, indentation + 1, prefix);
		for (var it = 0; it < indentation; it++)
			writer.Write("  ");
		writer.WriteLine("↓");
		if (Caller != null)
			Caller.Write(writer, indentation, prefix);
	}
}
}
