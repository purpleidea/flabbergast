package flabbergast;

import java.util.Stack;

import flabbergast.RamblingIterator.GetNext;

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
	int getCount() {
		return head.getCount() + tail.getCount();
	}

	@Override
	public long getLength() {
		return chars;
	}

	@Override
	public String ramblingNext(Stack<GetNext<String>> stack) {
		stack.push(tail);
		return head.ramblingNext(stack);
	}
}