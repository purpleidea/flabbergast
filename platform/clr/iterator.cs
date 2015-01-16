using System;
using System.Collections.Generic;
using System.Linq;

namespace Flabbergast {
public abstract class MergeIterator : Computation {

	public delegate bool KeyDispatch();

	public string Current {
		get { return enumerator.Current; }
	}

	private SortedDictionary<string, KeyDispatch> dispatchers = new SortedDictionary<string, KeyDispatch>();
	private IEnumerator<string> enumerator = null;

	public MergeIterator(IAttributeNames[] inputs, KeyDispatch default_dispatcher) {
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
			if (!dispatchers[enumerator.Current]()) {
				return false;
			}
		}
		return true;
	}
}
}
