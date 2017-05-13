package flabbergast.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Process the results of each {@link LookupExplorer} and produce a final value that is the result
 * of the lookup operation
 *
 * <p>Each lookup will instantiate a fresh selector, so selector can be stateful. Some
 * implementations are stateless, so the instance may be reused.
 *
 * @see Lookup
 */
public interface LookupSelector {

  /** Select all the values from any successful column and produce a list */
  LookupOperation<LookupSelector> ALL =
      new LookupOperation<>() {
        @Override
        public String description() {
          return "all";
        }

        @Override
        public LookupSelector start(
            Future<?> future, SourceReference sourceReference, Context context) {
          return new LookupSelector() {
            private final List<Any> items = new ArrayList<>();

            @Override
            public void accept(Any value, LookupNextOperation next) {
              items.add(value);
              next.next();
            }

            @Override
            public void empty(LookupLastOperation next) {
              next.finish(
                  Any.of(
                      Frame.create(
                          future,
                          sourceReference,
                          context,
                          AttributeSource.listOfAny(items.stream()))));
            }
          };
        }
      };
  /** Produce a Boolean value as to whether lookup found a value */
  LookupOperation<LookupSelector> EXISTS =
      LookupOperation.of(
          "exists",
          new LookupSelector() {
            @Override
            public void accept(Any value, LookupNextOperation next) {
              next.finish(Any.of(true));
            }

            @Override
            public void empty(LookupLastOperation next) {
              next.finish(Any.of(false));
            }
          });
  /** Return the first value found by an explorer or an error if no values are found */
  LookupOperation<LookupSelector> FIRST =
      LookupOperation.of(
          "first",
          new LookupSelector() {
            @Override
            public void accept(Any value, LookupNextOperation next) {
              next.finish(value);
            }

            @Override
            public void empty(LookupLastOperation next) {
              next.fail();
            }
          });
  /**
   * Collect all values, which must be frame, and composite them as if the first one was overriding
   * the second and so on
   */
  LookupOperation<LookupSelector> MERGING =
      new LookupOperation<>() {
        @Override
        public String description() {
          return "merging";
        }

        @Override
        public LookupSelector start(
            Future<?> future, SourceReference sourceReference, Context context) {
          return new LookupSelector() {
            private final Map<Name, Promise<Any>> composite = new TreeMap<>();

            @Override
            public void accept(Any value, LookupNextOperation next) {
              value.accept(
                  new WhinyAnyConsumer() {
                    @Override
                    public void accept(Frame frame) {
                      context
                          .accessor(frame)
                          .names()
                          .filter(name -> !composite.containsKey(name))
                          .forEach(
                              name ->
                                  frame.get(name).ifPresent(value -> composite.put(name, value)));
                      next.next();
                    }

                    @Override
                    protected void fail(String type) {
                      next.fail();
                    }
                  });
            }

            @Override
            public void empty(LookupLastOperation next) {
              next.finish(
                  Any.of(
                      Frame.create(
                          future,
                          sourceReference,
                          context,
                          AttributeSource.fromMapOfAny(composite))));
            }
          };
        }
      };

  /**
   * Perform another selection operation and then only emit the output if a function-like template
   * allows it
   */
  static LookupOperation<LookupSelector> filter(
      Template filter, LookupOperation<LookupSelector> inner) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " then filtering by template";
      }

      @Override
      public LookupSelector start(
          Future<?> future, SourceReference sourceReference, Context context) {
        return new LookupSelector() {
          final LookupSelector innerCollector = inner.start(future, sourceReference, context);

          private Optional<Promise<Any>> call(
              Future<?> future, SourceReference sourceReference, Any argument) {
            return Frame.create(
                    future,
                    sourceReference.specialJunction(
                        "instantiate template inside lookup handler", filter.source()),
                    Context.EMPTY,
                    false,
                    Stream.empty(),
                    AttributeSource.of(Attribute.of("args", argument)),
                    filter)
                .get(Name.of("value"));
          }

          @Override
          public void accept(Any value, LookupNextOperation next) {
            innerCollector.accept(
                value,
                new LookupNextOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    next.fail();
                  }

                  @Override
                  public void finish(Any result) {
                    call(future, sourceReference, value)
                        .ifPresentOrElse(
                            promise ->
                                next.await(
                                    promise,
                                    v ->
                                        v.accept(
                                            new WhinyAnyConsumer() {
                                              @Override
                                              public void accept(boolean condition) {
                                                if (condition) {
                                                  next.finish(result);
                                                } else {
                                                  next.next();
                                                }
                                              }

                                              @Override
                                              protected void fail(String type) {
                                                next.fail();
                                              }
                                            })),
                            next::fail);
                  }

                  @Override
                  public void next() {
                    next.next();
                  }
                });
          }

          @Override
          public void empty(LookupLastOperation next) {
            innerCollector.empty(
                new LookupLastOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    next.fail();
                  }

                  @Override
                  public void finish(Any value) {
                    call(future, sourceReference, value)
                        .ifPresentOrElse(
                            promise ->
                                next.await(
                                    promise,
                                    v ->
                                        v.accept(
                                            new WhinyAnyConsumer() {
                                              @Override
                                              public void accept(boolean condition) {
                                                if (condition) {
                                                  next.finish(value);
                                                } else {
                                                  next.finish(null);
                                                }
                                              }

                                              @Override
                                              protected void fail(String type) {
                                                next.fail();
                                              }
                                            })),
                            next::fail);
                  }
                });
          }
        };
      }
    };
  }

  /**
   * Processes the selected values through a fricassée operation
   *
   * @param name the name to bind the value to in the operation
   * @param context the context in which the fricassée should be evaluated
   * @param definition the definition that will resolve the lookup-driven fricassée chain to a value
   */
  static LookupOperation<LookupSelector> fricassee(
      Name name, Context context, CollectorDefinition definition) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return "fricassée selector";
      }

      @Override
      public LookupSelector start(
          Future<?> future, SourceReference sourceReference, Context lookupContext) {
        return new FricasseeLookupSelector(
            future, sourceReference, lookupContext, context, name, definition);
      }
    };
  }

  /**
   * Perform another slelection operation and then transform the output with a function-like
   * template
   */
  static LookupOperation<LookupSelector> map(
      Template mapper, LookupOperation<LookupSelector> inner) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " then calling template";
      }

      @Override
      public LookupSelector start(
          Future<?> future, SourceReference sourceReference, Context context) {
        return new LookupSelector() {
          private final LookupSelector innerCollector =
              inner.start(future, sourceReference, context);

          private Optional<Promise<Any>> call(
              Future<?> future, SourceReference sourceReference, Any argument) {
            return Frame.create(
                    future,
                    sourceReference.specialJunction(
                        "instantiate template inside lookup handler", mapper.source()),
                    Context.EMPTY,
                    false,
                    Stream.empty(),
                    AttributeSource.of(Attribute.of("args", argument)),
                    mapper)
                .get(Name.of("value"));
          }

          @Override
          public void accept(Any value, LookupNextOperation next) {
            innerCollector.accept(
                value,
                new LookupNextOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    next.fail();
                  }

                  @Override
                  public void finish(Any result) {
                    call(future, sourceReference, value)
                        .ifPresentOrElse(promise -> next.await(promise, next::finish), next::fail);
                  }

                  @Override
                  public void next() {
                    next.next();
                  }
                });
          }

          @Override
          public void empty(LookupLastOperation next) {
            innerCollector.empty(
                new LookupLastOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    next.fail();
                  }

                  @Override
                  public void finish(Any value) {
                    call(future, sourceReference, value)
                        .ifPresentOrElse(promise -> next.await(promise, next::finish), next::fail);
                  }
                });
          }
        };
      }
    };
  }

  /**
   * Modifies a selector to substitute a default value if the lookup produces no output
   *
   * @param inner the selector to use
   * @param value the default value
   */
  static LookupOperation<LookupSelector> orElse(LookupOperation<LookupSelector> inner, Any value) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " or default value";
      }

      @Override
      public LookupSelector start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var innerCollector = inner.start(future, sourceReference, context);
        return new LookupSelector() {
          @Override
          public void accept(Any value, LookupNextOperation next) {
            innerCollector.accept(value, next);
          }

          @Override
          public void empty(LookupLastOperation next) {
            innerCollector.empty(
                new LookupLastOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    next.finish(value);
                  }

                  @Override
                  public void finish(Any result) {
                    next.finish(result);
                  }
                });
          }
        };
      }
    };
  }

  /**
   * Modifies a selector to substitute a default value by instantiating a template if the lookup
   * produces no output
   *
   * @param inner the selector to use
   * @param template the template to produce the default value
   */
  static LookupOperation<LookupSelector> orElseCompute(
      LookupOperation<LookupSelector> inner, Template template) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " or default value from template";
      }

      @Override
      public LookupSelector start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var innerCollector = inner.start(future, sourceReference, context);
        return new LookupSelector() {
          @Override
          public void accept(Any value, LookupNextOperation next) {
            innerCollector.accept(value, next);
          }

          @Override
          public void empty(LookupLastOperation next) {
            innerCollector.empty(
                new LookupLastOperation() {
                  @Override
                  public void await(Promise<Any> promise, Consumer<Any> consumer) {
                    next.await(promise, consumer);
                  }

                  @Override
                  public void fail() {
                    Frame.create(
                            future,
                            sourceReference.specialJunction(
                                "instantiate template inside lookup selector", template.source()),
                            context,
                            template)
                        .get(Name.of("value"))
                        .ifPresentOrElse(promise -> next.await(promise, next::finish), next::fail);
                  }

                  @Override
                  public void finish(Any result) {
                    next.finish(result);
                  }
                });
          }
        };
      }
    };
  }

  /**
   * Process the value emitted at the end of an exploration (column)
   *
   * @param value the value found by the explorer
   * @param next a callback to handle the results of this step; exactly one of {@link
   *     LookupNextOperation#next()}, {@link LookupNextOperation#finish(Any)}, {@link
   *     LookupLastOperation#fail()} or {@link Future#error(SourceReference, String)} must be
   */
  void accept(Any value, LookupNextOperation next);

  /**
   * Determine what should happen when there are no more columns to examine
   *
   * <p>Some welectors want to see the results of all the explorations; in which case, they should
   * save values and then emit a final answer when this method is called.
   *
   * @param next a callback to handle the results of the lookup; exactly one of {@link
   *     LookupLastOperation#finish(Any)}, {@link LookupLastOperation#fail()} or {@link
   *     Future#error(SourceReference, String)} must be called
   */
  void empty(LookupLastOperation next);
}
