package flabbergast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The collection of frames in which lookup should be performed. */
public class Context implements Iterable<Frame> {
  /**
   * Conjoin two contexts, placing all the frames of the provided context after all the frames in
   * the original context.
   */
  public static Context append(Context original, Context new_tail) {
    if (original == null) {
      throw new IllegalArgumentException("Cannot append to null context.");
    }
    if (new_tail == null || original == new_tail) {
      return original;
    }
    int filter = 0;
    List<Frame> list = new ArrayList<Frame>(original.getLength() + new_tail.getLength());
    for (Frame f : original.frames) {
      list.add(f);
      filter |= f.hashCode();
    }
    for (Frame frame : new_tail.frames) {
      int hash = frame.hashCode();
      if ((hash & filter) != hash || !list.contains(frame)) {
        list.add(frame);
        filter |= hash;
      }
    }
    return new Context(list);
  }

  public static Context prepend(Frame head, Context tail) {
    if (head == null) {
      throw new IllegalArgumentException("Cannot prepend a null frame to a context.");
    }
    List<Frame> list = new ArrayList<Frame>(tail == null ? 1 : (tail.getLength() + 1));
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
