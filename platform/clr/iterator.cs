using System;
using System.Collections.Generic;
using System.Linq;

namespace Flabbergast {

/**
 * Iterate over the keys of several of frames and templates.
 */
public abstract class MergeIterator : Computation {
	/**
	 * A delegate to be invoked upon access of a particular key.
	 */
	public delegate bool KeyDispatch();

	/**
	 * The current attribute name.
	 */
	public string Current {
		get { return enumerator.Current; }
	}
	/**
	 * The current attribute ordinal, 1-based per the language spec.
	 */
	public long Position {
		get;
		private set;
	}

	private SortedDictionary<string, KeyDispatch> dispatchers = new SortedDictionary<string, KeyDispatch>();
	private IEnumerator<string> enumerator = null;

	public MergeIterator(IAttributeNames[] inputs, KeyDispatch default_dispatcher) {
		foreach (var input in inputs) {
			foreach (var key in input.GetAttributeNames()) {
				dispatchers[key] = default_dispatcher;
			}
		}
		Position = 0;
	}

	/**
	 * Add dispatcher for a particular key name.
	 *
	 * When the key is encoutered, this dispatcher will be invoked, instead of
	 * the default dispatcher in the constructor. Added dispatchers are always
	 * invoked, even if they do not occur in the input key space.
	 */
	public void AddDispatcher(string name, KeyDispatch dispatcher) {
		dispatchers[name] = dispatcher;
	}

	protected override bool Run() {
		if (enumerator == null) {
			enumerator = dispatchers.Keys.GetEnumerator();
		}
		while (enumerator.MoveNext()) {
			Position++;
			if (!dispatchers[enumerator.Current]()) {
				return false;
			}
		}
		return true;
	}
}
}
