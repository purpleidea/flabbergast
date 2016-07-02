using System.Collections.Generic;

namespace Flabbergast {
/**
 * Iterate over the keys of several of frames and templates.
 */

public class MergeIterator {
    /**
     * The current attribute name.
     */
    public string Current {
        get {
            return enumerator.Current.Key;
        }
    }

    public Stringish Currentish {
        get {
            return new SimpleStringish(enumerator.Current.Key);
        }
    }

    /**
     * The current attribute ordinal, 1-based per the language spec.
     */
    public long Position {
        get;
        private set;
    }
    private readonly SortedDictionary<string, int> dispatchers = new SortedDictionary<string, int>();
    private IEnumerator<KeyValuePair<string, int>> enumerator;
    private readonly int exit_dispatcher;

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
public class OrderByIterator<T> {
    /**
     * The current attribute name.
     */
    public string Current {
        get;
        private set;
    }

    public Stringish Currentish {
        get {
            return new SimpleStringish(Current);
        }
    }

    /**
     * The current attribute ordinal, 1-based per the language spec.
     */
    public long Position {
        get;
        private set;
    }
    private readonly SortedDictionary<T, List<string>> dispatchers = new SortedDictionary<T, List<string>>();
    private IEnumerator<string> setup_enumerator;
    private IEnumerator<List<string>> enumerator;
    private IEnumerator<string> inner_enumerator;

    public OrderByIterator(IAttributeNames[] inputs) {
        var dictionary = new Dictionary<string, bool>();
        foreach (var input in inputs) {
            foreach (var key in input.GetAttributeNames()) {
                dictionary[key] = true;
            }
        }
        setup_enumerator = dictionary.Keys.GetEnumerator();
        Position = 0;
    }

    public bool Next() {
        if (enumerator == null) {
            enumerator = dispatchers.Values.GetEnumerator();
            if (!enumerator.MoveNext()) {
                return false;
            }
        }
        do {
            if (inner_enumerator == null) {
                inner_enumerator = enumerator.Current.GetEnumerator();
            }
            if (inner_enumerator.MoveNext()) {
                Current = inner_enumerator.Current;
                Position++;
                return true;
            }
            inner_enumerator = null;
        } while (enumerator.MoveNext());
        return false;
    }

    public bool SetupNext() {
        if (setup_enumerator != null && setup_enumerator.MoveNext()) {
            Current = setup_enumerator.Current;
            return true;
        }
        return false;
    }

    public void SetupReturn(T order) {
        if (!dispatchers.ContainsKey(order)) {
            dispatchers[order] = new List<string>();
        }
        dispatchers[order].Add(setup_enumerator.Current);
    }
}
}
