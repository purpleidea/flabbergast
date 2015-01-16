using System;
using System.Collections.Generic;
using System.Linq;

namespace Flabbergast {
public abstract class MergeIterator<T> : Computation {

	public delegate bool KeyDispatch(string name, ref T current);

	private SortedDictionary<string, KeyDispatch> dispatchers = new SortedDictionary<string, KeyDispatch>();
	private IEnumerator<string> enumerator = null;
	private T current;

	public MergeIterator(IAttributeNames[] inputs, KeyDispatch default_dispatcher, T initial) {
		current = initial;
		foreach (var input in inputs) {
			foreach (var key in input.GetAttributeNames()) {
				dispatchers[key] = default_dispatcher;
			}
		}
	}

	public void AddDispatcher(string name, KeyDispatch dispatcher) {
		dispatchers[name] = dispatcher;
	}
	protected override bool Run() {
		if (enumerator == null) {
			enumerator = dispatchers.Keys.GetEnumerator();
		}
		while (enumerator.MoveNext()) {
			if (!dispatchers[enumerator.Current](enumerator.Current, ref current)) {
				return false;
			}
		}
		result = current;
		return true;
	}
}
}
