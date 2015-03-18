package flabbergast;

import java.io.IOException;
import java.io.Writer;

public class SimpleStringish extends Stringish {
	private String str;

	public SimpleStringish(String str) {
		this.str = str;
	}

	@Override
	void collect(String[] bits, int start) {
		bits[start] = str;
	}

	@Override
	int getCount() {
		return 1;
	}

	public long getLength() {
		return str.length();
	}

	public void write(Writer writer) throws IOException {
		writer.write(str);
	}
}