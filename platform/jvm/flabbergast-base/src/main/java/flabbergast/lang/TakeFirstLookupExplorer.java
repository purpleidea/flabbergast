package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;

class TakeFirstLookupExplorer implements LookupExplorer {

  private final long count;
  private final LookupExplorer firstExplorer;
  private final LookupExplorer secondExplorer;

  TakeFirstLookupExplorer(long count, LookupExplorer firstExplorer, LookupExplorer secondExplorer) {
    this.count = count;
    this.firstExplorer = firstExplorer;
    this.secondExplorer = secondExplorer;
  }

  @Override
  public LookupExplorer duplicate() {
    return new flabbergast.lang.TakeFirstLookupExplorer(
        count, firstExplorer.duplicate(), secondExplorer.duplicate());
  }

  @Override
  public void process(
      Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next) {
    if (seen < count) {
      firstExplorer.process(targetName, frame, seen, Math.min(remaining, count), next);
    } else {
      secondExplorer.process(targetName, frame, seen - count, remaining, next);
    }
  }
}
