# General Compiler Design

The compiler, like all compilers, does three phases: parse, analyse, and
generate. The compiler can target multiple platforms because it comes in two
parts: a declarative description of the compiler in Flabbergast and a matching
platform-specific set of templates that generate appropriate code for the
declarative elements. Each platform is able to build a rather different
compiler, but effort is made to keep them conceptually similar.

## Parsing
Declarative descriptions of parsing are straight forward, so this will be
brief. The compiler describes all of the syntax elements in the language in
groups, where a group is a production rule and an element is an alternate
choice in that rule. The description of the parser is a parsing expression
grammar and the generated code should ideally be a packrat parser.

Each syntax group (production rule) corresponds to an abstract class in the
generated code and each element is a concrete subclass. Certain syntax groups
can then provide abstract methods implemented by their members. Some of this is
regular, for example, type checking, and some is more ad hoc, for example, the
real length of time units.

## Analysis
The analysis phase has two goals: separate the program into pieces where each
piece has only local program flow and check the program for correctness. As a
by-product, it is desirable to provide as much type information as possible to
code generation.

Given Flabbergast's dynamic nature, checking the program for correctness and
type information. The most important set is creating _environments_. An
environment is a part of the program where name resolution always returns the
same value. This means that every time the control flow of the program changes
environment, non-local program flow might be required.

First, environments are created in the program. Some nodes share environments
and environments can inherit from one another. There are also masks associated
with some environments. For instance, consider:

    (Let x : X In Y) + Z

In this expression, `X` and `Z` are in the same environment, say _E_, and `Y`
is in an environment that inherits from the environment of `X` and `Z`, say
_F_. Everything that is true in _E_ is true in _F_ unless it concerns names
starting with `x` since we have redefined `x`. Therefore, _F_ masks the name
`x` and backs it with the expression `X`. _Masked_ names are backed by an
expression. Suppose, during type inference it is learnt that `x` must be a
string, then `X` must also return a type string. It is also possible to add
_free_ names which are not backed.

Second, after environments are created, type checking is performed; this
indirectly fill the environments with the names needed by the program (and the
possible types of those names).

Type checking uses a kind of type inference. Hindley-Milner algorithm W is not
appropriate for Flabbergast, mostly due to the fact that Flabbergast does not
have composite or polymorphic types. Instead, each expression must return some
subset of the primitive types. A containing AST node can make _demands_ that an
expression must return a particular set of types. Each expression, in turn,
_ensures_ that it can return a subset of the required types. The assurances
bubble from the outermost AST node (attributes) through the expressions to the
constants and lookups. Each lookup is given a type variable, initially the
union of all types, and every assurance intersects that set with the imposed
set. If any lookup has an empty set of allowed types, an error occurs.

Finally, environments are determined to be dirty or clean. A dirty environment
is one which requires lookups and non-local flow. For instance, in the case of
`Let` above, if `x` is determined to be a string, then the program does not
need non-local flow even when crossing an environment boundary.
