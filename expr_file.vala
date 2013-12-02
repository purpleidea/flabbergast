namespace Flabbergast.Expressions {
	public class File : Expression {
		public Gee.List<Import> imports {
			get;
			set;
			default = Gee.List.empty<Import> ();
		}
		public Gee.List<Attribute> attributes {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create ();
			var tuple = new Tuple (context);

			var state = engine.state;
			state.context = context;
			state.this_tuple = tuple;
			state.containers = null;
			engine.state = state;

			foreach (var import in imports) {
				if (tuple.attributes.has_key (import.name.name)) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(import.name.name).");
				}
				var attr_value = new ReturnLiteral (engine.find_import (import.uri.path));
				tuple.attributes[import.name.name] = attr_value;
				engine.environment[context, import.name.name] = attr_value;
			}
			foreach (var attribute in attributes) {
				if (tuple.attributes.has_key (attribute.name.name)) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attribute.name.name).");
				}
				var attr_value = engine.create_closure (attribute.expression);
				tuple.attributes[attribute.name.name] = attr_value;
				engine.environment[context, attribute.name.name] = attr_value;
			}
			engine.operands.push (tuple);
		}
	}
	public class Import : Object {
		public Name name {
			get;
			set;
		}
		public UriReference uri {
			get;
			set;
		}
	}
	public class UriReference : Object {
		public string path {
			get;
			private set;
		}
		public UriReference (string path) {
			this.path = path;
		}
	}
}