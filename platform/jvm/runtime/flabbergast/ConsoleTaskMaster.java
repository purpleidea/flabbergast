package flabbergast;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConsoleTaskMaster extends TaskMaster {
	private String pad(String str, int length) {
		return String.format("%1$-" + length + "s", str);
	}

	@Override
	public void reportExternalError(String uri, LibraryFailure reason) {
		switch (reason) {
		case BAD_NAME:
			System.err.printf("The URI “%s” is not a valid name.\n", uri);
			break;
		case CORRUPT:
			System.err.printf("The URI “%s” could not be loaded.\n", uri);
			break;
		case MISSING:
			System.err.printf("The URI “%s” could not be found.\n", uri);
			break;
		default:
			break;
		}
	}

	@Override
	public void reportLookupError(Lookup lookup, Class<?> fail_type) {
		try {
			Writer output = new PrintWriter(System.err);
			if (fail_type == null) {
				System.err.printf(
						"Undefined name “%s”. Lookup was as follows:\n",
						lookup.getName());
			} else {
				System.err
						.printf("Non-frame type %s while resolving name “%s”. Lookup was as follows:\n",
								fail_type, lookup.getName());
			}
			int col_width = Math.max(
					(int) Math.log10(lookup.getFrameCount()) + 1, 3);
			for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
				col_width = Math.max(col_width, lookup.getName(name_it)
						.length());
			}
			for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
				System.err.printf("| %s",
						pad(lookup.getName(name_it), col_width));
			}
			System.err.println("|");
			Map<Frame, String> known_frames = new HashMap<Frame, String>();
			java.util.List<Frame> frame_list = new ArrayList<Frame>();
			String null_text = pad("| ", col_width + 2);
			for (int frame_it = 0; frame_it < lookup.getFrameCount(); frame_it++) {
				for (int name_it = 0; name_it < lookup.getNameCount(); name_it++) {
					Frame frame = lookup.get(name_it, frame_it);
					if (frame == null) {
						System.err.print(null_text);
						continue;
					}
					if (!known_frames.containsKey(frame)) {
						frame_list.add(frame);
						known_frames.put(
								frame,
								pad(Integer.toString(frame_list.size()),
										col_width));
					}
					System.err.printf("| %s", known_frames.get(frame));
				}
				System.err.println("|");
			}
			for (int it = 0; it < frame_list.size(); it++) {
				System.err.printf("Frame %s defined:\n", it + 1);
				frame_list.get(it).getSourceReference().write(output, "  ");
			}
			System.err.println("Lookup happened here:");
			lookup.getSourceReference().write(output, "  ");
		} catch (IOException e) {
		}
	}

	@Override
	public void reportOtherError(SourceReference reference, String message) {
		System.err.println(message);
		try {
			reference.write(new PrintWriter(System.err), "  ");
		} catch (IOException e) {
		}

	}
}
