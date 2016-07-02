using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;

namespace Flabbergast {
/**
 * The collection of frames in which lookup should be performed.
 */

public class Context {
    /**
    * The total number of frames in this context.
    */

    public int Length {
        get {
            return frames.Count;
        }
    }

    private readonly List<Frame> frames;

    private Context(List<Frame> frames) {
        this.frames = frames;
    }

    /**
    * Conjoin two contexts, placing all the frames of the provided context after
    * all the frames in the original context.
    */

    public static Context Append(Context original, Context new_tail) {
        if (original == null) {
            throw new InvalidOperationException("Cannot append to null context.");
        }
        if (new_tail == null || original == new_tail) {
            return original;
        }
        int filter = 0;
        var list = new List<Frame>(original.Length + new_tail.Length);
        foreach (var frame in original.frames) {
            list.Add(frame);
            filter |= frame.GetHashCode();
        }
        foreach (var frame in new_tail.frames) {
            int hash = frame.GetHashCode();
            if ((hash & filter) != hash || !list.Contains(frame)) {
                list.Add(frame);
                filter |= hash;
            }
        }
        return new Context(list);
    }

    public IEnumerable<Frame> Fill() {
        return frames;
    }

    public static Context Prepend(Frame head, Context tail) {
        if (head == null) {
            throw new InvalidOperationException("Cannot prepend a null frame to a context.");
        }
        var list = new List<Frame>(tail == null ? 1 : (tail.Length + 1));
        list.Add(head);
        if (tail != null) {
            list.AddRange(tail.frames.Where(frame => head != frame));
        }
        return new Context(list);
    }
}

/**
 * The null type.
 *
 * For type dispatch, there are plenty of reasons to distinguish between the
 * underlying VM's null and Flabbergast's Null, and this type makes this
 * possible.
 */

public class Unit {
    public static readonly Unit NULL = new Unit();
    private Unit() {}

    public override string ToString() {
        return "Null";
    }
}

/**
 * Objects which have strings that can be iterated in alphabetical order for
 * the MergeIterator.
 */

public interface IAttributeNames {
    /**
    * Provide all the attribute names (keys) in the collection. They need not be
    * ordered.
    */
    IEnumerable<string> GetAttributeNames();
}

/**
 * A Flabbergast Template, holding functions for computing attributes.
 */

public class Template : IAttributeNames {
    public Frame Container {
        get;
        private set;
    }
    /**
    * The context in which this template was created.
    */
    public Context Context {
        get;
        private set;
    }
    /**
    * Access the functions in the template. Templates should not be mutated, but
    * this policy is not enforced by this class; it must be done in the calling
    * code.
    */

    public ComputeValue this[string name] {
        get {
            return attributes.ContainsKey(name) ? attributes[name] : null;
        }
        set {
            if (value == null) {
                return;
            }
            if (attributes.ContainsKey(name)) {
                throw new InvalidOperationException("Redefinition of attribute " + name + ".");
            }
            attributes[name] = value;
        }
    }

    /**
    * The stack trace at the time of creation.
    */
    public SourceReference SourceReference {
        get;
        private set;
    }
    private readonly IDictionary<string, ComputeValue> attributes = new SortedDictionary<string, ComputeValue>();

    public Template(SourceReference source_ref, Context context, Frame container) {
        SourceReference = source_ref;
        Context = context;
        Container = container;
    }

    public IEnumerable<string> GetAttributeNames() {
        return attributes.Keys;
    }

    public ComputeValue Get(Stringish name) {
        return this[name.ToString()];
    }
}

}
