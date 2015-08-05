using System.Collections.Generic;
using System.Diagnostics;
using System.IO;

namespace Flabbergast {
/**
 * Description of the current Flabbergast stack.
 *
 * Since the Flabbergast stack is utterly alien to the underlying VM (it can
 * bifurcate), this object records it such that it can be presented to the user
 * when needed.
 */

	public abstract class SourceReference {
		public abstract void Write(TextWriter writer, string prefix, Dictionary<SourceReference, bool> seen);
	}
	public class BasicSourceReference : SourceReference {
		public SourceReference Caller { get; private set; }
		public int EndColumn { get; private set; }
		public int EndLine { get; private set; }
		public string FileName { get; private set; }
		public string Message { get; private set; }
		public int StartColumn { get; private set; }
		public int StartLine { get; private set; }

		public BasicSourceReference(string message, string filename, int start_line, int start_column, int end_line,
			int end_column, SourceReference caller) {
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

		public override void Write(TextWriter writer, string prefix, Dictionary<SourceReference, bool> seen) {
			writer.Write(prefix);
			writer.Write(Caller == null ? "└ " : "├ ");
			WriteMessage(writer);
			bool before = seen.ContainsKey(this);
			if (before) {
				writer.Write(" (previously mentioned)");
			} else {
				seen[this] = true;
			}
			writer.Write("\n");
			if (Caller != null) {
				if (before) {
					writer.Write(prefix);
					writer.Write("┊\n");
				} else {
					Caller.Write(writer, prefix, seen);
				}
			}
		}

		protected void WriteMessage(TextWriter writer) {
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
		}
	}

/**
 * A stack element that bifurcates.
 *
 * These are typical of instantiation and attribute overriding that have both a
 * container and an ancestor.
 */

	public class JunctionReference : BasicSourceReference {
		/**
		 * The stack trace of the non-local component. (i.e., the ancestor's stack trace).
		 */
		public SourceReference Junction { get; private set; }

		public JunctionReference(string message, string filename, int start_line, int start_column, int end_line,
			int end_column, SourceReference caller, SourceReference junction)
			: base(message, filename, start_line, start_column, end_line, end_column, caller) {
			Junction = junction;
		}

		public override void Write(TextWriter writer, string prefix, Dictionary<SourceReference, bool> seen) {
			writer.Write(prefix);
			writer.Write(Caller == null ? "└─┬ " : "├─┬ ");
			WriteMessage(writer);
			bool before = seen.ContainsKey(this);
			if (before) {
				writer.Write(" (previously mentioned)");
			} else {
				seen[this] = true;
			}
			writer.Write("\n");
			if (before) {
				writer.Write(prefix);
				writer.Write(Caller == null ? "  " : "┊ ");
				writer.Write("┊\n");
			} else {
				Junction.Write(writer, prefix + (Caller == null ? "  " : "│ "), seen);
				if (Caller != null)
					Caller.Write(writer, prefix, seen);
			}
		}
	}
	public class NativeSourceReference : SourceReference {
		private readonly string name;
		public NativeSourceReference(string name) {
			this.name = name;
		}
		public override void Write(TextWriter writer, string prefix, Dictionary<SourceReference, bool> seen) {
			writer.Write(prefix);
			writer.Write("└ <");
			writer.Write(name);
			writer.Write(">\n");
		}
	}
	public class ClrSourceReference : SourceReference {
		private const int SKIP = 2;
		private readonly StackTrace trace;
		public ClrSourceReference() {
			trace = new StackTrace();
		}
		public override void Write(TextWriter writer, string prefix, Dictionary<SourceReference, bool> seen) {
			bool before = seen.ContainsKey(this);
			seen[this] = true;

			var length = before ? SKIP + 1 : trace.FrameCount;

			for (int it = SKIP; it < length; it++) {
				writer.Write(prefix);
				writer.Write(it < trace.FrameCount - 1 ? "├ " : "└ ");
				writer.Write(trace.GetFrame(it).ToString());
				if (before) {
					writer.Write(" (previously mentioned)\n");
					writer.Write(prefix);
					writer.Write("┊");
				}
				writer.Write("\n");
			}
		}
	}
}
