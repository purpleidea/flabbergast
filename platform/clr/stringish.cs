using System;
using System.IO;

namespace Flabbergast {
public abstract class Stringish {
	public abstract int Length { get; }
	public abstract void Write(TextWriter writer);
	public override string ToString() {
		var writer = new StringWriter();
		this.Write(writer);
		return writer.ToString();
	}
}

public class SimpleString : Stringish {
	private string str;
	public override int Length { get { return str.Length; } }
	public SimpleString(string str) {
		this.str = str;
	}
	public override void Write(TextWriter writer) {
		writer.Write(str);
	}
}

public class ConcatString : Stringish {
	private Stringish head;
	private Stringish tail;
	private int chars;
	public override int Length { get { return chars; } }
	public ConcatString(Stringish head, Stringish tail) {
		this.head = head;
		this.tail = tail;
		this.chars = head.Length + tail.Length;
	}
	public override void Write(TextWriter writer) {
		head.Write(writer);
		tail.Write(writer);
	}
}
}
