package flabbergast;

import java.util.Iterator;
import java.util.Stack;

public final class RamblingIterator<T> implements Iterator<T> {

  public interface GetNext<T> {
    public T ramblingNext(Stack<GetNext<T>> stack);
  }

  private Stack<GetNext<T>> stack = new Stack<GetNext<T>>();

  public RamblingIterator(GetNext<T> first) {
    stack.push(first);
  }

  @Override
  public boolean hasNext() {
    return !stack.isEmpty();
  }

  @Override
  public T next() {
    return stack.pop().ramblingNext(stack);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
