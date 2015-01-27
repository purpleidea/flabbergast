using System;
using System.Collections.Generic;
using System.IO;

namespace Flabbergast {
public abstract class Stringish : IComparable<Stringish> {
	public static Stringish[] BOOLEANS = new Stringish[] { new SimpleStringish("False"), new SimpleStringish("True") };
	public abstract int Length { get; }
	public abstract void Write(TextWriter writer);
	public int CompareTo(Stringish other) {
		var this_stream = this.Stream();
		var other_stream = this.Stream();
		if (!this_stream.MoveNext()) {
			return other_stream.MoveNext() ? -1 : 0;
		}
		if (!other_stream.MoveNext()) {
			return 1;
		}
		var this_offset = 0;
		var other_offset = 0;
		var result = 0;
		while (result == 0) {
			if (UpdateIterator(this_stream, ref this_offset)) break;
			if (UpdateIterator(other_stream, ref other_offset)) break;
			var length = Math.Min(this_stream.Current.Length - this_offset, other_stream.Current.Length - other_offset);
			result = String.Compare(this_stream.Current, this_offset, other_stream.Current, other_offset, length, StringComparison.CurrentCulture);
			this_offset += length;
			other_offset += length;
		}
		return result;
	}
	private bool UpdateIterator(IEnumerator<string> enumerator, ref int offset) {
		while (enumerator.Current.Length <= offset) {
			if (!enumerator.MoveNext()) return true;
			offset = 0;
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
	public override int Length { get { return str.Length; } }
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
	private int chars;
	public override int Length { get { return chars; } }
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
