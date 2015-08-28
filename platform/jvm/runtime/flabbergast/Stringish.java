package flabbergast;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Stack;

public abstract class Stringish
		implements
			Comparable<Stringish>,
			Iterable<String>,
			RamblingIterator.GetNext<String> {
	public static Stringish[] BOOLEANS = new Stringish[]{
			new SimpleStringish("False"), new SimpleStringish("True")};

	public static Stringish fromCodepoint(long codepoint) {
		return new SimpleStringish(new String(new int[]{(int) codepoint}, 0, 1));
	}

	public static Stringish fromDouble(double value, boolean exponential,
			long digits) {
		DecimalFormat format = new DecimalFormat(exponential ? "#.#E0" : "#.#");
		format.setMinimumFractionDigits((int) digits);
		format.setMaximumFractionDigits((int) digits);
		return new SimpleStringish(format.format(value));
	}

	public static Stringish fromInt(long value, boolean hex, long digits) {
		return new SimpleStringish(String.format("%"
				+ (digits > 0 ? "0" + digits : "") + (hex ? "X" : "d"), value));
	}

	public static Stringish fromObject(Object o) {
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

	public static String nameForClass(Class<?> t) {
		if (Stringish.class.isAssignableFrom(t))
			return "Str";
		if (t == Boolean.class || t == boolean.class)
			return "Bool";
		if (t == Double.class || t == double.class)
			return "Float";
		if (t == Frame.class)
			return "Frame";
		if (t == Long.class || t == long.class)
			return "Int";
		if (t == Template.class)
			return "Template";
		if (t == Unit.class)
			return "Null";
		return t.getSimpleName();
	}

	@Override
	public int compareTo(Stringish other) {
		Collator collator = Collator.getInstance();
		Iterator<String> this_stream = iterator();
		Iterator<String> other_stream = other.iterator();
		Ptr<Integer> this_offset = new Ptr<Integer>(0);
		Ptr<Integer> other_offset = new Ptr<Integer>(0);
		Ptr<String> this_current = new Ptr<String>();
		Ptr<String> other_current = new Ptr<String>();
		int result = 0;
		boolean first = true;
		while (result == 0) {
			boolean this_empty = updateIterator(this_stream, this_offset,
					this_current, first);
			boolean other_empty = updateIterator(other_stream, other_offset,
					other_current, first);
			first = false;
			if (this_empty) {
				return other_empty ? 0 : -1;
			}
			if (other_empty) {
				return 1;
			}
			int length = Math.min(
					this_current.get().length() - this_offset.get(),
					other_current.get().length() - other_offset.get());
			result = collator.compare(
					this_current.get().substring(this_offset.get(),
							this_offset.get() + length),
					other_current.get().substring(other_offset.get(),
							other_offset.get() + length));
			this_offset.set(this_offset.get() + length);
			other_offset.set(other_offset.get() + length);
		}
		return result;
	}

	abstract int getCount();

	public abstract long getLength();

	public abstract long getUtf8Length();

	@Override
	public Iterator<String> iterator() {
		return new RamblingIterator<String>(this);
	}

	@Override
	public abstract String ramblingNext(
			Stack<RamblingIterator.GetNext<String>> stack);

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			this.write(writer);
			return writer.toString();
		} catch (IOException e) {
			return "<Error rendering string.>";
		}
	}

	private boolean updateIterator(Iterator<String> iterator,
			Ptr<Integer> offset, Ptr<String> current, boolean first) {
		// This is in a loop to consume any empty strings at the end of input.
		while (first || offset.get() >= current.get().length()) {
			if (!iterator.hasNext()) {
				return true;
			}
			current.set(iterator.next());
			offset.set(0);
			first = false;
		}
		return false;
	}

	public final void write(Writer writer) throws IOException {
		for (String s : this) {
			writer.write(s);
		}
	}
}
