package flabbergast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class OrderByIterator<T extends Comparable<T>> {
	private String current;
	private final SortedMap<T, List<String>> dispatchers = new TreeMap<T, List<String>>();
	private Iterator<String> setup_iterator;
	private Iterator<List<String>> iterator;
	private Iterator<String> inner_iterator;
	private long position;

	public OrderByIterator(Iterable<String>[] inputs) {
		TreeSet<String> set = new TreeSet<String>();
		for (Iterable<String> input : inputs) {
			for (String key : input) {
				set.add(key);
			}
		}
		setup_iterator = set.iterator();
	}

	/**
	 * The current attribute name.
	 */
	public String getCurrent() {
		return current;
	}

	public Stringish getCurrentish() {
		return new SimpleStringish(current);
	}

	/**
	 * The current attribute ordinal, 1-based per the language spec.
	 */
	public long getPosition() {
		return position;
	}

	public boolean next() {
		if (iterator == null) {
			iterator = dispatchers.values().iterator();
		}
		do {
			if (inner_iterator == null) {
				if (!iterator.hasNext()) {
					return false;
				}
				inner_iterator = iterator.next().iterator();
			}
			if (inner_iterator.hasNext()) {
				current = inner_iterator.next();
				position++;
				return true;
			}
			inner_iterator = null;
		} while (iterator.hasNext());
		return false;
	}

	public boolean setupNext() {
		if (setup_iterator != null && setup_iterator.hasNext()) {
			current = setup_iterator.next();
			return true;
		}
		return false;
	}

	public void setupReturn(T order) {
		if (!dispatchers.containsKey(order)) {
			dispatchers.put(order, new ArrayList<String>());
		}
		dispatchers.get(order).add(current);
	}
}
