package flabbergast;

import java.util.Iterator;

/**
 * Objects which have strings that can be iterated in alphabetical order for the
 * MergeIterator.
 */
public interface AttributeNames {
	/**
	 * Provide all the attribute names (keys) in the collection. They need not
	 * be ordered.
	 */
	Iterator<String> getAttributeNames();
}
