using System;
using System.Collections.Generic;
using System.Linq;

namespace Flabbergast {

/**
 * Iterate over the keys of several of frames and templates.
 */
public class MergeIterator {
	/**
	 * The current attribute name.
	 */
	public string Current {
		get { return enumerator.Current.Key; }
	}
	public Stringish Currentish {
		get { return new SimpleStringish(enumerator.Current.Key); }
	}
	/**
	 * The current attribute ordinal, 1-based per the language spec.
	 */
	public long Position {
		get;
		private set;
	}

	private SortedDictionary<string, int> dispatchers = new SortedDictionary<string, int>();
	private int exit_dispatcher;
	private IEnumerator<KeyValuePair<string, int>> enumerator = null;

	public MergeIterator(IAttributeNames[] inputs, int default_dispatcher, int exit_dispatcher) {
		this.exit_dispatcher = exit_dispatcher;
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
	 * When the key is encoutered, this dispatcher will be returned, instead of
	 * the default dispatcher in the constructor. Added dispatchers are always
	 * invoked, even if they do not occur in the input key space.
	 */
	public void AddDispatcher(string name, int dispatcher) {
		dispatchers[name] = dispatcher;
	}

	public int Next() {
		if (enumerator == null) {
			enumerator = dispatchers.GetEnumerator();
		}
		if (enumerator.MoveNext()) {
			Position++;
			return enumerator.Current.Value;
		}
		return exit_dispatcher;
	}
}
}
