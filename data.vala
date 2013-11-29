namespace Flabbergast {
	public abstract class Datum {}

	public class Tuple : Datum {
		Gee.SortedMap<string, Expression> attributes = new Gee.TreeMap<string, Expression> ();
		public Expression? get(string name) {
			return attributes.has_key(name) ? attributes[name] : null;
		}
	}
}