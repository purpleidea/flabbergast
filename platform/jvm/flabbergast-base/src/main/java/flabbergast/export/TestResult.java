package flabbergast.export;

import flabbergast.lang.Any;
import flabbergast.lang.AnyFunction;
import flabbergast.lang.Attribute;
import flabbergast.lang.AttributeSource;
import flabbergast.lang.Context;
import flabbergast.lang.DeadlockInformation;
import flabbergast.lang.Frame;
import flabbergast.lang.Lookup;
import flabbergast.lang.LookupHandler;
import flabbergast.lang.NameSource;
import flabbergast.lang.Scheduler;
import flabbergast.lang.ServiceFlag;
import flabbergast.lang.SourceReference;
import flabbergast.lang.Str;
import flabbergast.lang.TaskResult;
import flabbergast.lang.Template;
import flabbergast.lang.UriService;
import flabbergast.util.Pair;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/** The result of a unit test */
public enum TestResult {
  /** The test was good (returned true) */
  PASS,
  /** The test was good (returned false) */
  FAIL,
  /** The test failed to start */
  NOT_RUN,
  /** The test returned a non-Boolean type */
  BAD_TYPE,
  /** The test deadlocked */
  DEADLOCK,
  /** The test threw an error */
  ERROR,
  /** The test had some internal error */
  INTERNAL_ERROR;

  /**
   * Run a test
   *
   * <p>This will create a root frame with the attributes provided and then perform a lookup for
   * <tt>value</tt> which must return Boolean
   *
   * @param service a custom URI service to include
   * @param attributes the attributes to add to the root frame
   */
  public static TestResult run(UriService service, Attribute... attributes) {
    final var taskMaster = Scheduler.builder().set(ServiceFlag.SANDBOXED).add(service).build();
    final var testResult = new AtomicReference<>(NOT_RUN);
    taskMaster.run(
        future ->
            () ->
                NameSource.EMPTY
                    .add("value")
                    .collect(
                        future,
                        SourceReference.root("root lookup"),
                        Context.EMPTY.prepend(
                            Frame.create(
                                future,
                                SourceReference.root("test"),
                                Context.EMPTY,
                                AttributeSource.of(attributes))),
                        LookupHandler.CONTEXTUAL,
                        future::complete),
        new TaskResult() {
          @Override
          public void succeeded(Any result) {
            testResult.set(
                result.apply(
                    new AnyFunction<>() {
                      @Override
                      public TestResult apply() {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(boolean value) {
                        return value ? PASS : FAIL;
                      }

                      @Override
                      public TestResult apply(byte[] value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(double value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(Frame value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(long value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(LookupHandler value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(Str value) {
                        return BAD_TYPE;
                      }

                      @Override
                      public TestResult apply(Template value) {
                        return BAD_TYPE;
                      }
                    }));
          }

          @Override
          public void deadlocked(DeadlockInformation information) {
            testResult.set(DEADLOCK);
          }

          @Override
          public void error(
              Stream<Pair<SourceReference, String>> errors,
              Stream<Pair<Lookup, Optional<String>>> lookupErrors) {
            errors.forEach(e -> System.out.println(e.second()));
            lookupErrors.forEach(e -> System.out.println(e.second()));
            testResult.set(ERROR);
          }

          @Override
          public void failed(Exception e) {
            e.printStackTrace();
            testResult.set(INTERNAL_ERROR);
          }
        });
    return testResult.get();
  }
}
