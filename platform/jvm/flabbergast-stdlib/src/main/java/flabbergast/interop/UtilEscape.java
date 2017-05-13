package flabbergast.interop;

import flabbergast.export.BaseFrameTransformer;
import flabbergast.export.LookupAssistant;
import flabbergast.export.NativeBinding;
import flabbergast.lang.*;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Escape a string */
final class UtilEscape extends BaseFrameTransformer<String> {

  private enum CharFormat {
    DECIMAL {
      @Override
      public String get(int bits) {
        return "%d";
      }
    },
    HEX_LOWER {
      @Override
      public String get(int bits) {
        switch (bits) {
          case 32:
            return "%08x";
          case 16:
            return "%04x";
          case 8:
            return "%02x";
          default:
            throw new IllegalArgumentException();
        }
      }
    },
    HEX_UPPER {
      @Override
      public String get(int bits) {
        switch (bits) {
          case 32:
            return "%08X";
          case 16:
            return "%04X";
          case 8:
            return "%02X";
          default:
            throw new IllegalArgumentException();
        }
      }
    };

    public abstract String get(int bits);
  }

  private interface DefaultConsumer extends IntFunction<Stream<String>> {
    boolean matches(int codepoint);
  }

  interface RangeAction extends IntFunction<String> {}

  static class Range implements DefaultConsumer {
    private final int end;
    private final List<RangeAction> replacement;
    private final int start;

    Range(int start, int end, List<RangeAction> replacement) {
      this.start = start;
      this.end = end;
      this.replacement = replacement;
    }

    @Override
    public Stream<String> apply(int codepoint) {
      return replacement.stream().map(action -> action.apply(codepoint));
    }

    @Override
    public boolean matches(int codepoint) {
      return start <= codepoint && codepoint <= end;
    }
  }

  private static final DefaultConsumer DEFAULT =
      new DefaultConsumer() {
        @Override
        public Stream<String> apply(int codepoint) {
          return Stream.of(new String(new int[] {codepoint}, 0, 1));
        }

        @Override
        public boolean matches(int codepoint) {
          return true;
        }
      };

  private static NativeBinding bind(
      int bits, int index, CharFormat format, IntUnaryOperator encode) {
    final var interopUrl =
        "str.escape.utf" + bits + "_" + index + "." + format.name().toLowerCase();
    final var name = "utf" + bits + "_" + index + "_" + format.name().toLowerCase();
    final RangeAction action =
        codepoint -> String.format(format.get(bits), encode.applyAsInt(codepoint));

    return NativeBinding.of(
        interopUrl, Any.of(Frame.proxyOf(name, "<escape>", action, Stream.empty())));
  }

  static Definition create(
      Map<Integer, String> singleSubstitutions,
      Map<String, List<Integer>> lookups,
      Stream<Range> ranges) {
    final List<DefaultConsumer> defaultConsumerList =
        ranges.sorted(Comparator.comparingInt(s -> s.start)).collect(Collectors.toList());
    return create(
        AnyConverter.asString(false),
        () -> new UtilEscape(new TreeMap<>(singleSubstitutions), defaultConsumerList),
        lookups
            .entrySet()
            .stream()
            .map(
                e ->
                    LookupAssistant.find(
                        AnyConverter.asString(false),
                        (i, s) -> {
                          for (final var c : e.getValue()) {
                            i.singleSubstitutions.put(c, s);
                          }
                        },
                        e.getKey())));
  }

  static Stream<NativeBinding> createUnicodeActions() {
    return Stream.of(CharFormat.values())
        .flatMap(
            format ->
                Stream.of(
                    bind(32, 0, format, IntUnaryOperator.identity()),
                    // http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-appendixa
                    bind(
                        16, 0, format, c -> (c < 65536 ? c : ((c - 65536) % 1024 + 56320) % 65536)),
                    bind(16, 1, format, c -> c < 65536 ? 0 : ((c - 65536) / 1024 + 55296) % 65536),
                    bind(
                        8,
                        0,
                        format,
                        c ->
                            (c <= 127
                                    ? c
                                    : c <= 2047
                                        ? c / 64 + 192
                                        : c <= 65535 ? c / 4096 + 224 : c / 262144 + 240)
                                % 256),
                    bind(
                        8,
                        1,
                        format,
                        c ->
                            (c <= 127
                                    ? 0
                                    : c <= 2047
                                        ? c % 64 + 128
                                        : c <= 65535 ? c / 64 % 64 + 128 : c % 262144 * 4096 + 128)
                                % 256),
                    bind(
                        8,
                        2,
                        format,
                        c ->
                            (c <= 2047 ? 0 : c <= 65535 ? c % 64 + 128 : c % 4096 / 64 + 128)
                                % 256),
                    bind(8, 3, format, c -> (c <= 65535 ? 0 : c % 64 + 128) % 256)));
  }

  private final List<DefaultConsumer> ranges;
  private final Map<Integer, String> singleSubstitutions;

  private UtilEscape(Map<Integer, String> singleSubstitutions, List<DefaultConsumer> ranges) {
    this.singleSubstitutions = singleSubstitutions;
    this.ranges = ranges;
  }

  @Override
  protected void apply(
      Future<Any> future,
      SourceReference sourceReference,
      Context context,
      Name name,
      String input) {
    future.complete(
        Any.of(
            input
                .codePoints()
                .boxed()
                .flatMap(
                    c -> {
                      if (singleSubstitutions.containsKey(c)) {
                        return Stream.of(singleSubstitutions.get(c));
                      } else {
                        return ranges
                            .stream()
                            .filter(r -> r.matches(c))
                            .findFirst()
                            .orElse(DEFAULT)
                            .apply(c);
                      }
                    })
                .collect(Collectors.joining())));
  }
}
