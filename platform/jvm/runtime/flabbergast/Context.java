package flabbergast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The collection of frames in which lookup should be performed.
 */
public class Context implements Iterable<Frame> {
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
		List<Frame> list = new ArrayList<Frame>();
		list.addAll(original.frames);
		for (Frame frame : new_tail.frames) {
			if (!list.contains(frame)) {
				list.add(frame);
			}
		}
		return new Context(list);
	}

	public static Context prepend(Frame head, Context tail) {
		if (head == null) {
			throw new IllegalArgumentException(
					"Cannot prepend a null frame to a context.");
		}
		List<Frame> list = new ArrayList<Frame>();
		list.add(head);
		if (tail != null) {
			for (Frame frame : tail.frames) {
				if (head != frame) {
					list.add(frame);
				}
			}
		}
		return new Context(list);
	}

	private List<Frame> frames;

	private Context(List<Frame> frames) {
		this.frames = frames;
	}

	public int getLength() {
		return frames.size();
	}

	@Override
	public Iterator<Frame> iterator() {
		return frames.iterator();
	}

}
