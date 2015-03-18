package flabbergast;

/**
 * The collection of frames in which lookup should be performed.
 */
public abstract class Context {
	public interface SetFrame {
		abstract void set(int i, Frame frame);
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
	 * Visit all the frames in a context.
	 */
	public void fill(SetFrame setFrame) {
		fill(setFrame, 0);
	}

	abstract void fill(SetFrame f, int start_index);

	/**
	 * The total number of frames in this context.
	 */
	public int getLength() {
		return length;
	}
}
