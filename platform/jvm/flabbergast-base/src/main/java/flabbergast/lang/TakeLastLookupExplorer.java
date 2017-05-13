package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;

class TakeLastLookupExplorer implements LookupExplorer {

  private final long count;
  private final LookupExplorer firstExplorer;
  private final LookupExplorer secondExplorer;

  public TakeLastLookupExplorer(
      long count, LookupExplorer firstExplorer, LookupExplorer secondExplorer) {
    this.count = count;
    this.firstExplorer = firstExplorer;
    this.secondExplorer = secondExplorer;
  }

  @Override
  public LookupExplorer duplicate() {
    return new flabbergast.lang.TakeLastLookupExplorer(
        count, firstExplorer.duplicate(), secondExplorer.duplicate());
  }

  @Override
  public void process(
      Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next) {
    if (remaining > count) {
      firstExplorer.process(targetName, frame, seen, Math.max(remaining - count, 0), next);
    } else {
      secondExplorer.process(targetName, frame, count - remaining, remaining, next);
    }
  }
}
