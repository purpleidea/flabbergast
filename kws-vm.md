# KWS VM

KWS is the standard virtual machine for running Flabbergast programs on other virtual machines (e.g., CLR and JVM). It makes the following assumptions:

 - The target VM can cope with cyclic references between objects.
 - There is some kind of scheduling system.
 - There is a type-based dynamic dispatch system.

Obviously, if the target VM does not support these features, the target platform is still a reasonable choice if they can be emulated. The purpose of this VM is to make the Flabbergast compiler easier to implement, by pushing some of the repetitive code into KWS VM, and make it easier to target a new VM by having the KWS VM be more like the target VM. In particular, the KWS VM ensures:

 - Everything is statically typed.
 - Lookup semantics are encapsulated.

The name of the VM is for the person who inspired it, not an acronym.

## Purpose

This virtual machine does not exist. The compiler theoretically emits byte-code for such a VM, except that it does not. This is meant to serve as a mental model for execution. The compiler, conceptually, emits KWS VM byte-code, each of which could be translated to an available VM.

However, for performance reasons, much of what is discussed here does not happen. For instance, this VM describes conversion operations for creating strings. In the implementation, there is only one operation for creating strings which attempts to do more efficient things based on the rules of the underlying VM. Moreover, the compiler has a static type dispatcher, similar to the one described here. It does not have a dynamic dispatcher as described; it has a dynamic dispatcher that integrates with a simpler dynamic dispatcher. It also allows eliding any types not of interest and automatically produces a type error.

The purpose of this VM is to provide intermediate semantics that bridge between Flabbergast and the underlying VM. Ultimately, the CLR, JVM, and LLVM are much more like each other than Flabbergast, so this provides a bridge; something that has semantics usable by Flabbergast, but unified across target VMs.

## Design

The VM uses static single assignment with the note that variable resolution and errors can introduce non-local control flow. The KWS has a different set of types from the Flabbergast language:

 - an integer (`Int`), as in the Flabbergast language.
 - a floating point number (`Float`), as in the Flabbergast language.
 - a Boolean (`Bool`), as in the Flabbergast language.
 - an array of bytes (`Bin`), as in the Flabbergast language.
 - a string (`Str`), as in the Flabbergast language.
 - a frame (`Frame`), where keys are strings and values are any type.
 - the null type (`Unit`). This is distinct from the empty list.
 - a boxed value containing any of the above types (`Any`). It cannot contain any of the following types.
 - a linked-list of frames (`List`).
 - a function closure (`Function`). The arguments are: the context (`List`), the self frame (`Frame`), and the containing frame (`Frame`) and it returns a boxed result (`Any`)
 - an override function closure (`FunctionOverride`). The arguments are: the original value (`Any`), the context (`List`), the self frame (`Frame`), and the containing frame (`Frame`) and it returns a boxed result (`Any`)

Most operations do not mutate values, unless explicitly specified. Each operation is described as follows:

    return_variable : ReturnType = operation<compile-time arguments>(run-time arguments)

Operations are organised in blocks:

    [predefined_variable]{
      o1 = operation1(predefined_variable)
      o2 = operation2(o1)
      return o2
    }

The VM uses single static assignment. Unlike most SSA, there are no phi nodes are paths are not permitted to merge again; each basic block must end in a terminal instruction, typically a `return`.

The self-hosting Flabbergast compiler generates a compiler in a language that then emits instructions in the target VM format. The self-hosting compiler requires some other facilities beyond what is described here in order to generate the target compiler. These are described in the self-hosting compiler and include, principally, the parser.

## Functions and Flow Control

The VM has a scheduler capable of executing functions. Functions always terminate and provide the exit scenarios: return or fail. Every function has one or more listeners, that is, other functions which rely on the result of this function are _notified_ of the result.

Real implementation need to manage the state of the language in particular, deciding when to schedule individual computations. A program is invalid if it creates mutations that are visible to other scheduled units. For example, it is possible to add items to a frame. A computation should make all the modifications to a frame and then make it visible to other computations via a return. None of the receivers can modify the frame (though the implementation is not required to prevent this from happening).

## Basic Blocks

Each basic block, denoted by `{ ... }` must end in a terminal operation, as follows. A block cannot directly join back to another flow.

### Return Value
Passes control to the listeners and provide the value given as an argument.

    return(value)

If the value is a promise, it is evaluated and that value is returned instead (i.e., tail-call).

### Raise an Error
Produce a user-visible error message. The listeners must never execute.

    error(message : Str)

### Loop Continue
Passes control to the next iteration of a loop. This can only be used in a block that is part of a loop.

    continue(next)

## Operations

Below defines the behaviour for each operation in the VM.

### Boolean Operations

#### Boolean False Constant
Set the result to Boolean false.

    r : Bool = bool_false()

#### Boolean True Constant
Set the result to Boolean true.

    r : Bool = bool_true()

#### Boolean Negate
Set the result to Boolean complement.

    r : Bool = bool_negate(i : Bool)

### Floating-Point Number Operations

#### Add Floating-Point Numbers
Add `left` and `right`.

    r : Float = float_add(left : Float, right : Float)

#### Compare Floating-Point Numbers
Return the sign of the different of `left` and `right`, or zero is they are equal. Behaviour is implementation-defined if either value is not-a-number.

    r : Int = float_compare(left : Float, right : Float)

#### Floating-Point Constant
Returns the floating-point number provided at compile-time.

    r : Float = float_const<value : Float>()

#### Divide Floating-Point Numbers
Divides `left` by `right`.

    r : Float = float_divide(left : Float, right : Float)

#### Floating-Point Infinity Constant
Produce an infinite value representable as a floating-point number.

    r : Float = infinity_constant()

#### Truncate Floating-Point Number
Convert a floating-point number to an integer by truncation.

    r : Int = float_to_int(value : Float)

#### Finite Check
Check if a floating-point number is finite.

    r : Bool = float_is_finite(value : Float)

#### Not-a-number Check
Check if a floating-point value is a number.

    r : Bool = float_is_nan(value : Float)

#### Floating-Point Maximum Constant
Produce the largest value representable as a floating-point number.

    r : Float = float_max()

#### Floating-Point Minimum Constant
Produce the smallest value representable as a floating-point number.

    r : Float = float_min()

#### Multiply Floating-Point Numbers
Multiply `left` and `right`.

    r : Float = float_multiply(left : Float, right : Float)

#### Floating-Point Non-a-Number Constant
Produce a not-a-number value representable as a floating-point value.

    r : Float = float_nan()

#### Negate Floating-Point Number
Produce the additive inverse of `value.

    r : Float = float_negate(value : Float)

#### String from Floating-Point Number
Create a string representation of a floating-point number.

    r : Str = flt_str(value : Float)

#### Subtract Floating-Point Numbers
Subtract `right` from `left`.

    r : Float = flt_sub(left : Float, right : Float)

### Integral Number Operations

#### Add Integral Numbers
Add `left` and `right`.

    r : Int = int_add(left : Int, right : Int)

#### Bit-wise AND Integral Numbers
Produce the bit-wise AND of `left` and `right`.

    r : Int = int_and(left : Int, right : Int)

#### Convert to Boolean
Convert integer `value` to Boolean by comparing it to `reference`. If `value` and `reference` are equal, true is returned, false otherwise.

    r : Bool = int_to_bool<reference : Int>(value : Int)

#### Compare Integral Numbers
Compare `left` and `right`, returning the sign of the difference, or zero if they are identical.

    r : Int = int_compare(left : Int, right : Int)

#### Bit-wise Complement of Integral Number
Produce the bit-wise NOT of `value`.

    r : Int = int_complement(value : Int)

#### Integral Number Constant
Returns the integral constant provided at compile-time.

    r : Int = int_const<value : Int>()

#### Divide Integral Numbers
Divides `left` by `right`, and set the result to the dividend.

    r : Int = int_divide(left : Int, right : Int)

#### Upgrade Integral Number
Create a floating-point representation of an integral number.

    r : Float = int_to_float(value : Int)

#### Integral Maximum Constant
Produce the largest value representable as a integral number.

    r : Int = int_max()

#### Integral Minimum Constant
Produce the smallest value representable as a integral number.

    r : Int = int_min()

#### Integral Remainder
Divide `left` by `right` and set the result to the remainder.

    r : Int = int_modulus(left : Int, right : Int)

#### Multiply Integral Numbers
Multiple `left` and `right.

    r : Int = int_muliply(left : Int, right : Int)

#### Negate Integral Number
Produce the additive inverse of `value`.

    r : Int = int_negate(value : Int)

#### Bit-wise OR Integral Numbers
Produce the bit-wise OR of `left` and `right`.

    r : Int = int_or(left : Int, right : Int)

#### String from Integral Number
Create a string representation of an integral number.

    r : Str = int_str(value : Int)

#### Subtract Integral Numbers
Subtract `right` from `left`.

    r : Int = int_subtract(left : Int, right : Int)

#### Bit-wise Exclusive-OR Integral Numbers
Produce the bit-wise XOR of `left` and `right`.

    r : Int = int_xor(left : Int, right : Int)

### Frame Operations
For all operations, the names provided must be valid Flabbergast identifiers.

#### Is Item in Frame
Checks if the provided string is the name of an entry in the frame.

    r : Bool = frame_has(frame : Frame, name : Str)

#### Frame Context
Extract the context embedded in a frame. This is the context provided during creation prepended with the frame itself.

    r : List  = frame_context(frame : Frame)

#### Frame Identifier
Extract a unique identifier from a frame. This string must be a valid identifier.

    r : Str = frame_id(frame : Frame)

#### Create Frame
Create an empty frame.

    r : Frame = frame_new(context : List, container : Frame)

#### Create Frame (Numeric)
Create a frame containing integral numbers, over the range specified, assigned to keys from 1 to the number of items, translated through `string_ordinal`.

    r : Frame = frame_new_through(context : List, container : Frame, start : Int, end : Int)

#### Set Item in Frame (Any)
Add an attribute in a frame. If the name is already present in the frame, an error occurs.

    frame_set(frame : Frame, name : Str, value : Any)

#### Set Item in Frame (Function)
Add an attribute in a frame. If the name is already present in the frame, an error occurs.

    frame_set(frame : Frame, name : Str, value : Function)

Result of the function will be added to the frame, not the frame itself. The VM is must not schedule the function until the frame becomes visible by being returned or used in a lookup or iteration operation.

When executed, the function will receive, as arguments, a context containing `frame` prepended to the context provided during the frame's creation, `frame` as the self frame, and the container frame provided during the frame's creation.

### Template Operations
For all operations, the names provided must be valid Flabbergast identifiers.

#### Create Template
Creates an empty template.

    r : Template = template_new(context : List, container : Frame)

#### Template Container
Extract the container embedded in a template. This is the context provided during creation.

    r : Frame = tmpl_container(tmpl : Template)

#### Template Context
Extract the context embedded in a template. This is the context provided during creation.

    r : List = tmpl_context(tmpl : Template)

#### Set Item in Template
Add item to a template. If the requested name is already present, an error occurs. If the “null function” is added, the operation is ignored.

    tmpl_set(tmpl : Template, name : Str, value : Function)

#### Get Item in Template
Get item from a template. If the requested name is not in the template, a “null function” is returned.

    r : Function = template_get(tmpl : Template, name : Str)

### Bin Operations

#### Bin Length
Returns the number of bytes in a binary string.

    r : Int = binary_length(b : Bin)

### String Operations

#### Concatenate Strings
Create a new string of `first` followed by `second`.

    r : Str = string_concatenate(first : Str, second : Str)

#### String Constant
Create a string constant.

    r : Str = string_constant<s : Str>()

#### String Collation
Determine if `left` collates before, the same, or after `right` and set the result to be -1, 0, or 1, respectively.

    r : Int = string_compare(left : Str, right : Str)

#### String Length
Returns the number of Unicode characters in a string.

    r : Int = string_length(s : Str)

#### String from Ordinal
Create a string from an integer such that ordering of integers is preserved when using `string_compare` and the resulting string is a valid Flabbergast identifier.

    r : Str = string_ordinal(value : Int)

### List Operations

#### List Append
Create a new list where all the items of `first` precede all the items of `second`.

    r : List = list_append(first : List, second : List)

#### List Prepend
Create a new list containing the provided frame.

    r : List = list_prepend(head : Frame, tail : List)

#### Create Empty List
Create an empty list.

    r : List = list_null()

### Access Library
Load external data. Since the URL is fixed, this can be thought of as information to the dynamic loader rather than part of the execution.

    r : Any = external<url : Str>()

### Apply Override
Create a new function by applying an existing function to an override. The new function will first evaluate the `original` function, then pass the result to the `override` function and return the result of that function. The other parameters are passed directly.

     r : Function = apply_override(original : Function, override : FunctionOverride)

### Box
Put a value in a box (i.e., convert a value of another type to the `Any` type).

     r : Any = box(value : Bool)
     r : Any = box(value : Float)
     r : Any = box(value : Frame)
     r : Any = box(value : Int)
     r : Any = box(value : Template)
     r : Any = box(value : Unit)

### Capture
Create a function that will return a provided value.

     r : Function = capture(value : Any)

### Conditional
Perform the block specified if the condition is true.

    conditional(cond: Bool) { ... }

### Contextual Lookup
Perform contextual lookup over a list of frames.

    r : Any = lookup(context : List, name1 : Str, name2 : Str, ...)

If no matching item is found, an error occurs.

### Iterate
Iterate over all the keys (attribute names) in a collection of frames and/or templates. At least one frame or template must be provided. For every key, a matching dispatcher will be invoked. The iterator has an _accumulator_ associated with it. It may be of any type. The value for the accumulator is set by the `initial` argument.

A dispatcher is a block that takes the current value of the accumulator and the 1-based position in the iterator. In addition to `return` and `error`, the dispatcher may terminate with `continue`, in which case, the value of the accumulator is mutated and the next key in the collection is processed. Side effects from the dispatcher re visible, but assignments are not.

Dispatchers come in two flavours: a default dispatcher and zero or more special dispatchers. The default dispatcher also has the current key name bound upon entry. The special dispatchers define a key name; they are always invoked, even if the key is not present in any of the inputs. They are invoked at the lexicographically appropriate time.

    r : T = iterate(initial : T, item1 : (Frame|Template), ..., itemN : (Frame | Template))
				[name : Str, accumulator : T, ord : Int] { ... }
				<name1 : Str>[accumulator : T, ord : Int] { ... }
        ...
				<nameM : Str>[accumulator : T, ord : Int] { ... }

### Iterate with Custom Order
Iterate over all the keys (attribute names) in a collection of frames and/or templates, in a user-specified order. The order is determined by the natural ordering of integers, floating pointer numbers or strings. Iteration proceeds in two phases: during the first phase, each item is converted into a value, of the right type, and then the iteration is performed a second time, in the correct sequence.

    r : T = iterate_ordered<S : (Float|Int|Str)>(initial : T, item1 : (Frame|Template), ..., itemN : (Frame | Template))
				[name : Str] { ... continue(v : S) }
				[name : Str, accumulator : T, ord : Int] { ... continue(v : T) }

### Null Constant
Set the result to be the null value. Note that since there are two null values, this should probably not be the target platform's null value.

    r : Unit = null()

### Unbox
Convert a value of an `Any` type into another type.

    unbox(value : Any)
      [b : Bool] { ... }
      [f : Float] { ... }
      [a : Frame] { ... }
      [i : Int] { ... }
      [s : Str] { ... }
      [t : Template] { ... }
      [u : Unit] { ... }

### Verify Identifier Name
Checks that a string is a valid Flabbergast symbol name.

    r : Bool = verify_symbol(s : Str)

## Porting to a New VM

Porting KWS VM to a new target VM requires a few tools and several steps. The Flabbergast self-hosting compiler emits code in for a compiler in a language usable on the target VM. Therefore, the following tools are needed:

 - a target language for the Flabbergast compiler (e.g., Java, C#)
 - a compiler for the target
 - a working Flabbergast implementation (cross Flabbergast implementation)
 - the self-hosting Flabbergast compiler
 - a library, usable by the target language, to generate byte code for the target VM (e.g., Java ASM, CLR `System.Reflection.Emit`)

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
 1. Create appropriate redistributable packaging and submit the code. The compiler must be bootstrappable using another platform's compiler–implementation specific features must not be used. 
