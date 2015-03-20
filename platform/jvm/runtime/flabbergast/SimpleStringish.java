package flabbergast;

import java.util.Iterator;
import java.util.Stack;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String ramblingNext(Stack<GetNext<String>> stack) {
		return str;
	}
}