package flabbergast;

import java.util.Stack;

/**
 * A join between two contexts.
 * 
 * This is an optimisation: rather than creating a new linked-list of contexts,
 * store a fork and it can be linearised when needed.
 */
class ForkedContext extends Context {
	private Context head;
	private Context tail;

	ForkedContext(Context head, Context tail) {
		this.head = head;
		this.tail = tail;
		length = head.length + tail.length;
	}

	@Override
	public Frame iterateHelper(Stack<Context> stack) {
		stack.push(tail);
		return head.iterateHelper(stack);
	}

}
