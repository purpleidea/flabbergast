package flabbergast;

import java.util.Iterator;

public class ConcatIterator<T> implements Iterator<T> {
    public Iterator<T> first;
    public Iterator<T> second;

    public ConcatIterator(Iterator<T> first, Iterator<T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean hasNext() {
        return first.hasNext() || second.hasNext();
    }

    @Override
    public T next() {
        if (first.hasNext()) {
            return first.next();
        }
        return second.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
