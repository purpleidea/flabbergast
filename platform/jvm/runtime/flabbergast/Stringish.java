package flabbergast;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Stack;

public abstract class Stringish
    implements Comparable<Stringish>, Iterable<String>, RamblingIterator.GetNext<String> {
  public static Stringish[] BOOLEANS =
      new Stringish[] {new SimpleStringish("False"), new SimpleStringish("True")};

  public static Stringish fromCodepoint(long codepoint) {
    return new SimpleStringish(new String(new int[] {(int) codepoint}, 0, 1));
  }

  public static Stringish fromDouble(double value, boolean exponential, long digits) {
    DecimalFormat format = new DecimalFormat(exponential ? "#.#E0" : "#.#");
    format.setMinimumFractionDigits((int) digits);
    format.setMaximumFractionDigits((int) digits);
    return new SimpleStringish(format.format(value));
  }

  public static Stringish fromInt(long value, boolean hex, long digits) {
    return new SimpleStringish(
        String.format("%" + (digits > 0 ? "0" + digits : "") + (hex ? "X" : "d"), value));
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
      boolean this_empty =
          updateIterator(
              this_stream, this_offset,
              this_current, first);
      boolean other_empty =
          updateIterator(
              other_stream, other_offset,
              other_current, first);
      first = false;
      if (this_empty) {
        return other_empty ? 0 : -1;
      }
      if (other_empty) {
        return 1;
      }
      int length =
          Math.min(
              this_current.get().length() - this_offset.get(),
              other_current.get().length() - other_offset.get());
      result =
          collator.compare(
              this_current.get().substring(this_offset.get(), this_offset.get() + length),
              other_current.get().substring(other_offset.get(), other_offset.get() + length));
      this_offset.set(this_offset.get() + length);
      other_offset.set(other_offset.get() + length);
    }
    return result;
  }

  public Long find(String str, long start, boolean backward) {
    long original_start = (start >= 0) ? start : (start + this.getLength());
    long real_start = backward ? (this.getLength() - original_start - 1) : original_start;
    if (real_start < 0 || real_start > this.getLength()) {
      return null;
    }
    String this_str = this.toString();
    int this_str_start = this_str.offsetByCodePoints(0, (int) real_start);
    long pos =
        backward
            ? this_str.lastIndexOf(str, this_str_start)
            : this_str.indexOf(str, this_str_start);
    return pos == -1 ? null : pos;
  }

  abstract int getCount();

  public abstract long getLength();

  public abstract long getUtf16Length();

  public abstract long getUtf8Length();

  @Override
  public Iterator<String> iterator() {
    return new RamblingIterator<String>(this);
  }

  @Override
  public abstract String ramblingNext(Stack<RamblingIterator.GetNext<String>> stack);

  public String slice(long start, Long end, Long length) {
    if ((end == null) == (length == null)) {
      throw new IllegalArgumentException("Only one of “length” or “end” maybe specified.");
    }
    long original_start = (start >= 0) ? start : (start + this.getLength());
    if (original_start > this.getLength() || start < 0) {
      return null;
    }
    String this_str = this.toString();
    int real_start = this_str.offsetByCodePoints(0, (int) original_start);
    int real_end;
    if (length != null) {
      if (length < 0) {
        throw new IllegalArgumentException("“length” must be non-negative.");
      }
      real_end = this_str.offsetByCodePoints(0, (int) (original_start + length));
    } else {
      long original_end = (end >= 0) ? end : (this.getLength() + end);
      if (original_end < original_start) {
        return null;
      }
      real_end = this_str.offsetByCodePoints(0, (int) original_end);
    }
    return this_str.substring(real_start, real_end);
  }

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

  public final byte[] toUtf16(boolean big) {
    try {
      return toString().getBytes("UTF-16" + (big ? "BE" : "LE"));
    } catch (UnsupportedEncodingException e) {
      return new byte[0];
    }
  }

  public final byte[] toUtf32(boolean big) {
    try {
      return toString().getBytes("UTF-32" + (big ? "BE" : "LE"));
    } catch (UnsupportedEncodingException e) {
      return new byte[0];
    }
  }

  public final byte[] toUtf8() {
    try {
      return toString().getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new byte[0];
    }
  }

  private boolean updateIterator(
      Iterator<String> iterator, Ptr<Integer> offset, Ptr<String> current, boolean first) {
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
