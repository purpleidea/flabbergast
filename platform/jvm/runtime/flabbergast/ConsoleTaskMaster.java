package flabbergast;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fusesource.jansi.Ansi;

public class ConsoleTaskMaster extends TaskMaster {
    boolean dirty = false;

    private String pad(String str, int length) {
        return String.format("%1$-" + length + "s", str);
    }

    public void reportCircularEvaluation() throws IOException {
        boolean exit = !hasInflightLookups() || dirty;
        dirty = false;
        if (exit) {
            clearInFlight();
            return;
        }
        PrintWriter output = new PrintWriter(System.err);
        Set<SourceReference> seen = new HashSet<SourceReference>();
        output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).toString());
        output.println("Circular evaluation detected.");
        output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
        for (Lookup lookup : this) {
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
            output.printf("Lookup for “%s” blocked. Lookup initiated at:\n",
                          lookup.getName());
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
            lookup.getSourceReference().write(output, "  ", seen);
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).toString());
            output.printf(" is waiting for “%s” in frame defined at:\n",
                          lookup.getLastName());
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
            lookup.getLastFrame().getSourceReference()
            .write(output, "  ", seen);
        }
        output.flush();
        clearInFlight();
    }

    @Override
    public void reportExternalError(String uri, LibraryFailure reason) {
        dirty = true;
        System.err.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).toString());
        switch (reason) {
        case BAD_NAME :
            System.err.printf("The URI “%s” is not a valid name.\n", uri);
            break;
        case CORRUPT :
            System.err.printf("The URI “%s” could not be loaded.\n", uri);
            break;
        case MISSING :
            System.err.printf("The URI “%s” could not be found.\n", uri);
            break;
        default :
            break;
        }
        System.err.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
    }

    @Override
    public void reportLookupError(Lookup lookup, Class<?> fail_type) {
        dirty = true;
        try {
            PrintWriter output = new PrintWriter(System.err);
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).toString());
            if (fail_type == null) {
                output.printf("Undefined name “%s”. Lookup was as follows:\n",
                              lookup.getName());
            } else {
                output.printf(
                    "Non-frame type %s while resolving name “%s”. Lookup was as follows:\n",
                    fail_type, lookup.getName());
            }
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
            int col_width = Math.max(
                                (int) Math.log10(lookup.getFrameCount()) + 1, 3);
            for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
                col_width = Math.max(col_width, lookup.getName(name_it)
                                     .length());
            }
            for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
                output.printf("│ %s", pad(lookup.getName(name_it), col_width));
            }
            output.println("│");
            for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
                output.print(name_it == 0 ? "├" : "┼");
                for (int s = 0; s <= col_width; s++) {
                    output.print("─");
                }
            }
            output.println("┤");
            Map<Frame, String> known_frames = new HashMap<Frame, String>();
            java.util.List<Frame> frame_list = new ArrayList<Frame>();
            String null_text = pad("│ ", col_width + 2);
            for (int frame_it = 0; frame_it < lookup.getFrameCount(); frame_it++) {
                for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
                    Frame frame = lookup.get(name_it, frame_it);
                    if (frame == null) {
                        output.print(null_text);
                        continue;
                    }
                    if (!known_frames.containsKey(frame)) {
                        frame_list.add(frame);
                        known_frames.put(
                            frame,
                            pad(Integer.toString(frame_list.size()),
                                col_width));
                    }
                    output.printf("│ %s", known_frames.get(frame));
                }
                output.println("│");
            }
            Set<SourceReference> seen = new HashSet<SourceReference>();
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
            output.println("Lookup happened here:");
            output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
            lookup.getSourceReference().write(output, "  ", seen);
            for (int it = 0; it < frame_list.size(); it++) {
                output.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).toString());
                output.printf("Frame %s defined:\n", it + 1);
                output.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
                frame_list.get(it).getSourceReference()
                .write(output, "  ", seen);
            }
            output.flush();
        } catch (IOException e) {
        }
    }

    @Override
    public void reportOtherError(SourceReference reference, String message) {
        dirty = true;
        System.err.print(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).toString());
        System.err.println(message);
        System.err.print(Ansi.ansi().a(Ansi.Attribute.RESET).toString());
        try {
            PrintWriter output = new PrintWriter(System.err);
            reference.write(output, "  ");
            output.flush();
        } catch (IOException e) {
        }

    }
}
