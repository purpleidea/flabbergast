namespace Flabbergast.Expressions {
	public abstract class TemplatePart : Object, GTeonoma.SourceInfo {
		public Name name {
			get;
			set;
		}
		public GTeonoma.source_location source {
			get;
			set;
		}
	}
	public abstract class FramePart : TemplatePart {}
	public class Attribute : FramePart {
		public Expression expression {
			get;
			set;
		}
	}
	internal class External : TemplatePart {}
	internal class Informative : TemplatePart {}
	internal class NamedOverride : FramePart {
		public Expression expression {
			get;
			set;
		}
		public Name original {
			get;
			set;
		}
	}
	internal class Override : FramePart {
		public Gee.List<TemplatePart> attributes {
			get;
			set;
		}
	}
	internal class Undefine : FramePart {}

	internal class FrameLiteral : Expression {
		public Gee.List<Attribute> attributes {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create ();
			var frame = new Data.Frame (context);
			frame.source = source;
			frame.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);

			var state = engine.state;
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);
			state.context = context;
			state.container_frame = state.this_frame;
			state.this_frame = frame;

			engine.state = state;

			var attr_names = new Gee.HashSet<string> ();
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr.name.name) in literal frame. $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
				}
				var attr_value = engine.create_closure (attr.expression);
				frame.attributes[attr.name.name] = attr_value;
				engine.environment[context, attr.name.name] = attr_value;
				attr_names.add (attr.name.name);
			}
			engine.operands.push (frame);
		}
		public override Expression transform () {
			foreach (var attribute in attributes) {
				attribute.expression = attribute.expression.transform ();
			}
			return this;
		}
	}
	internal class TemplateLiteral : Expression {
		public Gee.List<TemplatePart> attributes {
			get;
			set;
		}
		public Expression? source_expr {
			get;
			internal set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			Data.Template source_data = null;
			if (source_expr != null) {
				engine.call (source_expr);
				var result = engine.operands.pop ();
				if (result is Data.Template) {
					source_data = (Data.Template)result;
				} else {
					throw new EvaluationError.TYPE_MISMATCH (@"Template based on expression which is not a template. $(source_expr.source.source):$(source_expr.source.line):$(source_expr.source.offset)");
				}
			}
			var attr_names = new Gee.HashSet<string> ();

			var template = new Data.Template ();
			template.source = source;
			template.containers = new Utils.ContainerReference (engine.state.context, Utils.ContainerReference.append (engine.state.containers, source_data == null ? null : source_data.containers));
			foreach (var attr in attributes) {
				if (attr is Informative) {
					/* Skip informative attributes. They don't do anything. */
					continue;
				}
				if (attr.name.name in attr_names) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr.name.name) in template definition. $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
				}
				attr_names.add (attr.name.name);
				if (attr is External) {
					template.externals.add (attr.name.name);
				} else if (attr is NamedOverride) {
					if (source_data == null) {
						throw new EvaluationError.OVERRIDE (@"Attemping to override without a source template. $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					if (!source_data.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existent attribute $(attr.name.name). $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					var named_override = (NamedOverride) attr;
					var let = new Let ();
					let.source = attr.source;
					let.expression = named_override.expression;
					var list = new Gee.ArrayList<Attribute> ();
					var original_attribute = new Attribute ();
					original_attribute.source = attr.source;
					original_attribute.name = named_override.original;
					original_attribute.expression = source_data.attributes[attr.name.name];
					list.add (original_attribute);
					let.attributes = list;
					template.attributes[attr.name.name] = let;
				} else if (attr is Override) {
					if (source_data == null) {
						throw new EvaluationError.OVERRIDE (@"Attemping to override without a source template. $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					if (!source_data.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existent attribute $(attr.name.name). $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					var child_override = new TemplateLiteral ();
					child_override.source = attr.source;
					child_override.source_expr = source_data.attributes[attr.name.name];
					child_override.attributes = new Gee.ArrayList<Attribute> ();
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
		public override Expression transform () {
			if (source_expr != null) {
				source_expr = source_expr.transform ();
			}
			foreach (var attribute in attributes) {
				if (attribute is Attribute) {
					((Attribute) attribute).expression = ((Attribute) attribute).expression.transform ();
				}
			}
			return this;
		}
	}
	internal abstract class InstantiateLike : Expression {
		public Expression source_expr {
			get;
			set;
		}
		protected void prepare_frame (ExecutionEngine engine, out Data.Frame frame, out uint context, out Data.Template template, bool inherit_environment) throws EvaluationError {
			engine.call (source_expr);
			var result = engine.operands.pop ();
			if (!(result is Data.Template)) {
				throw new EvaluationError.TYPE_MISMATCH (@"Attempting to instantiate something which is not a template. $(source_expr.source.source):$(source_expr.source.line):$(source_expr.source.offset)");
			}

			template = (Data.Template)result;
			context = engine.environment.create ();
			frame = new Data.Frame (context);
			frame.source = source;
			frame.containers = inherit_environment ? new Utils.ContainerReference (engine.state.context, Utils.ContainerReference.append (engine.state.containers, template.containers)) : template.containers;

			var state = engine.state;
			state.containers = inherit_environment ? new Utils.ContainerReference (state.context, state.containers) : template.containers;
			engine.environment.append_containers (context, frame.containers);
			state.context = context;
			state.container_frame = state.this_frame;
			state.this_frame = frame;

			engine.state = state;
		}
		protected void finish_frame(ExecutionEngine engine, Data.Frame frame, Data.Template template, Gee.Set<string> attr_names, uint context) throws EvaluationError {
			foreach (var entry in template.attributes.entries) {
				if (entry.key in attr_names) {
					continue;
				}
				attr_names.add (entry.key);
				var attr_value = engine.create_closure (entry.value);
				frame.attributes[entry.key] = attr_value;
				engine.environment[context, entry.key] = attr_value;
			}
			foreach (var external in template.externals) {
				if (!(external in attr_names)) {
					throw new EvaluationError.EXTERNAL_REMAINING (@"External attribute $(external) not overridden at $(source.source):$(source.line):$(source.offset). $(template.source.source):$(template.source.line):$(template.source.offset)");
				}
			}
			engine.operands.push (frame);
		}
	}
	internal class Instantiate : InstantiateLike {
		public Gee.List<FramePart> attributes {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			Data.Frame frame;
			uint context;
			Data.Template template;
			prepare_frame(engine, out frame, out context, out template, true);
			var attr_names = new Gee.HashSet<string> ();
			foreach (var attr in attributes) {
				if (attr.name.name in attr_names) {
					throw new EvaluationError.NAME (@"Duplicate attribute name $(attr.name.name) in instantiation. $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
				}
				attr_names.add (attr.name.name);
				if (attr is Attribute) {
					var attr_value = engine.create_closure (((Attribute) attr).expression);
					frame.attributes[attr.name.name] = attr_value;
					engine.environment[context, attr.name.name] = attr_value;
				} else if (attr is NamedOverride) {
					if (!template.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existent attribute $(attr.name.name). $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					var named_override = (NamedOverride) attr;
					var let = new Let ();
					let.source = attr.source;
					let.expression = named_override.expression;
					var list = new Gee.ArrayList<Attribute> ();
					var original_attribute = new Attribute ();
					original_attribute.source = attr.source;
					original_attribute.name = named_override.original;
					original_attribute.expression = template.attributes[attr.name.name];
					list.add (original_attribute);
					let.attributes = list;
					var attr_value = engine.create_closure (let);
					frame.attributes[attr.name.name] = attr_value;
					engine.environment[context, attr.name.name] = attr_value;
				} else if (attr is Override) {
					if (!template.attributes.has_key (attr.name.name)) {
						throw new EvaluationError.OVERRIDE (@"Attempting to override non-existent attribute $(attr.name.name). $(attr.source.source):$(attr.source.line):$(attr.source.offset)");
					}
					var child_override = new TemplateLiteral ();
					child_override.source = attr.source;
					child_override.source_expr = template.attributes[attr.name.name];
					child_override.attributes = new Gee.ArrayList<TemplatePart> ();
					child_override.attributes.add_all (((Override) attr).attributes);
					var attr_value = engine.create_closure (child_override);
					frame.attributes[attr.name.name] = attr_value;
					engine.environment[context, attr.name.name] = attr_value;
				} else if (attr is Undefine) {
					/* Do nothing. We've effectively squatted on this attribute name. */
				} else {
					assert_not_reached ();
				}
			}
			finish_frame(engine, frame, template, attr_names, context);
		}
		public override Expression transform () {
			source_expr = source_expr.transform ();
			foreach (var attribute in attributes) {
				if (attribute is Attribute) {
					((Attribute) attribute).expression = ((Attribute) attribute).expression.transform ();
				}
			}
			return this;
		}
	}
	internal class FunctionCall : InstantiateLike {
		internal class FunctionArg : Object {
			public Name? name {
				get;
				set;
			}
			public Expression parameter {
				get;
				set;
			}
		}
		public Gee.List<FunctionArg> args {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			/* Start constructing the instantiated template, but go back to our original environment. */
			var original_state = engine.state;
			Data.Frame frame;
			uint context;
			Data.Template template;
			prepare_frame(engine, out frame, out context, out template, false);
			var attr_names = new Gee.HashSet<string> ();
			var inside_state = engine.state;

			engine.state = original_state;

			/* Create a frame for args */
			var args_context = engine.environment.create ();
			var args_frame = new Data.Frame (args_context);
			args_frame.source = source;

			var it = 0;
			var has_args = false;
			var passed_args = false;
			foreach (var arg in args) {
				if (arg.name == null) {
					has_args = true;
					var id = make_id (it++);
					var @value = engine.create_closure (arg.parameter);
					args_frame.attributes[id] = @value;
					engine.environment[args_context, id] = @value;
				} else {
					if (arg.name.name == "value") {
						throw new EvaluationError.OVERRIDE ("Function call has an argument named “value”, which break the function.");
					}
					if (arg.name.name == "args") {
						passed_args = true;
					}
					var attr_value = engine.create_closure (arg.parameter);
					frame.attributes[arg.name.name] = attr_value;
					engine.environment[context, arg.name.name] = attr_value;
					attr_names.add (arg.name.name);
				}
			}
			if (passed_args && has_args) {
				throw new EvaluationError.OVERRIDE ("Function call has both unnamed arguments and a named argument “args”.");
			}
			if (!passed_args) {
				var args_expr = new ReturnLiteral(args_frame);
				frame.attributes["args"] = args_expr;
				engine.environment[context, "args"] = args_expr;
				attr_names.add ("args");
			}
			/* Move back inside the nascent frame and continue expansion. */
			engine.state = inside_state;
			finish_frame(engine, frame, template, attr_names, context);
			/* Then do the lookup. */
			var names = new Gee.ArrayList<Name> ();
			names.add (new Name ("value"));
			engine.call(engine.lookup_direct(names, this ));
		}
		public override Expression transform () {
			source_expr = source_expr.transform ();
			foreach (var arg in args) {
				arg.parameter = arg.parameter.transform ();
			}
			return this;
		}
	}
	public string make_id (int id) {
		var len = (int) (sizeof (int) * 8 * Math.log (2) / Math.log (62)) + 1;
		var id_str = new uint8[len + 2];
		if (id < 0) {
			id_str[0] = 'e';
			id = int.MAX + id;
		} else {
			id_str[0] = 'f';
		}
		id_str[len + 1] = '\0';
		for (var it = len; it > 0; it--) {
			var digit = (uint8) (id % 62);
			id = id / 62;
			if (digit < 10) {
				id_str[it] = '0' + digit;
			} else if (digit < 36) {
				id_str[it] = 'A' + (digit - 10);
			} else {
				id_str[it] = 'a' + (digit - 36);
			}
		}
		return ((string) id_str).dup ();
	}
	internal class ListLiteral : Expression {
		public Gee.List<Expression> elements {
			get;
			set;
		}
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var context = engine.environment.create ();
			var frame = new Data.Frame (context);
			frame.source = source;
			frame.containers = new Utils.ContainerReference (engine.state.context, engine.state.containers);
			for (var it = 0; it < elements.size; it++) {
				frame.attributes[make_id (it)] = engine.create_closure (elements[it]);
			}

			var state = engine.state;
			state.containers = new Utils.ContainerReference (state.context, state.containers);
			engine.environment.append_containers (context, state.containers);
			state.context = context;
			state.container_frame = state.this_frame;
			state.this_frame = frame;

			engine.state = state;

			engine.operands.push (frame);
		}
		public override Expression transform () {
			for (var it = 0; it < elements.size; it++) {
				elements[it] = elements[it].transform ();
			}
			return this;
		}
	}
	public class This : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var this_frame = engine.state.this_frame;
			if (this_frame == null) {
				throw new EvaluationError.INTERNAL ("This references non-existent frame.");
			}
			engine.operands.push (this_frame);
		}
		public override Expression transform () {
			return this;
		}
	}
	public class Container : Expression {
		public override void evaluate (ExecutionEngine engine) throws EvaluationError {
			var container_frame = engine.state.container_frame;
			if (container_frame == null) {
				throw new EvaluationError.INTERNAL ("This references non-existent frame.");
			}
			engine.operands.push (container_frame);
		}
		public override Expression transform () {
			return this;
		}
	}
}
