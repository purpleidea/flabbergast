package flabbergast;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

public abstract class Stringish implements Comparable<Stringish> {
	public static Stringish[] BOOLEANS = new Stringish[] {
			new SimpleStringish("False"), new SimpleStringish("True") };

	public static Stringish FromObject(Object o) {
		if (Stringish.class.isAssignableFrom(o.getClass())) {
			return (Stringish) o;
		}
		if (Long.class.isAssignableFrom(o.getClass())) {
			return new SimpleStringish(((Long) o).toString());
		}
		if (Double.class.isAssignableFrom(o.getClass())) {
			return new SimpleStringish(((Double) o).toString());
		}
		if (Boolean.class.isAssignableFrom(o.getClass())) {
			return BOOLEANS[((Boolean) o) ? 1 : 0];
		}
		return null;
	}

	public static Class<?> hideImplementation(Class<?> t) {
		return Stringish.class.isAssignableFrom(t) ? Stringish.class : t;
	}

	abstract void collect(String[] bits, int start);

	public int compareTo(Stringish other) {
		String[] this_stream = new String[this.getCount()];
		this.collect(this_stream, 0);
		String[] other_stream = new String[other.getCount()];
		other.collect(other_stream, 0);
		Ptr<Integer> this_offset = new Ptr<Integer>(0);
		Ptr<Integer> other_offset = new Ptr<Integer>(0);
		Ptr<Integer> this_index = new Ptr<Integer>(0);
		Ptr<Integer> other_index = new Ptr<Integer>(0);
		int result = 0;
		boolean first = true;
		while (result == 0) {
			boolean this_empty = updateIterator(this_stream, this_index,
					this_offset, first);
			boolean other_empty = updateIterator(other_stream, other_index,
					other_offset, first);
			first = false;
			if (this_empty) {
				return other_empty ? 0 : -1;
			}
			if (other_empty) {
				return 1;
			}
			int length = Math.min(
					this_stream[this_index.get()].length() - this_offset.get(),
					other_stream[other_index.get()].length()
							- other_offset.get());
			result = this_stream[this_index.get()].regionMatches(
					this_offset.get(), other_stream[other_index.get()],
					other_offset.get(), length);
			this_offset.set(this_offset.get() + length);
			other_offset.set(other_offset.get() + length);
		}
		return result;
	}

	abstract int getCount();

	public abstract long getLength();

	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			this.write(writer);
			return writer.toString();
		} catch (IOException e) {
			return "<Error rendering string.>";
		}
	}

	private boolean updateIterator(String[] bits, Ptr<Integer> index,
			Ptr<Integer> offset, boolean first) {
		// This is in a loop to consume any empty strings at the end of input.
		while (first || offset.get() >= bits[index.get()].length()) {
			index.set(index.get() + 1);
			if (index.get() < bits.length)
				return true;
			offset.set(0);
			first = false;
		}
		return false;
	}

	public abstract void write(Writer writer) throws IOException;
}
