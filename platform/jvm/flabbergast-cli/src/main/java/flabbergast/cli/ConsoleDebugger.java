package flabbergast.cli;

import flabbergast.export.InteractiveDebugger;
import flabbergast.lang.Any;
import flabbergast.lang.RootDefinition;
import flabbergast.lang.SourceReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsoleDebugger extends InteractiveDebugger {
  private final Map<Long, Any> currentValue = new ConcurrentHashMap<>();

  @Override
  protected void handleError(
      InterruptionHandle interruptionHandle, SourceReference sourceReference, String message) {}

  @Override
  protected void interrupt(InterruptionHandle point) {
    // TOOD parse command
  }

  // Resume
  // Return (run owner with magic variable)
  private void returnToCaller(InterruptionHandle point) {
    point.accept(currentValue.getOrDefault(point.id(), Any.NULL));
  }
  // Step (run inner, then set magic variable)
  private void step(InterruptionHandle point) {
    rewrite(point, point.normalReturnValue());
  }
  // Show (dump magic variable)
  // Rewrite x In body (compute new magic variable using existing one)
  private void rewrite(InterruptionHandle point, RootDefinition body) {
    point.attempt(
        body,
        v -> {
          currentValue.put(point.id(), v);
          interrupt(point);
        });
  }
  // Breakpoints (list active breakpoints)
  // Switch # (switch to breakpoint)
}
