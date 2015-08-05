package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A stack element that bifurcates.
 * 
 * These are typical of instantiation and attribute overriding that have both a
 * container and an ancestor.
 */
public class JunctionReference extends BasicSourceReference {
	private SourceReference junction;

	public JunctionReference(String message, String filename, int start_line,
			int start_column, int end_line, int end_column,
			SourceReference caller, SourceReference junction) {
		super(message, filename, start_line, start_column, end_line,
				end_column, caller);
		this.junction = junction;
	}

	/**
	 * The stack trace of the non-local component. (i.e., the ancestor's stack
	 * trace).
	 */
	public SourceReference getJunction() {
		return junction;
	}

	@Override
	public void write(Writer writer, String prefix, Set<SourceReference> seen)
			throws IOException {
		writer.write(prefix);
		writer.write(this.caller == null ? "└─┬ " : "├─┬ ");
		writeMessage(writer);
		boolean before = seen.contains(this);
		if (before) {
			writer.write(" (previously mentioned)");
		} else {
			seen.add(this);
		}
		writer.write("\n");

		if (before) {
			writer.write(prefix);
			writer.write(caller == null ? "  " : "┊ ");
			writer.write("┊\n");
		} else {
			junction.write(writer, prefix + (caller == null ? "  " : "│ "),
					seen);
			if (caller != null) {
				caller.write(writer, prefix, seen);
			}
		}
	}
}
