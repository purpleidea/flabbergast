package flabbergast;

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

	void fill(SetFrame set_frame, int start_index) {
		set_frame.set(start_index, frame);
		if (tail != null) {
			tail.fill(set_frame, start_index + 1);
		}
	}
}