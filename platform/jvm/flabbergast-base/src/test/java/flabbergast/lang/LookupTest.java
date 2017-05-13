package flabbergast.lang;

import flabbergast.export.TestResult;
import org.junit.Assert;
import org.junit.Test;

public class LookupTest {
  @Test
  public void test1() {
    Assert.assertEquals(
        TestResult.PASS, TestResult.run(UriService.EMPTY, Attribute.of("value", Any.of(true))));
  }

  @Test
  public void test2() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of("value", LookupHandler.CONTEXTUAL.create("foo", "bar")),
            Attribute.of(
                "foo", Frame.define(AttributeSource.of(Attribute.of("bar", Any.of(true)))))));
  }

  @Test
  public void test3() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of("value", LookupHandler.CONTEXTUAL.create("x", "y", "z")),
            Attribute.of("y", Frame.define(AttributeSource.of(Attribute.of("b", Any.of(true))))),
            Attribute.of(
                "x",
                Frame.define(
                    AttributeSource.of(
                        Attribute.of(
                            "y",
                            Frame.define(
                                AttributeSource.of(
                                    Attribute.of(
                                        "z", LookupHandler.CONTEXTUAL.create("y", "b"))))))))));
  }

  @Test
  public void test4() {
    Assert.assertEquals(
        TestResult.ERROR,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of("value", LookupHandler.CONTEXTUAL.create("foo", "bar"))));
  }

  @Test
  public void test5() {
    Assert.assertEquals(
        TestResult.DEADLOCK,
        TestResult.run(
            UriService.EMPTY, Attribute.of("value", LookupHandler.CONTEXTUAL.create("value"))));
  }

  @Test
  public void test6() {
    Assert.assertEquals(
        TestResult.PASS,
        TestResult.run(
            UriService.EMPTY,
            Attribute.of(
                "value",
                new LookupHandler(LookupExplorer.FLAG_CONTAINED, LookupSelector.FIRST)
                    .create(NameSource.EMPTY.add("y").add(2).add("a"))),
            Attribute.of(
                "y",
                Frame.define(
                    AttributeSource.of(
                        Attribute.of(
                            Name.of(3),
                            Frame.define(AttributeSource.of(Attribute.of("a", Any.of(true))))),
                        Attribute.of(
                            Name.of(6),
                            Frame.define(
                                AttributeSource.of(Attribute.of("n", Any.of(false))))))))));
  }
}
