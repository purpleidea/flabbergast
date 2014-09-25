using Flabbergast.Expressions;
public class Flabbergast.Rules : GTeonoma.Rules {
	string? fix_name (string name) {
		if (!name[0].isupper ()) {
			return null;
		}
		for (var it = 1; it < name.length; it++) {
			if (!name[it].islower ()) {
				return null;
			}
		}
		return name.down ();
	}
	public Rules () throws GTeonoma.RegisterError {
		register_double ();
		register_int ((int) sizeof (int) * 8, typeof (int));
		register_string_literal (false);
		register_enum<Data.Ty> ("type", fix_name);

		/* Tuple attributes */
		register<Attribute> ("attribute", 0, "%P{name}%-:%-%P{expression}");
		register<External> ("external attribute", 0, "%P{name}%-?:");
		register<Informative> ("informative attribute", 0, "%P{name}%-%%:");
		register<Override> ("override", 0, "%P{name}%-+:%!%-{%I%n%L{attributes}{%n}%i%n}", new Type[] { typeof (TemplatePart) });
		register<NamedOverride> ("named override", 0, "%P{name}%-+% %P{original}% :%!%-%P{expression}");
		register<Undefine> ("definition erasure", 0, "%P{name}%--:");

		/* Identifiers */
		register<ContainerName> ("container reference", 0, "Container");
		register_custom<Name> ("identifier", () =>  new IdentifierParser (), (identifier) => identifier.name);

		/* Function call arguments */
		register<FunctionCall.FunctionArg> ("named argument", 0, "%P{name}%-:%!%-%P{parameter}");
		register<FunctionCall.FunctionArg> ("argument", 0, "%P{parameter}");

		/* Files */
		register<File> ("file", 0, "% %L{attributes}{%n}", new Type[] { typeof (Attribute) });
		register_custom<File.UriReference> ("URI", () =>  new UriParser (), (uri) => uri.path);

		/* Expressions */
		var precedence = 0;
		register<Let> ("name binding", precedence, "Let %L{attributes}{% ,%-} In %P{expression}", new Type[] { typeof (Attribute) });

		precedence++;
		register<Fricassee.ForExpression> ("for⋯where⋯", precedence, "For %P{selector} Where%! %P{-where} %P{result}");
		register<Fricassee.ForExpression> ("for⋯", precedence, "For %P{selector} %P{result}");

		precedence++;
		register<Conditional> ("conditional", precedence, "If%! %P{-condition} Then %P{-truepart} Else %P{falsepart}");

		precedence++;
		register<StringConcatenate> ("string concatenation", precedence, "%P{+left}%-&%!%-%P{right}");

		precedence++;
		register<LogicalOr> ("logical disjunction", precedence, "%P{+left}%-||%!%-%P{right}");

		precedence++;
		register<LogicalAnd> ("logical conjunction", precedence, "%P{+left}%-&&%!%-%P{right}");

		precedence++;
		register<Equality> ("equality", precedence, "%P{+left}%-==%!%-%P{+right}");
		register<Inequality> ("inequality", precedence, "%P{+left}%-!=%!%-%P{+right}");
		register<LessThan> ("less than", precedence, "%P{+left}%-<%-%P{+right}");
		register<LessThanOrEqualTo> ("less than or equal to", precedence, "%P{+left}%-<=%-%P{+right}");
		register<GreaterThan> ("greater than", precedence, "%P{+left}%->%-%P{+right}");
		register<GreaterThanOrEqualTo> ("greater than or equal to", precedence, "%P{+left}%->=%-%P{+right}");

		precedence++;
		register<Shuttle> ("comparison", precedence, "%P{+left}%-<=>%!%-%P{+right}");

		precedence++;
		register<Addition> ("addition", precedence, "%P{+left}%-+%!%-%P{right}");
		register<Subtraction> ("subtraction", precedence, "%P{+left}%--%!%-%P{right}");

		precedence++;
		register<Multiplication> ("multiplication", precedence, "%P{+left}%-*%!%-%P{right}");

		precedence++;
		register<Division> ("division", precedence, "%P{+left}%-/%!%-%P{right}");
		register<Modulus> ("modulus", precedence, "%P{+left}%-%%%!%-%P{right}");

		precedence++;
		register<Through> ("range", precedence, "%P{+start} Through%! %P{+end}");

		precedence++;
		register<IsFinite> ("finite check", precedence, "%P{+expression}%-Is Finite");
		register<IsNaN> ("not-a-number check", precedence, "%P{+expression}%-Is NaN");
		register<IsNull> ("null check", precedence, "%P{+expression}%-Is Null");
		register<TypeCheck> ("type check", precedence, "%P{+expression} Is %P{ty}");
		register<TypeEnsure> ("type ensuring", precedence, "%P{+expression} As%! %P{ty}");
		register<Coerce> ("type coercion", precedence, "%P{+expression} To%! %P{ty}");

		precedence++;
		register<RaiseError> ("raise error", precedence, "Error%! %P{+expression}");
		register<StringLength> ("string length", precedence, "Length%! %P{+expression}");

		precedence++;
		register<IndirectLookup> ("remote lookup", precedence, "Lookup %L{names}{% .% } In%! %P{expression}", new Type[] { typeof (Nameish) });

		precedence++;
		register<Instantiate> ("template instantiation", precedence, "%P{+source_expr}%-{%!%I%n%l{attributes}{%n}%i%n}", new Type[] { typeof (TuplePart) });

		precedence++;
		register<NullCoalesce> ("null coalescence", precedence, "%P{+expression}%-??%!%-%P{+alternate}");

		precedence++;
		register<TupleLiteral> ("tuple literal", precedence, "{%!%I%n%l{attributes}{%n}%i%n}", new Type[] { typeof (Attribute) });
		register<TemplateLiteral> ("template", precedence, "Template%! %p{+source_expr}%_{%I%n%l{attributes}{%n}%i%n}", new Type[] { typeof (TemplatePart) });
		register<Not> ("logical not", precedence, "!%-%P{+expression}");
		register<Negation> ("negation", precedence, "-%-%P{+expression}");

		precedence++;
		register<FunctionCall> ("function call", precedence, "%P{+function}%-(%!% %l{args}{% ,%-}% )", new Type[] { typeof (FunctionCall.FunctionArg) });

		precedence++;
		register<File.Import> ("import", 0, "From%! %P{uri}");
		register<ContextualLookup> ("contextual lookup", precedence, "%L{names}{% .% }", new Type[] { typeof (Nameish) });
		register<DirectLookup> ("direct lookup", precedence, "%P{+expression}%-.%!%-%L{names}{% .% }", new Type[] { typeof (Nameish) });

		precedence++;
		register<ListLiteral> ("list literal", precedence, "[%!%-%L{-elements}{% ,%-}%-]", new Type[] { typeof (Expression) });
		register<FalseLiteral> ("false literal", precedence, "False");
		register<FloatLiteral> ("floating point literal", precedence, "%P{value}");
		register<IntegerLiteral> ("integer literal", precedence, "%P{value}");
		register<NullLiteral> ("null literal", precedence, "Null");
		register<StringLiteral> ("string literal", precedence, "\"%!%P{literal}%l{contents}{}\"", new Type[] { typeof (StringPiece) });
		register<StringLiteral> ("empty string literal", precedence, "\"\"");
		register<SubExpression> ("subexpression", precedence, "(%!% %P{-expression}% )");
		register<This> ("self-reference", precedence, "This");
		register<TrueLiteral> ("true literal", precedence, "True");
		register<IntMaxLiteral> ("maximum integer literal", precedence, "IntMax");
		register<IntMinLiteral> ("minimum integer literal", precedence, "IntMin");
		register<FloatMaxLiteral> ("maximum floating point literal", precedence, "FloatMax");
		register<FloatMinLiteral> ("minimum floating point literal", precedence, "FloatMin");
		register<FloatInfinityLiteral> ("infinity floating point literal", precedence, "Infinity");
		register<FloatNaNLiteral> ("not-a-number floating point literal", precedence, "NaN");
		register<ContinueLiteral> ("continue", precedence, "Continue");
		register<IdentifierStringLiteral> ("identifier string", precedence, "$%P{name}");

		register<StringPiece> ("string contents", 0, "\\(%!% %P{expression}% )%p{literal}");

		/* Selectors */
		register<Fricassee.PassThrough> ("pass-through (Each)", 0, "Each %P{source}");
		register<Fricassee.MergedTuples> ("merged tuple", 0, "%L{sources}{% ,%-}", new Type[] { typeof (Fricassee.Source) });
		/* Results */
		register<Fricassee.AnonymousTuple> ("ordered anonymous tuple", 0, "%P{order} Select %P{result}");
		register<Fricassee.NamedTuple> ("named tuple", 0, "Select %P{result_attr}%-:%-%P{result_value}");
		register<Fricassee.AnonymousTuple> ("anonymous tuple", 0, "Select %P{result}");
		register<Fricassee.Reduce> ("reduce (ordered)", 0, "%P{order} Reduce %P{result} With %P{initial_attr}%-:%-%P{initial}");
		register<Fricassee.Reduce> ("reduce", 0, "Reduce %P{result} With %P{initial_attr}%-:%-%P{initial}");
		/* Order Clauses */
		register<Fricassee.OrderBy> ("orderby", 0, "Order By %P{order}");
		register<Fricassee.Reverse> ("reverse", 0, "Reverse");
		/* Sources */
		register<Fricassee.AttributeSource> ("name", 0, "%P{name}%-:%-Name");
		register<Fricassee.OrdinalSource> ("ordinal", 0, "%P{name}%-:%-Ordinal");
		register<Fricassee.TupleSource> ("expression", 0, "%P{name}%-:%-%P{expression}");
	}
}
internal class Flabbergast.IdentifierParser : GTeonoma.CustomParser<Name> {
	internal enum IdentifierState {
		START,
		CONTAINER,
		CONTAINER_DONE,
		PART,
		JUNK;
		internal GTeonoma.CustomParser.StateType get_state () {
			switch (this) {
			 case IdentifierState.START :
			 case IdentifierState.CONTAINER:
				 return StateType.INTERMEDIATE;

			 case IdentifierState.CONTAINER_DONE:
			 case IdentifierState.PART:
				 return StateType.ACCEPTING;

			 default:
				 return StateType.INVALID;
			}
		}
	}

	private IdentifierState state = IdentifierState.START;
	private int container_position = 1;

	public IdentifierParser () {}

	public override GTeonoma.CustomParser.StateType next_state (unichar input) {
		state = parse_input (input);
		return state.get_state ();
	}

	private IdentifierState parse_input (unichar input) {
		switch (state) {
		 case IdentifierState.START:
			 if (input == 'C') {
				 return IdentifierState.CONTAINER;
			 } else if (input.islower () && input.isalpha ()) {
				 return IdentifierState.PART;
			 } else {
				 return IdentifierState.JUNK;
			 }

		 case IdentifierState.CONTAINER:
			 if (input == "Container"[container_position++]) {
				 if (container_position == "Container".length) {
					 return IdentifierState.CONTAINER_DONE;
				 } else {
					 return IdentifierState.CONTAINER;
				 }
			 } else {
				 return IdentifierState.JUNK;
			 }

		 case IdentifierState.PART:
			 if (input.isdigit () || input.isalpha () || input == '_') {
				 return IdentifierState.PART;
			 } else {
				 return IdentifierState.JUNK;
			 }

		 default:
			 return IdentifierState.JUNK;
		}
	}
	public override Name build_object (string str) {
		return new Name (str);
	}
}
internal class Flabbergast.UriParser : GTeonoma.CustomParser<File.UriReference> {
	internal enum UriState {
		SCHEMA,
		PATH,
		JUNK;
		internal GTeonoma.CustomParser.StateType get_state () {
			switch (this) {
			 case UriState.SCHEMA:
				 return StateType.INTERMEDIATE;

			 case UriState.PATH:
				 return StateType.ACCEPTING;

			 default:
				 return StateType.INVALID;
			}
		}
	}

	private UriState state = UriState.SCHEMA;

	public UriParser () {}

	public override GTeonoma.CustomParser.StateType next_state (unichar input) {
		if (state == UriState.JUNK) {
			return state.get_state ();
		}
		if (input == '.' || input == '+' || input == '-' || input >= '0' && input <= '9' || input >= 'A' && input <= 'Z' || input >= 'a' && input <= 'z') {
			return state.get_state ();
		} else if (input == ':') {
			return (state = UriState.PATH).get_state ();
		} else if (state == UriState.PATH && "~!*'();@&=+$,/?%#[]".index_of_char (input) != -1) {
			return state.get_state ();
		} else {
			return (state = UriState.JUNK).get_state ();
		}
	}
	public override File.UriReference build_object (string str) {
		return new File.UriReference (str);
	}
}
