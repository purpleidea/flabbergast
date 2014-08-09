# KWS VM

KWS is the standard virtual machine for running Flabbergast programs on other virtual machines (e.g., CLR and JVM). It makes the following assumptions:

 - The target VM can cope with cyclic references between objects.
 - There is some kind of scheduling system.
 - There is a type-based dynamic dispatch system.

Obviously, if the target VM does not support these features, the target platform is still a reasonable choice if they can be emulated. The purpose of this VM is to make the Flabbergast compiler easier to implement, by pushing some of the repetitive code into KWS VM, and make it easier to target a new VM by having the KWS VM be more like the target VM. In particular, the KWS VM ensures:

 - Everything is statically typed.
 - Lookup semantics are encapsulated.

The name of the VM is for the person who inspired it, not an acronym.

## Design

The VM uses static single assignment with the note that variable resolution and errors can introduce non-local control flow. The KWS has a different set of types from the Flabbergast language:

 - an integer (`Int`), as in the Flabbergast language.
 - a floating point number (`Float`), as in the Flabbergast language.
 - a Boolean (`Bool`), as in the Flabbergast language.
 - a string (`Str`), as in the Flabbergast language.
 - a map (`Map`), where keys are strings and values are any type.
 - a linked-list of maps (`List`).
 - the null-family type (`NullFamily`). Note that there are two distinct null values: `Null` and `Continue`. These are distinct from the empty list.
 - a function pointer (`Function`_n_). This is a function pointer of arity _n_.
 - a promise (`Promise`). That is a closure with a memorised answer.

Most operations do not mutate values, unless explicitly specified. Types marked with `*` must be constant (i.e., specified at compile-time, not run-time).

The VM uses single static assignment. Unlike most SSA, there are no phi nodes are paths are not permitted to merge again; each basic block must end in a terminal instruction.

The self-hosting Flabbergast compiler generates a compiler in a language that then emits instructions in the target VM format. The self-hosting compiler requires some other facilities beyond what is described here in order to generate the target compiler. These are described in the self-hosting compiler and include, principally, the parser.

## Functions and Flow Control

The VM has a scheduler capable of executing functions. Functions always terminate and provide the exit scenarios: return or fail. Every function has one or more listeners, that is, other functions which rely on the result of this function.

Each function may take typed arguments and there can be multiple implementations of a function with different types of arguments, though the number must be the same. Each function has a basic block for a body. All functions, theoretically, return a value, but the type is not specified as it is implied by the basic blocks and different basic blocks need not return the same type. Each signature of a function must be distinct (i.e., no two signatures may have the same types in the same order). No signature may request a promise. The scheduling system must evaluate the promise and then dispatch based on the type of the result from the promise.

    foo
    (x : Int, y : Float) {
     t = int_const(2)
     a = int_mul(x, t)
     b = int_flt(a)
     c = flt_max(b, y)
     return(c)
    }
    (x : Int, y : Int) {
     t = int_const(2)
     a = int_mul(x, t)
     b = int_max(a, y)
     return(b)
    }

## Basic Blocks

Each basic block, denoted by `{ ... }` must end in a terminal operation, as follows. A block cannot directly join back to another flow.

### Return Value
Passes control to the listeners and provide the value given as an argument.

    return(value)

If the value is a promise, it is evaluated and that value is returned instead (i.e., tail-call).

### Raise an Error
Produce a user-visible error message. The listeners must never execute.

    error(message : Str)

## Operations

Below defines the behaviour for each operation in the VM.

### Boolean Branch
Perform the block specified if the condition is true.

    bool_br(cond: Bool) { ... }

### Select Value
Select value based on Boolean. This is generic over the type `T`.

    r : T = bool_choose(cond : Bool, true_value : T, false_value : T)

### Boolean False Constant
Set the result to Boolean false.

    r : Bool = bool_false()

### Boolean True Constant
Set the result to Boolean true.

    r : Bool = bool_true()

### Create Promise for Function
Run a function, eventually, and return a promise for the result.

    r : Promise = [function](...)

The values provided can be of the needed type or promises. If promises, the promises will be evaluated before calling the function. After all promises have been resolved, if no type signature of the function matches, a runtime error occurs.

### Get the Reference to a Function
Create a handle for a function, where _X_ is the arity of the function.

    r : FunctionX = [function]

### Access Library
Load external data. Since the URL is fixed, this can be thought of as information to the dynamic loader rather than part of the execution.

    r : Promise = external(url : Str*)

### Evaluate a Delayed Function
Evaluate a delayed function with the provided arguments, which may be promises. The arity of the function must match the number of parameters provided.

    r : Promise = evaluate(function : Function<X>, ...)

### Add Floating-Point Numbers
Add `left` and `right`.

    r : Float = flt_add(left : Float, right : Float)

### Floating-Point Constant
Returns the floating-point number provided at compile-time.

    r : Float = flt_const(value : Float*)

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

### Is Integer Greater Than
Perform the block provided if `value` is strictly greater than zero.

    r : Bool = int_gt(value : Int)

### Is Integer Less Than
Perform the block provided if `value` is strictly less than zero.

    r : Bool = int_lt(value : Int)

### Is Integer Equal
Perform the block provided if `value` is zero.

    r : Bool = int_eq(value : Int)

### Integral Number Constant
Returns the integral constant provided at compile-time.

    r : Int = int_const(value : Int*)

### Divide Integral Numbers
Divides `left` by `right`, and set the result to the dividend.

    r : Int = int_div(left : Int, right : Int)

### Upgrade Integral Number
Create a floating-point representation of an integral number.

    r : Float = int_flt(value : Int)

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

### Is List Empty
Check if `list` has no items. That is, the same as the result from `list_nil`.

    r : Bool = list_empty(list : List)

### List Item
Get the first map in the list.

    r : Map = list_item(list : List)

### List Join
Create a new list where all the items of `first` precede all the items of `second`.

    r : List = list_join(first : List, second : List)

### Create List
Create a new list containing the provided map.

    r : List = list_new(head : Map, tail : List)

### Create Empty List
Create an empty list.

    r : List = list_nil()

### List Remainder
Get a list holding the remaining elements in the list.

    r : List = list_tail(list : List)

### Contextual Lookup
Perform contextual lookup over a list of maps.

    r : Promise = lookup(list : List, name1 : Str*, name2 : Str*, ...)

This promise will perform contextual lookup and return the value after lookup, if one exists.

### Continue Constant
Set the result to be the continue value. Note that since there are two null values, this should probably not be the target platform's null value.

    r : NullFamily = null_cont()

### Check if Continue
Checks if `value` is the same as the result of `null_cont`.

    r : Bool = null_is_cont(value : NullFamily)

### Null Constant
Set the result to be the null value. Note that since there are two null values, this should probably not be the target platform's null value.

    r : NullFamily = null()

### Concatenate Strings
Create a new string of `first` followed by `second`.

    r : Str = str_cat(left : Str, right : Str)

### Unicode Character
Create a string with the Unicode character represented at the code-point `value`.

    r : Str = str_chr(value : Int)

### String Collation
Determine if `left` collates before, the same, or after `right` and set the result to be -1, 0, or 1, respectively.

    r : Int = str_col(left : Str, right : Str)

### String from Ordinal
Create a string from an integer such that ordering of integers is preserved when using `str_col` and the resulting string is a valid Flabbergast identifier.

    r : Str = str_ord(value : Int)

### Map Item Retrieval
Get a value from a map. If the item exists in the map, but is not of the correct type, an error occurs.

    r : Bool = map_get_bool(map : Map, name : Str*)
    r : Delayed = map_get_delayed(map : Map, name : Str*)
    r : Float = map_get_float(map : Map, name : Str*)
    r : Int = map_get_int(map : Map, name : Str*)
    r : List = map_get_list(map : Map, name : Str*)
    r : Map = map_get_map(map : Map, name : Str*)
    r : NullFamily = map_get_null(map : Map, name : Str*)
    r : Str = map_get_str(map : Map, name : Str*)

### Is Item in Map
Checks if the provided string is the name of an entry in the map.

    r : Bool = map_has(map : Map, name : Str*)

### Map Item Lookup
Do a recursive get from a map.

     r : Promise = map_lookup(map : Map, name1 : Str*, ...)

### Create Map
Create an empty map.

    r : Map = map_new()

### Set Item in Map
Add or replace an attribute in a map. This is generic over the type `T`. If the value is a promise, the result of evaluating the promise will be stored in the map.

    map_set(map : Map, name : Str, value : T)

If value is a promise, then the value returned by the promise will be put into the map. However, the promise must not be evaluated until the current function has returned.

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
