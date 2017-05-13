# KWS VM

KWS is the standard virtual machine for running Flabbergast programs on other virtual machines. The purpose of this VM is to provide intermediate semantics that bridge between Flabbergast and the underlying VM. Ultimately, the CLR, JVM, WASM, and LLVM are much more like each other than Flabbergast, so this provides a bridge; something that has semantics usable by Flabbergast, but unified across potential target VMs. Any VM can be a potential target provided:

 - Memory management is provided.
 - The target VM can cope with cyclic references between objects.
 - A co-routine scheduler is available.

As features of KWS, the design ensures:

 - Everything is statically typed.
 - Lookup semantics are handled by native code.
 - Users do not have direct control over scheduling.
 - All values are immutable.

The name of the VM is for the person who inspired it, not an acronym.

## Functions and Blocks
Each KWS programs declare three kinds of functions: a _definition_, which is the computation for the value of an attribute; an _override definition_, which the computation for the value of an attribute that is overriding an existing value; a file-scoped program. There are no global variables or shared state.

### Blocks
Each function is a list of basic blocks. Each block takes a list of parameters, performs a list of operations, and then terminates with one of the terminal instructions: one of the `br` instructions, `error`, or `ret`. This is a variant of single static assignment with block parameters. Swift Intermediate Language also uses block parameters.  Here is an example basic basic blocks:

    # Compute a*a + b and return it
    block name(a:i, b:f):
      as = mul.i a, a
      asf = itof as
      s = add.f asf, b
      sa = ftoa s
      ret sa

Blocks each have a type signature denoted by `(T1 T2 ...)`, (_e.g._, `(ai)`, is a block that has two parameters: an _any_ and an integer). The types are discussed in a later section.

### Functions
Each function declaration specifies the kind of function, a name, and a list of basic blocks used by that function. The first block is assumed to be the entry block. The entry block's name is arbitrary, but the parameter types must match the kind of function.

For a _definition_, it is defined as:

    KWSDefinition <name>[([param1:X[, param2:X[, ...]]])] {
    block entry(context : c):
    ...
    }

For an _override definition_, it is defined as:

    KWSOverride <name>[([param1:X[, param2:X[, ...]]])] {
    block entry(context:c, original:a):
    ...
    }

For a _collector definition_, it is defined as:

    KWSCollector <name>[([param1:X[, param2:X[, ...]]])] {
    block entry(context:c, fricassee:e):
    ...
    }

For a _distributor definition_, it is defined as:

    KWSDistributor <name>[([param1:X[, param2:X[, ...]]])] {
    block entry(context:c):
    ...
    }

For an _accumulator definition_, it is defined as:

    KWSAccumulator <name>[([param1:X[, param2:X[, ...]]])] {
    block entry(context:c, previous:a):
    ...
    }

The names used for definitions must be declared in the same file. In order to use Flabbergast code mixed with KWS, the following declarations are also available:

    Definition <name> : <Flabbergast expression>
    Override <name> +<name>: <Flabbergast expression>
    Collector <name>: <Flabbergast fricassée terminal>

This explicitly names the Flabbergast definitions so that they might be used by the KWS code.

All of these may be prefixed with `Export` and it will be available to other modules.

A file can contain any number of other declarations followed by:

    Root {
    block entry():
    ...
    }

This creates to file-level computation.

Definitions, override definitions, and accumulator definitions can take arguments, useful for capturing lexical variables in containing scope. Root definitions cannot (since they have no containing scope) and Flabbergast definitions cannot since they will look in the environment for all their values. The parameter list is optional if there are no captures. When referencing a definition, if it has no arguments, it can simply be referenced by name:

    KWSDefinition foo { .... }
    Root {
      ...
      fname = s "foo"
      f = new.x.d fname, foo

If it takes captures, it must be first called like a function:

    KWSDefinition foo(count:i) { .... }
    Root {
      ...
      c = i 3
      fdef = foo(c)
      fname = s "foo"
      f = new.x.d fname, fdef

### Flow Control
Each _definition_ or _override definition_ may run continuously until a lookup is required. It must then wait until the lookup's target has finished executing before proceeding. Therefore, the VM divides everything into independently scheduled futures and the _task master_ executes futures until it is finished. Futures terminate with two scenarios: return or fail. Every future has one or more listeners, that is, other futures (or the user) which rely on the result of this future.
If deadlock occurs, then circular evaluation has been detected and the VM should dump all the in-flight lookups for inspection.

## Types
KWS's types and Flabbergast's types do not match. KWS lacks Flabbergast's `Null` type and contains several new types. Flabbergast values are passed in a boxed representation, called an _any_. It can contain a single value of the allowed types or no value, representing Flabbergast's `Null`.

An _identifier_ is a string that follows the attribute naming rules of the Flabbergast language.

The Fricassée operations are built by incrementally constructing a chain.

Instructions that can take multiple type use a signature `{a|b}`.

### Any Type (`a`)
This contains a boxed Flabbergast value: one of `Bin`, `Bool`, `Float`, `Frame`, `Int`, `LookupHandler`, `Template`, or `Null`

Frames contain only boxed values and lookups return only boxed values. There are `br` instructions for unboxing.

### Binary Type (`b`)
This corresponds to the Flabbergast `Bin` type and is an array of bytes.

### Context (`c`)
A list of frames for lookup to search and a `This` frame.

### Definition (`d`)
A definition function declared in this file. Once incorporated into a frame, the scheduler will execute this definition passing in the context corresponding with the contents of the frame.

### Fricassée Chain (`e`)
A step in a Fricassée operation. Each operation instruction appends a new instruction to the growing chain. A step can be reused multiple times.

### Fricassée Grouper (`g`)
A definition for creating `Group` Fricassée chains.

### Float (`f`)
An IEEE-754 floating point number. This corresponds to the Flabbergast `Float` type.

### Integer (`i`)
A signed integer. The size is not specified, but larger is preferred. This corresponds to the Flabbergast `Int` type.

### Collector (`k`)
A collector function declared in this file. Once incorporated into a grouping expression, the scheduler will execute this definition passing in the context corresponding fricassée source.

### Lookup Handler (`l`)
A name resolution algorithm. This corresponds to the Flabbergast `Int` type.

### Accumulator Definition (`m`)
An accumulator definition function declared in this file. Once incorporated into a reducing operation, this definition may be invoked many times with the different contexts seen by the fricassée chain with the previous Any value produced. The builders produced will be used to form an output frame or context for downstream operations.

### Name List (`n`)
A list of names to be resolved by a lookup handler.

### Override Definition (`o`)
An override definition function declared in this file. Once incorporated into a frame, the scheduler will execute this definition passing in the context corresponding with the contents of the frame and the original value.

### Fricassée Zipper (`p`)
A definition for creating Fricassée chains by combining the attributes of one or more frames.

### Frame (`r`)
A context, unique identifier, containing frame, map from identifiers to boxed values (or, more specifically, futures yielding boxed values). This corresponds to the Flabbergast `Frame` type.

### String (`s`)
A block of Unicode text and always addressed by codepoint. This corresponds to the Flabbergast `Str` type.

### Template (`t`)
A context and a map from identifiers to boxed values or functions capable of generating boxed values. This corresponds to the Flabbergast `Template` type.

### Distributor (`u`)
A distributor definition function declared in this file. This is used to flatten in a fricassée operation.

### Fricassée Window (`w`)
Computes the length or next window during a windowed grouping operation.

### Attribute Builder (`x`)
Holds new attributes to be included in a frame or template.

### Boolean (`z`)
A value that is true or false.

## Terminal Instructions

### Jump to Block
Passes control to another block. The correct number and type of parameters must be specified for the block being called. A block has access to only the values passed as parameters and those values defined in the least common ancestor of all the blocks in its callers, excluding itself.

    br <name:([t1 [t2 [...]]])> ([<p1:t1>[, <p2:t2>[, ...]]])

Examples:

    br foo()
    br bar(x, y)

### Unboxing Dispatch (`br.a`)
Branch to another block based on the type of an _any_ value. Blocks for all target types do not need to be provided. Any that are missing, will produce an error: Got value of type _provided_, but expected one of _types for provided blocks_.

    br.a <value:a>[, <targetb:([tb1 [tb2 [...] ]]b)>([<pb1:tb1>[, <pb2:tb2>[, ...]]])][, <targetz:([tz1 [tz2 [...] ]]z)>([<pz1:tz1>[, <pz2:tz2>[, ...]]])][, <targetf:([tf1 [tf2 [...] ]]f)>([<pf1:tf1>[, <pf2:tf2>[, ...]]])][, <targetr:([tr1 [tr2 [...] ]]r)>([<pr1:tr1>[, <pr2:tr2>[, ...]]])][, <targeti:([ti1 [ti2 [...] ]]i)>([<pi1:ti1>[, <pi2:ti2>[, ...]]])][, <targetl:([tl1 [tl2 [...] ]]l)>([<pl1:tl1>[, <pl2:tl2>[, ...]]])][, <targets:([ts1 [ts2 [...] ]]s)>([<ps1:ts1>[, <ps2:ts2>[, ...]]])][, <targett:([tt1 [tt2 [...] ]]t)>([<pt1:tt1>[, <pt2:tt2>[, ...]]])][, <targetnull:([t1 [t2 [...] ]])>([<p1:t1>[, <p2:t2>[, ...]]])] ["<error context>"]

The order of the target blocks does not matter.

Examples:

    br.a v, handle_bin(), handle_str(x), handle_int(x, y)

### Numeric Unboxing Dispatch (`br.aa`)
Unbox two _any_ values, which must be either a floating point number or integer and dispatch based on their types. If one is floating point and the other is an integer, upgrade the integer.

    br.aa <left:a>, <right:a>, <int_target:([t1[ t2[ ...]]ii])>([<p1:t1>[, <p2:t2>[, ...]]]), <float_target:([s1[ s2[ ...]]]ff)>([<q1:s1>[, <q2:s2>[, ...]]])

Examples:

    br.aa x, y, sum_i(), sum_f()

### Integer and Numeric Unboxing Dispatch (`br.ia`)
Unbox one _any_ value, which must be either a floating point number or integer and dispatch based on the types. If the unboxed value is floating point, upgrade the provided integer.

    br.ia <left:i>, <right:a>, <int_target:([t1[ t2[ ...]]]ii)>([<p1:t1>[, <p2:t2>[, ...]]]), <float_target:([s1[ s2[ ...]]]ff)>([<q1:s1>[, <q2:s2>[, ...]]])

Examples:

    br.ia x, y, sum_i(), sum_f()

### Floating Pointer and Numeric Unboxing Dispatch (`br.fa`)
Unbox one _any_ value, which must be either a floating point number or integer and dispatch based on the types. If the unboxed value is an integer, upgrade the integer.

    br.fa <left:f>, <right:a>, <target:([t1[ t2[ ...]]]ff)>([<p1:t1>[, <p2:t2>[, ...]]])

### Conditional Dispatch (`br.z`)
Switch to one of two blocks based on a conditional value. The target blocks may take any number of arguments, which must be provided.

    br.z <cond:z>, <when_true:([t1 [t2 [...]]])>([<p1:t1>, [<p2:t2>[, ...]]]), <when_false:([s1[ s2 [...]]])>([<q1:s1>, [<q2:s2>[, ...]]])

Examples:

    br.z is_ok, ok(), fail(constraint_type)

### Raimse an Error (`error`)
Produce a user-visible error message. Any other functions waiting on the result from this one must never execute.

    error <message : s>

### Return Value (`ret`)
Passes control to the listeners and provide the value given as an argument. In definitions, override definitions, collector definitions, and the root, the following return is available:

    ret <value:a>

In accumulator definitions:

    ret <value:a>, <builder:x>[, ...]

In distributor definitions:

    ret <value:e>

## Block Instructions

### Add Floating Point Numbers (`add.f`)
Add two floating point numbers in the standard IEEE-754 way.

    <result:f> = add.f <left:f>, <right:f>

### Add Integral Numbers (`add.i`)
Add two integral numbers.

    <result:i> = add.i <left:i>, <right:i>

### Add Literal Names to Name List (`add.n`)
Add predefined names to a name list.

    <result:n> = add.n <source:n>, ("<name1>", ...)

### Add Type Name to Name List (`add.n.a`)
Add the name of the type of the value in box to a name list.

    <result:n> = add.n.a <source:n>, <value:a>

| Type            | Name             |
|-----------------|------------------|
| `Bin`           | `bin`            |
| `Bool`          | `bool`           |
| `Float`         | `float`          |
| `Frame`         | `frame`          |
| `Int`           | `int`            |
| `LookupHandler` | `lookup_handler` |
| `Null`          | `null`           |
| `Str`           | `str`            |
| `Template`      | `template`       |

### Add Ordinal Name to Name List (`add.n.i`)
Add an ordinal name to a name list.

    <result:n> = add.n.i <source:n>, <ordinal:i>

### Add Frame Values to Name List (`add.n.r`)
Add values from a frame to a name list. The values in the frame are taken as either strings, which are strings or integers; otherwise an error occurs.

    <result:n> = add.n.r <source:n>, <frame:r>

The values in the frame are taken in order.

### Add String Name to Name List (`add.n.s`)
Add an variable string name to a name source.

    <result:n> = add.n.s <source:n>, <name:s>

### Create Adjacent Grouper with Floating-Point Number Classifier (`adjacent.f`)
Create a grouper which classifies contexts using a floating-point number. Any contiguous run of the same value will be grouped together.

    <result:g> = adjacent.f <name:s>, <key:d>

### Create Adjacent Grouper with Integer Classifier (`adjacent.i`)
Create a grouper which classifies contexts using an integer. Any contiguous run of the same value will be grouped together.

    <result:g> = adjacent.i <name:s>, <key:d>

### Create Adjacent Grouper with String Classifier (`adjacent.s`)
Create a grouper which classifies contexts using a string. Any contiguous run of the same value will be grouped together.

    <result:g> = adjacent.s <name:s>, <key:d>

### Create Adjacent Grouper with Boolean Classifier (`adjacent.z`)
Create a grouper which classifies contexts using a Boolean value. Any contiguous run of the same value will be grouped together.

    <result:g> = adjacent.z <name:s>, <key:d>

### Create Always-include Grouper with Floating-Point Number Classifier (`alwaysinclude.f`)
Create a grouper which classifies contexts using a floating-point number. Contexts with the same classification will be grouped together except when the classifier returns null, in which case, the context will be placed in all output groups.

    <result:g> = alwaysinclude.f <name:s>, <key:d>

### Create Always-include Grouper with Integer Classifier (`alwaysinclude.i`)
Create a grouper which classifies contexts using an integer. Contexts with the same classification will be grouped together except when the classifier returns null, in which case, the context will be placed in all output groups.

    <result:g> = alwaysinclude.i <name:s>, <key:d>

### Create Always-include Grouper with String Classifier (`alwaysinclude.s`)
Create a grouper which classifies contexts using a string. Contexts with the same classification will be grouped together except when the classifier returns null, in which case, the context will be placed in all output groups.

    <result:g> = alwaysinclude.s <name:s>, <key:d>

### Create Always-include Grouper with Boolean Classifier (`alwaysinclude.z`)
Create a grouper which classifies contexts using a Boolean value. Contexts with the same classification will be grouped together except when the classifier returns null, in which case, the context will be placed in all output groups.

    <result:g> = alwaysinclude.z <name:s>, <key:d>

### Aggregate Groupers (`and.g`)
Produce a new grouper that would aggregate the existing groupers in the order specified.

    <result:g> = and.g (<grouper:g>, ...]

### Bit-wise AND (`and.i`)
Produce the bit-wise AND of the arguments.

    <result:i> = and.i <left:i>, <right:i>

### Convert Boxed Value to String (`atos`)
Convert a boxed value to a string, if possible.

    <result:s> = atos <value:a>

This makes use of the appropriate `Xtos` function on the value inside the box. If the value is not `Bool`, `Float`, `Int` or `Str`, an error occurs.

### Check Boxed Value Type (`atoz`)
Checks whether the box contains a value with one of the types listed or is empty if `-` is included.

    <result:z> = atoz <value:a>[, b][, f][, i][, l][, r][, t][, s][, z][, -]

### Create Boundary-driven Grouper (`boundary`)
Creates a grouper that evaluates `definition` in each context, which must return a Boolean, and creates a new group whenever the definition returns true. If `trailing` is true, then the context that produced the true value will be placed in the previous group (_i.e._, it signals that it is the last item in that group); if false, then the context that produced the true value will be placed in the next group (_i.e._, it signals that it is the first item in a new group).

     <result:g> = boundary <definition:d>, <trailing:z>

### Box Binary Value (`btoa`)
Create a box containing the provided binary value.

    <result:a> = btoa <value:b>

### Create Buckets over Floating-point Key Space (`buckets.f`)
Creates a grouper which evaluates `definition` in each context, which must return a floating-point number, sorts the keys from smallest to largest, and divides the keys into `count` buckets and places the contexts in the corresponding buckets. This divides the keys even, not the context (_i.e._, if there were 10 contexts, 9 of which returned one value and 1 of which returned another, for `count` > 2, only two buckets would ever be produced).

    <result:g> = buckets.f <definition:d>, <count:i>

### Create Buckets over Integer Key Space (`buckets.i`)
Creates a grouper which evaluates `definition` in each context, which must return an integer, sorts the keys from smallest to largest, and divides the keys into `count` buckets and places the contexts in the corresponding buckets. This divides the keys even, not the context (_i.e._, if there were 10 contexts, 9 of which returned one value and 1 of which returned another, for `count` > 2, only two buckets would ever be produced).

    <result:g> = buckets.i <definition:d>, <count:i>

### Create Buckets over String Key Space (`buckets.f`)
Creates a grouper which evaluates `definition` in each context, which must return a string, sorts the keys from smallest to largest, and divides the keys into `count` buckets and places the contexts in the corresponding buckets. This divides the keys even, not the context (_i.e._, if there were 10 contexts, 9 of which returned one value and 1 of which returned another, for `count` > 2, only two buckets would ever be produced).

    <result:g> = buckets.s <definition:d>, <count:i>

### Evaluate Definition (`call.d`)
Evaluate a definition using the provided context.

     <result:a> = call.d <definition:d>, <context:c>

### Evaluate Override Definition (`call.o`)
Evaluate a override definition using the provided context and original value.

     <result:a> = call.o <definition:d>, <context:c>, <original:a>

### Concatenate Fricassée Chains (`cat.e`)
Create a new fricassée chain with iterates over all the output from `chain1`, then iterates over all the output from `chain2`, and so on.

    <result:e> = cat.e <context:c>, <chain1:e>, <chain2:e>[, <chain3:e>[, ...]]

### Create Partial Closure over Collector (`cat.ke`)
Create a new definition from a collector by binding the fricassée chain.

     <result:d> = cat.ke <collector:k>, <chain:e>

### Concatenate Frames (`cat.r`)
Create a new frame with all the elements of `first` followed by all the elements of `second`, numbering the items.

    <result:r> = cat.r <context:c>, <first:r>, <second:r>

### Concatenate Frame onto Context and Update This (`cat.rc`)
Create a new list containing the provided frame as the first element and the elements in the provided context as the remaining elements. If the `tail` contains the same frame as `head`, it may be removed from the output. The `This` for the result is set to `head`.

    <result:c> = cat.rc <head:r>, <tail:c>

### Concatenate Strings (`cat.s`)
Create a new string of `first` followed by `second`.

    <result:s> = cat.s <first:s>, <second:s>

### Group into Chunks (`chunk.e`)
Create a discriminator for a grouping operation that assigns items to groups in blocks based on their position.

    <result:g> = chunk.e <width:i>

If items `123456789` was processed with `chunk.e 4`, the groups would be assigned as `AAAABBBBC`.

### Compare Floating Point Numbers (`cmp.f`)
Return the sign of the difference of `left` and `right`, or zero is they are equal. Behaviour is implementation-defined if either value is not-a-number.

    <result:i> = cmp.f <left:f>, <right:f>

### Compare Integers (`cmp.i`)
Compare `left` and `right`, returning the sign of the difference, or zero if they are identical.

    <result:i> = cmp.i <left:i>, <right:i>

### Collate Strings (`cmp.s`)
Determine if `left` collates before, the same, or after `right` and set the result to be -1, 0, or 1, respectively.

    <result:i> = cmp.s <left:s>, <right:s>

### Compare Booleans (`cmp.z`)
Compare `left` and `right`, returning 1 if `left` is false and `right` is true, -1 if `left` is true and `right` is false, or zero if they are identical.

    <result:i> = cmp.z <left:z>, <right:z>

### Contextual Lookup Handler (`contextual`)
Gets the standard contextual lookup scheme defined the Flabbergast Language.

    <result:l> = contextual

### Create Count Window (`count.w`)
Create a window that handles `count` items.

    <result:w> = count.w <count:i>

### Cross-table Grouper with Floating-Point Number Classifier (`crosstab.f`)
Create a grouper that will classify each context using a floating-point number and then create groups that contains one context from each classification.

    <result:g> = crosstab.f <key:d>

### Cross-table Grouper with Integer Classifier (`crosstab.i`)
Create a grouper that will classify each context using an integer and then create groups that contains one context from each classification.

    <result:g> = crosstab.i <key:d>

### Cross-table Grouper with String Classifier (`crosstab.s`)
Create a grouper that will classify each context using a string and then create groups that contains one context from each classification.

    <result:g> = crosstab.s <key:d>

### Cross-table Grouper with Boolean Classifier (`crosstab.z`)
Create a grouper that will classify each context using a Boolean value and then create groups that contains one context from each classification.

    <result:g> = crosstab.z <key:d>

### Containing Frame for Context (`ctr.c`)
Get the `This` value from a context.

    <result:r> = ctr.c <value:c>

### Containing Frame for Frame (`ctr.r`)
Extract the containing frame of a frame. This is the `This` frame of the context provided during creation. If there was no containing frame, it returns itself.

    <result:r> = ctr.r <frame:r>

### Context for Frame (`ctxt.r`)
Extract the context embedded in a frame. The calling context is provided in `ctxt` and this determines what private attributes will be visible in the resulting context.

    <result:c> = ctxt.r <ctxt:c>, <frame:r>

### Debug Definition (`debug.d`)
This evaluates a definition using the provided context. When this instruction is evaluated, the debugger maybe invoked where the user can choose to evaluate the provided definition and return that value or the user may choose to return any other value interactively. If the debugger is not available, this will have the same behaviour as `call.d`.

     <result:a> = debug.d <definition:d>, <context:c>

### Discriminate by Floating Point Number (`disc.g.f`)
Create a floating point discriminator during a grouping operation. The value is determined by the provided definition and the type must match or an error occurs.

    <result:g> = disc.g.f <name:s>, <getter:d>

### Discriminate By Integer (`disc.g.i`)
Create an integer discriminator during a grouping operation. The value is determined by the provided definition and the type must match or an error occurs.

    <result:g> = disc.g.i <name:s>, <getter:d>

### Discriminate by String (`disc.g.s`)
Create a string discriminator during a grouping operation. The value is determined by the provided definition and the type must match or an error occurs.

    <result:g> = disc.g.s <name:s>, <getter:d>

### Discriminate by Boolean (`disc.g.z`)
Create a Boolean discriminator during a grouping operation. The value is determined by the provided definition and the type must match or an error occurs.

    <result:g> = disc.g.z <name:s>, <getter:d>

### Disperse Value using Ordinal (`disperse.i`)
Place a value in a frame's _gather_ buckets so that it can be picked up later.

     disperse.i <ordinal:i>, <value:a>

This will place the value in the frame and all of it containing frames. Frames allow dispersements only while attributes are still being computed for the frame (or it's descendants). Therefore, this should only be called on the `This` frame (or it's containers), otherwise an error may occur.

Any frame which is a candidate for dispersements is not a candidate for gathering.

### Disperse Value using Name (`disperse.s`)
Place a value in a frame's _gather_ buckets so that it can be picked up later.

     disperse.s <name:s>, <value:a>

This will place the value in the frame and all of it containing frames. Frames allow dispersements only while attributes are still being computed for the frame (or it's descendants). Therefore, this should only be called on the `This` frame (or it's containers), otherwise an error may occur.

Any frame which is a candidate for dispersements is not a candidate for gathering.

### Floating Point Division (`div.f`)
Divides `left` by `right`.

    <result:f> = div.f <left:f>, <right:f>

### Integer Division (`div.i`)
Divides `left` by `right`, and set the result to the dividend.

    <result:i> = div.i <left:i>, <right:i>

### Drop Fricassée Results by Condition (`drop.ed`)
Discard elements from the chain as long as `clause` returns true. Once it returns false, all remaining contexts will be kept. An error occurs if it returns a non-boolean value.

    <result:e> = drop.ed <source:e>, <clause:d>

### Discard First Fricassée Results by Constant (`drop.ei`)
Discard up to `count` elements from the start of the chain.

    <result:e> = drop.ei <source:e>, <count:i>

### Drop Definition (`drop.x`)
Remove the definition of an attribute. If this builder is used sequentially after a builder that defines this name, the previous definition will be discarded.

    <result:x> = drop.x <name:s>

### Discard Last Fricassée Results by Constant (`dropl.ei`)
Discard up to `count` elements from the end of the chain.

    <result:e> = dropl.ei <source:e>, <count:i>

### Create a Window with a Floating-Point Range (`duration.f`)
Create a window that moves by a range, `duration`. The value for each item is calculated using `definition`, which must return a floating-point number.

    <result:w> = duration.f <definition:d>, <duration:f>

### Create a Window with an Integer Range (`duration.i`)
Create a window that moves by a range, `duration`. The value for each item is calculated using `definition`, which must return an integer.

    <result:w> = duration.i <definition:d>, <duration:i>

### Reduce Fricassée Chain (`etoa.ao`)
Reduce the Fricassée chain to a single value. The override function will be applied to the previous value, initially `initial` for each context produced by the chain.

    <r:a> = etoa.ao <source:e>, <initial:a>, <reducer:o>

### Single Null From Fricassée Chain (`etoa.d`)
Extract single value from the fricassée chain.

    <r:a> = etoa.d <source:e>, <extractor:d>

### Single From Fricassée Chain (`etoa.dd`)
Extract single value from the fricassée chain with alternate if not univalued.

    <r:a> = etoa.d <source:e>, <extractor:d>, <alternate:d>

### Create Yielding Definition (`etod`)
Create a definition that, when instantiated, will yield the next value in from the fricassée chain. If there are no more values, an error occurs.

    <result:d> = etod <source:e>, <compute_value:d>

### Create Yielding Definition or Empty (`etod.a`)
Create a definition that, when instantiated, will yield the next value in from the fricassée chain. If there are no more values, a default value is used.

    <result:d> = etod.a <source:e>, <compute_value:d>, <empty:a>

### Create Fricassée Grouping (`etoe.g`)
Group the items in a fricassée chain using the groupers provided to determine which items belong in the same group and then create the feed those items through the collectors specified by groupers.

    <result:e> = etoe.g <source:e>, <grouper:g>[, <grouper:g>[, ...]]

### Accumulate (`etoe.m`)
Accumulate an additional value during fricassée. This supplies Flabbergast's `Accumulate` clause. It works like a reduce operation, but yields the intermediate results. The accumulator is evaluated in each context of the fricassée chain with the previous value (or `initial` for the first). A new frame is created using the builders returned in the input fricassée context and this frame is used as the context for downstream operations.

    <result:e> = acc.e <source:e>, <initial:a>, <accumulator:m>

### Flatten Fricassée  (`etoe.u`)
Flatten values in a fricassée chain using the supplied flattening steps.

    <result:e> = etoe.u <source:e>, <flattener:u>

### Count Fricassée Chain (`etoi`)
Count the number of items processed in this chain.

    <result:i> = etoi <source:e>

### Cumulatively Reduce/Collect Frame with Anonymous Attributes (`etor.am`)
Collect the items into a frame with the attributes given by the accumulator. As with `etoa.ao`, the accumulator will be invoked in the context of the fricassée chain and the previous value emitted by the accumulator (or `initial` for the first). At every step, the builders returned by the accumulator are collected, in order, and used to build a frame, which is returned as output. If the chain produces no items, the frame will be empty.

    <result:r> = etor.am <source:e>, <initial:a>, <accumulator:m>

### Collect Frame with Anonymous Attributes (`etor.i`)
Collect the items into a frame with the attributes numbered based on the order in which they were received. The attribute value is computed using `compute_value`.

    <result:r> = etor.i <source:e>, <compute_value:d>

### Collect Frame with Named Attributes (`etor.s`)
Collect the items into a frame with the attribute names provided. The attribute name generator, `compute_name`, may return either a `Str` or an `Int`. If the same identifier is produced more than once, an error will occur. The attribute value is computed using `compute_value`.

    <result:r> = etor.s <source:e>, <compute_name:d>, <compute_value:d>

### Access External URI (`ext`)
Load external data. Since the URI is fixed, this can be thought of as information to the dynamic loader rather than part of the execution.

    <r:a> = ext "<uri>"

### Floating Point Constant
Returns the floating-point number provided at compile-time.

    <result:f> = f <value>

### Filter Fricassée Chain (`filt.e`)
Eliminate some elements from further processing. The result of `clause` must be `Bool`, otherwise an error occurs. If it returns true, the element is passed to downstream processors. If false, it is discarded from further processing.

    <result:e> = filt.e <source:e>, <clause:d>

### Box Floating Point Value (`ftoa`)
Create a box containing the provided floating point value.

    <result:a> = ftoa <value:f>

### Convert Floating Point to Integer (`ftoi`)
Convert a floating-point number to an integer by truncation.

    <result:i> = ftoi <value:f>

### Convert Floating Point to String (`ftos`)
Create a string representation of a floating-point number.

    <result:s> = ftos <value:f>

### Gather Items from a Frame by Ordinal (`gather.i`)
Get values placed in a frame's _gather_ buckets.

    <result:r> = gather.i <frame:r>, <ordinal:i>

This operation must wait for all attributes in a frame are evaluated to ensure that all dispersements have completed. Therefore, gathering from `This` or any of its containers will cause deadlock. Gathering should only be attempted on frames which are siblings or descendants of siblings.

Any frame which is a candidate for dispersements is not a candidate for gathering.

### Gather Items from a Frame by Name (`gather.s`)
Get values placed in a frame's _gather_ buckets.

    <result:r> = gather.s <frame:r>, <name:s>

This operation must wait for all attributes in a frame are evaluated to ensure that all dispersements have completed. Therefore, gathering from `This` or any of its containers will cause deadlock. Gathering should only be attempted on frames which are siblings or descendants of siblings.

Any frame which is a candidate for dispersements is not a candidate for gathering.

### Integer Constant (`i`)
Returns the integral constant provided at compile-time.

    <result:i> = i <value>

### Identifier for Frame (`id`)
Extract a unique identifier from a frame.

    <result:s> = id <frame:r>

### Import (`import`)
Import a function from another file. The type of the function is specified as `<X>`. If no package has exported the function, an error will occur.

    <result:X> = import.<X> <name>, <arg1:T1>, ...

### Infinity Constant (`inf.f`)
Produce a positive infinite value representable as a floating-point number.

    <result:f> = inf.f

### Finite Check (`isfinite`)
Check if a floating-point number is finite.

    <result:z> = isfinite <value:f>

### Not-a-number Check (`isnan`)
Check if a floating-point value is a number.

    <result:z> = isnan <value:f>

### Box Integer Value (`itoa`)
Create a box containing the provided integer value.

    <result:a> = itoa <value:i>

### Convert Integer to Floating-point (`itof`)
Create a floating-point representation of an integral number.

    <result:f> = itof <value:i>

### Convert Integer to String (`itos`)
Create a string representation of an integral number.

    <result:s> = itos <value:i>

### Convert Integer to Boolean (`itoz`)
Convert integer `value` to Boolean by comparing it to `reference`, which is an integer literal. If `value` and `reference` are equal, true is returned, false otherwise.

    <result:z> = itoz <reference>, <value:i>

### Convert Collector into Lookup Handler (`ktol`)
Create a new lookup handler with the same _exact_ explorer as contextual lookup and a lookup collector that uses a fricassée collector. Each value discovered by the explorer will be made available as `name` in the collector `definition`. The context provided to `definition` will be `context` rather than the context in which lookup occurs.

    <result:l> = ktol <name:s>, <context:c>, <definition:k>

### Binary Length (`len.b`)
Returns the number of bytes in a Bin.

    <result:i> = len.b <value:b>

### String Length (`len.s`)
Returns the number of Unicode characters in a string.

    <length:i> = len.s <str:s>

### Fricassée Let Clause (`let.e`)
Adds new attributes to the downstream operations in a chain. This builder must not contain _override definitions_ as it does not override the existing data.

    <result:e> = let.e <source:e>, <builder:x>[, <builder:x>[, ...]]

### Perform Lookup: Contextual (`lookup`)
Performs contextual lookup from a set of fixed names.

    <r:a> = lookup <context:c>, "<name1>"[, "<name2>"[, ...]]

This would be the same as:

    s = new.n
    na s, "name1", "name2", ...
    cl = contextual
    r = llookup cl, context, s

It is provided for convenience and efficiency.

### Perform Lookup (`llookup`)
Perform lookup over a list of frames.

    <r:a> = lookup.l <handler:l>, <context:c>, <names:n>

### Box Lookup Handler Value (`ltoa`)
Create a box containing the provided lookup handler value.

    <result:a> = ltoa <value:l>

### Maximum Floating Point Value (`max.f`)
Produce the largest value representable as a floating-point number.

    <result:f> = max.f

### Maximum Integer (`max.i`)
Produce the largest value representable as a integral number.

    <result:i> = max.i

### Boolean True (`max.z`)
Set the result to Boolean true.

    <result:z> = max.z

### Minimum Floating Point Value (`min.f`)
Produce the smallest value representable as a floating-point number.

    <result:f> = min.f

### Minimum Integer (`min.i`)
Produce the smallest value representable as a integral number.

    <result:i> = min.i

### Boolean False (`min.z`)
Set the result to Boolean false.

    <result:z> = min.z

### Floating-point Remainder (`mod.f`)
Divide `left` by `right` and set the result to the remainder.

    <result:f> = mod.f <left:f>, <right:f>

### Integer Remainder (`mod.i`)
Divide `left` by `right` and set the result to the remainder.

    <result:i> = mod.i <left:i>, <right:i>

### Fricassée from Sequence (`mtoe`)
Create an infinite sequence by applying the same function to the input. This is the dual of `Reduce`. The accumulator is evaluated in the calling context with the previous value (or `initial` for the first). A new frame is created using the builders returned in the calling context and this frame is used as the context for downstream operations.

    <result:e> = mote <context:c>, <initial:a>, <generator:m>

### Multiply Floating Point Values (`mul.f`)
Multiply `left` and `right`.

    <result:f> = mul.f <left:f>, <right:f>

### Multiply Integers (`mul.i`)
Multiple `left` and `right`.

    <result:i> = mul.i <left:i>, <right:i>

### Non-a-Number Constant (`nan.f`)
Produce a not-a-number value representable as a floating-point value.

    <result:f> = nan.f

### Negate Floating Point (`neg.f`)
Produce the additive inverse of `value`.

    <result:f> = neg.f <value:f>

### Negate Integer (`neg.i`)
Produce the additive inverse of `value`.

    <result:i> = neg.i <value:i>

### Create Collecting Grouper (`new.g`)
Creates a Fricassée grouper with a collection operation for the output for this grouping operation.

    <result:g> = new.g <name:s>, <collector:k>

### Create Fixed-Value Grouper (`new.g.a`)
Creates a fricassée grouper which acts like a grouping key, but sets the name to a fixed value. In the case of the `powerset` operation, every grouper included must be converted into a null version of itself to define the same variables when in the not-aggregating state. In order to create similar operations, this allows creating fixed keys.

    <result:g> = new.g.a <name:s>, <value:a>

### Create Fricassée Zipping (`new.p`)
Create a new fricassée chain by merging the attributes of frames. If `intersect` is true, only attribute names that are shared among all frames in the zippers will be considered; if false, then all attributes not present in a frame provided by `new.p.r` will be replaced with `nil.a`.

    <result:e> = new.p <context:c>, <intersect:z>, (<zipper:p>, ...)

### Create Zipper for Ordinal (`new.p.i`)
Create a fricassée zipper that binds the current iteration ordinal to a name.

    <result:p> = new.p.i <name:s>

### Create Zipper for Frame (`new.p.r`)
Create a fricassée zipper that includes a frame in the iteration process.

    <result:p> = new.p.r <name:s>, <frame:r>

### Create Zipper for Name (`new.p.s`)
Create a fricassée zipper that binds the current iteration attribute name to a name.

    <result:p> = new.p.s <name:s>

### Create Frame from Builders (`new.r`)
Create frame whose attributes will be those in the supplied builders. The builders are applied in order. That is, later builders can override values from previous ones. The frame will have the gatherers specified here and from any templates provided. Gatherers must be integers or strings.

The new frame will have a context made of itself, followed by the context provided, followed by the context of any builders provided. If `self_is_this` is true, then `This` in the created context will refer to the newly created frame. Otherwise, `This` from the provided frames will be maintained.

    <result:r> = new.r <self_is_this:z>, <context:c>, (<gatherer:{i|s}, ...), (<builder:{r|t|x}>, ...)

### Create Frame from Range (`new.r.i`)
Create a frame containing integral numbers, over the range specified, assigned to keys from 1 to the number of items.

    <result:r> = new.r.i <context:c>, <start:i>, <end:i>

### Create Template (`new.t`)
Create a new definition builder from zero or more existing builders. The builders are gathered in the order supplied. If one of the builders sets a `Definition`, a subsequent builder will be able to apply a `DefinitionOverride` to it. If a `DefinitionOverride` is encountered and no previous `Definition` or `Any` is available, then an error will be produced: Cannot override not existent attribute “_name_”. The template will have the gatherers specified here and from any templates provided. Gatherers must be integers or strings.

    <result:t> = new.t <context:c>, (<gatherer:{i|s}, ...), (<builder:{r|t|x}>, ...)

### Create Builder for Value by Ordinal (`new.x.ia`)
Creates a builder with an ordinal name set to a fixed value.

    <result:x> = new.x.ia <ordinal:i>, <value:a>

### Create Builder for Value by Name (`new.x.sa`)
Creates a builder with a string name set to a fixed value.

    <result:x> = new.x.sa <name:s>, <value:a>

### Create Builder for Definition (`new.x.sd`)
Creates a builder with a string name set to a definition.

    <result:x> = new.x.d <name:s>, <definition:d>

### Create Builder for Definition Override (`new.x.so`)
Creates a builder with a string name set to an override definition.

    <result:x> = new.x.o <name:s>, <override:o>

### Empty Any Value (`nil.a`)
Create a box containing no value.

    <result:a> = nil.a

### Empty Context (`nil.c`)
Create an empty context.

    <result:c> = nil.c

### Empty Name List (`nil.n`)
Create a name source with no names in it.

    <result:n> = nil.n

### Empty Frame (`nil.r`)
Get a frame containing no attributes. This frame will always have the id `empty`.

    <result:r> = nil.r

### Empty Window (`nil.w`)
Get a window that, when used for length, will consume all the item, and, when used for next, will start at the end of the previous window.

    <result:w> = nil.w

### Grouper Null Replacement (`not.g`)
Produce a new grouper which defines the same attributes but replaces their definitions with `nil.a` as if by `new.g.a`.

    <result:i> = not.g <value:i>

### Integer Bit-wise Complement (`not.i`)
Produce the bit-wise NOT of `value`.

    <result:i> = not.i <value:i>

### Boolean Complement (`not.z`)
Set the result to Boolean complement.

    <result:z> = not.z <value:z>

### Alternate Groupers (`or.g`)
Produce a new grouper that splits the input contexts to every grouper provided and then aggregates the resulting groups.

    <result:i> = or.g (<grouper:g>, ...)

### Integer Bit-wise OR (`or.i`)
Produce the bit-wise OR of `left` and `right`.

    <result:i> = or.i <left:i>, <right:i>

### Order By Clause: Floating Point (`ord.e.f`)
Reorder the results of a fricassée chain by computing a value, which must be a floating point value (or integer) and then reordering the input provided by the source based on `cmp.f`. If `ascending` is true, it is given in this order; if false, the reverse of this order. If two items produce the same value, their relative order will be preserved.

    <result:e> = ord.e.f <source:e>, <ascending:z>, <clause:d>

### Order By Clause: Integer (`ord.e.i`)
Reorder the results of a fricassée chain by computing a value, which must be an integer and then reordering the input provided by the source based on `cmp.i`. If `ascending` is true, it is given in this order; if false, the reverse of this order. If two items produce the same value, their relative order will be preserved.

    <result:e> = ord.e.i <source:e>, <ascending:z>, <clause:d>

### Order By Clause: String (`ord.e.s`)
Reorder the results of a fricassée chain by computing a value, which must be Boolean, floating point, integer, or string, and then reordering the input provided by the source based on `cmp.s`. If `ascending` is true, it is given in this order; if false, the reverse of this order. If two items produce the same value, their relative order will be preserved.

    <result:e> = ord.e.s <source:e>, <ascending:z>, <clause:d>

### Order By Clause: Boolean (`ord.e.z`)
Reorder the results of a fricassée chain by computing a value, which must be Boolean and then reordering the input provided by the source based on `cmp.z`. If `ascending` is true, it is given in this order; if false, the reverse of this order. If two items produce the same value, their relative order will be preserved.

    <result:e> = ord.e.z <source:e>, <ascending:z>, <clause:d>

### Create Context for Single Frame (`priv.rc`)
Create a context for a single frame with private visibility. This uses the context `ctxt` to determine if `frame` should have public or private access.

    <result:c> = priv.rc <ctxt:c>, <frame:r>


### Create a Private Attribute (`priv.x`)
Creates a new attribute which has the same behaviour as the one provided but does not allow the attribute to be visible outside of the frame of its children.

    <result:x> = priv.x <inner:x>

### Produce Grouper Power Set (`powerset`)
Creates a new grouper which is the alternate (`or.g`) of every combination of the provided groupers (`and.g`) and the complement (`not.g`) of the others.


    <result:g> = powerset (<grouper:g>, ...)

### Require Override (`require.x`)
Create an attribute with the specified name to be an error requesting this attribute be overridden. If something is assigned to this name already, it is discarded.

    <result:x> = require.x <name:s>

### Fricassée Reverse Values (`rev.e`)
Reverse the order in which items in a fricassée chain are processed.

    <result:e> = rev.e <source:e>

### Group by Modular Exponentiation (`ring.g`)
Create a grouper that puts items into groups based on performing `pow(primitive, position) % size`.

    <result:g> = ring.g <primitive:i>, <size:i>

### Box Frame Value (`rtoa`)
Create a box containing the provided frame value.

    <result:a> = rtoa <value:r>

### Create Fricassée from Single Frame (`rtoe`)
This iterates over a frame of frames and put each inner frame as the head of the context in which the chain is evaluated.

    <result:e> = rtoe <source:r>, <context:c>

### String Constant (`s`)
Create a string constant.

    <result:s> = s "<value>"

### Seal Definition (`seal.d`)
Create a new definition that discards the calling context and uses the context provided.

    <result:d> = seal.d <definition:d>, <context:c>

### Seal Override Definition (`seal.o`)
Create a new override definition that discards the calling context and uses the context provided.

    <result:o> = seal.o <definition:o>, <context:c>

### Create a Window with Integral Variable-length Sessions (`session.f`)
Create a window that requires that adjacent items have less than an adjacent distance and that the whole window has less than a maximum distance.

    <result:w> = session.i <definition:d>, <adjacent:f>, <maximum:f>

### Create a Window with Integral Variable-length Sessions (`session.i`)
Create a window that requires that adjacent items have less than an adjacent distance and that the whole window has less than a maximum distance.

    <result:w> = session.i <definition:d>, <adjacent:i>, <maximum:i>

### Shift Bits of Integer (`sh.i`)
Shift `value` to the left by `offset`. If `offset` is negative, shift to the right.

    <result:i> = sh.i <value:i>, <offset:i>

### Shuffle Fricassée Results (`shuf.e`)
Randomly permute the order of a fricassée chain.

    <result:e> = shuf.e <source:e>

### Box String Value (`stoa`)
Create a box containing the provided string value.

    <result:a> = stoa <value:s>

### Group into Stripes (`stripe.e`)
Create a discriminator for a grouping operation that assigns items to rotating groups based on their position.

    <result:g> = stripe.e <width:i>

If items `123456789` was processed with `stripe.e 4`, the groups would be assigned as `ABCDABCDA`.

### Subtract Floating-Point Numbers (`sub.f`)
Subtract `right` from `left`.

    <result:f> = sub.f <left:f>, <right:f>

### Subtract Integers (`sub.i`)
Subtract `right` from `left`.

    <result:i> = sub.i <left:i>, <right:i>

### Keep Fricassée Results by Condition (`take.ed`)
Allow elements from a fricassée chain as long as `clause` returns true. Once it returns false, all remaining contexts will be discarded. An error occurs if it returns a non-boolean value.

    <result:e> = take.ed <source:e>, <clause:d>

### Keep First Fricassée Results by Constant (`take.ei`)
Keep up to `count` elements from the start of a fricassée chain.

    <result:e> = take.ei <source:e>, <count:i>

### Keep Last Fricassée Results by Constant (`takel.ei`)
Keep up to `count` elements from the end of a fricassée chain.

    <result:e> = takel.ei <source:e>, <count:i>

### Box Template Value (`ttoa`)
Create a box containing the provided template value.

    <result:a> = ttoa <value:t>

### Produce a Windowed Grouper
Produce grouper that creates windows of `length` and moving to the next one using `next`.

    <result:g> = window.g <length:w>, <next:w>

### Bit-wise Exclusive-OR on Integer
Produce the bit-wise XOR of `left` and `right`.

    <result:i> = xor.i <left:i>, <right:i>

### Box Boolean Value (`ztoa`)
Create a box containing the provided Boolean value.

    <result:a> = ztoa <value:z>

### Convert Boolean to String (`ztos`)
Create a string representation of a Boolean value.

    <result:s> = ztos <value:z>

# Functions
The standard library exports the following functions:

- `lookup.composite_grouping(ssck)l` -- Create a lookup handler that composites the results of all frames and does a grouping operation. Each lookup explorer must return a frame. Each frame discovered will be grouped and a fricassée operation computed over all the values discovered. The first argument is the name of the attribute name in the fircassée operation; the second argument is the name of the value collected; the third argument is the context in which the operation is evaluated; and the final argument is a fricassée collector which will provide the result of the lookup operation.
- `lookup.fricassee(sck)l` -- Create a lookup handler where the collectors is turned into a fricassée operation. The first argument is the name of the values collected; the second argument is the context in which the operation is evaluated; and the final argument is a fricassée collector which will provide the result of the lookup operation.
