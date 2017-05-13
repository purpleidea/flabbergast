package flabbergast.lang;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Flabbergast <tt>Str</tt> implementation
 *
 * <p>This wraps {@link String} to allow very cheap string concatentation
 */
public final class Str implements Comparable<Str> {
  private static final Str[] BOOLEANS = new Str[] {from("False"), from("True")};
  private static final Collator COLLATOR = Collator.getInstance();

  /** Get a string for a <tt>Bool</tt> value. */
  public static Str from(boolean value) {
    return BOOLEANS[value ? 1 : 0];
  }

  /** Create a string for a <tt>Float</tt> value. */
  public static Str from(double value) {
    return from(Double.toString(value));
  }

  /**
   * Create a string for a <tt>Float</tt> value with a specific format.
   *
   * @param value the floating point value to be converted
   * @param exponential should the value be put in exponential notation
   * @param digits the number of digits after the decimal point
   */
  public static Str from(double value, boolean exponential, long digits) {
    final var format = new DecimalFormat(exponential ? "#.#E0" : "#.#");
    format.setMinimumFractionDigits((int) digits);
    format.setMaximumFractionDigits((int) digits);
    return from(format.format(value));
  }

  /** Create a <tt>Str</tt> from an <tt>Int</tt> */
  public static Str from(long value) {
    return from(Long.toString(value));
  }

  /**
   * Create a <tt>Str</tt> from an <tt>Int</tt> with special formatting
   *
   * @param value the integral value to be converted
   * @param hex whether to use hexadecimal or decimal
   * @param digits the number of digits to zero-pad the result
   */
  public static Str from(long value, boolean hex, long digits) {
    return from(String.format("%" + (digits > 0 ? "0" + digits : "") + (hex ? "X" : "d"), value));
  }

  /** Create a <tt>Str</tt> from a Java string. */
  public static Str from(String value) {
    return new Str(
        List.of(value),
        value.codePointCount(0, value.length()),
        value.length(),
        value.getBytes(StandardCharsets.UTF_8).length);
  }

  /** Create a <tt>Str</tt> from a single Unicode codepoint. */
  public static Str fromCodepoint(long codepoint) {
    return from(new String(new int[] {(int) codepoint}, 0, 1));
  }

  private int hash;

  private final long length;

  private final long length16;

  private final long length8;
  private List<String> parts;

  private Str(List<String> parts, long length, long length16, long length8) {
    this.parts = parts;
    this.length = length;
    this.length16 = length16;
    this.length8 = length8;
  }

  /** Compare two strings lexicographically */
  @Override
  public int compareTo(Str other) {
    final var thisStr = toString();
    final var otherStr = other.toString();
    return COLLATOR.compare(thisStr, otherStr);
  }

  /** Concatenate this string with another. */
  public Str concat(Str other) {
    return new Str(
        Stream.concat(parts.stream(), other.parts.stream()).collect(Collectors.toList()),
        length + other.length,
        length16 + other.length16,
        length8 + other.length8);
  }

  /** Check if two strings are lexicographically equal */
  @Override
  public boolean equals(Object other) {
    if (other instanceof Str) {
      return compareTo((Str) other) == 0;
    }
    return false;
  }

  /**
   * Find a substring in this <tt>Str</tt>
   *
   * @param needle the needle
   * @param start the index to start searching
   * @param backward whether to search backwards or forwards
   * @return the position of the substring or null if not found
   */
  public Long find(String needle, long start, boolean backward) {
    final var originalStart = start >= 0 ? start : start + length();
    final var realStart = backward ? length() - originalStart - 1 : originalStart;
    if (realStart < 0 || realStart > length()) {
      return null;
    }
    final var thisStr = toString();
    final var thisStrStart = thisStr.offsetByCodePoints(0, (int) realStart);
    final long pos =
        backward
            ? thisStr.lastIndexOf(needle, thisStrStart)
            : thisStr.indexOf(needle, thisStrStart);
    return pos == -1 ? null : pos;
  }

  /**
   * Compute a hash code of the string
   *
   * <p>This is hashed the same way as an equivalent Java string
   */
  @Override
  public int hashCode() {
    if (hash == 0 && length16 > 0) {
      hash = parts.stream().flatMapToInt(String::chars).reduce(0, (a, v) -> 31 * a + v);
    }
    return hash;
  }

  /** Get the number of codepoints in this string. */
  public long length() {
    return length;
  }

  /** Get the number of bytes to represent this string as UTF-16. */
  public long length16() {
    return length16;
  }

  /** Get the number of bytes to represent this string as UTF-8. */
  public long length8() {
    return length8;
  }

  /**
   * Extract a substring from this one.
   *
   * <p>Exactly one of the end position or substring length may be specified. All values are in
   * codepoints.
   *
   * @param start the initial index
   * @param end the end index to take the substring; if negative, takes as an index from the end of
   *     the string
   * @param length the length of the substring; may not be negative
   */
  public String slice(long start, Long end, Long length) {
    if ((end == null) == (length == null)) {
      throw new IllegalArgumentException("Only one of “length” or “end” maybe specified.");
    }
    final var originalStart = start >= 0 ? start : start + length();
    if (originalStart > length() || start < 0) {
      return null;
    }
    final var thisStr = toString();
    final var realStart = thisStr.offsetByCodePoints(0, (int) originalStart);
    int realEnd;
    if (length != null) {
      if (length < 0) {
        throw new IllegalArgumentException("“length” must be non-negative.");
      }
      realEnd = thisStr.offsetByCodePoints(0, (int) (originalStart + length));
    } else {
      final var originalEnd = end >= 0 ? end : length() + end;
      if (originalEnd < originalStart) {
        return null;
      }
      realEnd = thisStr.offsetByCodePoints(0, (int) originalEnd);
    }
    return thisStr.substring(realStart, realEnd);
  }

  /** Convert this to a Java string representation, flattening any structure */
  @Override
  public synchronized String toString() {
    if (parts.size() > 1) {
      parts = List.of(String.join("", parts));
    }
    return parts.get(0);
  }

  /**
   * Convert to bytes encoded as UTF-16.
   *
   * @param big use big-endian encoding
   */
  public byte[] toUtf16(boolean big) {
    return toString().getBytes(big ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE);
  }

  /**
   * Convert to bytes encoded as UTF-32.
   *
   * @param big use big-endian encoding
   */
  public byte[] toUtf32(boolean big) {
    try {
      return toString().getBytes(big ? "UTF-32BE" : "UTF-32LE");
    } catch (final UnsupportedEncodingException e) {
      return new byte[0];
    }
  }

  /** Convert to bytes encoded as UTF-8. */
  public byte[] toUtf8() {
    return toString().getBytes(StandardCharsets.UTF_8);
  }
}
