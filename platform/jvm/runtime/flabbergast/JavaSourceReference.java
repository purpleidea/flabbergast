package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A stack element that captures a Java context.
 */
public class JavaSourceReference extends SourceReference {
	private static final int SKIP = 2;

	private final StackTraceElement[] trace;

	public JavaSourceReference() {
		trace = Thread.currentThread().getStackTrace();
	}

	@Override
	public void write(Writer writer, String prefix, Set<SourceReference> seen)
			throws IOException {
		boolean before = seen.contains(this);
		seen.add(this);

		int length = before ? SKIP + 1 : trace.length;

		for (int it = SKIP; it < length; it++) {
			writer.write(prefix);
			writer.write(it < trace.length - 1 ? "├ " : "└ ");
			writer.write(trace[it].toString());
			if (before) {
				writer.write(" (previously mentioned)\n");
				writer.write(prefix);
				writer.write("┊");
			}
			writer.write("\n");
		}
	}
}
