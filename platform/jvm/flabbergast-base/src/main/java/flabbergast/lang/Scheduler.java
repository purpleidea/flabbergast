package flabbergast.lang;

import flabbergast.util.Pair;
import flabbergast.util.Result;
import java.lang.invoke.*;
import java.net.URI;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;

/** Scheduler and global state for executing Flabbergast programs. */
public final class Scheduler {
  private enum Mode {
    COMPLETE {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        throw new IllegalStateException("Future is complete. Cannot perform more operations.");
      }
    },
    ERROR {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        switch (desired) {
          case COMPLETE:
            throw new IllegalStateException("Future is in error. Cannot assign it a value.");
          case ERROR:
            return ERROR;
          case RUN:
            if (active.decrementAndGet() == 0) {
              lock.countDown();
            }
            return RUN_ERROR;
          case WAIT:
            return WAIT_ERROR;
          case WAIT_ERROR:
          case RUN_ERROR:
            throw new IllegalArgumentException();
          default:
            return null;
        }
      }
    },
    RUN {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        switch (desired) {
          case RUN:
            return RUN;
          case WAIT:
          case COMPLETE:
          case ERROR:
            if (active.decrementAndGet() == 0) {
              lock.countDown();
            }
            return desired;
          case WAIT_ERROR:
          case RUN_ERROR:
            throw new IllegalArgumentException();
          default:
            return null;
        }
      }
    },
    RUN_ERROR {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        switch (desired) {
          case COMPLETE:
            throw new IllegalStateException("Future is in error. Cannot assign it a value.");
          case ERROR:
            return ERROR;
          case RUN:
            return RUN_ERROR;
          case WAIT:
            return WAIT_ERROR;
          case RUN_ERROR:
          case WAIT_ERROR:
            throw new IllegalArgumentException();
          default:
            return null;
        }
      }
    },
    WAIT {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        switch (desired) {
          case COMPLETE:
            if (active.decrementAndGet() == 0) {
              lock.countDown();
            }
            return COMPLETE;
          case ERROR:
            if (active.decrementAndGet() == 0) {
              lock.countDown();
            }
            return ERROR;
          case RUN:
            active.incrementAndGet();
            return RUN;
          case WAIT:
            return WAIT;
          case RUN_ERROR:
          case WAIT_ERROR:
            throw new IllegalArgumentException();
          default:
            return null;
        }
      }
    },
    WAIT_ERROR {
      @Override
      public Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active) {
        switch (desired) {
          case COMPLETE:
            throw new IllegalStateException("Future is in error. Cannot assign it a value.");
          case ERROR:
            return ERROR;
          case RUN:
            return RUN_ERROR;
          case WAIT:
            return WAIT_ERROR;
          case RUN_ERROR:
          case WAIT_ERROR:
            throw new IllegalArgumentException();
          default:
            return null;
        }
      }
    };

    public abstract Mode transition(Mode desired, CountDownLatch lock, AtomicInteger active);
  }

  /** Specify the configuration of a new task master */
  public static final class Builder {
    private static final ServiceLoader<KwsService> KWS_FUNCTION_SERVICES =
        ServiceLoader.load(KwsService.class);
    private Debugger debugger = Debugger.RUN;
    private final List<KwsService> kwsServices = new ArrayList<>();
    private int maxStackDepth = 10;
    private final Set<ServiceFlag> rules = EnumSet.noneOf(ServiceFlag.class);
    private final List<Path> searchPath = new ArrayList<>();
    private final List<UriService> services = new ArrayList<>();

    private Builder() {}

    /** Adds a path to the resource search list */
    public Builder add(Path directory) {
      searchPath.add(directory);
      return this;
    }

    /** Add a KWS function service */
    public Builder add(KwsService service) {
      kwsServices.add(service);
      return this;
    }

    /** Add a URI source */
    public Builder add(UriService service) {
      services.add(service);
      return this;
    }

    /** Create the new task master. */
    public Scheduler build() {
      final var finder = new ResourcePathFinder(searchPath.stream());
      return new Scheduler(
          services.stream().flatMap(service -> service.create(finder, rules::contains)),
          kwsServices.stream().flatMap(service -> service.discover(rules::contains)),
          debugger,
          maxStackDepth);
    }

    /** Get the debugger. */
    public Debugger debugger() {
      return debugger;
    }

    /**
     * Specify a new debugger
     *
     * <p>The debugger will be invoked whenever a <tt>debug</tt> KWSVM instruction is reached.
     */
    public Builder debugger(Debugger debugger) {
      this.debugger = debugger;
      return this;
    }

    /** Add standard KWS library services */
    public Builder defaultKwsFunctions() {
      for (final var service : KWS_FUNCTION_SERVICES) {
        kwsServices.add(service);
      }
      return this;
    }

    /** Add standard library and resource paths */
    public Builder defaultPaths() {
      ResourcePathFinder.defaultPaths().forEach(searchPath::add);
      return this;
    }

    /** Add URI handlers in the class path */
    public Builder defaultUriServices() {
      Stream.concat(
              URI_SERVICES.stream().map(ServiceLoader.Provider::get),
              Stream.of(
                  (finder, flags) ->
                      Stream.of(Frame.TIME_INTEROP, new LibraryLoaderUriHandler(finder, flags))))
          .forEach(services::add);
      return this;
    }

    /** Gets the KWS services in order. */
    public Stream<KwsService> kwsServices() {
      return kwsServices.stream();
    }

    /** Gets the maximum number of nested Flabbergast stack frames allowed per thread */
    public int maxStackDepth() {
      return maxStackDepth;
    }

    /**
     * Sets the maximum number of nested Flabbergast stack frames allowed per thread
     *
     * <p>Flabbergast uses an asynchronous scheduler to run parallel operations. However, for
     * efficiency, the task master will attempt to run a computation rather than queue it. This sets
     * the maximum number of nested operations that will be run by a single thread. Increasing this
     * number may improve processor locality, but will reduce the number of operations available to
     * other processors. Since Flabbergast (and the runtime support code written in Java) is very
     * tail-call heavy but the JVM does not provide tail-call optimisation, then JVM stack will have
     * more execution frames added than it would appear from a Flabbergast trace. Therefore, this
     * number must be much lower than the JVM's maximum stack depth. Additionally, bulk operations,
     * which occur when instantiating a frame, are always put in the execution pool for parallel
     * execution.
     *
     * @param maxStackDepth the number of Flabbergast operations that can be executed in a nested
     *     fashion
     */
    public Builder maxStackDepth(int maxStackDepth) {
      this.maxStackDepth = maxStackDepth;
      return this;
    }

    /** Get the paths that will be searched for modules, source files, and resources */
    public Stream<Path> paths() {
      return searchPath.stream();
    }

    /** Clear a URI loading configuration */
    public Builder reset(ServiceFlag rule) {
      rules.remove(rule);
      return this;
    }

    /**
     * Checks if a rule is set
     *
     * @param rule the rule to check
     */
    public boolean rule(ServiceFlag rule) {
      return rules.contains(rule);
    }

    /**
     * Set or reset a rule based on the provided flag
     *
     * @param rule the rule of set or reset
     * @param state if true, set the rule; if false, clear it
     */
    public Builder rule(ServiceFlag rule, boolean state) {
      if (state) {
        rules.add(rule);
      } else {
        rules.remove(rule);
      }
      return this;
    }

    /** Gets the URI services in order. */
    public Stream<UriService> services() {
      return services.stream();
    }

    /** Set a URI loading configuration */
    public Builder set(ServiceFlag rule) {
      rules.add(rule);
      return this;
    }
  }

  static class FunctionDescriptor {
    private final String name;
    private final Class<?> returnType;
    private final MethodType type;

    private FunctionDescriptor(String name, MethodType type, Class<?> returnType) {
      this.name = name;
      this.type = type;
      this.returnType = returnType;
    }

    public CallSite createException() {
      return new MutableCallSite(
          MethodHandles.dropArgumentsToMatch(
              MethodHandles.insertArguments(
                  FUTURE_ERROR,
                  1,
                  String.format(
                      "Unbound KWS function %s with method type %s returning %s",
                      name, type, returnType)),
              0,
              type.parameterList(),
              0));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FunctionDescriptor that = (FunctionDescriptor) o;
      return name.equals(that.name) && type.equals(that.type) && returnType.equals(that.returnType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type, returnType);
    }
  }

  static class PromiseHolder<T> extends Promise<T> {

    private final AtomicReference<Promise<T>> value = new AtomicReference<>();

    public PromiseHolder(Promise<T> promise) {
      value.set(promise);
    }

    @Override
    public void accept(PromiseConsumer<? super T> consumer) {
      value.get().accept(consumer);
    }

    @Override
    public <R> R apply(PromiseFunction<? super T, R> function) {
      return value.get().apply(function);
    }

    @Override
    void await(Future<?> waiter, Consumer<? super T> consumer) {
      value.get().await(waiter, consumer);
    }

    public void set(Promise<T> result) {
      value.set(result);
    }
  }

  class State {

    class Inflight<T extends Promise<T>> extends Future<T> {
      private final Map<Inflight<?>, AtomicInteger> dependencies = new ConcurrentHashMap<>();
      private List<Consumer<? super T>> listeners = new ArrayList<>();
      private Mode mode = Mode.WAIT;
      private T value;
      private final PromiseHolder<T> result =
          new PromiseHolder<>(
              new Promise<>() {

                @Override
                public void accept(PromiseConsumer consumer) {
                  consumer.unfinished();
                }

                @Override
                public <R> R apply(PromiseFunction<? super T, R> function) {
                  return function.unfinished();
                }

                @Override
                public void await(Future<?> waiter, Consumer<? super T> consumer) {
                  synchronized (Inflight.this) {
                    if (value == null) {
                      final Runnable edge =
                          waiter == null ? () -> {} : waiter.waitEdge(Inflight.this);
                      listeners.add(
                          v -> {
                            edge.run();
                            consumer.accept(v);
                          });
                      return;
                    }
                  }
                  consumer.accept(value);
                }
              });

      public Inflight() {}

      @Override
      public void await(Promise<Any> promise, Lookup lookup, Consumer<? super Any> consumer) {
        lookups.add(lookup);
        await(
            promise,
            v -> {
              lookups.remove(lookup);
              consumer.accept(v);
            });
      }

      @Override
      public void await(
          Promise<Any> promise,
          SourceReference sourceReference,
          String description,
          Consumer<? super Any> consumer) {
        await(
            promise, new InflightWaitingOperation<>(this, sourceReference, description, consumer));
      }

      public <X> void await(Promise<X> promise, Consumer<? super X> consumer) {
        final var immediate = new AtomicBoolean();
        promise.await(
            this,
            v -> {
              synchronized (this) {
                mode = mode.transition(Mode.RUN, lock, active);
                immediate.set(true);
              }
              consumer.accept(v);
            });
        synchronized (this) {
          if (!immediate.get()) {
            mode = mode.transition(Mode.WAIT, lock, active);
          }
        }
      }

      public void awaitDisbursement(
          Promise<Any> promise,
          SourceReference sourceReference,
          String description,
          Runnable gatherer) {
        promise.await(
            null,
            new InflightWaitingOperation<>(
                null, sourceReference, description, v -> gatherer.run()));
      }

      @Override
      public MethodHandle bind(FunctionDescriptor descriptor) {
        return callsites
            .computeIfAbsent(descriptor, FunctionDescriptor::createException)
            .dynamicInvoker();
      }

      @Override
      public void complete(T value) {
        List<Consumer<? super T>> listeners;
        synchronized (this) {
          this.value = value;
          result.set(value);
          listeners = this.listeners;
          this.listeners = null;
        }
        listeners.forEach(listener -> listener.accept(value));
        mode = mode.transition(Mode.COMPLETE, lock, active);
      }

      @Override
      public void debug(
          RootDefinition normalReturnValue,
          SourceReference sourceReference,
          Context context,
          Consumer<? super Any> next) {
        debugger.handle(inner(next), sourceReference, context, normalReturnValue);
      }

      @Override
      public synchronized void error(Lookup lookup) {
        lookupErrors.add(Pair.of(lookup, Optional.empty()));
        mode = mode.transition(Mode.ERROR, lock, active);
      }

      @Override
      public synchronized void error(Lookup lookup, String failType) {
        lookupErrors.add(Pair.of(lookup, Optional.of(failType)));
        mode = mode.transition(Mode.ERROR, lock, active);
      }

      @Override
      public synchronized void error(SourceReference sourceReference, String message) {
        errors.add(Pair.of(sourceReference, message));
        mode = mode.transition(Mode.ERROR, lock, active);
      }

      @Override
      void export(String name, Class<?> returnType, MethodHandle handle) {
        Scheduler.this.bind(
            name,
            returnType,
            MethodHandles.dropArguments(
                MethodHandles.foldArguments(CONSUMER_ACCEPT, 1, handle),
                0,
                Future.class,
                SourceReference.class));
      }

      @Override
      public synchronized void external(
          String uri, SourceReference sourceReference, Consumer<? super Any> target) {
        await(
            externalCache.computeIfAbsent(uri, State.this::loadExternal),
            sourceReference,
            String.format("From “%s”", uri),
            target);
      }

      @Override
      public LaunchBatch launch() {
        return new LaunchBatch() {
          private List<Runnable> todo = new ArrayList<>();

          @Override
          public void execute() {
            todo.forEach(Runnable::run);
            todo = null;
          }

          @Override
          public Promise<Any> launch(RootDefinition definition) {
            if (todo == null) {
              return Inflight.this.launch(definition);
            }
            final var inflight = new Inflight<Any>();
            todo.add(
                () -> {
                  final var work = definition.launch(inflight);
                  inflight.mode = inflight.mode.transition(Mode.RUN, lock, active);
                  executor.execute(work);
                });
            return inflight.result;
          }

          @Override
          public Future<?> owner() {
            return Inflight.this;
          }
        };
      }

      @Override
      public Promise<Any> launch(RootDefinition definition) {
        return State.this.launch(definition);
      }

      public Consumer<Frame> park(
          Frame frame,
          SourceReference sourceReference,
          Name name,
          Consumer<? super Frame> consumer) {
        return new InflightWaitingOperation<>(
            this, sourceReference, String.format("Gather %s on %s", name, frame.id()), consumer);
      }

      @Override
      public Inflight<?> real() {
        return this;
      }

      @Override
      public synchronized void reschedule(Runnable runnable) {
        final var counter = COUNTER.get();
        if (counter.get() >= maxStackDepth) {
          executor.execute(runnable);
        } else {
          counter.incrementAndGet();
          try {
            runnable.run();
          } finally {
            counter.decrementAndGet();
          }
        }
      }

      @Override
      Runnable waitEdge(Future<?> callee) {
        final var counter = dependencies.computeIfAbsent(callee.real(), k -> new AtomicInteger());
        counter.incrementAndGet();
        return counter::decrementAndGet;
      }
    }

    private class InflightWaitingOperation<T> implements WaitingOperation, Consumer<T> {
      private final Consumer<? super T> consumer;
      private final String description;
      private final Inflight<?> future;
      private final SourceReference source;

      private InflightWaitingOperation(
          Inflight<?> future,
          SourceReference source,
          String description,
          Consumer<? super T> consumer) {
        this.future = future;
        this.source = source;
        this.description = description;
        this.consumer = consumer;
        waitingOperations.add(this);
      }

      @Override
      public void accept(T result) {
        waitingOperations.remove(this);
        consumer.accept(result);
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public SourceReference source() {
        return source;
      }
    }

    private class Tarjan {
      private final Supplier<? extends DeadlockCycleConsumer> cycleConsumer;
      private int index = 0;
      private final Deque<TarjanNode> stack = new ArrayDeque<>();
      final Map<Optional<? extends Future<?>>, TarjanNode> vertices =
          Stream.concat(
                  lookups
                      .stream()
                      .map(
                          lookup ->
                              new TarjanNode() {
                                @Override
                                void emit(DeadlockCycleConsumer consumer) {
                                  consumer.accept(lookup);
                                }

                                @Override
                                Inflight<?> future() {
                                  return lookup.future().real();
                                }
                              }),
                  waitingOperations
                      .stream()
                      .map(
                          waiting ->
                              new TarjanNode() {
                                @Override
                                void emit(DeadlockCycleConsumer consumer) {
                                  consumer.accept(waiting);
                                }

                                @Override
                                Inflight<?> future() {
                                  return waiting.future;
                                }
                              }))
              .collect(
                  Collectors.toMap(
                      node -> Optional.ofNullable(node.future()),
                      Function.identity(),
                      (a, b) ->
                          new TarjanNode() {
                            @Override
                            void emit(DeadlockCycleConsumer consumer) {
                              a.emit(consumer);
                              b.emit(consumer);
                            }

                            @Override
                            Inflight<?> future() {
                              return a.future();
                            }
                          }));

      private Tarjan(Supplier<? extends DeadlockCycleConsumer> cycleConsumer) {
        this.cycleConsumer = cycleConsumer;
      }

      public void run() {
        for (final var vertex : vertices.values()) {
          if (vertex.index == TarjanNode.UNDEFINED) {
            strongConnect(vertex);
          }
        }
      }

      private void strongConnect(TarjanNode vertex) {
        vertex.index = index;
        vertex.lowLink = index;
        index++;
        stack.push(vertex);
        vertex.onStack = true;
        successors(vertex, vertex.future());

        if (vertex.lowLink == vertex.index) {
          final var consumer = cycleConsumer.get();
          TarjanNode item;
          while ((item = stack.pop()) != vertex) {
            item.onStack = false;
            item.emit(consumer);
          }
          vertex.emit(consumer);
          vertex.onStack = false;
          consumer.finish();
        }
      }

      private void successors(TarjanNode vertex, Inflight<?> future) {
        for (final var entry : future.dependencies.entrySet()) {
          if (entry.getValue().get() > 0) {
            if (vertices.containsKey(Optional.ofNullable(entry.getKey()))) {
              final var other = vertices.get(Optional.ofNullable(entry.getKey()));
              if (other.index == TarjanNode.UNDEFINED) {
                strongConnect(other);
                vertex.lowLink = Math.min(vertex.lowLink, other.lowLink);
              } else if (other.onStack) {
                vertex.lowLink = Math.min(vertex.lowLink, other.index);
              }
            } else if (entry.getKey() != null) {
              successors(vertex, entry.getKey());
            }
          }
        }
      }
    }

    private final List<Pair<SourceReference, String>> errors = new ArrayList<>();
    private final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executor =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            0L,
            TimeUnit.MILLISECONDS,
            blockingQueue);
    /**
     * Lock to determine if execution is on going.
     *
     * <p>Readers in this case are number of actively executing futures and the writer is the thread
     * attempting to end the thread pool.
     */
    private final CountDownLatch lock = new CountDownLatch(1);

    private final AtomicInteger active = new AtomicInteger();

    private final List<Pair<Lookup, Optional<String>>> lookupErrors = new ArrayList<>();
    private final Set<Lookup> lookups = ConcurrentHashMap.newKeySet();
    private final PromiseHolder<Any> result;
    private final Set<InflightWaitingOperation<?>> waitingOperations =
        ConcurrentHashMap.newKeySet();

    public State(RootDefinition initial) {
      final var inflight = new Inflight<Any>();
      final var work = initial.launch(inflight);
      inflight.mode = inflight.mode.transition(Mode.RUN, lock, active);
      executor.execute(work);
      result = inflight.result;
    }

    public void finish(TaskResult resultHandler) {
      try {
        lock.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      executor.shutdown();
      try {
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!errors.isEmpty() || !lookupErrors.isEmpty()) {
          resultHandler.error(errors.stream(), lookupErrors.stream());
          return;
        }

        if (!lookups.isEmpty() || !waitingOperations.isEmpty()) {
          resultHandler.deadlocked(
              new DeadlockInformation() {
                @Override
                public void describeCycles(
                    Supplier<? extends DeadlockCycleConsumer> cycleConsumer) {
                  new Tarjan(cycleConsumer).run();
                }

                @Override
                public Stream<Lookup> lookups() {
                  return lookups.stream();
                }

                @Override
                public Stream<WaitingOperation> waitingOperations() {
                  return waitingOperations.stream().map(x -> x);
                }
              });
          return;
        }

        result.await(null, resultHandler::succeeded);

      } catch (final InterruptedException e) {
        resultHandler.failed(e);
      }
    }

    private Promise<Any> launch(RootDefinition task) {
      final var inflight = new Inflight<Any>();
      final var work = task.launch(inflight);
      inflight.mode = inflight.mode.transition(Mode.RUN, lock, active);
      executor.execute(work);
      return inflight.result;
    }

    private Promise<Any> loadExternal(String uri) {
      final var address = Result.of(uri).map(URI::new);
      final var initial =
          address
              .filter(x -> x.getScheme().equals("lib"))
              .map(URI::getSchemeSpecificPart)
              .filter(
                  x ->
                      x.length() == 0
                          || x.chars().anyMatch(c -> c != '/' && !Character.isLetterOrDigit(c)))
              .map(
                  x -> {
                    errors.add(
                        Pair.of(
                            SourceReference.root(uri),
                            String.format("“%s” is not a valid library name.", uri)));
                    return Promise.<Any>broken();
                  });

      return initial
          .reduce(
              handlers
                  .stream()
                  .map(
                      handler ->
                          () ->
                              address.flatMap(
                                  x ->
                                      handler.resolveUri(
                                          new UriExecutor() {
                                            @Override
                                            public void error(
                                                SourceReference sourceReference, String message) {
                                              errors.add(Pair.of(sourceReference, message));
                                            }

                                            @Override
                                            public Promise<Any> launch(RootDefinition definition) {
                                              return State.this.launch(definition);
                                            }
                                          },
                                          x))))
          .orElseGet(
              () -> {
                errors.add(
                    Pair.of(
                        SourceReference.root(uri),
                        String.format("Unable to resolve URI “%s”.", uri)));
                return Promise.broken();
              });
    }
  }

  abstract static class TarjanNode {
    public static final int UNDEFINED = -1;
    public int index = UNDEFINED;
    public int lowLink;
    public boolean onStack;

    abstract void emit(DeadlockCycleConsumer consumer);

    abstract State.Inflight<?> future();
  }
  /** Build identifier of the Flabbergast runtime system */
  public static final String BUILD;
  /** Compilation time for the Flabbergast runtime system */
  public static final Instant BUILD_TIME;

  private static final MethodHandle CONSUMER_ACCEPT;
  private static final ThreadLocal<AtomicInteger> COUNTER =
      ThreadLocal.withInitial(AtomicInteger::new);
  private static final MethodHandle FUTURE_BIND;
  private static final MethodHandle FUTURE_ERROR;
  private static final MethodHandle FUTURE_EXPORT;
  private static final ServiceLoader<UriService> URI_SERVICES =
      ServiceLoader.load(UriService.class);
  /** Version of the Flabbergast runtime system */
  public static final String VERSION;

  static {
    try (final var in = Scheduler.class.getResourceAsStream("flabbergast.properties")) {
      final var prop = new Properties();
      prop.load(in);
      VERSION = prop.getProperty("version");
      BUILD =
          prop.getProperty("githash")
              + (Boolean.parseBoolean(prop.getProperty("gitdirty")) ? "-dirty" : "");
      BUILD_TIME =
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
              .parse(prop.getProperty("buildtime"))
              .toInstant();
      final var lookup = MethodHandles.lookup();
      CONSUMER_ACCEPT =
          lookup.findVirtual(
              Consumer.class, "accept", MethodType.methodType(void.class, Object.class));
      FUTURE_BIND =
          lookup.findVirtual(
              Future.class,
              "bind",
              MethodType.methodType(MethodHandle.class, FunctionDescriptor.class));
      FUTURE_ERROR =
          lookup.findVirtual(
              Future.class,
              "error",
              MethodType.methodType(void.class, SourceReference.class, String.class));
      FUTURE_EXPORT =
          lookup.findVirtual(
              Future.class,
              "export",
              MethodType.methodType(void.class, String.class, Class.class, MethodHandle.class));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Bootstrap method to export KWS functions into code. */
  public static CallSite binder(
      MethodHandles.Lookup lookup,
      String name,
      MethodType type,
      Class<?> returnType,
      MethodHandle target) {
    return new ConstantCallSite(
        MethodHandles.insertArguments(FUTURE_EXPORT, 1, name, returnType, target));
  }

  /** Bootstrap method to import KWS functions into code. */
  public static CallSite bootstrap(
      MethodHandles.Lookup lookup, String name, MethodType type, Class<?> returnType) {
    final var descriptor = new FunctionDescriptor(name, type, returnType);
    return new ConstantCallSite(
        MethodHandles.foldArguments(
            MethodHandles.invoker(type),
            MethodHandles.insertArguments(FUTURE_BIND, 1, descriptor)));
  }

  /** Begin configuration of a new task master */
  public static Builder builder() {
    return new Builder();
  }

  /** Allow binding Flabbergast promises, frames, and templates to dynalink consumers */
  public static GuardingDynamicLinkerExporter provider() {
    return new GuardingDynamicLinkerExporter() {
      @Override
      public List<GuardingDynamicLinker> get() {
        return List.of(new FrameGettingLinker(), new TemplateInvokerLinker());
      }
    };
  }

  private final Map<FunctionDescriptor, CallSite> callsites = new ConcurrentHashMap<>();
  private final Debugger debugger;
  private final Map<String, Promise<Any>> externalCache = new ConcurrentHashMap<>();
  private final List<UriHandler> handlers;
  private final int maxStackDepth;

  private Scheduler(
      Stream<UriHandler> handlers,
      Stream<KwsBinding> bindings,
      Debugger debugger,
      int maxStackDepth) {
    this.handlers =
        handlers.sorted(Comparator.comparingInt(UriHandler::priority)).collect(Collectors.toList());
    this.debugger = debugger;
    this.maxStackDepth = maxStackDepth;
    bindings.forEach(b -> bind(b.name(), b.returnType(), b.handle()));
  }

  private void bind(String name, Class<?> returnType, MethodHandle handle) {
    if (handle.type().parameterCount() < 3
        || !handle.type().returnType().equals(void.class)
        || !handle.type().parameterType(0).equals(Future.class)
        || !handle.type().parameterType(1).equals(SourceReference.class)
        || !handle.type().parameterType(2).equals(Consumer.class)) {

      throw new IllegalArgumentException(
          String.format(
              "Method type %s must return void, take Future, SourceReference, and Consumer<%s> as the first parameters",
              handle.type(), returnType));
    }
    callsites
        .computeIfAbsent(
            new FunctionDescriptor(name, handle.type(), returnType),
            FunctionDescriptor::createException)
        .setTarget(handle);
  }

  /**
   * Perform computations until the Flabbergast program is complete or deadlocked.
   *
   * <p>A task master creates an environment that defines the scope of all imports with in a
   * Flabbergast program. It also serves as the scope in which unique frame identifiers (<tt>Id</tt>
   * and <tt>GenerateId</tt>) are created. Frames generated inside other task masters should not be
   * returned or unique identifiers can not be guaranteed.
   *
   * <p>This method may be invoke multiple times and will stop when all tasks are complete or the
   * system reaches a state of unrecoverable error or deadlock. Success and failure are not mutually
   * exclusive in this context; the initial task may successfully return a result, but errors or
   * deadlock may still be raised due to problems in other parts of the program.
   *
   * <p>If a debugger is provided, this method will not exit until debugged tasks have been
   * finished, either by returning a value or raising an error.
   *
   * <p>This method is thread-safe, so multiple tasks maybe executed in the same task master
   * simultaneously. However, errors for imports shared between simultaneously executing tasks will
   * be randomly assigned to one {@link TaskResult}.
   */
  public void run(RootDefinition initial, TaskResult resultHandler) {
    final var state = new State(initial);
    state.finish(resultHandler);
  }
}
