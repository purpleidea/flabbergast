using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;

namespace Flabbergast {
public abstract class Stringish : IComparable<Stringish> {
    public static Stringish[] BOOLEANS = {new SimpleStringish("False"), new SimpleStringish("True")};

    public abstract long Length {
        get;
    }
    public abstract long Utf8Length {
        get;
    }
    public abstract long Utf16Length {
        get;
    }

    public int CompareTo(Stringish other) {
        var this_stream = Stream();
        var other_stream = other.Stream();
        var this_offset = 0;
        var other_offset = 0;
        var result = 0;
        var first = true;
        while (result == 0) {
            var this_empty = UpdateIterator(this_stream, ref this_offset, first);
            var other_empty = UpdateIterator(other_stream, ref other_offset, first);
            first = false;
            if (this_empty) {
                return other_empty ? 0 : -1;
            }
            if (other_empty) {
                return 1;
            }
            var length = Math.Min(this_stream.Current.Length - this_offset,
                                  other_stream.Current.Length - other_offset);
            result = String.Compare(this_stream.Current, this_offset, other_stream.Current, other_offset, length,
                                    StringComparison.CurrentCulture);
            this_offset += length;
            other_offset += length;
        }
        return result;
    }

    public long? Find(string str, long start, bool backward) {
        var original_start = (start >= 0) ? start : (start + this.Length);
        var real_start = backward ? (this.Length - original_start - 1) : original_start;
        if (real_start < 0 || real_start > this.Length) {
            return null;
        }
        var this_str_start = this.OffsetByCodePoints(real_start);
        var this_str = this.ToString();
        var pos = backward ? this_str.LastIndexOf(str, (int)this_str_start) : this_str.IndexOf(str, (int)this_str_start);
        if (pos == -1) return null;
        return pos;
    }

    public static Stringish FromObject(object o) {
        if (o == null)
            return null;
        if (o is Stringish) {
            return (Stringish) o;
        }
        if (o is long) {
            return new SimpleStringish(((long) o).ToString());
        }
        if (o is double) {
            return new SimpleStringish(((double) o).ToString(CultureInfo.InvariantCulture));
        }
        if (o is bool) {
            return BOOLEANS[((bool) o) ? 1 : 0];
        }
        return null;
    }

    public long OffsetByCodePoints(long offset) {
        long real_offset = 0;
        IEnumerator<string> stream = Stream();
        while (offset > 0 && stream.MoveNext()) {
            var index = 0;
            while (index < stream.Current.Length && offset > 0) {
                offset--;
                index += Char.IsSurrogatePair(stream.Current, index) ? 2 : 1;
            }
            real_offset += index;
        }
        return real_offset;
    }

    public string Slice(long start, long? end, long? length) {
        if ((end == null) == (length == null)) {
            throw new ArgumentException("Only one of “length” or “end” maybe specified.");
        }
        var original_start = (start >= 0) ? start : (start + this.Length);
        if (original_start > this.Length || start < 0) {
            return null;
        }
        var real_start = this.OffsetByCodePoints(original_start);
        long real_length;
        if (length != null) {
            if (length < 0) {
                throw new ArgumentException("“length” must be non-negative.");
            }
            real_length = this.OffsetByCodePoints(original_start + length.Value) - real_start;
        } else {
            var original_end = (end.Value >= 0) ? end.Value : (this.Length + end.Value);
            if (original_end < 0) {
                return null;
            }
            real_length =  this.OffsetByCodePoints(original_end) - real_start;
            if (real_length < 0) {
                return null;
            }
        }
        return this.ToString().Substring((int)real_start, (int) real_length);
    }
    public abstract IEnumerator<string> Stream();

    public override string ToString() {
        var writer = new StringWriter();
        Write(writer);
        return writer.ToString();
    }

    private bool UpdateIterator(IEnumerator<string> enumerator, ref int offset, bool first) {
        // This is in a loop to consume any empty strings at the end of input.
        while (first || offset >= enumerator.Current.Length) {
            if (!enumerator.MoveNext()) return true;
            offset = 0;
            first = false;
        }
        return false;
    }

    public void Write(TextWriter writer) {
        var stream = Stream();
        while (stream.MoveNext()) {
            writer.Write(stream.Current);
        }
    }

    public byte[] ToUtf8() {
        return System.Text.Encoding.UTF8.GetBytes(this.ToString());
    }
    public byte[] ToUtf16(bool big) {
        return new System.Text.UnicodeEncoding(big, false).GetBytes(this.ToString());
    }
    public byte[] ToUtf32(bool big) {
        return new System.Text.UTF32Encoding(big, false).GetBytes(this.ToString());
    }
}

public class SimpleStringish : Stringish {
    public override long Length {
        get {
            return length;
        }
    }

    public override long Utf8Length {
        get {
            return System.Text.Encoding.UTF8.GetByteCount(str);
        }
    }

    public override long Utf16Length {
        get {
            return str.Length;
        }
    }

    private readonly string str;
    private readonly long length;

    public SimpleStringish(string str) {
        this.str = str;
        length = str.Length;
        for (var index = 0; index < str.Length; index++) {
            if (Char.IsSurrogatePair(str, index)) {
                length--;
            }
        }
    }

    public override IEnumerator<string> Stream() {
        yield return str;
    }
}

public class ConcatStringish : Stringish {
    public override long Length {
        get {
            return chars;
        }
    }
    public override long Utf16Length {
        get {
            return head.Utf16Length + tail.Utf16Length;
        }
    }
    public override long Utf8Length {
        get {
            return head.Utf8Length + tail.Utf8Length;
        }
    }

    private readonly long chars;
    private readonly Stringish head;
    private readonly Stringish tail;

    public ConcatStringish(Stringish head, Stringish tail) {
        this.head = head;
        this.tail = tail;
        chars = head.Length + tail.Length;
    }

    public override IEnumerator<string> Stream() {
        var head_enumerator = head.Stream();
        while (head_enumerator.MoveNext()) yield return head_enumerator.Current;
        var tail_enumerator = tail.Stream();
        while (tail_enumerator.MoveNext()) yield return tail_enumerator.Current;
    }
}
}
