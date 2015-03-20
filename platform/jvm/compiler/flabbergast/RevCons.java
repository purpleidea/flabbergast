package flabbergast;

import java.util.ArrayList;
import java.util.List;

class RevCons<T> {
	private final T head;
	private final RevCons<T> tail;
	private final int index;
	RevCons(T item, RevCons<T> tail) {
		head = item;
		this.tail = tail;
		index = tail == null ? 0 : (tail.index + 1);
	}
	private void assign(List<T> array) {
		array.set(index, head);
		if (tail != null) {
			tail.assign(array);
		}
	}
	List<T> ToList() {
		List<T> array = new ArrayList<T>();
		assign(array);
		return array;
	}
}
