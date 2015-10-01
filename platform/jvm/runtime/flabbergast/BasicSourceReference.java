package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Description of the current Flabbergast stack.
 *
 * Since the Flabbergast stack is utterly alien to the underlying VM (it can
 * bifurcate), this object records it such that it can be presented to the user
 * when needed.
 */
public class BasicSourceReference extends SourceReference {
    protected SourceReference caller;
    protected int end_column;
    protected int end_line;
    protected String file_name;
    protected String message;
    protected int start_column;

    protected int start_line;

    public BasicSourceReference(String message, String filename,
                                int start_line, int start_column, int end_line, int end_column,
                                SourceReference caller) {
        this.message = message;
        this.file_name = filename;
        this.start_line = start_line;
        this.start_column = start_column;
        this.end_line = end_line;
        this.end_column = end_column;
        this.caller = caller;
    }

    public SourceReference getCaller() {
        return caller;
    }

    public int getEndColumn() {
        return end_column;
    }

    public int getEndLine() {
        return end_line;
    }

    public String getFileName() {
        return file_name;
    }

    public String getMessage() {
        return message;
    }

    public int getStartColumn() {
        return start_column;
    }

    public int getStartLine() {
        return start_line;
    }

    @Override
    public void write(Writer writer, String prefix, Set<SourceReference> seen)
    throws IOException {
        writer.write(prefix);
        writer.write(caller == null ? "└ " : "├ ");
        writeMessage(writer);
        boolean before = seen.contains(this);
        if (before) {
            writer.write(" (previously mentioned)");
        } else {
            seen.add(this);
        }
        writer.write("\n");
        if (caller != null) {
            if (before) {
                writer.write(prefix);
                writer.write("┊\n");
            } else {
                caller.write(writer, prefix, seen);
            }
        }
    }

    protected void writeMessage(Writer writer) throws IOException {
        writer.write(file_name);
        writer.write(": ");
        writer.write(Integer.toString(start_line));
        writer.write(":");
        writer.write(Integer.toString(start_column));
        writer.write("-");
        writer.write(Integer.toString(end_line));
        writer.write(":");
        writer.write(Integer.toString(end_column));
        writer.write(": ");
        writer.write(message);
    }
}
