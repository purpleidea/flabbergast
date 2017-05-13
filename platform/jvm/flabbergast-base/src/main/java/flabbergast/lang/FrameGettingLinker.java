package flabbergast.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import jdk.dynalink.*;
import jdk.dynalink.linker.*;

final class FrameGettingLinker implements GuardingDynamicLinker {
  private static final MethodHandle FRAME_GET;
  private static final MethodHandle GUARD;
  private static final MethodHandle NAME__OF_LONG;
  private static final MethodHandle NAME__OF_STR;
  private static final MethodHandle UNPACK_PROMISE;

  static {
    try {
      final var lookup = MethodHandles.lookup();
      FRAME_GET =
          lookup.findVirtual(Frame.class, "get", MethodType.methodType(Optional.class, Name.class));
      GUARD =
          lookup.findStatic(
              FrameGettingLinker.class,
              "guard",
              MethodType.methodType(boolean.class, Object.class, Name.class));
      NAME__OF_LONG =
          lookup.findStatic(Name.class, "of", MethodType.methodType(Name.class, long.class));
      NAME__OF_STR =
          lookup.findStatic(Name.class, "of", MethodType.methodType(Name.class, String.class));
      UNPACK_PROMISE =
          lookup.findStatic(
              FrameGettingLinker.class,
              "unpackPromise",
              MethodType.methodType(Object.class, Optional.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean guard(Object object, Name name) {
    return object instanceof Frame && ((Frame) object).has(name);
  }

  public static Object unpackPromise(Optional<Promise<Any>> result) {
    return result
        .<Object>map(
            promise ->
                promise.apply(
                    new PromiseFunction<>() {
                      @Override
                      public Object apply(Any any) {
                        return any.apply(
                            new AnyFunction<>() {

                              @Override
                              public Object apply() {
                                return null;
                              }

                              @Override
                              public Object apply(boolean value) {
                                return value;
                              }

                              @Override
                              public Object apply(byte[] value) {
                                return value;
                              }

                              @Override
                              public Object apply(double value) {
                                return value;
                              }

                              @Override
                              public Object apply(Frame value) {
                                if (value instanceof ProxyFrame) {
                                  return ((ProxyFrame) value).backing();
                                }
                                return value;
                              }

                              @Override
                              public Object apply(long value) {
                                return value;
                              }

                              @Override
                              public Object apply(LookupHandler value) {
                                return value;
                              }

                              @Override
                              public Object apply(Str value) {
                                return value.toString();
                              }

                              @Override
                              public Object apply(Template value) {
                                return value;
                              }
                            });
                      }

                      @Override
                      public Object unfinished() {
                        return promise;
                      }
                    }))
        .orElse(null);
  }

  private GuardedInvocation castAndGet(
      MethodHandle nameMethod,
      Class<?> targetType,
      LinkerServices linkerServices,
      CallSiteDescriptor callSiteDescriptor,
      Object name) {
    final var createName =
        MethodHandles.foldArguments(
                nameMethod, linkerServices.getTypeConverter(name.getClass(), targetType))
            .bindTo(name);
    return new GuardedInvocation(
        linkerServices.asTypeLosslessReturn(
            MethodHandles.filterReturnValue(
                MethodHandles.foldArguments(FRAME_GET, 1, createName), UNPACK_PROMISE),
            callSiteDescriptor.getMethodType()),
        MethodHandles.foldArguments(GUARD, 1, createName));
  }

  @Override
  public GuardedInvocation getGuardedInvocation(
      LinkRequest linkRequest, LinkerServices linkerServices) {
    final var receiver = linkRequest.getReceiver();
    if (!(receiver instanceof Frame)) {
      return null;
    }

    final var callSiteDescriptor = linkRequest.getCallSiteDescriptor();
    final var name = NamedOperation.getName(callSiteDescriptor.getOperation());
    final var operation = NamedOperation.getBaseOperation(callSiteDescriptor.getOperation());
    if (operation != StandardOperation.GET
        || List.of(NamespaceOperation.getNamespaces(callSiteDescriptor.getOperation()))
            .contains(StandardNamespace.PROPERTY)) {
      return null;
    }
    if (name == null) {
      return new GuardedInvocation(
          linkerServices.asTypeLosslessReturn(
              MethodHandles.filterReturnValue(
                  MethodHandles.foldArguments(FRAME_GET, 1, NAME__OF_STR), UNPACK_PROMISE),
              callSiteDescriptor.getMethodType()),
          MethodHandles.foldArguments(GUARD, 1, NAME__OF_STR));
    } else if (linkerServices.canConvert(name.getClass(), long.class)) {
      return castAndGet(NAME__OF_LONG, long.class, linkerServices, callSiteDescriptor, name);
    } else if (linkerServices.canConvert(name.getClass(), String.class)) {
      return castAndGet(
          NAME__OF_STR, String.class, linkerServices, callSiteDescriptor, name.getClass());
    } else {
      return null;
    }
  }
}
