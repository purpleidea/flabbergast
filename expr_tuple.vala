namespace Flabbergast.Expressions {
	internal abstract class TemplatePart : Object {
		public Name name {
			get;
			set;
		}
	}
	internal abstract class TuplePart : TemplatePart {}
	internal class Attribute : TuplePart {
		public Expression expression {
			get;
			set;
		}
	}
	internal class External : TemplatePart {}
	internal class Override : TuplePart {
		public Gee.List<TemplatePart> attributes {
			get;
			set;
		}
	}
	internal class Undefine : TuplePart {}

	internal class TupleLiteral : Expression {
		public Gee.List<Attribute> attributes {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create ();
			var tuple = new Tuple (context);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}
			state.containers = new ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);
			state.context = context;
			state.this_tuple = tuple;

			engine.state = state;

			var attr_names = new Gee.HashSet<string> ();
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME (@"Duplicate attribute name $(attr.name.name).");
				}
				var attr_value = engine.create_closure (attr.expression);
				tuple.attributes[attr.name.name] = attr_value;
				engine.environment[context, attr.name.name] = attr_value;
				attr_names.add (attr.name.name);
			}
			engine.operands.push (tuple);
		}
	}
	internal class TemplateLiteral : Expression {
		public Gee.List<TemplatePart> attributes {
			get;
			set;
		}
		public Expression? source {
			get;
			internal set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			Template source_data = null;
			if (source != null) {
				engine.call (source);
				var result = engine.operands.pop ();
				if (result is Template) {
					source_data = (Template) result;
				} else {
					throw new EvaluationError.TYPE_MISMATCH ("Template based on expression which is not a template.");
				}
			}
			var attr_names = new Gee.HashSet<string> ();

			var template = new Template ();
			template.containers = engine.state.containers;
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME (@"Duplicate attribute name $(attr.name.name).");
				}
				attr_names.add (attr.name.name);
				if (attr is External) {
					template.externals.add (attr.name.name);
				} else if (attr is Override) {
					if (source_data == null) {
						throw new EvaluationError.OVERRIDE (@"Attemping to override without a source template.");
					}
					if (!source_data.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existant attribute $(attr.name.name).");
					}
					var child_override = new TemplateLiteral ();
					child_override.source = source_data.attributes[attr.name.name];
					child_override.attributes.add_all (((Override) attr).attributes);
					template.attributes[attr.name.name] = child_override;
				} else if (attr is Undefine) {
					/* Do nothing. We've effectively squatted on this attribute name. */
				} else if (attr is Attribute) {
					template.attributes[attr.name.name] = ((Attribute) attr).expression;
				} else {
					assert_not_reached ();
				}
			}
			if (source_data != null) {
				foreach (var entry in source_data.attributes.entries) {
					if (entry.key in attr_names) {
						continue;
					}
					template.attributes[entry.key] = entry.value;
				}
				foreach (var external in source_data.externals) {
					if (external in attr_names) {
						continue;
					}
					template.externals.add (external);
				}
			}
			engine.operands.push (template);
		}
	}
	internal class Instantiate : Expression {
		public Gee.List<TuplePart> attributes {
			get;
			set;
		}
		public Expression source {
			get;
			set;
		}
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call (source);
			var result = engine.operands.pop ();
			if (!(result is Template)) {
				throw new EvaluationError.TYPE_MISMATCH ("Attempting to instantiate something which is not a template.");
			}

			var template = (Template) result;
			var context = engine.environment.create ();
			var tuple = new Tuple (context);

			var state = engine.state;
			if (state.this_tuple != null) {
				var container_expr = new ReturnLiteral (tuple);
				tuple.attributes["Container"] = container_expr;
				engine.environment[context, "Container"] = container_expr;
			}
			state.containers = new ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);
			state.context = context;
			state.this_tuple = tuple;

			engine.state = state;

			var attr_names = new Gee.HashSet<string> ();
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME (@"Duplicate attribute name $(attr.name.name).");
				}
				attr_names.add (attr.name.name);
				if (attr is Attribute) {
					var attr_value = engine.create_closure (((Attribute) attr).expression);
					tuple.attributes[attr.name.name] = attr_value;
					engine.environment[context, attr.name.name] = attr_value;
				} else if (attr is Override) {
					if (!template.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existant attribute $(attr.name.name).");
					}
					var child_override = new TemplateLiteral ();
					child_override.source = template.attributes[attr.name.name];
					child_override.attributes = new Gee.ArrayList<TemplatePart> ();
					child_override.attributes.add_all (((Override) attr).attributes);
					var attr_value = engine.create_closure (child_override);
					tuple.attributes[attr.name.name] = attr_value;
					engine.environment[context, attr.name.name] = attr_value;
				} else if (attr is Undefine) {
					/* Do nothing. We've effectively squatted on this attribute name. */
				} else {
					assert_not_reached ();
				}
			}
			foreach (var entry in template.attributes.entries) {
				if (entry.key in attr_names) {
					continue;
				}
				attr_names.add (entry.key);
				var attr_value = engine.create_closure (entry.value);
				tuple.attributes[entry.key] = attr_value;
				engine.environment[context, entry.key] = attr_value;
			}
			foreach (var external in template.externals) {
				if (!(external in attr_names)) {
					throw new EvaluationError.EXTERNAL_REMAINING (@"External attribute $(external) not overridden.");
				}
			}
			engine.operands.push (tuple);
		}
	}
/*
   fn:expr "("(arg0:expr ("," argn:expr)*) (name0 "=" kwarg0:expr ("," namen "=" kwargn:expr)*)")"

   "[" (arg0:expr ("," argn:expr)*)? "]"

 */
	internal class This : Expression {
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var this_tuple = engine.state.this_tuple;
			if (this_tuple == null) {
				throw new EvaluationError.INTERNAL ("This references non-existent tuple.");
			}
			engine.operands.push (this_tuple);
		}
	}
}