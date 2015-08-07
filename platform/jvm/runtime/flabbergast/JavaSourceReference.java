package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A stack element that captures a Java context.
 */
public class JavaSourceReference extends SourceReference {
	private static final int SKIP = 2;
	private final String task_master_name = TaskMaster.class.getName();
	private final StackTraceElement[] trace;

	public JavaSourceReference() {
		StackTraceElement[] stack_trace = Thread.currentThread()
				.getStackTrace();
		int end;
		for (end = SKIP; end < stack_trace.length; end++) {
			if (stack_trace[end].getClassName().equals(task_master_name)) {
				break;
			}
		}

		trace = new StackTraceElement[end - SKIP];
		for (int it = 0; it < trace.length; it++) {
			trace[it] = stack_trace[it + SKIP];
		}
	}

	@Override
	public void write(Writer writer, String prefix, Set<SourceReference> seen)
			throws IOException {
		boolean before = seen.contains(this);
		seen.add(this);

		int length = before ? 1 : trace.length;

		for (int it = 0; it < length; it++) {
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
