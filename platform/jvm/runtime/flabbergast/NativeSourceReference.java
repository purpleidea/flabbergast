package flabbergast;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A stack element that captures part of the language infrastructure.
 */
public class NativeSourceReference extends SourceReference {

    private final String name;

    public NativeSourceReference(String name) {
        this.name = name;
    }

    @Override
    public void write(Writer writer, String prefix, Set<SourceReference> seen)
    throws IOException {
        writer.write(prefix);
        writer.write("└ <");
        writer.write(name);
        writer.write(">\n");
    }
}
