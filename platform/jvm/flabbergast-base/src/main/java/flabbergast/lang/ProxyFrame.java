package flabbergast.lang;

import java.util.Optional;

/** A Frame wrapper over a Java object. */
class ProxyFrame extends Frame {

  private final Object backing;

  ProxyFrame(Str id, SourceReference sourceReference, Context context, Object backing) {
    super(id, sourceReference, context, Frame.ILLEGAL_GATHERER, true);
    this.backing = backing;
  }

  /** Get the wrapped object. */
  public Object backing() {
    return backing;
  }

  public <T> Optional<? extends T> extractProxy(Class<T> type) {
    return Optional.of(backing).filter(type::isInstance).map(type::cast);
  }
}
