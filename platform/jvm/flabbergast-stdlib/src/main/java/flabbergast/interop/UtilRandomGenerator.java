package flabbergast.interop;

import flabbergast.export.NativeBinding;
import flabbergast.export.NativeBinding.ResultType;
import flabbergast.lang.Any;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Definition;
import java.util.Random;

enum UtilRandomGenerator {
  BOOL {
    @Override
    Any next(Random random) {
      return Any.of(random.nextBoolean());
    }
  },
  FLOAT {
    @Override
    Any next(Random random) {
      return Any.of(random.nextDouble());
    }
  },
  GAUSSIAN {
    @Override
    Any next(Random random) {
      return Any.of(random.nextGaussian());
    }
  },
  INT {
    @Override
    Any next(Random random) {
      return Any.of(random.nextLong());
    }
  };

  final Definition create() {
    return NativeBinding.function(
        ResultType.GENERATOR_TEMPLATE,
        seed -> {
          final var random = seed == null ? new Random() : new Random(seed);
          return () -> next(random);
        },
        AnyConverter.asInt(true),
        "seed");
  }

  abstract Any next(Random random);
}
