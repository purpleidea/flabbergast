package flabbergast;

import java.util.Iterator;
import java.util.Stack;

import javax.management.openmbean.InvalidOpenTypeException;

import flabbergast.RamblingIterator.GetNext;

public class SimpleStringish extends Stringish {
	private String str;

	public SimpleStringish(String str) {
		this.str = str;
	}

	@Override
	int getCount() {
		return 1;
	}

	@Override
	public long getLength() {
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
				throw new InvalidOpenTypeException();
			}
		};
	}

	@Override
	public String ramblingNext(Stack<GetNext<String>> stack) {
		return str;
	}
}