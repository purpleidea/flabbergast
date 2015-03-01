using System;
using System.Collections.Generic;
using System.IO;

namespace Flabbergast {
public abstract class Stringish : IComparable<Stringish> {
	public static Stringish[] BOOLEANS = new Stringish[] { new SimpleStringish("False"), new SimpleStringish("True") };
	public abstract long Length { get; }
	public abstract void Write(TextWriter writer);
	public int CompareTo(Stringish other) {
		var this_stream = this.Stream();
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
			var length = Math.Min(this_stream.Current.Length - this_offset, other_stream.Current.Length - other_offset);
			result = String.Compare(this_stream.Current, this_offset, other_stream.Current, other_offset, length, StringComparison.CurrentCulture);
			this_offset += length;
			other_offset += length;
		}
		return result;
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
	public abstract IEnumerator<string> Stream();
	public override string ToString() {
		var writer = new StringWriter();
		this.Write(writer);
		return writer.ToString();
	}
}

public class SimpleStringish : Stringish {
	private string str;
	public override long Length { get { return str.Length; } }
	public SimpleStringish(string str) {
		this.str = str;
	}
	public override IEnumerator<string> Stream() {
		yield return str;
	}
	public override void Write(TextWriter writer) {
		writer.Write(str);
	}
}

public class ConcatStringish : Stringish {
	private Stringish head;
	private Stringish tail;
	private long chars;
	public override long Length { get { return chars; } }
	public ConcatStringish(Stringish head, Stringish tail) {
		this.head = head;
		this.tail = tail;
		this.chars = head.Length + tail.Length;
	}
	public override IEnumerator<string> Stream() {
		var head_enumerator = head.Stream();
		while(head_enumerator.MoveNext()) yield return head_enumerator.Current;
		var tail_enumerator = tail.Stream();
		while(tail_enumerator.MoveNext()) yield return tail_enumerator.Current;
	}
	public override void Write(TextWriter writer) {
		head.Write(writer);
		tail.Write(writer);
	}
}
}
