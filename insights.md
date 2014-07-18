# Insights into Running Flabbergast

Creating an efficient VM for Flabbergast is very important. It's predecessor was slow and used large amounts of memory. As with most functional languages, Flabbergast has a tendency to create lots of garbage. The lookup semantics have the potential to create breaks in computation (i.e., the needed value has not yet be computed). The design goals of the VM are:

1. Create as little garbage as possible. Alternately, make sure that garbage is as temporary as possible.
2. Make computation flows short-lived and finish as much as possible.
3. Be easy to parallelise.

The bootstrap VM uses a pull model: every attribute is wrapped in a promise and the promise is computed at the time another process attempts to read it. This is rather trivial to implement and allows detection of circular evaluation rather easily; re-entering a promise is a sign of circular evaluation. However, this does not work easily with the above design goals. First, threading is extremely difficult since if a promise is re-entered by a different thread, that could be circular evaluation or just unfortunate timing on the part of threads. The call stack of the interpreter now resembles the call stack of the references in Flabbergast, but the language doesn't have the facility to really control the stack in the way a procedural or functional language does. It also the case that promises are generated for all kinds of expressions, including literals and other expressions that could be evaluated immediately.

The major problem with the design is the difficulty for the user to debug any problems. Circular evaluation is difficult to debug and the bootstrap interpreter can, at best, provide only the cycle. In more general debugging, it would be ideal to provide users with information about lookups, which are the most confusing part of the language. There isn't much beneficial information about how lookups happen in the bootstrap interpreter. Error propagation is another problem. The interpreter will propagate the error through the entire system (i.e., an attribute will have an error because it references an error). Simply stopping on the first error forces the user to fix on error at a time. The ideal solution is to compute everything that can be computed or results in a real error, and no anything that references an error.

The goal for the VM is to use a push model for computation. The VM maintains a graph of reverse dependences between computations. When a computation has no unresolved dependencies, it can be computed and then notify all of its reverse dependencies. This becomes very easy to parallelise. Moreover, it provides excellent debugging information: anything referencing an error will never be queued to run and if there are no errors but unfinished computations, then all the unfinished computations are part of circular evaluation.

On the part of the users, it is expected that they will write relatively short expressions.

## Computational Units
The ideal computational unit for Flabbergast would contain the following information:

- parameters
- lookups
- the code to execute
- the attribute into which to write the result

Once queued, the lookups reference unfinished attributes. When there are no outstanding lookups, the computation begins and returns a value. The return sets the attribute value and adjusts the lookups of any waiting computations. If all the lookups of a computation are satisfied, it can be executed.

Lookups can be tables. Imagine each lookup as a matrix where the rows are the parts of a lookup (e.g., `a.b.c`) and the columns are the contexts in which to start lookup. The algorithm to complete the table is straight forward:

1. Populate the matrix with the lookup name parts as the rows and initial contexts as the columns. Mark each column as “unfinished”.
2. For each column, in parallel, check if the row name exists in the bottom-most filled in row in the table.
  - The value does not exist: mark the column “ignored”.
  - The value is not computed: set a listener.
  - This is the last row: mark the column “good”.
  - The value is a tuple: repeat in the next row.
  - The value is not a tuple: mark the column “bad”.
3. If, at anytime, the first column that is not marked “ignored” is:
  - marked “good”: the lookup is complete with the result being the last row in this column.
  - marked “bad”: the lookup has an error where the user has attempted to lookup a value inside a non-tuple.
  - none: the reference does not exist and this is an error.

## “Free” Variables
In lexically-scoped languages, there is a distinction between free and bound variables. Since Flabbergast is dynamically scoped, no such distinction can be drawn. However, it is possible to discussed variables based on need an containment. Consider:

    If x > 3 Then x + 1 Else y + 2

The conditional clause of the `If` expression requires `x`, so `x` is needed for this expression. The `Then` clause also needs `x`, but the `Else` clause needs `y`. The `If` statement cannot need `y` since the `Else` clause may not be evaluated. The reference for `y` must be optional. In fact, any needed variables in the `Then` clause must be optional too, but if a variable is needed and optional, it must be needed in the union of those expressions.

Because lookups are atomic, this must also be true of their needs:

    If a.x Then a.y Else 0

Here, `a.x` and `a.y` must be considered separately.

However, given:

    If a.enabled Then a Else Null

It is clear that `a.enabled` is needed and `a` is optional, but `a` being computed is implied by having resolved `a.enabled`, even if they are not the same `a`. This is a consequence of the lookup algorithm above. In fact, for any sub-lookup, the algorithm above will produce a correct value as a consequence of finding the longer lookup. There is also the guarantee that the type of any sub-lookup must be a tuple, otherwise the longer lookup would have failed.

Why is any of this relevant? In the case of:

    If a.x Then a.y Else 0

code must be generated compatible with the JVM, CLR, and LLVM, none of which have a native coroutine system. Therefore, the options for generating code are:

  - transform the whole program to continuation passing style, where variable resolution can trampoline out of the execution path to block. Save the needed variables in new allocated objects.
  - transform the entire program to a state machine that can trampoline out at any point, saving all the temporaries into an object (i.e., don't use the VM's stack).

In either case, it is extremely beneficial to know whether a block of code will need non-local flow.

## Types and Dispatch
Although Flabbergast is dynamically typed, mostly on account of being dynamically scoped, some type inference can be done based on variable lookups. For instance:

    (If x Then y Else z) { }

By type inference, `x` must be a Boolean and `y` and `z` must be templates. That allows checking types at the time of lookup rather than the time of use. It also means that certain type errors can be detected statically. For instance:

    (If x Then x Else x) { }
    If y Then y + 3 Else y
    a + a.x

are always a type error since `x` needs to be both a Boolean, a template and `y` need to be both Boolean and a numeric type (integer or floating point), and `a` must be a tuple on account of `a.x` but the addition requires it to be a numeric type.

Most of the targeted virtual machines make a distinction between integral and floating point types. If type inference is done, selecting the correct path can be done immediately after lookup. The following code:

    a + b * 3.0

is only well-type if `a` and `b` are each either `Int` or `Float`. Since the result of the multiplication will always be floating point, the result of the addition will also be floating point. Therefore, a lookup dispatcher could check that each of the lookups are of the right type and upgrade them to a floating point number.

