namespace Flabbergast.Data {
	public abstract class Datum {
		public Type g_type {
			get {
				return Type.from_instance (this);
			}
		}
		public abstract string to_string ();
	}

	public class Boolean : Datum {
		public bool @value;
		public Boolean (bool @value) {
			this.value = @value;
		}
		public override string to_string () {
			return @value ? "True" : "False";
		}
	}

	public class Integer : Datum {
		public int @value;
		public Integer (int @value) {
			this.value = @value;
		}
		public override string to_string () {
			return @value.to_string ();
		}
	}

	public class Float : Datum {
		public double @value;
		public Float (double @value) {
			this.value = @value;
		}
		public override string to_string () {
			return @value.to_string ();
		}
	}

	public class Null : Datum {
		public override string to_string () {
			return "Null";
		}
	}

	public class String : Datum {
		public string @value;
		public String (string @value) {
			this.value = @value;
		}
		public override string to_string () {
			return @value;
		}
	}

	public abstract class Tupleish : Datum {
		internal Gee.SortedMap<string, Expression> attributes = new Gee.TreeMap<string, Expression> ();
	}

	public class Template : Tupleish {
		internal Utils.ContainerReference? containers;
		internal Gee.SortedSet<string> externals = new Gee.TreeSet<string> ();
		public override string to_string () {
			return "Template";
		}
	}

	public class Tuple : Tupleish {
		public uint context {
			get;
			private set;
		}
		public Tuple (uint context) {
			this.context = context;
		}
		public Gee.Iterator<Gee.Map.Entry<string, Expression> > iterator () {
			return attributes.entries.iterator ();
		}
		public Expression? get (string name) {
			return attributes.has_key (name) ? attributes[name] : null;
		}
		public override string to_string () {
			return "tuple";
		}
	}

	public enum Ty {
		BOOL,
		FLOAT,
		INT,
		STR,
		TEMPLATE,
		TUPLE;
		public Type get_real_type () {
			switch (this) {
			 case BOOL :
				 return typeof (Boolean);

			 case FLOAT :
				 return typeof (Float);

			 case INT:
				 return typeof (Integer);

			 case STR:
				 return typeof (String);

			 case TEMPLATE:
				 return typeof (Template);

			 case TUPLE:
				 return typeof (Tuple);

			 default:
				 assert_not_reached ();
			}
		}
	}
}
