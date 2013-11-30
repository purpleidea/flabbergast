namespace Flabbergast.Expressions {
	public abstract class TemplatePart {
		public string name { get; private set; }
	}
	public abstract class TuplePart : TemplatePart {}
	public class Attribute : TuplePart {
		public Expression expression { get; private set; }
	}
	public class External : TemplatePart {}
	public class Override : TuplePart {
		public Gee.List<TemplatePart> attributes { get; private set; }
	}
	public class Undefine : TuplePart {}

	public class TupleLiteral : Expression {
		public Gee.List<Attribute> attributes { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create();
			engine.state.containers = new ContainerReference(engine.state.context, engine.state.containers);
			engine.environment.append_containers(context, engine.state.containers);
			engine.state.context = context;
			var attr_names = new Gee.HashSet<string> ();
			var tuple = new Tuple(context);
			engine.state.this_tuple = tuple;
			foreach (var attr in attributes) {
				if (attr.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME(@"Duplicate attribute name $(attr.name).");
				}
				var attr_value = engine.create_closure(attr.expression);
				tuple.attributes[attr.name] = attr_value;
				engine.environment[context, attr.name] = attr_value;
			}
			engine.operands.push(tuple);
		}
	}
	public class TemplateLiteral : Expression {
		public Gee.List<TemplatePart> attributes { get; private set; }
		public Expression? source { get; internal set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			Template source_data = null;
			if (source == null) {
				engine.call(source);
				var result = engine.operands.pop();
				if (result is Template) {
					source_data = (Template) result;
				} else {
					throw new EvaluationError.TYPE_MISMATCH("Template based on expression which is not a template.");
				}
			}
			var undefines = new Gee.TreeSet<string> ();
			var attr_names = new Gee.HashSet<string> ();

			var template = new Template();
			template.containers = engine.state.containers;
			foreach (var attr in attributes) {
				if (attr.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME(@"Duplicate attribute name $(attr.name).");
				}
				attr_names.add(attr.name);
				if (attr is External) {
					template.externals.add(attr.name);
				} else if (attr is Override) {
					if (source_data == null) {
						throw new EvaluationError.OVERRIDE(@"Attemping to override without a source template.");
					}
					if (source_data.attributes.has_key(attr.name)) {
						throw new EvaluationError.OVERRIDE(@"Attempting to override non-existant attribute $(attr.name).");
					}
					var previous_template = source_data.attributes[attr.name];
					var child_override = new TemplateLiteral();
					child_override.source = previous_template;
					child_override.attributes.add_all(((Override) attr).attributes);
					template.attributes[attr.name] = child_override;
				} else if (attr is Attribute) {
					template.attributes[attr.name] = ((Attribute) attr).expression;
				} else {
					assert_not_reached();
				}
			}
			if (source_data != null) {
				foreach (var entry in source_data.attributes.entries) {
					if (entry.key in attr_names || entry.key in undefines) {
						continue;
					}
					template.attributes[entry.key] = entry.value;
				}
				foreach (var external in source_data.externals) {
					if (external in attr_names || external in undefines) {
						continue;
					}
					template.externals.add(external);
				}
			}
			engine.operands.push(template);
		}
	}
	public class Instantiate : Expression {
		public Gee.List<TuplePart> attributes { get; private set; }
		public Expression source { get; private set; }
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			engine.call(source);
			var result = engine.operands.pop();
			if (!(result is Template)) {
				throw new EvaluationError.TYPE_MISMATCH("Attempting to instantiate something which is not a template.");
			}
			var template = (Template) result;
			var context = engine.environment.create();
			engine.state.containers = new ContainerReference(engine.state.context, engine.state.containers);
			engine.environment.append_containers(context, engine.state.containers);
			engine.environment.append_containers(context, template.containers);
			engine.state.context = context;
			var attr_names = new Gee.HashSet<string> ();
			var tuple = new Tuple(context);
			engine.state.this_tuple = tuple;
			foreach (var attr in attributes) {
				if (attr.name in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME(@"Duplicate attribute name $(attr.name).");
				}
				attr_names.add(attr.name);
				if (attr is Attribute) {
					var attr_value = engine.create_closure(((Attribute) attr).expression);
					tuple.attributes[attr.name] = attr_value;
					engine.environment[context, attr.name] = attr_value;
				} else if (attr is Override) {
					if (template.attributes.has_key(attr.name)) {
						throw new EvaluationError.OVERRIDE(@"Attempting to override non-existant attribute $(attr.name).");
					}
					var child_override = new TemplateLiteral();
					child_override.source = template.attributes[attr.name];
					child_override.attributes.add_all(((Override) attr).attributes);
					var attr_value = engine.create_closure(child_override);
					tuple.attributes[attr.name] = attr_value;
					engine.environment[context, attr.name] = attr_value;
				} else {
					assert_not_reached();
				}
			}
			foreach (var entry in template.attributes.entries) {
				if (entry.key in attr_names) {
					throw new EvaluationError.DUPLICATE_NAME(@"Duplicate attribute name $(entry.key).");
				}
				attr_names.add(entry.key);
				var attr_value = engine.create_closure(entry.value);
				tuple.attributes[entry.key] = attr_value;
				engine.environment[context, entry.key] = attr_value;
			}
			foreach (var external in template.externals) {
				if (!(external in attr_names)) {
					throw new EvaluationError.EXTERNAL_REMAINING(@"External attribute $(external) not overridden.");
				}
			}
			engine.operands.push(tuple);
		}
	}
/*
   fn:expr "("(arg0:expr ("," argn:expr)*) (name0 "=" kwarg0:expr ("," namen "=" kwargn:expr)*)")"

   "[" (arg0:expr ("," argn:expr)*)? "]"

 */
	public class This : Expression {
		public override void evaluate(ExecutionEngine engine) throws EvaluationError {
			var this_tuple = engine.state.this_tuple;
			if (this_tuple == null) {
				throw new EvaluationError.INTERNAL("This references non-existent tuple.");
			}
			engine.operands.push(this_tuple);
		}
	}
}
