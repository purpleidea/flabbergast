package flabbergast;

import java.io.UnsupportedEncodingException;

import java.util.Iterator;
import java.util.Stack;

import flabbergast.RamblingIterator.GetNext;

public class SimpleStringish extends Stringish {
    private String str;
    private long num_codepoints;

    public SimpleStringish(String str) {
        this.str = str;
        num_codepoints = str.codePointCount(0, str.length());
    }

    @Override
    int getCount() {
        return 1;
    }

    @Override
    public long getLength() {
        return num_codepoints;
    }

    @Override
    public long getUtf8Length() {
        try {
            return str.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            return -1;
        }
    }

    @Override
    public long getUtf16Length() {
        return str.length();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            boolean state = true;

            @Override
            public boolean hasNext() {
                return state;
            }

            @Override
            public String next() {
                state = false;
                return str;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String ramblingNext(Stack<GetNext<String>> stack) {
        return str;
    }
}
