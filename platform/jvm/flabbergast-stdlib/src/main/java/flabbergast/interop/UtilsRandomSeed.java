package flabbergast.interop;

import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.Any;
import flabbergast.lang.Context;
import flabbergast.lang.Future;
import flabbergast.lang.SourceReference;
import java.util.Random;

class UtilsRandomSeed implements LookupAssistant.Recipient {
  private Long seed;
  private final UtilRandomGenerator generator;

  UtilsRandomSeed(UtilRandomGenerator generator) {
    this.generator = generator;
  }

  @Override
  public void run(Future<Any> future, SourceReference sourceReference, Context context) {
    final var random = seed == null ? new Random() : new Random(seed);
    future.complete(
        Any.of(
            NativeBinding.generatorTemplate(
                sourceReference, context, () -> generator.next(random))));
  }

  void set(Long seed) {
    this.seed = seed;
  }
}
