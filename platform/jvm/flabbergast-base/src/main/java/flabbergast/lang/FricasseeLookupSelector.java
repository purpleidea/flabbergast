package flabbergast.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

final class FricasseeLookupSelector implements LookupSelector, Consumer<Any> {
  private class LookupThunkerator extends Thunkerator {

    private final LookupNextOperation nextLookup;
    private Thunkerator nextThunkerator;
    private Context result;
    private final SourceReference sourceReference;
    private final Deque<ThunkeratorConsumer> queueConsumers = new ArrayDeque<>();

    public LookupThunkerator(LookupNextOperation nextLookup, SourceReference sourceReference) {
      this.nextLookup = nextLookup;
      this.sourceReference = sourceReference;
    }

    public synchronized void accept(Future<?> future, Any value, LookupNextOperation next) {
      nextThunkerator = consumer = new LookupThunkerator(next, sourceReference);
      result =
          Frame.create(
                  future,
                  sourceReference,
                  callingContext,
                  AttributeSource.of(Attribute.of(name, value)))
              .context();
      drainQueue();
    }

    private void drainQueue() {
      ThunkeratorConsumer queuedConsumer;
      while ((queuedConsumer = queueConsumers.poll()) != null) {
        queuedConsumer.next(sourceReference, result, nextThunkerator);
      }
    }

    public synchronized void finish() {
      nextThunkerator =
          new Thunkerator() {
            @Override
            void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
              thunkerator.end();
            }
          };
      drainQueue();
    }

    private boolean started;

    @Override
    synchronized void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
      if (nextThunkerator == null) {
        queueConsumers.offer(thunkerator);
        if (started) {
          nextLookup.next();
          started = false;
        }
      } else {
        thunkerator.next(sourceReference, result, nextThunkerator);
      }
    }
  }

  private LookupThunkerator consumer;
  private final Context context;
  private final CollectorDefinition definition;
  private final Future<?> future;
  private final Name name;
  private final Context callingContext;
  private Consumer<Any> output;
  private final SourceReference sourceReference;

  public FricasseeLookupSelector(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Context callingContext,
      Name name,
      CollectorDefinition definition) {
    this.future = future;
    this.sourceReference = sourceReference;
    this.context = context;
    this.definition = definition;
    this.name = name;
    this.callingContext = callingContext;
  }

  @Override
  public void accept(Any value) {
    output.accept(value);
  }

  private void launch(
      Future<?> future,
      SourceReference sourceReference,
      Context context,
      Consumer<ThunkeratorConsumer> handler) {
    future.launch(
        definition,
        sourceReference,
        context,
        new Fricassee() {
          @Override
          Context context() {
            return callingContext;
          }

          @Override
          void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
            handler.accept(thunkerator);
          }
        },
        this);
  }

  @Override
  public synchronized void accept(Any value, LookupNextOperation next) {
    output = next::finish;
    if (consumer == null) {
      consumer = new LookupThunkerator(next, sourceReference);
      final var innerContext =
          Frame.create(
                  future,
                  sourceReference,
                  callingContext,
                  AttributeSource.of(Attribute.of(name, value)))
              .context();
      launch(
          future, sourceReference, context, tc -> tc.next(sourceReference, innerContext, consumer));
    } else {
      consumer.accept(future, value, next);
    }
  }

  @Override
  public synchronized void empty(LookupLastOperation next) {
    output = next::finish;
    if (consumer == null) {
      launch(future, sourceReference, context, ThunkeratorConsumer::end);
    } else {
      consumer.finish();
    }
  }
}
