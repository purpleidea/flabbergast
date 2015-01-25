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

## Code Generation
It should be obvious that code generation is complex. The goal of the KWS VM
specification is to simplify the compiler. The KWS VM instructions are more
similar to the kinds of instructions one would expect on a typical VM, so
providing translations for most of them are straight forward. Iteration and
lookup are the two most complicated. In addition to the KWS VM, the compiler
has several “code generation” templates. These are rules that do not, in
general, generate code in the target VM, but instead control flow in the
compiler itself.

As a simplification, iteration handles both fricassée expressions and
frame/template generation. This limits the scope of what needs to be
implemented on the VM.

The compiler is trying to produce two types of artefacts: native functions that
take a “this” frame, a “container” frame, and a lookup context and return a
value and native functions that take the same arguments plus an additional
original value. These functions are the stuff that language can then use to
assemble frames and do overrides.

The most difficult problem is determining the order in which to do the
computations, since lookup makes this not possible to figure out at run time.
There are effectively two options: convert the entire program to
continuation-passing style or be able to suspend and resume computations. The
intellectually correct answer is to use continuation-passing style (CPS), but,
in practice,
this means repeated copying of variables to new closures, so the
suspend-and-resume method is used (essentially Duff's device). The compiler
template does not make this distinction, given the way it is defined. The
implementation does.

Each of the function types is represented by a class and the fields of the
class hold the state information and intermediate values needed. A function can
await another where it saves its state, appends itself to the “return” callback
of the other function, then yields. Upon completion, the other function queues
all waiting functions for execution and exits. In CLI and the JVM, each one of
these return callbacks is implemented as a method+delegate or anonymous inner
class, respectively. Upon re-entry, the main method of each function reads its
current state and jumps to the appropriate point in the code.

One of the quirky problems is this: CPS is a more natural for the language and
Duff's device is more natural for the VM. The transformation is to use CPS in
the compiler itself to generate the bytecode for the Duff's device. Sorry, the
compiler is a labyrinth of callbacks.

Another issue with the compiler is the linearisation order. A KWS VM
instruction that takes two arguments doesn't explicitly know or care in what
order they are computed; the VM specification assumes they _are_ already
computed. For more complicated instructions, there can be several parameters
and it is possible that one of them may need to exit the current execution
flow, there by disrupting anything on the VM's operand stack (or currently
assigned variables when targeting a SSA system, such as LLVM). To solve this,
each representation of an instruction provide a generator generator, or
`gen_gen`. The `gen_gen` provides the mechanism to assemble the declarative
description of the KWS VM instructions into a form that can be converted into a
compiler. In effect, `gen_gen` templates convert the declarative description of
the behaviour in to continuation-passing style and then the target-language
compiler converts the CPS into a Duff's device.
