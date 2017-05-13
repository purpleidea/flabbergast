package flabbergast.lang;

final class WindowItem<L, N> {
  private final Context context;
  private final L lengthValue;
  private final N nextValue;
  private final SourceReference sourceReference;

  WindowItem(SourceReference sourceReference, Context context, L lengthValue, N nextValue) {
    this.sourceReference = sourceReference;
    this.context = context;
    this.lengthValue = lengthValue;
    this.nextValue = nextValue;
  }

  public Context context() {
    return context;
  }

  public L lengthValue() {
    return lengthValue;
  }

  public N nextValue() {
    return nextValue;
  }

  public SourceReference sourceReference() {
    return sourceReference;
  }
}
