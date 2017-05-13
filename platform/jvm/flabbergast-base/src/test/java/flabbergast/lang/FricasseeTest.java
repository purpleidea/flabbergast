package flabbergast.lang;

import flabbergast.export.NativeBinding;
import flabbergast.export.TestResult;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class FricasseeTest {
  @Test
  public void test01() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame1 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(1))));
                      final var frame2 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(2))));
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(Attribute.of(1, frame1), Attribute.of(2, frame2)));
                      Fricassee.forEach(frame, c, "test", 0, 0, 0, 0, s)
                          .count(f, s, count -> f.complete(Any.of(count == 2)));
                    })));
  }

  @Test
  public void test02() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame1 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(1))));
                      final var frame2 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(2))));
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(Attribute.of(1, frame1), Attribute.of(2, frame2)));
                      Fricassee.forEach(frame, c, "test", 0, 0, 0, 0, s)
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  Long::sum,
                                  AnyConverter.asInt(false),
                                  "a"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              sum -> f.complete(Any.of(sum == 3)))));
                    })));
  }

  @Test
  public void test03() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)), Attribute.of(2, Any.of(2))));
                      Fricassee.zip(
                              false, c, "test", 0, 0, 0, 0, s, FricasseeZipper.frame("a", frame))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  Long::sum,
                                  AnyConverter.asInt(false),
                                  "a"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              sum -> f.complete(Any.of(sum == 3)))));
                    })));
  }

  @Test
  public void test04() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)), Attribute.of(2, Any.of(2))));
                      Fricassee.zip(
                              false,
                              c,
                              "test",
                              0,
                              0,
                              0,
                              0,
                              s,
                              FricasseeZipper.frame("a", frame),
                              FricasseeZipper.ordinal("o"),
                              FricasseeZipper.name("n"))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, a, o, n) -> i + (a.equals(o) && a.equals(n) ? a : 0),
                                  AnyConverter.asInt(false),
                                  "a",
                                  AnyConverter.asInt(false),
                                  "o",
                                  AnyConverter.asInt(false),
                                  "n"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              sum -> f.complete(Any.of(sum == 3)))));
                    })));
  }

  @Test
  public void test05() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x * 2),
                                                                Attribute.of("a", previous))))))
                            .take(3)
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    Long::sum,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 7))))))));
  }

  @Test
  public void test06() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x * 2),
                                                                Attribute.of("a", previous))))))
                            .drop(1)
                            .take(2)
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    Long::sum,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 6))))))));
  }

  @Test
  public void test07() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x * 2),
                                                                Attribute.of("a", previous))))))
                            .take(3)
                            .let(Attribute.of("b", LookupHandler.CONTEXTUAL.create("a")))
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    Long::sum,
                                    AnyConverter.asInt(false),
                                    "b"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 7))))))));
  }

  @Test
  public void test08() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x * 2),
                                                                Attribute.of("a", previous))))))
                            .takeWhile(
                                NativeBinding.function(
                                    NativeBinding.BOOL,
                                    x -> x <= 4,
                                    AnyConverter.asInt(false),
                                    "a"))
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    Long::sum,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 7))))))));
  }

  @Test
  public void test09() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame1 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(1))));
                      final var frame2 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(2))));
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(Attribute.of(1, frame1), Attribute.of(2, frame2)));
                      Fricassee.forEach(frame, c, "test", 0, 0, 0, 0, s)
                          .takeLast(1)
                          .single(
                              f,
                              s,
                              LookupHandler.CONTEXTUAL.create("a"),
                              Definition.error("Fail"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              count -> f.complete(Any.of(count == 2)))));
                    })));
  }

  @Test
  public void test10() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame1 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(1))));
                      final var frame2 =
                          Frame.define(AttributeSource.of(Attribute.of("a", Any.of(2))));
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(Attribute.of(1, frame1), Attribute.of(2, frame2)));
                      Fricassee.forEach(frame, c, "test", 0, 0, 0, 0, s)
                          .dropLast(1)
                          .singleOrNull(
                              f,
                              s,
                              LookupHandler.CONTEXTUAL.create("a"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              count -> f.complete(Any.of(count == 1)))));
                    })));
  }

  @Test
  public void test11() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(4)),
                                  Attribute.of(3, Any.of(2))));
                      Fricassee.zip(
                              false, c, "test", 0, 0, 0, 0, s, FricasseeZipper.frame("a", frame))
                          .dropWhile(
                              NativeBinding.function(
                                  NativeBinding.BOOL, x -> x < 4, AnyConverter.asInt(false), "a"))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  Long::sum,
                                  AnyConverter.asInt(false),
                                  "a"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              sum -> f.complete(Any.of(sum == 6)))));
                    })));
  }

  @Test
  public void test12() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(4)),
                                  Attribute.of(3, Any.of(2))));
                      Fricassee.zip(
                              false, c, "test", 0, 0, 0, 0, s, FricasseeZipper.frame("a", frame))
                          .takeWhile(
                              NativeBinding.function(
                                  NativeBinding.BOOL, x -> x < 4, AnyConverter.asInt(false), "a"))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  Long::sum,
                                  AnyConverter.asInt(false),
                                  "a"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              sum -> f.complete(Any.of(sum == 1)))));
                    })));
  }

  @Test
  public void test13() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.zip(
                                false,
                                c,
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                FricasseeZipper.frame(
                                    "a",
                                    Frame.create(
                                        f,
                                        s,
                                        c,
                                        AttributeSource.of(
                                            Attribute.of(1, Any.of(1)),
                                            Attribute.of(2, Any.of(2)),
                                            Attribute.of(3, Any.of(3))))))
                            .reverse()
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    (x, y) -> y + x * y,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 9))))))));
  }

  @Test
  public void test14() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.zip(
                                false,
                                c,
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                FricasseeZipper.frame(
                                    "a",
                                    Frame.create(
                                        f,
                                        s,
                                        c,
                                        AttributeSource.of(
                                            Attribute.of(1, Any.of(1)),
                                            Attribute.of(2, Any.of(2)),
                                            Attribute.of(3, Any.of(3))))))
                            .orderByInt(false, LookupHandler.CONTEXTUAL.create("a"))
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    (x, y) -> y + x * y,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 9))))))));
  }

  @Test
  public void test15() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x + 1),
                                                                Attribute.of("a", previous))))))
                            .take(3)
                            .toList(
                                f,
                                s,
                                (future, sourceReference, context) ->
                                    () -> future.complete(Any.NULL),
                                r ->
                                    f.complete(
                                        Any.of(
                                            r.names()
                                                .collect(Collectors.toSet())
                                                .equals(
                                                    Set.of(
                                                        Name.of(1), Name.of(2), Name.of(3)))))))));
  }

  @Test
  public void test16() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.generate(
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                c,
                                Any.of(1),
                                (future, sourceReference, context, previous) ->
                                    () ->
                                        previous.accept(
                                            AnyConverter.asInt(false)
                                                .asConsumer(
                                                    future,
                                                    sourceReference,
                                                    TypeErrorLocation.UNKNOWN,
                                                    x ->
                                                        future.complete(
                                                            Accumulator.of(
                                                                Any.of(x + 1),
                                                                Attribute.of("a", previous))))))
                            .take(3)
                            .toFrame(
                                f,
                                s,
                                LookupHandler.CONTEXTUAL.create("a"),
                                (ff, fs, fc) -> () -> ff.complete(Any.NULL),
                                r ->
                                    f.complete(
                                        Any.of(
                                            r.names()
                                                .collect(Collectors.toSet())
                                                .equals(
                                                    Set.of(
                                                        Name.of(1), Name.of(2), Name.of(3)))))))));
  }

  @Test
  public void test17() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                (f, s, c) ->
                    () ->
                        Fricassee.zip(
                                false,
                                c,
                                "test",
                                0,
                                0,
                                0,
                                0,
                                s,
                                FricasseeZipper.frame(
                                    "a",
                                    Frame.create(
                                        f,
                                        s,
                                        c,
                                        AttributeSource.of(
                                            Attribute.of(1, Any.of(1)),
                                            Attribute.of(2, Any.of(2))))))
                            .flatten(
                                (future, sourceReference, context) ->
                                    () ->
                                        future.complete(
                                            Fricassee.zip(
                                                false,
                                                c,
                                                "test",
                                                0,
                                                0,
                                                0,
                                                0,
                                                s,
                                                FricasseeZipper.frame(
                                                    "a",
                                                    Frame.create(
                                                        f,
                                                        s,
                                                        c,
                                                        AttributeSource.of(
                                                            Attribute.of(1, Any.of(1)),
                                                            Attribute.of(2, Any.of(2)),
                                                            Attribute.of(3, Any.of(3))))))))
                            .reduce(
                                f,
                                s,
                                Any.of(0),
                                NativeBinding.override(
                                    NativeBinding.INT,
                                    AnyConverter.asInt(false),
                                    Long::sum,
                                    AnyConverter.asInt(false),
                                    "a"),
                                r ->
                                    r.accept(
                                        AnyConverter.asInt(false)
                                            .asConsumer(
                                                f,
                                                s,
                                                TypeErrorLocation.UNKNOWN,
                                                sum -> f.complete(Any.of(sum == 12))))))));
  }
}
