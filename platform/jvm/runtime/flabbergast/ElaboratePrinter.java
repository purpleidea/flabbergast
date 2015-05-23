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
				write("\n");
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
		} else if (result instanceof Computation) {
			write("<unfinished>");
			write("\n");
		} else if (result instanceof Stringish) {
			write("\"");
			for (String s : (Stringish) result) {
				for (int it = 0; it < s.length(); it++) {
					if (s.charAt(it) == 7) {
						write("\\a");
					} else if (s.charAt(it) == 8) {
						write("\\b");
					} else if (s.charAt(it) == 12) {
						write("\\f");
					} else if (s.charAt(it) == 10) {
						write("\\n");
					} else if (s.charAt(it) == 13) {
						write("\\r");
					} else if (s.charAt(it) == 9) {
						write("\\t");
					} else if (s.charAt(it) == 11) {
						write("\\v");
					} else if (s.charAt(it) == 34) {
						write("\\\"");
					} else if (s.charAt(it) == 92) {
						write("\\\\");
					} else if (s.charAt(it) < 16) {
						write("\\x0");
						write(Integer.toHexString(s.charAt(it)));
					} else if (s.charAt(it) < 32) {
						write("\\x");
						write(Integer.toHexString(s.charAt(it)));
					} else {
						write(s.substring(it, it + 1));
					}
				}
			}
			write("\"\n");
		} else {
			write(result.toString());
			write("\n");
		}
	}

	protected abstract void write(String string) throws IOException;
}
