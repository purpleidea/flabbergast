package flabbergast;

import java.util.Stack;

/**
 * An element in a single-linked list of contexts.
 */
class LinkedContext extends Context {
	private Frame frame;
	private Context tail;

	LinkedContext(Frame frame, Context tail) {
		this.frame = frame;
		this.tail = tail;
		length = tail == null ? 1 : (tail.length + 1);
	}

	@Override
	public Frame iterateHelper(Stack<Context> stack) {
		if (tail != null) {
			stack.push(tail);
		}
		return frame;
	}

}