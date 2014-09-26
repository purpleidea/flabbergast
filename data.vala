namespace Flabbergast.Data {
	public abstract class Datum : Object {
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
			if (@value.is_nan ()) {
				return "NaN";
			} else if (!@value.is_finite ()) {
				if (@value < 0) {
					return "-Infinity";
				} else {
					return "Infinity";
				}
			}
			return @value.to_string ();
		}
	}

	public class Null : Datum {
		public override string to_string () {
			return "Null";
		}
	}

	public class Continue : Null {
		public override string to_string () {
			return "Continue";
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

	public abstract class Tupleish : Datum, GTeonoma.SourceInfo {
		internal Gee.SortedMap<string, Expression> attributes = new Gee.TreeMap<string, Expression> ();
		public Utils.ContainerReference? containers {
			get;
			internal set;
		}
		public GTeonoma.source_location source { get; set; }
	}

	public class Template : Tupleish {
		internal Gee.SortedSet<string> externals = new Gee.TreeSet<string> ();
		private class TemplateExternal : Gee.Map.Entry<string, Expression ? > {
			private string name;
			public override string key {
				get {
					return name;
				}
			}
			public override Expression? value {
				set {} get {
					return null;
				}
			}
			public override bool read_only {
				get {
					return false;
				}
			}
			internal TemplateExternal (string name) {
				this.name = name;
			}
		}
		public Gee.Iterator<Gee.Map.Entry<string, Expression? > > iterator () {
			var list = new Gee.ArrayList<Gee.Iterator<Gee.Map.Entry<string, Expression? > > > ();
			list.add (externals.iterator ().map<Gee.Map.Entry<string, Expression? > > ((name) => new TemplateExternal (name)));
			list.add (attributes.entries.iterator ());
			return Gee.Iterator.concat<Gee.Map.Entry<string, Expression? > > (list.iterator ());
		}
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
		public new Expression? get (string name) {
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

			 case INT :
				 return typeof (Integer);

			 case STR :
				 return typeof (String);

			 case TEMPLATE :
				 return typeof (Template);

			 case TUPLE :
				 return typeof (Tuple);

			 default :
				 assert_not_reached ();
			}
		}
	}
}
