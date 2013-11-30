namespace Flabbergast {
	public abstract class Datum {}

	public class Boolean : Datum {
		public bool @value;
		public Boolean(bool @value) {
			this.value = @value;
		}
	}

	public class Integer : Datum {
		public int @value;
		public Integer(int @value) {
			this.value = @value;
		}
	}

	public class Float : Datum {
		public double @value;
		public Float(double @value) {
			this.value = @value;
		}
	}

	public class Null : Datum {}

	public class String : Datum {
		public string @value;
		public String(string @value) {
			this.value = @value;
		}
	}

	public class Template : Datum {
		internal ContainerReference? containers;
		internal Gee.SortedMap<string, Expression> attributes = new Gee.TreeMap<string, Expression> ();
		internal Gee.SortedSet<string> externals = new Gee.TreeSet<string> ();
	}

	public class Tuple : Datum {
		uint context;
		internal Gee.SortedMap<string, Expression> attributes = new Gee.TreeMap<string, Expression> ();
		public Tuple(uint context) {
			this.context = context;
		}
		public Expression? get(string name) {
			return attributes.has_key(name) ? attributes[name] : null;
		}
	}

	public enum Ty {
		BOOL,
		FLOAT,
		INT,
		STR,
		TEMPLATE,
		TUPLE;
		public Type get_real_type() {
			switch (this) {
			case BOOL :
				return typeof(Boolean);
			case FLOAT :
				return typeof(Float);
			case INT:
				return typeof(Integer);
			case STR:
				return typeof(String);
			case TEMPLATE:
				return typeof(Template);
			case TUPLE:
				return typeof(Tuple);
			default:
				assert_not_reached();
			}
		}
	}
}