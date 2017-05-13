# Using Flabbergast's Foreign Function Interface

Flabbergast has three important mechanisms for integrating Java code:

- URI handlers which allow importing values into Flabbergast programs
- Futures and promises, the unit of executable code in Flabbergast
- Marshalled frames, which encapsulate values not representable by Flabbergast

This allows exporting Java code and values into a Flabbergast program.
Additionally, a Flabbergast library should be provided to document and expand
on the code.

This codebase serves as an example on how to use all of these mechanisms to
integrate foreign code with Flabbergast code.

## Background on ZooKeeper
For the purpose of understanding this code, ZooKeeper is a distributed lock
manager. A connection must be opened to a cluster and then read and write
operations can be performed to blobs in the cluster, addressed by paths. For
the most part, it looks like a file system for our purposes.

Users will need to be able to open a connection, access specific values in the
hierarchy by path and traverse the hierarchy. Since Flabbergast is meant to be
side-effect-free, only read operations will be bound.

## URI Handlers and URI Services
URI handlers are interrogated whenever a `From` is used in program. Multiple
handlers for the same URI can be loaded and each has a priority that determines
the query order.

A URI service decides if a URI handler should be attached to a Flabbergast
session. This can be based on parameters such as whether the session is
sandboxed or not. In this case, ZooKeeper should not be available in a sandbox
as it requires network access.

The class `ZooKeeperUriService` is a URI service that, when not sandboxed, adds
one URI handler that interprets `zookeeper:` URIs and creates a new `ZooKeeper`
connection. This connection object is not a type compatible with Flabbergast,
so it must be marshalled.

## Marshalled Frames
To a Flabbergast program, a marshalled frame is indistinguishable from a
regular frame. However, it is different in an important way: it holds a value
that can be retrieved by Java code. Effectively, it wraps an arbitrary value
in a frame-compatible cloak so the value can be shuttled through a program and
back out to foreign code that needs it.

Marshalled frames can be created using `MarshalledFrame.create`. There are two
variants of this method: one where the frame's `Id` will be automatically
generated like a normal frame and one where the `Id` must be provided.
Typically, the second is useful for constant or singleton values.

Since the user may connect to several ZooKeeper instances, we will generate an
identifier from the connection string.

When creating a marshalled frame, a list of `Transformer` objects can also be
specified.  These allow extracting Flabbergast-compatible information from the
wrapped object. For instance, in the SQL connection frames, the database driver
and version is listed. For ZooKeeper connections, the `uri` (`Str`) and
`session_id` (`Int`) will be reported.

It is important to remember that Java erases types and this applies to
marshalled frames. Avoid using common generic classes including `Collection`,
`List`, `Set`, `Optional`, `Function`, `Consumer`, `Supplier`, `Maybe`, and
`Ptr`. For the functional interfaces, it is better to define a new functional
interface. For collections, it is best to either subclass or create a unique
holder class.

## Interop
As previously mentioned, URI handlers are the mechanism to import data into
Flabbergast program. This includes functions. To make this easier, the
`Interop` class provides utilities to import constants and functions.

For ZooKeeper, the `ZooKeeperInterop` class derives from `Interop` and, in its
constructor, binds all the ZooKeeper utilities. For every bound item, a path
must be specified. For instance, the version is bound as `apache/zookeeper/version`
and the value can be imported into Flabbergast using `From
interop:apache/zookeeper/version`. There are several utility bindings available:

- The `add` method will provide bindings for frames, strings, and numbers.
- The `addHandler` method will bind a lookup handler.
- The `add` method binds functions as function-like templates take one, two, or
  three arguments.
- The `addMap` method binds functions as function-like templates that apply the
  provided function to every item in `args`, preserving the attribute names.
  Up to three additional fixed arguments can be provided.
- The `add` method can also bind a `Definition`, a custom computation.

The functions provided for wrapping must:

- take parameters with types that are either a Flabbergast type, a
  non-Flabbergast type as marshalled frame, a datetime, or null
- must return a value of a single type that matches a Flabbergast type (or null)
- avoid blocking if practical
- throw on an error

Any exceptions will be caught, stripped of their stack traces and message
displayed on top of the Flabbergast stack that instantiated the template.

Parameters are converted by a `Matcher` that performs type check on the
Flabbergast value. The `Matcher` interface provides methods for converting all
the Flabbergast types, marshalled frames, and date-times.

Here are the fixed-value bindings for ZooKeeper:

    add("apache/zookeeper/revision", Version.getRevision());
    add("apache/zookeeper/version", Version.getVersion());

In the Flabbergast library, these should be included as:

    {{{The revision of the ZooKeeper client.}}}
    revision : (From interop:apache/zookeeper/revision) Enforce Int
    {{{The version of the ZooKeeper client.}}}
    version : (From interop:apache/zookeeper/version) Enforce Int

The `Enforce` checks are not strictly needed, but make the documentation better.

There is a function that validates whether a path is valid. Unfortunately, the
function throws rather than returns a Boolean, so it's necessary to catch the
exception in this case:

    addMap(Any::of, Matcher.asString(false), "apache/zookeeper/validate_path", (path, isSequential) -> {
      try {
        PathUtils.validatePath(path, isSequential);
        return true;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }, Matcher.asBool(false), "is_sequential");

This is a `addMap`, so the lambda will be applied to each attribute value in
`args`, which will be converted to a string, as specified by the
`Matcher.asString(false)`. In this case, `Null` is treated as a type error, but
if `Matcher.asString(true)` was used, any Flabbergast `Null` would be converted
to a Java `null`. This function also takes a fixed parameter, `is_sequential`,
which must be a Boolean.

Here's the matching library code:

    {{{Checks that the paths provided are well-formed ZooKeeper paths.}}}
    validate_paths : Template From interop:apache/zookeeper/validate_path {
      {{{A list of paths to validate, which must be strings.}}}
      args : Used
      {{{Whether the paths are being created with a sequential flag.}}}
      is_sequential : Used
    }

Note that the utility function creates a template with only `value` bound. All
of the parameters are not included, so it is up to the library to decide if
`Used` or `Required` is appropriate. 

## Futures, Promises, and Definitions
The most important ZooKeeper function is the one that retrieves data. It is
also the most tricky. It is asynchronous, has a complicated set of callbacks,
and takes a number of parameters. All of these things are handled using the
more sophisticated `Future`, `Promise`, and `Definitions` classes. The `Future` class itself is not trivial to
use, but the `LookupAssister` class provides a number of utilities to make it
easier.

    package flabbergast.apache.zookeeper;

    import java.util.Arrays;
    import java.util.stream.Collectors;
    
    import org.apache.zookeeper.ZooKeeper;
    
    import flabbergast.lang.Any;
    import flabbergast.lang.ArrayValueBuilder;
    import flabbergast.lang.Context;
    import flabbergast.lang.Definition;
    import flabbergast.lang.DefinitionBuilder;
    import flabbergast.lang.Frame;
    import flabbergast.lang.Future;
    import flabbergast.lang.AnyConverter;
    import flabbergast.lang.SourceReference;
    import flabbergast.lang.Template;
    import flabbergast.lang.ValueBuilder;
    import flabbergast.export.LookupAssistant;
    
    public final class ZooKeeperGet {
      public static final Definition INSTANCE;
      static {
        final LookupAssister<ZooKeeperGet> assister = new LookupAssister<>(ZooKeeperGet::new,
            (future, sourceReference, context, h) -> h.resolve(future, sourceReference, context));
    
        assister.find(Matcher.asString(false), (i, x) -> i.path = x, "path");
        assister.find(Matcher.asTemplate(false), (i, x) -> i.template = x, "zookeeper", "node");
        assister.find(Matcher.asMarshalled(ZooKeeper.class, false, "From “zookeeper:”"), (i, x) -> i.owner = x,
            "connection");
    
        INSTANCE = assister;
      }
      private ZooKeeper owner;
      private String path;
      private Template template;
    
      public ZooKeeperGet() {
      }
    
      private void resolve(Future future, SourceReference sourceReference, Context context) {
           ...
      }
    }

First, notice that the `LookupAssister` implements `Definition`. In the interop class, it is bound using:

    add("apache/zookeeper/get", ZooKeeperGet.INSTANCE);

All of the parameters are stored as mutable fields and the `static` block
defines the lookups that should be performed to set them. The `find` method
lookups up simple values or the boxed representation of a value. Boxed values
are useful if the type is unimportant as it will only ever be handed back to
the Flabbergast program. There is also `findAll` that unpacks a frame into
key-value pairs.

Once all the lookups are complete, the `resolve` method will be invoked to
perform the computation. Computations have great latitude in how they behave.
They can stop executing without returning a value, asynchronously wake up later
and return one. They can perform additional lookups or wait for other
computations to complete.

The rules are:

1. A computation can only return one value (`Future.complete`).
1. A computation cannot return a value after an error has been raised (`Future.error`).
1. A computation cannot raise an error after it has returned a value.
1. A computation can raise many errors.
1. Every computation must either return or raise one or more errors.

When a computation is started, two objects are created: a `Future`, given to
the new computation, and a `Promise`, given to the caller/launcher. The
`Future` has methods to output data to the caller and the `Promise` can be used
to read the value of a computation if it exists. `Promise` objects should be
treated as opaque references and the `Future.await` method accesses their
contents.

The `resolve` method is as follows:

    private void resolve(Future future, SourceReference sourceReference, Context context) {
      owner.exists(path, false, (statErrorCode, _1, _2, stat) -> {
        if (stat == null && statErrorCode == KeeperException.Code.NONODE.intValue()) {
          future.complete(Any.unit());
          return;
        }
        if (stat == null) {
          future.error(sourceReference, messageForError(statErrorCode));
          return;
        }
        owner.getData(path, false, (dataErrorCode, _a, _b, data, _c) -> {
          if (data == null) {
            future.error(sourceReference, messageForError(dataErrorCode));
            return;
          }
          final ValueBuilder valueBuilder = new ValueBuilder();
          valueBuilder.set("path", Any.of(path));
          final String[] pathParts = path.split("/");
          valueBuilder.set("parent_path", pathParts.length > 1 ? Any.of(
              Arrays.stream(pathParts).limit(pathParts.length - 1).collect(Collectors.joining("/", "/", "")))
              : Any.unit());
          valueBuilder.set("data", Any.of(data));
          owner.getChildren(path, false, (childrenErrorCode, _y, _z, children) -> {
            if (children == null) {
              future.error(sourceReference, messageForError(childrenErrorCode));
              return;            
            }
            final DefinitionBuilder definitionBuilder = new DefinitionBuilder();
            definitionBuilder.set("child_paths",
                Frame.create(true,
                    new ArrayValueBuilder(
                        children.stream().map(child -> String.format("%s/%s", path, child))
                            .map(Any::of).collect(Collectors.toList()))));
    
            future.complete(Any.of(Frame.create(future, sourceReference, context.append(template.context()),
                template, valueBuilder, definitionBuilder)));
          }, null);
        }, null);
      }, null);
    }
    
    private String messageForError(int dataErrorCode) {
      return String.format("ZooKeeper failure “%s” on “%s”.", KeeperException.Code.get(dataErrorCode).name(), path);
    }

Various calls are made to ZooKeeper to retrieve the information and the results
are packed into a frame, which inherits from a template. When finished, the
result is passed to `Future.complete`. If an error occurs, `Future.error` is
called.

Despite all this complexity, the library definition is terse:

    {{{Directly retrieve a ZooKeeper data object.}}}
    retrieve : Template From interop:apache/zookeeper/get {
      {{{The path in the ZooKeeper hierarchy.}}}
      path : Used
      {{{The ZooKeeper cluster connection retrieved using \Mono{From zookeeper:}.}}}
      connection : Used
    }

## Conclusion
All of the Flabbergast standard library is built this way. There's no
privileged interface used by the built-in utilities. There are some behaviours
that are powerful.

For instance, the escaping functionality of `lib:utils` makes heavy used of
marshalled frames–not to store data, but to pass functions. The template for a
character substitution creates an object that does that substitution, stuff it
into a marshalled frame, and the function that builds an escape function
collects these marshalled frames. This kind of composition can allow very
sophisticated functions to be made available to Flabbergast.
