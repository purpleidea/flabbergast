# KWS VM

KWS is the standard virtual machine for running Flabbergast programs on other virtual machines (e.g., CLR and JVM). It makes the following assumptions:

 - The target VM can cope with cyclic references between objects.
 - There is some kind of co-routine system such that evaluation can seem to block.
 - There is no need to box or unbox data.

Obviously, if the target VM does not support these features, the target platform is still a reasonable choice if they can be emulated.

The name of the VM is for the person who inspired it, not an acronym.

## Design

The VM uses static single assignment with the note that variable resolution and errors can introduce non-local control flow. The KWS has the same types as the Flabbergast language with additional types:

 - a linked-list of tuples. The linked lists also have a _marked_ property. This is simply an extra bit of information used in lookup semantics.
 - a set of strings.
 - the iterator type. This iterates over the contents of a set.
 - a function.  All functions have the same type: they take a list of tuples (the context) and return a value.
 - the null-family type. Note that there are two distinct null values: `Null` and `Continue`.

All operations are assumed to not mutate the arguments except those ending in `_APPEND`. The special type `Flabbergast` is used to denote any of the types in the Flabbergast language, excluding the additional ones. Types marked with `*` must be constant (i.e., specified at compile-time, not run-time).

The self-hosting Flabbergast compiler generates a compiler in a language that then emits instructions in the target VM format. The self-hosting compiler requires some other facilities beyond what is described here in order to generate the target compiler. These are described in the self-hosting compiler and include, principally, the parser.

### Pseudo-blocking
In general, each operation in the virtual machine will complete immediately. However, some functions rely on values not yet computed. Therefore, they will block until the operation completes. The VM is therefore encouraged to pause the executing code and continue when the operation is complete. This is where the co-routine system is needed. There is no interaction between code other than during pseudo-blocking operations and the `return` operation; therefore KWS VM is trivially parallelisable.

## Operations

Below defines the behaviour for each operation in the VM. Pseudo-blocking functions are marked with _PB_.

### Boolean Branch
Branch to the provided label if the provided condition is true.

    bool_br(cond: Bool, label)

### Boolean False Constant
Set the result to Boolean false.

    r : Bool = bool_false()

### Boolean True Constant
Set the result to Boolean true.

    r : Bool = bool_true()

### Always Branch
Branch to the specified label.

    branch(label)

### Invoke a Function (PB)
Invokes the provided function, then assigns the result of that function to the result. Note that the function may pseudo-block.

    r : Flabbergast = call(function : Function)

### Raise an Error
Produce a user-visible error message. The remainder of the function and any functions that would pseudo-block on it must not execute.

    r : Str = error(message : Str)

### Add Floating-Point Numbers
Add `left` and `right`.

    r : Float = flt_add(left : Float, right : Float)

### Floating-Point Constant
Returns the floating-point number provided at compile-time.

    r : Float = flt_const(value : Float\*)

### Divide Floating-Point Numbers
Divides `left` by `right`.

    r : Float = flt_div(left : Float, right : Float)

### Floating-Point Infinity Constant
Produce an infinite value representable as a floating-point number.

    r : Float = flt_inf()

### Truncate Floating-Point Number
Convert a floating-point number to an integer by truncation.

    r : Int = flt_int(value : Float)

### Finite Check
Check if a floating-point number is finite.

    r : Bool = flt_is_finite(value : Float)

### Not-a-number Check
Check if a floating-point value is a number.

    r : Bool = flt_is_nan(value : Float)

### Floating-Point Type Check
Determine if a value is a floating-point number.

    r : Bool = flt_is(value : Flabbergast)

### Floating-Point Maximum Constant
Produce the largest value representable as a floating-point number.

    r : Float = flt_max()

### Floating-Point Minimum Constant
Produce the smallest value representable as a floating-point number.

    r : Float = flt_min()

### Multiply Floating-Point Numbers
Multiply `left` and `right`.

    r : Float = flt_mul(left : Float, right : Float)

### Floating-Point Non-a-Number Constant
Produce a not-a-number value representable as a floating-point value.

    r : Float = flt_nan()

### Negate Floating-Point Number
Produce the additive inverse of `value.

    r : Float = flt_neg(value : Float)

### Signum of Floating-Point Number
Produce the sign of a floating-point value (i.e., `-1` if `value < 0`, `0` if `value = 0`, and `1` otherwise).

    r : Int = flt_sgn(value : Float)

### String from Floating-Point Number
Create a string representation of a floating-point number.

    r : Str = flt_str(value : Float)

### Subtract Floating-Point Numbers
Subtract `right` from `left`.

    r : Float = flt_sub(left : Float, right : Float)

### Add Integral Numbers
Add `left` and `right`.

    r : Int = int_add(left : Int, right : Int)

### Branch if Greater Than
Branch to `label` if `value` is strictly greater than zero.

    int_brgt(value : Int, label)

### Branch if Less Than
Branch to `label` if `value` is strictly less than zero.

    int_brlt(value : Int, label)

### Branch if Equal
Branch to `label if `value` is zero.

    int_breq(value : Int, label)

### Integral Number Constant
Returns the integral constant provided at compile-time.

    r : Int = int_const(value : Int*)

### Divide Integral Numbers
Divides `left` by `right`, and set the result to the dividend.

    r : Int = int_div(left : Int, right : Int)

### Upgrade Integral Number
Create a floating-point representation of an integral number.

    r : Float = int_flt(value : Int)

### Integral Number Type Check
Determine if the provided value is an integral number.

    r : Bool = int_is(value : Flabbergast)

### Integral Maximum Constant
Produce the largest value representable as a integral number.

    r : Int = int_max()

### Integral Minimum Constant
Produce the smallest value representable as a integral number.

    r : Int = int_min()

### Integral Remainder
Divide `left` by `right` and set the result to the remainder.

    r : Int = int_mod(left : Int, right : Int)

### Multiply Integral Numbers
Multiple `left` and `right.

    r : Int = int_mul(left : Int, right : Int)

### Negate Integral Number
Produce the additive inverse of `value`.

    r : Int = int_neg(value : Int)

### String from Integral Number
Create a string representation of an integral number.

    r : Str = int_str(value : Int)

### Subtract Integral Numbers
Subtract `right` from `left`.

    r : Int = int_sub(left : Int, right : Int)

### Branch on Iterator
Branch to label if there are no values in the iterator. Otherwise, the result is set to the value in the iterator.

    r : Str = iter_br(iter : Iter, label)

### Access Library
Get the value associated with a library. The mechanism for this is implementation-defined.

    r : Flabbergast = library(uri : Str*)

### Is List Empty
Check if `list` has no items. That is, the same as the result from `list_nil`.

    r : Bool = list_empty(list : List)

### List Item
Get the first tuple in the list.

    r : Tuple = list_item(list : List)

### Is List Marked
Check if the first item in the list is marked (i.e., created with `list_marked` instead of `list_unmarked`). The behaviour of calling on the empty list is undefined.

    r : Bool = list_is_marked(list : List)

### List Join
Create a new list where all the items of `first` precede all the items of `second`, preserving their _marked_ state.

    r : List = list_join(first : List, second : List)

### Create Marked List
Create a new list containing the provided tuple with the _marked_ state set.

    r : List = list_marked(head : Tuple, tail : List)

### Create Empty List
Create an empty list.

    r : List = list_nil()

### List Remainder
Get a list holding the remaining elements in the list.

    r : List = list_tail(list : List)

### Create Unmarked List
Create a new list containing the provided tuple without the _marked_ state set.

    r : List = list_unmarked(head : Tuple, tail : List)

### Continue Constant
Set the result to be the continue value. Note that since there are two null values, this should probably not be the target platform's null value.

    r : NullFamily = null_cont()

### Check if Continue
Checks if `value` is the same as the result of `null_cont`.
    r : Bool = null_is_cont(value : Flabbergast)

### Check if Null
Checks if `value` is the same as the result of `null` or `null_cont`.

    r : Bool = null_is(value : Flabbergast)

### Null Constant
Set the result to be the null value. Note that since there are two null values, this should probably not be the target platform's null value.

    r : NullFamily = null()

### Phi Node
Creates a φ-node in the SSA. Understanding these is complicated enough, so it won't be explained badly here.

    r : 'a = phi(value0 : 'a, label0, value1 : 'a, label1, ...)

### Return
Sets the caller's result to the supplied value and stop execution.

    return(value : Flabbergast)

### Set Append
Add an additional string to the set, if it is not already present.

    r : Set = set_append(set : Set, name : Str)

### Is Set Empty
Check if the set contains no strings.

    r : Bool = set_empty(set : Set)

### Is String in Set
Checks if the provided string is presently in the set.

    r : Bool = set_has(set : Set, name : Str)

### Create Iterator
Creates an iterator over the items in the set. The iterator should return items in collated order.

    r : Iter = set_iter(set : Set)

### Create Set
Create a new set with no items in it.

    r : Set = set_new()

### Concatenate Strings
Create a new string of `first` followed by `second`.

    r : Str = str_cat(left : Str, right : Str)

### Unicode Character
Create a string with the Unicode character represented at the code-point `value`.

    r : Str = str_chr(value : Int)

### String Collation
Determine if `left` collates before, the same, or after `right` and set the result to be -1, 0, or 1, respectively.

    r : Int = str_col(left : Str, right : Str)

### String Type Check
Determine if the provided value is a string.

    r : Bool = str_is(value : Flabbergast)

### String from Ordinal
Create a string from an integer such that ordering of integers is preserved when using `str_col` and the resulting string is a valid Flabbergast identifier.

    r : Str = str_ord(value : Int)

### Append to Template
Add a new function to a template. If `name` already exists in the template, an error occurs. If the name is not a valid Flabbergast identifier, an error occurs.

    tmpl_append(tmpl : Template, name : Str, function : Function)

### Template Context
Get the context associated with a template.

    r : List = tmpl_ctxt(template : Template)

### Template Item
Get the function associated with the name in a template. If one is not associated, then an error occurs.

    r : Function = tmpl_get(tmpl : Template, name : Str)

### Is Attribute in Template
Checks if the provided string is the name of an attribute in a template.

    r : Bool = tmpl_has(tmpl : Template, name : Str)

### Template Type Check
Determine if the provided value is a template.

    r : Bool = tmpl_is(value : Flabbergast)

### Template Attribute Names
Create a set of all the attribute names in a template.

    r : Set = tmpl_names(tmpl : Template)

### Create Template
Create a template, associated with the provided context, containing no attributes.

    r : Template = tmpl_new(context : List)

### Add Attribute to Template
Add an attribute to a template. If an attribute of the same name exists, an error occurs. If the tuple is closed, an error occurs. If the name is not a valid Flabbergast identifier, an error occurs.

    tuple_append(tuple : Tuple, name : Str, function : Function)

### Close Tuple
Indicate that no more elements will be added to a tuple (i.e., make it immutable.) The VM should also dispatch evaluation of any attributes. If the tuple is already closed, an error occurs.

    tuple_close(tuple : Tuple)

### Tuple Context
Get the context associated with a tuple. Note that is is not exactly the same as the context with which it was created. This can be called if the tuple is open or closed.

    r : List Tuple = tuple_ctxt(tuple : Tuple)

### Tuple Item (PB)
Get the value of an attribute in a tuple. If the attribute was added as a function, then this may pseudo-block until the value is computed. If the tuple is open, an error occurs.

    r : Flabbergast = tuple_get(tuple : Tuple, name : Str)

### Is Attribute in Tuple
Check if there exists an attribute in the tuple with the name provided. This may be called when the tuple is open or closed.

    r : Bool = tuple_has(tuple : Tuple, name : Str)

### Tuple Type Check
Determine if the provided value is a tuple.

    r : Bool = tuple_is(value : Flabbergast)

### Tuple Attribute Names
Create a set of all the attribute names in a tuple.

    r : Set = tuple_names(tuple : Tuple)

### Create Tuple
Create a new tuple. The context of the tuple will be the context provided preceded by the newly-created tuple. Any functions added to the tuple will be evaluated in this context. The tuple will be in the open state.

    r : Tuple = tuple_new(context : List)

### Add Attribute to Tuple by Value
Add an existing value to a tuple. The tuple must be in the open state, otherwise an error occurs. If the attribute of the same name is present in the tuple, an error occurs.

    tuple_value_append(tuple : Tuple, name : Str, value : Flabbergast)

## Porting to a New VM

Porting KWS VM to a new target VM requires a few tools and several steps. The Flabbergast self-hosting compiler emits code in for a compiler in a language usable on the target VM. Therefore, the following tools are needed:

 - a target language for the Flabbergast compiler (e.g., Java, C#)
 - a compiler for the target
 - a working Flabbergast implementation (cross Flabbergast implementation)
 - the self-hosting Flabbergast compiler
 - a library, usable by the target language, to generate byte code for the target VM (e.g., Java ASM, CLR System.Reflection.Emit)

To port, perform the following:

 1. Create the common data structure library in the target language.
 1. Implement a library loader. In general, this is just a wrapper on the target VM's loader mechanism and a hook for users to add custom handlers.
 1. In a new Flabbergast file, write an implementation of:
    - each KWS VM instruction, in the target language, that generates byte code for the target VM
    - some of the glue code in the target compiler language (e.g., for-each loop syntax)
    - parser code for each syntax element
 1. Use the cross Flabbergast implementation to generate the compiler for the target language.
 1. Write the wrapper for the generated compiler (e.g., the command-line interface, as appropriate).
 1. Compile the compiler in the target language.
 1. Use the new compiler to regenerate the compiler for the target language and verify that it is the same as the cross implementation.
 1. Determine a foreign function interface and amend it to the Flabbergast compiler. The FFI will be specific to the target VM, but, in general, it needs to:

    - describe how to capture Flabbergast variables
    - marshal Flabbergast types to the corresponding native types
    - provide information about linking

    For examples, look at the existing ports.
 1. Port as many of the platform-specific libraries as possible using the FFI.
 1. Pre-compile all of the platform-independent libraries.
 1. Create appropriate redistributable packaging and submit the code. While it is normally discouraged, the generated compiler should be placed in source control for bootstrapping reasons.

If box/unboxing is required, this is the responsibility of either the target VM or the target compiler.
