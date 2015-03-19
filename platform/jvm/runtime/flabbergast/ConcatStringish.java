package flabbergast;

import java.io.IOException;
import java.io.Writer;

public class ConcatStringish extends Stringish {
	private long chars;
	private Stringish head;
	private Stringish tail;

	public ConcatStringish(Stringish head, Stringish tail) {
		this.head = head;
		this.tail = tail;
		this.chars = head.getLength() + tail.getLength();
	}

	@Override
	void collect(String[] bits, int start) {
		head.collect(bits, start);
		tail.collect(bits, start + head.getCount());
	}

	@Override
	int getCount() {
		return head.getCount() + tail.getCount();
	}

	@Override
	public long getLength() {
		return chars;
	}

	@Override
	public void write(Writer writer) throws IOException {
		head.write(writer);
		tail.write(writer);
	}
}
