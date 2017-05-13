package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;
import java.util.function.Predicate;

class TakeUntilLookupExplorer implements LookupExplorer {

  private long count;
  private final LookupExplorer firstExplorer;
  private final Predicate<Name> predicate;
  private final LookupExplorer secondExplorer;
  private boolean valid;

  TakeUntilLookupExplorer(
      LookupOperation<LookupExplorer> first,
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      LookupOperation<LookupExplorer> second,
      Predicate<Name> predicate) {
    this.predicate = predicate;
    count = 0;
    firstExplorer = first.start(future, sourceReference, context);
    secondExplorer = second.start(future, sourceReference, context);
    valid = true;
  }

  private TakeUntilLookupExplorer(flabbergast.lang.TakeUntilLookupExplorer original) {
    count = original.count;
    valid = original.valid;
    firstExplorer = original.firstExplorer.duplicate();
    secondExplorer = original.secondExplorer.duplicate();
    predicate = original.predicate;
  }

  @Override
  public LookupExplorer duplicate() {
    return new flabbergast.lang.TakeUntilLookupExplorer(this);
  }

  @Override
  public void process(
      Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next) {
    valid &= predicate.test(targetName);
    if (valid) {
      count++;
      firstExplorer.process(targetName, frame, seen, remaining, next);
    } else {
      secondExplorer.process(targetName, frame, seen - count, remaining, next);
    }
  }
}
