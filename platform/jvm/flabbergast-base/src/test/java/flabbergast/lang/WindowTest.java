package flabbergast.lang;

import flabbergast.export.NativeBinding;
import flabbergast.export.TestResult;
import org.junit.Assert;
import org.junit.Test;

public class WindowTest {

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
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(10)),
                                  Attribute.of(2, Any.of(20)),
                                  Attribute.of(3, Any.of(30)),
                                  Attribute.of(4, Any.of(40))));
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
                          .groupBy(
                              s,
                              FricasseeGrouper.collect(
                                  "c",
                                  (future, sourceReference, context, root) ->
                                      () ->
                                          root.count(
                                              future,
                                              sourceReference,
                                              count -> future.complete(Any.of(count == 2)))),
                              FricasseeGrouper.windowed(
                                  FricasseeWindow.count(2), FricasseeWindow.NON_OVERLAPPING))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, ok) -> i + (ok ? 1 : 0),
                                  AnyConverter.asBool(false),
                                  "c"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              v -> f.complete(Any.of(v == 2)))));
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
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(10)),
                                  Attribute.of(2, Any.of(20)),
                                  Attribute.of(3, Any.of(30)),
                                  Attribute.of(4, Any.of(40))));
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
                          .groupBy(
                              s,
                              FricasseeGrouper.collect(
                                  "c",
                                  (future, sourceReference, context, root) ->
                                      () ->
                                          root.count(
                                              future,
                                              sourceReference,
                                              count -> future.complete(Any.of(count == 2)))),
                              FricasseeGrouper.windowed(
                                  FricasseeWindow.duration(
                                      LookupHandler.CONTEXTUAL.create("a"), 15),
                                  FricasseeWindow.count(2)))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, ok) -> i + (ok ? 1 : 0),
                                  AnyConverter.asBool(false),
                                  "c"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              v -> f.complete(Any.of(v == 2)))));
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
                                  Attribute.of(1, Any.of(10)),
                                  Attribute.of(2, Any.of(20)),
                                  Attribute.of(3, Any.of(30)),
                                  Attribute.of(4, Any.of(40))));
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
                          .groupBy(
                              s,
                              FricasseeGrouper.collect(
                                  "c",
                                  (future, sourceReference, context, root) ->
                                      () ->
                                          root.count(
                                              future,
                                              sourceReference,
                                              count -> future.complete(Any.of(count == 2)))),
                              FricasseeGrouper.windowed(
                                  FricasseeWindow.count(2),
                                  FricasseeWindow.duration(
                                      LookupHandler.CONTEXTUAL.create("a"), 15)))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, ok) -> i + (ok ? 1 : 0),
                                  AnyConverter.asBool(false),
                                  "c"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              v -> f.complete(Any.of(v == 2)))));
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
                                  Attribute.of(1, Any.of(10)),
                                  Attribute.of(2, Any.of(20)),
                                  Attribute.of(3, Any.of(30)),
                                  Attribute.of(4, Any.of(40))));
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
                          .groupBy(
                              s,
                              FricasseeGrouper.collect(
                                  "c",
                                  (future, sourceReference, context, root) ->
                                      () ->
                                          root.count(
                                              future,
                                              sourceReference,
                                              count -> future.complete(Any.of(count == 4)))),
                              FricasseeGrouper.windowed(
                                  FricasseeWindow.NON_OVERLAPPING, FricasseeWindow.NON_OVERLAPPING))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, ok) -> i + (ok ? 1 : 0),
                                  AnyConverter.asBool(false),
                                  "c"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              v -> f.complete(Any.of(v == 1)))));
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
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(10)),
                                  Attribute.of(2, Any.of(20)),
                                  Attribute.of(3, Any.of(40)),
                                  Attribute.of(4, Any.of(50))));
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
                          .groupBy(
                              s,
                              FricasseeGrouper.collect(
                                  "c",
                                  (future, sourceReference, context, root) ->
                                      () ->
                                          root.count(
                                              future,
                                              sourceReference,
                                              count -> future.complete(Any.of(count == 2)))),
                              FricasseeGrouper.windowed(
                                  FricasseeWindow.session(
                                      LookupHandler.CONTEXTUAL.create("a"), 15, 25),
                                  FricasseeWindow.NON_OVERLAPPING))
                          .reduce(
                              f,
                              s,
                              Any.of(0),
                              NativeBinding.override(
                                  NativeBinding.INT,
                                  AnyConverter.asInt(false),
                                  (i, ok) -> i + (ok ? 1 : 0),
                                  AnyConverter.asBool(false),
                                  "c"),
                              r ->
                                  r.accept(
                                      AnyConverter.asInt(false)
                                          .asConsumer(
                                              f,
                                              s,
                                              TypeErrorLocation.UNKNOWN,
                                              v -> f.complete(Any.of(v == 2)))));
                    })));
  }
}
