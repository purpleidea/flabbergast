package flabbergast;

import flabbergast.RamblingIterator.GetNext;
import java.util.Stack;

public class ConcatStringish extends Stringish {
  private long chars;
  private Stringish head;
  private Stringish tail;

  public ConcatStringish(Stringish head, Stringish tail) {
    this.head = head;
    this.tail = tail;
    this.chars = head.getLength() + tail.getLength();
  }

  @Override
  int getCount() {
    return head.getCount() + tail.getCount();
  }

  @Override
  public long getLength() {
    return chars;
  }

  @Override
  public long getUtf16Length() {
    return head.getUtf16Length() + tail.getUtf16Length();
  }

  @Override
  public long getUtf8Length() {
    return head.getUtf8Length() + tail.getUtf8Length();
  }

  @Override
  public String ramblingNext(Stack<GetNext<String>> stack) {
    stack.push(tail);
    return head.ramblingNext(stack);
  }
}
