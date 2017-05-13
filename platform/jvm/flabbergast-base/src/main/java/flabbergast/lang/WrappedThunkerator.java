package flabbergast.lang;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class WrappedThunkerator extends Thunkerator {
  private enum State {
    UNINITIALIZED,
    ENTERED,
    FINISHED
  }

  private volatile State state = State.UNINITIALIZED;
  private final Deque<ThunkeratorConsumer> consumers = new ConcurrentLinkedDeque<>();
  private Consumer<ThunkeratorConsumer> next;
  private final Producer producer;

  WrappedThunkerator(Producer producer) {
    this.producer = producer;
  }

  @Override
  public void iterator(Future<?> future, ThunkeratorConsumer thunkerator) {
    State exitState;
    synchronized (this) {
      exitState = state;
      switch (state) {
        case UNINITIALIZED:
          state = State.ENTERED;
          consumers.offer(thunkerator);
          break;
        case ENTERED:
          consumers.offer(thunkerator);
          break;
        case FINISHED:
          break;
      }
    }
    switch (exitState) {
      case UNINITIALIZED:
        producer.produce(
            future,
            () -> {
              synchronized (this) {
                state = State.FINISHED;
                next = ThunkeratorConsumer::end;
              }
              drain();
            },
            (sourceReference, context) -> {
              synchronized (this) {
                state = State.FINISHED;
                final var continuation = new WrappedThunkerator(producer);
                next = c -> c.next(sourceReference, context, continuation);
              }
              drain();
            });
        break;
      case ENTERED:
        break;
      case FINISHED:
        next.accept(thunkerator);
        break;
    }
  }

  private void drain() {
    ThunkeratorConsumer consumer;
    while ((consumer = consumers.poll()) != null) {
      next.accept(consumer);
    }
  }

  interface Producer {
    void produce(Future<?> future, Runnable end, BiConsumer<SourceReference, Context> next);
  }
}
