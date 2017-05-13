package flabbergast.lang;

import flabbergast.util.Pair;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

class TemplateInvokerLinker implements TypeBasedGuardingDynamicLinker {
  private static final List<Pair<Class<?>, MethodHandle>> ANY_OF_HANDLE = new ArrayList<>();
  private static final MethodHandle ANY_UNIT;
  private static final MethodHandle CLASS_CAST_CTOR;
  private static final MethodHandle GUARD;
  private static final MethodHandle INVOKE_TEMPLATE;
  private static final MethodHandle INVOKE_TEMPLATE_WITH_TASK_MASTER;
  private static final SourceReference ROOT = SourceReference.root("template runner");

  static {
    try {
      final var lookup = MethodHandles.lookup();
      CLASS_CAST_CTOR =
          lookup.findConstructor(
              ClassCastException.class, MethodType.methodType(void.class, String.class));
      INVOKE_TEMPLATE =
          lookup.findStatic(
              TemplateInvokerLinker.class,
              "invoke",
              MethodType.methodType(Object.class, Template.class, Any[].class));
      INVOKE_TEMPLATE_WITH_TASK_MASTER =
          lookup.findStatic(
              TemplateInvokerLinker.class,
              "invoke",
              MethodType.methodType(Object.class, Template.class, Scheduler.class, Any[].class));
      GUARD =
          lookup.findStatic(
              TemplateInvokerLinker.class,
              "guard",
              MethodType.methodType(boolean.class, Object.class));
      ANY_UNIT =
          MethodHandles.dropArguments(
              lookup.findStaticGetter(Any.class, "NULL", Any.class), 1, Object.class);
      ANY_OF_HANDLE.add(Pair.of(Any.class, MethodHandles.identity(Any.class)));
      ANY_OF_HANDLE.add(
          Pair.of(
              Name.class, lookup.findVirtual(Name.class, "any", MethodType.methodType(Any.class))));
      ANY_OF_HANDLE.add(
          Pair.of(
              int.class,
              lookup
                  .findStatic(Any.class, "of", MethodType.methodType(Any.class, long.class))
                  .asType(MethodType.methodType(Any.class, int.class))));
      ANY_OF_HANDLE.add(
          Pair.of(
              Integer.class,
              MethodHandles.foldArguments(
                  lookup.findStatic(Any.class, "of", MethodType.methodType(Any.class, Long.class)),
                  lookup.findStatic(
                      TemplateInvokerLinker.class,
                      "convert",
                      MethodType.methodType(Long.class, Integer.class)))));

      for (Class<?> c :
          new Class[] {
            boolean.class,
            long.class,
            double.class,
            Frame.class,
            LookupHandler.class,
            Template.class,
            String.class,
            Str.class
          }) {
        ANY_OF_HANDLE.add(
            Pair.of(c, lookup.findStatic(Any.class, "of", MethodType.methodType(Any.class, c))));
      }

    } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static Long convert(Integer value) {
    return value == null ? null : value.longValue();
  }

  private static boolean guard(Object object) {
    return object instanceof Template && ((Template) object).isFunctionLike();
  }

  private static Object invoke(Template template, Any... args) {
    final var master = Scheduler.builder().build();
    return invoke(template, master, args);
  }

  private static Object invoke(Template template, Scheduler scheduler, Any... args) {
    final var resultHandler =
        new TaskResult() {
          Object output;

          @Override
          public void deadlocked(DeadlockInformation information) {
            throw new RuntimeException(
                "Circular lookups in Flabbergast: "
                    + information.lookups().count()
                    + information.waitingOperations().count());
          }

          @Override
          public void error(
              Stream<Pair<SourceReference, String>> errors,
              Stream<Pair<Lookup, Optional<String>>> lookupErrors) {
            lookupErrors.close();
            throw new RuntimeException(
                "Failed lookups in Flabbergast: "
                    + errors.map(Pair::second).collect(Collectors.joining(", ")));
          }

          @Override
          public void failed(Exception e) {
            throw new RuntimeException(e);
          }

          @Override
          public void succeeded(Any result) {
            output = FrameGettingLinker.unpackPromise(Optional.of(result));
          }
        };
    scheduler.run(
        future ->
            () ->
                future.await(
                    Frame.create(
                            future,
                            ROOT,
                            Context.EMPTY,
                            template,
                            AttributeSource.of(
                                Attribute.of(
                                    "args",
                                    Any.of(
                                        Frame.create(
                                            future,
                                            ROOT,
                                            Context.EMPTY,
                                            IntStream.range(0, args.length)
                                                .mapToObj(i -> Attribute.of(i, args[i]))
                                                .collect(AttributeSource.toSource()))))))
                        .get(Name.of("value"))
                        .orElse(Any.NULL),
                    ROOT.special("Java caller"),
                    "Function-like template as function invoker",
                    future::complete),
        resultHandler);
    return resultHandler.output;
  }

  private GuardedInvocation buildInvocation(
      MethodHandle rootHandle, int skip, LinkRequest linkRequest, LinkerServices linkerServices) {
    final var handle =
        MethodHandles.filterArguments(
            rootHandle.asCollector(
                1 + skip, Any[].class, linkRequest.getArguments().length - 2 - skip),
            0,
            filtersFor(1 + skip, linkerServices, linkRequest.getArguments()));

    return new GuardedInvocation(
        MethodHandles.dropArguments(
            linkerServices.asTypeLosslessReturn(
                handle, linkRequest.getCallSiteDescriptor().getMethodType()),
            1,
            Object.class),
        MethodHandles.dropArguments(
            MethodHandles.dropArgumentsToMatch(
                GUARD,
                0,
                handle.type().parameterList().subList(1, handle.type().parameterCount()),
                1),
            1,
            Object.class));
  }

  @Override
  public boolean canLinkType(Class<?> clazz) {
    return clazz == Template.class;
  }

  private MethodHandle[] filtersFor(int start, LinkerServices services, Object[] parameterArray) {
    return Stream.of(parameterArray)
        .skip(start)
        .map(
            o ->
                o == null
                    ? ANY_UNIT
                    : ANY_OF_HANDLE
                        .stream()
                        .filter(
                            e ->
                                e.first().isAssignableFrom(o.getClass())
                                    || services.canConvert(o.getClass(), e.first()))
                        .map(
                            e ->
                                e.first().isAssignableFrom(o.getClass())
                                    ? e.second()
                                    : MethodHandles.foldArguments(
                                        e.second(),
                                        services.getTypeConverter(o.getClass(), e.first())))
                        .findFirst()
                        .orElseGet(
                            () ->
                                MethodHandles.dropArguments(
                                    MethodHandles.foldArguments(
                                        MethodHandles.throwException(
                                            Any.class, ClassCastException.class),
                                        CLASS_CAST_CTOR.bindTo(
                                            String.format(
                                                "Cannot convert %s to a Flabbergast type",
                                                o.getClass()))),
                                    0,
                                    Any.class)))
        .toArray(MethodHandle[]::new);
  }

  @Override
  public GuardedInvocation getGuardedInvocation(
      LinkRequest linkRequest, LinkerServices linkerServices) {
    final var receiver = linkRequest.getReceiver();
    if (!(receiver instanceof Template)) {
      return null;
    }

    if (NamespaceOperation.getBaseOperation(linkRequest.getCallSiteDescriptor().getOperation())
        != StandardOperation.CALL) {
      return null;
    }
    if (linkRequest.getArguments().length > 0
        && linkRequest.getArguments()[0] instanceof Scheduler) {
      return buildInvocation(INVOKE_TEMPLATE_WITH_TASK_MASTER, 1, linkRequest, linkerServices);
    } else {
      return buildInvocation(INVOKE_TEMPLATE, 0, linkRequest, linkerServices);
    }
  }
}
