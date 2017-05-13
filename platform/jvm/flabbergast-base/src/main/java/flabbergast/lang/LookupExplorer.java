package flabbergast.lang;

import flabbergast.lang.Context.FrameAccessor;
import flabbergast.util.LongBiPredicate;
import flabbergast.util.Pair;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An algorithm to explore a “column” in a lookup operation
 *
 * <p>Each column will instantiate a fresh explorer, so explorers can be stateful. Some
 * implementations are stateless, so the instance may be reused.
 *
 * @see Lookup
 */
public interface LookupExplorer {

  /**
   * Perform a “simple” exploration where the name provided must be in the frame provided. A column
   * is discarded if there is no matching element.
   */
  LookupOperation<LookupExplorer> EXACT =
      LookupOperation.of(
          "exact",
          new LookupExplorer() {
            @Override
            public LookupExplorer duplicate() {
              return this;
            }

            @Override
            public void process(
                Name targetName,
                FrameAccessor frame,
                long seen,
                long remaining,
                LookupForkOperation next) {
              frame
                  .get(targetName)
                  .ifPresentOrElse(promise -> next.await(promise, next::finish), next::next);
            }
          });
  /**
   * Perform an exploration similar to “simple” but when matching numeric attributes, match if all
   * bits set in the query are found in the attribute name.
   */
  LookupOperation<LookupExplorer> FLAG_CONTAINED =
      numeric(
          "query contained bits in attribute",
          (needle, haystack) -> (needle & haystack) != haystack);
  /**
   * Perform an exploration similar to “simple” but when matching numeric attributes, match if all
   * bits set in the attribute name are found in the query.
   */
  LookupOperation<LookupExplorer> FLAG_CONTAINS =
      numeric(
          "query contains bits in attribute", (needle, haystack) -> (needle & haystack) != needle);
  /**
   * Perform an exploration similar to “simple” but when matching numeric attributes, match if query
   * can be evenly divided by the attribute name.
   */
  LookupOperation<LookupExplorer> FLAG_DIVIDED =
      numeric("query divided by attribute", (needle, haystack) -> (needle % haystack) == 0);
  /**
   * Perform an exploration similar to “simple” but when matching numeric attributes, match if
   * attribute name can be evenly divided by the query.
   */
  LookupOperation<LookupExplorer> FLAG_DIVIDES =
      numeric("query divides attribute", (needle, haystack) -> (haystack % needle) == 0);
  /**
   * Perform an exploration similar to “simple” but when matching numeric attributes, match if any
   * bits set in the query are set in the attribute name.
   */
  LookupOperation<LookupExplorer> FLAG_INTERSECTS =
      numeric(
          "query and attribute have bit-wise intersection",
          (needle, haystack) -> (needle & haystack) != 0);
  /**
   * Perform an exploration similar to “simple” but if a <tt>Null</tt> value is encountered, skip to
   * the next column.
   */
  LookupOperation<LookupExplorer> NULL_COALESCING =
      LookupOperation.of(
          "null coalescing",
          new LookupExplorer() {
            @Override
            public LookupExplorer duplicate() {
              return this;
            }

            @Override
            public void process(
                Name targetName,
                FrameAccessor frame,
                long seen,
                long remaining,
                LookupForkOperation next) {
              frame
                  .get(targetName)
                  .ifPresentOrElse(
                      promise ->
                          next.await(
                              promise,
                              result -> {
                                if (result == Any.NULL) {
                                  next.next();
                                } else {
                                  next.finish(result);
                                }
                              }),
                      next::next);
            }
          });

  /**
   * Perform an exploration and then invoke a function-like template, which must return
   * <tt>Bool</tt> and then keep this value if true or skip to the next column if false.
   */
  static LookupOperation<LookupExplorer> filter(
      Template filter, LookupOperation<LookupExplorer> inner) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " filtered using template";
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var innerExplorer = inner.start(future, sourceReference, context);
        return new FilterLookupExplorer(innerExplorer, future, sourceReference, filter);
      }
    };
  }

  /**
   * Perform an exploration where in each frame, the attributes are scored based on Levenshtein
   * distance (for strings) or difference (if numeric) and pick the best match.
   */
  static LookupOperation<LookupExplorer> fuzzy(long cutoff) {
    return LookupOperation.of(
        String.format("fuzzy with cut-off %d", cutoff),
        new LookupExplorer() {
          @Override
          public LookupExplorer duplicate() {
            return this;
          }

          @Override
          public void process(
              Name targetName,
              FrameAccessor frame,
              long seen,
              long remaining,
              LookupForkOperation next) {
            next.forkPromises(
                frame
                    .names()
                    .map(
                        n ->
                            Pair.of(
                                n,
                                n.apply(
                                    targetName,
                                    new NameBiFunction<Long>() {
                                      @Override
                                      public Long apply(long left, long right) {
                                        return Math.abs(left - right);
                                      }

                                      @Override
                                      public Long apply(long left, String right) {
                                        return Long.MAX_VALUE;
                                      }

                                      @Override
                                      public Long apply(String left, long right) {
                                        return Long.MAX_VALUE;
                                      }

                                      @Override
                                      public Long apply(String left, String right) {
                                        final var dp =
                                            new int[left.length() + 1][right.length() + 1];

                                        for (var i = 0; i <= left.length(); i++) {
                                          for (var j = 0; j <= right.length(); j++) {
                                            if (i == 0) {
                                              dp[i][j] = j;
                                            } else if (j == 0) {
                                              dp[i][j] = i;
                                            } else {
                                              dp[i][j] =
                                                  IntStream.of(
                                                          dp[i - 1][j - 1]
                                                              + (left.charAt(i - 1)
                                                                      == right.charAt(j - 1)
                                                                  ? 0
                                                                  : 1),
                                                          dp[i - 1][j] + 1,
                                                          dp[i][j - 1] + 1)
                                                      .min()
                                                      .getAsInt();
                                            }
                                          }
                                        }

                                        return (long) dp[left.length()][right.length()];
                                      }
                                    })))
                    .sorted(Comparator.comparingLong(Pair::second))
                    .filter(score -> score.second() < cutoff)
                    .flatMap(name -> frame.get(name.first()).stream()));
          }
        });
  }

  /**
   * Perform another exploration, transforming each intermediate value using the function-like
   * template provided
   *
   * @param mapper the transformation function
   * @param inner the driving exploration
   */
  static LookupOperation<LookupExplorer> map(
      Template mapper, LookupOperation<LookupExplorer> inner) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return inner.description() + " mapping output values using template";
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var innerExplorer = inner.start(future, sourceReference, context);
        return new MapLookupExplorer(innerExplorer, future, sourceReference, mapper);
      }
    };
  }

  /**
   * Perform a special comparison of numeric fields
   *
   * @param description a description of the comparison
   * @param predicate the comparison operation
   */
  static LookupOperation<LookupExplorer> numeric(String description, LongBiPredicate predicate) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return description;
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        return new LookupExplorer() {
          @Override
          public LookupExplorer duplicate() {
            return this;
          }

          @Override
          public void process(
              Name targetName,
              FrameAccessor frame,
              long seen,
              long remaining,
              LookupForkOperation next) {
            next.forkPromises(
                frame
                    .names()
                    .filter(
                        haystack ->
                            targetName.apply(
                                haystack,
                                new NameBiFunction<>() {
                                  @Override
                                  public Boolean apply(long left, long right) {
                                    return predicate.test(left, right);
                                  }

                                  @Override
                                  public Boolean apply(long left, String right) {
                                    return false;
                                  }

                                  @Override
                                  public Boolean apply(String left, long right) {
                                    return false;
                                  }

                                  @Override
                                  public Boolean apply(String left, String right) {
                                    return left.equals(right);
                                  }
                                }))
                    .map(frame::get)
                    .flatMap(Optional::stream));
          }
        };
      }
    };
  }

  /**
   * Combine two explorations where the first handles a specific number of names and the second
   * handles the remainder
   *
   * <p>If the number is greater than or equal to the number of names in the lookup, only the first
   * will be used
   *
   * @param count the number of names to allocate to the first
   * @param first the explorer for the first names
   * @param second the explorer for the remaining names
   */
  static LookupOperation<LookupExplorer> takeFirst(
      long count, LookupOperation<LookupExplorer> first, LookupOperation<LookupExplorer> second) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return String.format(
            "(%s for first %d then %s)", first.description(), count, second.description());
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var firstExplorer = first.start(future, sourceReference, context);
        final var secondExplorer = second.start(future, sourceReference, context);
        return new TakeFirstLookupExplorer(count, firstExplorer, secondExplorer);
      }
    };
  }

  /**
   * Combine two explorations where the first handles an unspecified number of names and the second
   * handles the specified number
   *
   * <p>If the number is greater than or equal to the number of names in the lookup, only the second
   * will be used
   *
   * @param count the number of names to allocate to the second
   * @param first the explorer for the first names
   * @param second the explorer for the last names
   */
  static LookupOperation<LookupExplorer> takeLast(
      long count, LookupOperation<LookupExplorer> first, LookupOperation<LookupExplorer> second) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return String.format(
            "(%s until last %d then %s)", first.description(), count, second.description());
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var firstExplorer = first.start(future, sourceReference, context);
        final var secondExplorer = second.start(future, sourceReference, context);
        return new TakeLastLookupExplorer(count, firstExplorer, secondExplorer);
      }
    };
  }

  /**
   * Combine two explorations where the first handles names until some condition is met
   *
   * <p>If the number is greater than or equal to the number of names in the lookup, only the second
   * will be used
   *
   * @param predicate a test that switches from the first to the second
   * @param first the explorer for the first names
   * @param second the explorer for the second names
   */
  static LookupOperation<LookupExplorer> takeUntil(
      Predicate<Name> predicate,
      LookupOperation<LookupExplorer> first,
      LookupOperation<LookupExplorer> second) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return String.format(
            "(%s until %s then %s)", first.description(), predicate, second.description());
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        return new TakeUntilLookupExplorer(
            first, future, sourceReference, context, second, predicate);
      }
    };
  }

  /**
   * Combine two explorations where the first handles names if some condition is met; otherwise the
   * second
   *
   * @param predicate a test that switches from the first to the second
   * @param ifTrue the explorer for the names where the predicate succeeds
   * @param ifFalse the explorer for the names where the predicate fails
   */
  static LookupOperation<LookupExplorer> where(
      Predicate<Name> predicate,
      LookupOperation<LookupExplorer> ifTrue,
      LookupOperation<LookupExplorer> ifFalse) {
    return new LookupOperation<>() {
      @Override
      public String description() {
        return String.format(
            "(if %s then %s else %s)", predicate, ifTrue.description(), ifFalse.description());
      }

      @Override
      public LookupExplorer start(
          Future<?> future, SourceReference sourceReference, Context context) {
        final var ifTrueExplorer = ifTrue.start(future, sourceReference, context);
        final var ifFalseExplorer = ifFalse.start(future, sourceReference, context);
        return new LookupExplorer() {
          @Override
          public LookupExplorer duplicate() {
            return this;
          }

          @Override
          public void process(
              Name targetName,
              FrameAccessor frame,
              long seen,
              long remaining,
              LookupForkOperation next) {
            (predicate.test(targetName) ? ifTrueExplorer : ifFalseExplorer)
                .process(targetName, frame, seen, remaining, next);
          }
        };
      }
    };
  }

  /**
   * Create a duplicate of this lookup explorer
   *
   * <p>If a clone operation is performed, this will be used to create duplicate explorers for
   * subsequent lookups. Any state must be duplicated so that each explorer is isolated from its
   * duplicates.
   */
  LookupExplorer duplicate();

  /**
   * Process the next name in the column
   *
   * @param targetName the name being looked up (i.e., the name being queried)
   * @param frame access to the frame in which the name should appear
   * @param seen the number of names previously found in this column
   * @param remaining the number of names yet to be looked up in this column
   * @param next a callback to handle the results of this step; exactly one of {@link
   *     LookupNextOperation#next()}, {@link LookupNextOperation#finish(Any)}, {@link
   *     LookupForkOperation#fork(Stream)}, {@link LookupLastOperation#fail()} or {@link
   *     Future#error(SourceReference, String)} must be
   */
  void process(
      Name targetName, FrameAccessor frame, long seen, long remaining, LookupForkOperation next);
}
