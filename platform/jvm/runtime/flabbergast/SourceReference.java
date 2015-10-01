package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Description of the current Flabbergast stack.
 */
public abstract class SourceReference {
    /**
     * Write the current stack trace.
     */
    public void write(Writer writer, String prefix) throws IOException {
        write(writer, prefix, new HashSet<SourceReference>());
    }

    public abstract void write(Writer writer, String prefix,
                               Set<SourceReference> seen) throws IOException;
}
