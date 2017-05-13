package flabbergast.lang;

import flabbergast.export.NativeBinding;
import flabbergast.export.TestResult;
import org.junit.Assert;
import org.junit.Test;

public class FricasseeGrouperTest {

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
                                  Attribute.of(1, Any.of(true)),
                                  Attribute.of(2, Any.of(true)),
                                  Attribute.of(1, Any.of(false)),
                                  Attribute.of(2, Any.of(false))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.disciminateByBool(
                                  Name.of("g"), LookupHandler.CONTEXTUAL.create("a")))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                                  Attribute.of(1, Any.NULL),
                                  Attribute.of(2, Any.of(true)),
                                  Attribute.of(2, Any.of(false))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.alwaysIncludeBool(
                                  Name.of("g"), LookupHandler.CONTEXTUAL.create("a")))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                                  Attribute.of(1, Any.of(true)),
                                  Attribute.of(2, Any.of(true)),
                                  Attribute.of(2, Any.of(false))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.crosstabBool(LookupHandler.CONTEXTUAL.create("a")))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(2)),
                                  Attribute.of(3, Any.of(2)),
                                  Attribute.of(4, Any.of(1))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.adjacentInt(
                                  Name.of("g"), LookupHandler.CONTEXTUAL.create("a")))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count, group) -> i && count.equals(group),
                                  AnyConverter.asInt(false),
                                  "c",
                                  AnyConverter.asInt(false),
                                  "g"),
                              f::complete);
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
                                  Attribute.of(1, Any.of(false)),
                                  Attribute.of(2, Any.of(true)),
                                  Attribute.of(3, Any.of(false)),
                                  Attribute.of(4, Any.of(true))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.boundary(LookupHandler.CONTEXTUAL.create("a"), true))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
                    })));
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
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(false)),
                                  Attribute.of(2, Any.of(false)),
                                  Attribute.of(3, Any.of(true)),
                                  Attribute.of(4, Any.of(false))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.boundary(
                                  LookupHandler.CONTEXTUAL.create("a"), false))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
                    })));
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
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(2)),
                                  Attribute.of(3, Any.of(3)),
                                  Attribute.of(4, Any.of(4))));
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
                                              l -> future.complete(Any.of(l)))),
                              FricasseeGrouper.bucketsInt(LookupHandler.CONTEXTUAL.create("a"), 2))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
                    })));
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
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(2)),
                                  Attribute.of(3, Any.of(1)),
                                  Attribute.of(4, Any.of(2))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "c"),
                                              future::complete)),
                              FricasseeGrouper.chunk(2))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 3,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
                    })));
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
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(1)),
                                  Attribute.of(3, Any.of(2)),
                                  Attribute.of(4, Any.of(2))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "c"),
                                              future::complete)),
                              FricasseeGrouper.stripe(2))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 3,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(0)),
                                  Attribute.of(2, Any.of(1)),
                                  Attribute.of(3, Any.of(3)),
                                  Attribute.of(4, Any.of(4))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "c"),
                                              future::complete)),
                              FricasseeGrouper.ringExponent(2, 3))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 3,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                                  Attribute.of(2, Any.of(2)),
                                  Attribute.of(3, Any.of(3)),
                                  Attribute.of(4, Any.of(4))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "a"),
                                              future::complete)),
                              FricasseeGrouper.alternate(
                                  FricasseeGrouper.chunk(2), FricasseeGrouper.stripe(2)))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, count) -> i && count == 2,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                                  Attribute.of(2, Any.of(2)),
                                  Attribute.of(3, Any.of(3)),
                                  Attribute.of(4, Any.of(4))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "a"),
                                              future::complete)),
                              FricasseeGrouper.chunk(2).invert())
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, sum) -> i && sum == 10,
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
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
                    () -> {
                      final var frame =
                          Frame.create(
                              f,
                              s,
                              c,
                              AttributeSource.of(
                                  Attribute.of(1, Any.of(1)),
                                  Attribute.of(2, Any.of(1)),
                                  Attribute.of(3, Any.of(1)),
                                  Attribute.of(4, Any.of(1))));
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
                                          root.reduce(
                                              future,
                                              sourceReference,
                                              Any.of(0),
                                              NativeBinding.override(
                                                  NativeBinding.INT,
                                                  AnyConverter.asInt(true),
                                                  Long::sum,
                                                  AnyConverter.asInt(false),
                                                  "a"),
                                              future::complete)),
                              FricasseeGrouper.powerset(
                                  FricasseeGrouper.chunk(2), FricasseeGrouper.stripe(2)))
                          .reduce(
                              f,
                              s,
                              Any.of(true),
                              NativeBinding.override(
                                  NativeBinding.BOOL,
                                  AnyConverter.asBool(false),
                                  (i, sum) -> i && (sum == 4 || sum == 2 || sum == 1),
                                  AnyConverter.asInt(false),
                                  "c"),
                              f::complete);
                    })));
  }
}
