package flabbergast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ElaboratePrinter implements ConsumeResult {

	@Override
	public void consume(Object result) {
		Map<Frame, String> seen = new HashMap<Frame, String>();
		try {
			print(result, "", seen);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void print(Object result, String prefix, Map<Frame, String> seen)
			throws IOException {
		if (result == null) {
			write("∅\n");
		} else if (result instanceof Frame) {
			Frame f = (Frame) result;
			if (seen.containsKey(f)) {
				write(f.getId().toString());
				write(" # Frame ");
				write(seen.get(f));
			} else {
				write("{ # Frame ");
				String id = Integer.toString(seen.size());
				write(id);
				seen.put(f, id);
				for (String name : f) {
					write(prefix);
					write(name);
					write(" : ");
					print(f.get(name), prefix + "  ", seen);
				}
				write(prefix);
				write("}\n");
			}
		} else if (result instanceof Boolean) {
			write(((Boolean) result) ? "True\n" : "False\n");
		} else if (result instanceof Template) {
			Template t = (Template) result;
			write("Template\n");
			for (String name : t) {
				write(" ");
				write(name);
			}
			write("\n");
		} else {
			write(result.toString());
			write("\n");
		}
	}

	protected abstract void write(String string) throws IOException;
}
