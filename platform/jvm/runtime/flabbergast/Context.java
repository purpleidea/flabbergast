package flabbergast;

import java.util.Iterator;
import java.util.Stack;

/**
 * The collection of frames in which lookup should be performed.
 */
public abstract class Context implements Iterable<Frame> {
	public static class FrameIterator implements Iterator<Frame> {
		private Stack<Context> stack = new Stack<Context>();

		public FrameIterator(Context context) {
			stack.push(context);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Frame next() {
			Context context = stack.pop();
			return context.iterateHelper(stack);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Conjoin two contexts, placing all the frames of the provided context
	 * after all the frames in the original context.
	 */
	public static Context append(Context original, Context new_tail) {
		if (original == null) {
			throw new IllegalArgumentException("Cannot append to null context.");
		}
		if (new_tail == null) {
			return original;
		}
		return new ForkedContext(original, new_tail);
	}

	public static Context Prepend(Frame head, Context tail) {
		if (head == null) {
			throw new IllegalArgumentException(
					"Cannot prepend a null frame to a context.");
		}
		return new LinkedContext(head, tail);
	}

	protected int length;

	/**
	 * The total number of frames in this context.
	 */
	public int getLength() {
		return length;
	}

	public abstract Frame iterateHelper(Stack<Context> stack);

	@Override
	public Iterator<Frame> iterator() {
		return new FrameIterator(this);
	}
}
