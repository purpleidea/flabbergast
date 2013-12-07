namespace Flabbergast {
	public class ForeignGenerator : Object {
		private class ListEntry : Gee.Map.Entry<string, Data.Datum> {
			private Data.Datum datum;
			private string str_key;
			public override string key {
				get {
					return str_key;
				}
			}
			public override Data.Datum value {
				set {
					datum = value;
				} get {
					return datum;
				}
			}
			public override bool read_only {
				get {
					return true;
				}
			}
			internal ListEntry (int order, Data.Datum datum) {
				this.datum = datum;
				str_key = Expressions.make_id (order);
			}
		}

		private class MapAdapter<T> :  Gee.Iterator<Gee.Map.Entry<string, Data.Datum> >, Gee.Traversable<Gee.Map.Entry<string, Data.Datum> >, Object {
			private Gee.Iterator<T> iterator;
			private int index = 0;
			internal Gee.MapFunc<Data.Datum, T> transform;
			public bool read_only {
				get {
					return true;
				}
			}
			public bool valid {
				get {
					return iterator.valid;
				}
			}
			internal MapAdapter (owned Gee.MapFunc<Data.Datum, T> transform, Gee.Iterable<T> list) {
				iterator = list.iterator ();
				this.transform = (owned) transform;
			}
			public bool next () {
				return iterator.next ();
			}
			public bool has_next () {
				return iterator.has_next ();
			}
			public new Gee.Map.Entry<string, Data.Datum> @get () {
				return new ListEntry (index++, transform (iterator.get ()));
			}
			public void remove () {}
			public bool @foreach (Gee.ForallFunc<Gee.Map.Entry<string, Data.Datum> > f) {
				var it = 0;
				return iterator.@foreach ((x) => f (new ListEntry (it++, transform (x))));
			}
		}
		public Data.Tuple result {
			get;
			private set;
		}
		private ExecutionEngine engine;

		public void add (string name, Data.Datum @value) throws EvaluationError {
			if (result.attributes.has_key (name)) {
				throw new EvaluationError.NAME (@"Duplicate attribute name $(name) during tuple synthesis.");
			}
			var attr_value = new Expressions.ReturnOwnedLiteral (@value);
			result.attributes[name] = attr_value;
			engine.environment[result.context, name] = attr_value;
		}
		public void add_int (string name, int @value) throws EvaluationError {
			add (name, new Data.Integer (@value));
		}
		public void add_float (string name, double @value) throws EvaluationError {
			add (name, new Data.Float (@value));
		}
		public void add_str (string name, string @value) throws EvaluationError {
			add (name, new Data.String (@value));
		}
		public void add_null (string name) throws EvaluationError {
			add (name, new Data.Null ());
		}

		private void add_iterable (string name, Gee.Iterator<Gee.Map.Entry<string, Data.Datum> > iterator) throws EvaluationError {
			while (iterator.next ()) {
				var entry = iterator.get ();
				add (entry.key, entry.value);
			}
		}
		public void add_tuple (string name, Gee.Iterable<Gee.Map.Entry<string, Data.Datum> > list) throws EvaluationError {
			add_iterable (name, list.iterator ());
		}

		public void add_list (string name, Gee.Iterable<Data.Datum> list) throws EvaluationError {
			add_iterable (name, new MapAdapter<Data.Datum> ((x) => x, list));
		}

		public void add_list_int (string name, Gee.Iterable<int> list) throws EvaluationError  {
			add_iterable (name, new MapAdapter<int> ((x) => new Data.Integer (x), list));
		}

		public void add_list_float (string name, Gee.Iterable<double? > list) throws EvaluationError  {
			add_iterable (name, new MapAdapter<double? > ((x) => new Data.Float (x), list));
		}

		public void add_list_string (string name, Gee.Iterable<string> list) throws EvaluationError  {
			add_iterable (name, new MapAdapter<string> ((x) => new Data.String (x), list));
		}

		public void add_function (string name, bool has_args, Gee.Set<string>? named_args, owned EvaluateFunc function) throws EvaluationError {
			if (result.attributes.has_key (name)) {
				throw new EvaluationError.NAME (@"Duplicate attribute name $(name) during tuple synthesis.");
			}
			var template = new  Expressions.TemplateLiteral ();
			template.source_expr = null;
			var attributes = new Gee.ArrayList<Expressions.TemplatePart> ();
			if (named_args != null) {
				foreach (var arg_name in named_args) {
					if (arg_name == "args") {
						throw new EvaluationError.INTERNAL ("Foreign function declares an args attribute.");
					}
					if (arg_name == "value") {
						throw new EvaluationError.INTERNAL ("Foreign function requests an argument named “value”, which is the return value.");
					}
					var attr = new Expressions.External ();
					attr.name = new Name (arg_name);
					attributes.add (attr);
				}
			}
			if (has_args) {
				var attr = new Expressions.External ();
				attr.name = new Name ("args");
				attributes.add (attr);
			}
			var value_attr = new Expressions.Attribute ();
			value_attr.name = new Name ("value");
			value_attr.expression = new LocalCall ((owned) function);
			attributes.add (value_attr);
			template.attributes = attributes;
			result.attributes[name] = template;
			engine.environment[result.context, name] = template;
		}
		class LocalCall : Expression {
			private EvaluateFunc function;
			internal LocalCall (owned EvaluateFunc function) {
				this.function = (owned) function;
			}
			public override void evaluate (ExecutionEngine engine) throws EvaluationError {
				var named_args = new Gee.TreeMap<string, Data.Datum> ();
				foreach (var entry in engine.state.this_tuple) {
					if (entry.key[0].islower ()) {
						engine.call (entry.value);
						named_args[entry.key] = engine.operands.pop ();
					}
				}
				engine.operands.push (function (engine, named_args));
			}
		}

		public delegate Data.Datum EvaluateFunc (ExecutionEngine engine, Gee.Map<string, Data.Datum> named_args);
		public ForeignGenerator (ExecutionEngine engine) throws EvaluationError {
			this.engine = engine;
			var context = engine.environment.create ();
			var tuple = new Data.Tuple (context);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new Expressions.ReturnLiteral (tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);

			result = tuple;
		}
	}
}
