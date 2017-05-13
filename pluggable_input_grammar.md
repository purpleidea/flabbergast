# Pluggable Input Grammar

Flabbergast allows adding new grammar plugins to the language. These plugins parse the input file and convert arbitrary input into Flabbergast values, frames, and templates.

A grammar file starts with at least one export: `Export` _x_ `;` where _x_ is the name of the format that will be available to Flabbergast programs. Grammars can also share symbols produced by other grammars using `Import` _x_ `;`

Following those is a collection of rules of the format: _name_ `=` _expression_ `;` where _name_ is the name that will be used in other rules. All exported rules must be defined. Non-exported names are private to the file and will not be visible to other files.

Rule names must be valid Java identifiers (not necessarily valid Flabbergast identifiers). Rules may cross reference on another and may be defined in any order.

## Expressions
Expressions parse rules and may return values. The types of values that rules can return is a restricted subset of Flabbergast code. Expressions that emit values are considered _returning_.

### Neutral Expressions
These expressions will may or may not be considered returning based on whether any of their arguments are returning.

- `(`_x_`)` -- Parse a sub-expression
- _x_`|`_y_ -- Attempt to parse _x_; if it fails, rewind the parser and attempt to parse _y_. All optional paths must either return or not; they cannot be mixed.
- _x_ _y_ -- Parse one value then another. At most one path may return a value unless in multiple-return context.
- _x_`::`_k_ -- Decorate parsed region with syntax highlighting _k_ which is a highlighting kind
- _name_ -- Parse another rule by name
- _x_ @ _name_ -- Parse _x_ and bind the matched pattern to _name_ for future back references
- _x_`*` -- Match _x_ zero or more times
- _x_`+` -- Match _x_ one or more times
- _x_`?` -- Match _x_ zero or one times; if used with a non-returning argument, is non-returning; if used with a returning argument, is multiple returning

### Non-Returning Parse Expressions
These expressions parse syntax but do not return values. The argument _x_ must also be non-returning.

- `[`_c_`]` -- Match by regular expression character class
- `'`_x_`'` -- Match word exactly
- '^' _name_ -- Parse back reference

### Returning Parse Expressions
These expressions parse syntax, optionally, and return a value. Arguments in lowercase italics are non-returning; arguments in uppercase italics are returning; non-italic arguments are literals.

- `"`x`"` -- returns a literal string
- `Concat(`_CONTENTS_\*`)` -- parse values and return a string concatenation of their results. This is a multiple-return context. 
- `DecimalCodepoint(`n`)` -- parse _n_ decimal digits and convert them to a code point value
- `DecimalCodepoint` -- parse as many decimal digits as possible and convert them to a code point value
- `False` -- returns a Boolean false
- `Flabbergast` -- parse Flabbergast expression and returns its result
- `Float(`_number_`)` -- parse contents as a floating point number
- `Frame {` attributes `}` -- parse attributes and output a frame
- `Frame` names `{` attributes `}` -- parse attributes and lookup _names_, which must be a template, and instantiate with the supplied attributes
- `HexCodepoint(`n`)` -- parse _n_ hexadecimal digits and convert them to a code point value
- `HexCodepoint` -- parse as many hexadecimal digits as possible and convert them to a code point value
- `Inf` -- returns positive infinity
- `Integer(`_number_`,` base`)` -- parse contents as an integer with radix _base_
- `List(`_CONTENTS_`)` -- parses expressions and collect output into a literal list. This is a multiple-return context.
- `Lookup(`_names_`)` -- parse contents and return the contextual lookup of _names_
- `NaN` -- returns a not-a-number
- `NegInf` -- returns negative infinity
- `Null` -- returns null
- `OctalCodepoint(`n`)` -- parse _n_ octal digits and convert them to a code point value
- `OctalCodepoint` -- parse as many octal digits as possible and convert them to a code point value
- `Override` name `{` attributes `}` -- parse attributes and output a template override with attributes
- `Repeat(`_ITEM_`, `_sep_`)` -- parse a list of items defined by _ITEM_ separated by _sep_
- `String(`_contents_`)` -- parse value and return the parsed text as a string
- `Template` _names_ `{` attributes `}` -- parse attributes and lookup _names_, which must be a template, and amend that template with the supplied attributes
- `Template` `{` attributes `}` -- parse attributes and output a template with attributes
- `True` -- returns a Boolean true

Additionally, integral or floating point numbers return themselves.

## Attributes
For frames and templates, attributes may be provided. _Unlike Flabbergast, attributes are semi-colon separated._ In Flabbergast, the order of attributes does not matter; this is not the case in the grammar. Attributes are parsed in the order provided based on the input file.

- _NAME_ `:` _VALUE_ -- Parse an attribute. The name must be a string or integral number; the validity of the attribute name as a Flabbergast identifier will be checked at runtime
- `Repeat(`_NAME_ `:` _VALUE_`)` -- Parses an attribute multiple times. In this case, _NAME_ must produce non-overlapping names, or an error will occur.
- `Repeat(`_NAME_ `:` _VALUE_`, `_sep_`)` -- Parses an attribute multiple times, separated by _sep_. In this case, _NAME_ must produce non-overlapping names, or an error will occur.

## Highlighting Kinds
There are two purposes to highlighting kinds: two format syntax in an editor and to provide a location of the definitions for names. In the latter case, the Java code `class Foo {}` would define `Foo` as a class definition and it should appear in the structural outline of the user's editor. Highlighting kinds are uses unless the name ends in `Def`. So, `Foo` in `class Foo {}` should be annotated as `::ClassDef` while `Foo` in `Foo.someMethod` should be annotated as `::Class`.

Highlighting can be layered. So, an entire block of code might be marked as deprecated, but still have an object definition inside it.

`Character` -- a single character constant (_.e.g._, `'a'` or `&amp;`)
`ClassDef` -- the name of a type which is a class at the point of definition
`Class` -- the name of a type which is a class
`CommentBlock` -- a block comment (_e.g._, `/*...*/` or `<!-- ... -->`)
`CommentLineDash` -- a single line comment start with a `--`
`CommentLineHash` -- a single line comment start with a `#`
`CommentLinePercent` -- a single line comment start with a `%`
`CommentLineSlash` -- a single line comment start with a `//`
`CommentLine` -- a single line comment
`Comment` -- an unusual comment
`ConstantDef` -- the name of a variable which is immutable at the point of definition
`ConstantSyntax` -- the name of a constant which is built into the language (_e.g._, `true`, `null`)
`Constant` -- the name of a variable which is immutable
`ConstructorDef` -- the name of a construction method in a class at the point of definition
`Constructor` -- the name of a construction method in a class
`ControlKeyword` -- a keyword that alters flow control (_e.g._, `continue`, `while`, `return`)
`Deprecated` -- any syntax or reference to code which is deprecated
`Documentation` -- a block of inline documentation
`EnumDef` -- a type which is an enumeration at the point of definition
`EnumMemberDef` -- a value in an enumeration at the point of definition
`EnumMember` -- a value in an enumeration
`Enum` -- a type which is an enumeration
`Escape` -- an escaped character (_e.g._, `\n`)
`EventDef` -- the name of method which is an event handler at the point of definition
`Event` -- the name of method which is an event handler
`FieldDef` -- the name of an object field at the point of definition (only for class-based systems; use `PropertyDef` otherwise)
`Field` -- the name of an object field (only for class-based systems; use `Property` otherwise)
`FunctionDef` --the name of a function not associated with a class at the point of definition
`Function` -- the name of function not associated with a class
`InterfaceDef` -- the name of a type which is an interface in a class-based language at the point of definition
`Interface` -- the name of a type which is an interface in a class-based language
`Keyword` -- any language keywords (use `Operator` preferentially)
`Markup` -- text in markup
`MarkupBold` -- bold text in markup
`MarkupBullets` -- a bulleted list in markup
`MarkupHeading` -- a section heading in markup
`MarkupItalic` -- italic text in markup
`MarkupLink` -- a URL or text that can server as a hyperlink in markup
`MarkupNumbered` -- a numbered list in markup
`MarkupQuote` -- a quoted section of text in markup
`MarkupRaw` -- unformatted text embedded in markup
`MarkupUnderline` -- underlined text in markup
`MethodDef` -- the name of a method associated with a class in an object-oriented language at the point of definition
`Method` -- the name of a method associated with a class in an object-oriented language
`ModuleDef` -- the name of a module or top-level aggregation at the point of definition
`Module` -- the name of a module or top-level aggregation
`NamespaceDef` -- the name of a namespace, Java-style package, or other code aggregation unit at the point of definition
`Namespace` -- the name of a namespace, Java-style package, or other code aggregation unit
`Number` -- a numeric literal
`Operator` -- a language keyword that manipulates data (may be text or symbols)
`ParameterDef` -- the name of a variable that is passed as a parameter to the current scope at the point of definition
`Parameter` -- the name of a variable that is passed as a parameter to the current scope
`PropertyDef` -- the name of a C#-style property or a member in a non-class-based language at the point of definition
`Property` -- the name of a C#-style property or a member in a non-class-based language
`RegExp` -- a regular expression
`StorageModifier` -- a storage modifier (_e.g._, `static`, `final`, `abstract`)
`StringDoubleQuoted` -- a text literal marked by `"`
`StringHereDoc` -- a multi-line text literal marked
`StringInterpolated` -- a text literal with substitution or interpolation
`StringQuoted` -- a text literal with some delimiters
`StringSingleQuoted` -- a text literal marked by `'`
`StringTripleQuoted` -- a text literal marked by `"""`
`String` -- a text literal
`StructDef` -- the name of a complex data-only type at the point of definition
`Struct` -- the name of a complex data-only type
`TypeDef` -- the name of any type, at the point of definition, not covered by `ClassDef`, `EnumDef`, `InterfaceDef`, `StructDef`, or `TypeParameterDef`
`TypeParameterDef` -- the name of a type which can be varied at the point of definition
`TypeParameter` -- the name of a type which can be varied
`Type` -- the name of any type not covered by `Class`, `Enum`, `Interface`, `Struct`, or `TypeParameter`
`VariableDef` -- the name of a variable, at the point of definition, not covered by `ConstantDef`, `FieldDef`, `ParameterDef`, or `PropertyDef`
`Variable` -- the name of a variable not covered by `Constant`, `Field`, `Parameter`, or `Property`
