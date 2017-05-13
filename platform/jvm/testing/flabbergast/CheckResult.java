package flabbergast;

import flabbergast.compiler.ErrorCollector;
import flabbergast.compiler.SourceLocation;
import flabbergast.lang.AcceptOrFail;
import flabbergast.lang.Any;
import flabbergast.lang.BaseLookup;
import flabbergast.lang.Frame;
import flabbergast.lang.PromiseChecker;
import flabbergast.lang.SourceReference;
import flabbergast.lang.TaskMaster.TaskResult;
import java.util.stream.Stream;

final class CheckResult extends AcceptOrFail implements ErrorCollector, TaskResult {
  private boolean success;

  private class ValueChecker extends AcceptOrFail implements PromiseChecker {
    @Override
    public void accept(boolean value) {
      success = value;
    }

    @Override
    protected void fail(String type) {
      success = false;
    }

    @Override
    public void unfinished() {
      success = false;
    }
  }

  @Override
  public void accept(Frame value) {
    value.get("value").check(new ValueChecker());
  }

  @Override
  public void emitError(SourceLocation location, String error) {
    success = false;
  }

  @Override
  protected void fail(String type) {
    success = false;
  }

  public boolean getSuccess() {
    return success;
  }

  @Override
  public void deadlocked(Stream<BaseLookup> lookups) {
    success = false;
  }

  @Override
  public void error(
      Stream<Pair<SourceReference, String>> errors, Stream<Pair<BaseLookup, String>> lookupErrors) {
    success = false;
  }

  @Override
  public void failed(Exception e) {
    success = false;
  }

  @Override
  public void succeeded(Any result) {
    result.accept(this);
  }
}
