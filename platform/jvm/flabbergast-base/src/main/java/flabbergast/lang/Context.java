package flabbergast.lang;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The collection of frames in which lookup should be performed. */
public final class Context {
  /**
   * A wrapper around a frame that allows accessing public or private attributes depending on the
   * calling context
   */
  public abstract static class FrameAccessor {
    private FrameAccessor() {}

    /** The frame this accessor is wrapping */
    public abstract Frame frame();

    /**
     * Get access to this frame with appropriate public/private visibility
     *
     * @param name the attribute name
     * @return the promise associated with this name or null of it does not exist
     */
    public abstract Optional<Promise<Any>> get(Name name);

    /** Get the names associated with this frame with appropriate visibility */
    public abstract Stream<Name> names();

    abstract Stream<Frame> privateFrames();

    abstract FrameAccessor reduceVisibility();
  }

  private static final class PrivateAccessor extends FrameAccessor {
    private final Frame frame;

    private PrivateAccessor(Frame frame) {
      this.frame = frame;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PrivateAccessor that = (PrivateAccessor) o;
      return frame.equals(that.frame);
    }

    @Override
    public Frame frame() {
      return frame;
    }

    @Override
    public Optional<Promise<Any>> get(Name name) {
      return frame.getPrivate(name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(frame);
    }

    @Override
    public Stream<Name> names() {
      return frame.namesPrivate();
    }

    @Override
    Stream<Frame> privateFrames() {
      return Stream.of(frame);
    }

    @Override
    FrameAccessor reduceVisibility() {
      return new PublicAccessor(frame);
    }
  }

  private static final class PublicAccessor extends FrameAccessor {
    private final Frame frame;

    private PublicAccessor(Frame frame) {
      this.frame = frame;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PublicAccessor that = (PublicAccessor) o;
      return frame.equals(that.frame);
    }

    @Override
    public Frame frame() {
      return frame;
    }

    @Override
    public Optional<Promise<Any>> get(Name name) {
      return frame.get(name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(frame);
    }

    @Override
    public Stream<Name> names() {
      return frame.names();
    }

    @Override
    Stream<Frame> privateFrames() {
      return Stream.empty();
    }

    @Override
    FrameAccessor reduceVisibility() {
      return this;
    }
  }
  /** A context containing no frames. */
  public static final Context EMPTY = new Context(null, Stream.empty());

  private final FrameAccessor[] frames;
  private final Frame self;

  Context(Frame self, Stream<FrameAccessor> frames) {
    this.self = self;
    this.frames = frames.sequential().distinct().toArray(FrameAccessor[]::new);
  }

  FrameAccessor accessor(Frame frame) {
    return Stream.of(frames)
        .filter(a -> a.frame() == frame)
        .findFirst()
        .orElseGet(() -> new PublicAccessor(frame));
  }

  /**
   * Conjoin two contexts, placing all the frames of the provided context after all the frames in
   * the original context. The value of <tt>This</tt> is unchanged.
   */
  Context append(Context tail) {
    return new Context(self != null ? self : tail.self(), Stream.concat(stream(), tail.stream()));
  }

  /**
   * Get the context of a frame as visible from the calling context
   *
   * @param frame the frame to inspect
   */
  public Context forFrame(Frame frame) {
    final Set<Frame> allowed =
        Stream.of(frames).flatMap(FrameAccessor::privateFrames).collect(Collectors.toSet());
    return new Context(
        frame.context().self(),
        frame.context().stream().map(a -> allowed.contains(a.frame()) ? a : a.reduceVisibility()));
  }

  /** List all names that can be seen from this context */
  public Stream<Name> names() {
    return Stream.of(frames).flatMap(FrameAccessor::names).distinct();
  }

  /**
   * Get the context of a frame as visible from the calling context
   *
   * @param frame the frame to inspect
   */
  public Context ofFrame(Frame frame) {
    return new Context(
        frame.context().self(),
        Stream.of(
            Stream.of(frames)
                .filter(a -> a.frame() == frame)
                .findFirst()
                .orElseGet(() -> new PublicAccessor(frame))));
  }

  /** Add a new frame to the head of this context which will become <tt>This</tt>. */
  public Context prepend(Frame head) {
    if (head == null) {
      throw new IllegalArgumentException("Cannot prepend a null frame to a context.");
    }
    return new Context(head, Stream.concat(Stream.of(new PublicAccessor(head)), stream()));
  }

  /** Add a new frame to the head of this context, but do not modify <tt>This</tt>. */
  Context prependHidden(Frame head) {
    if (head == null) {
      throw new IllegalArgumentException("Cannot prepend a null frame to a context.");
    }
    return new Context(self, Stream.concat(Stream.of(new PrivateAccessor(head)), stream()));
  }

  Context prependPrivate(Frame head) {
    if (head == null) {
      throw new IllegalArgumentException("Cannot prepend a null frame to a context.");
    }
    return new Context(head, Stream.concat(Stream.of(new PrivateAccessor(head)), stream()));
  }

  /** Create an equivalent context where any private access is reduced to public access. */
  public Context reduceVisibility() {
    return new Context(self, stream().map(FrameAccessor::reduceVisibility));
  }

  /** Get the frame considered <tt>This</tt> in this context. */
  public Frame self() {
    return self == null ? Frame.EMPTY : self;
  }

  /** List all the frames in this context */
  Stream<FrameAccessor> stream() {
    return Stream.of(frames);
  }
}
