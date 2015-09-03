# Words: What do they mean?
This is a list of Flabbergast-specific terms, and what they mean.

## Amend
Create a new template based on an existing one with changes. This is analogous to sub-classing in an object-oriented language.
## Ancestor
The template that was used to create a frame or template.
## Capture
Creating a definition in a location to change the value observed by a lookup without changing the original definition. For instance, if a template did a lookup for `x` and `x` was defined in the file-level scope, it might be useful to define `x` in a frame where the template will be instantiated to capture that lookup. This is distinct from _overrides_ which replace values.
## Container
The frame in which another frame nests. In `vim`, this is generally the frame found by typing `[{`.
## Frame
A labelled collection of values. In other languages, you might call this a dictionary, map, hash, associative array, or, in JavaScript-land, object.
## Fricassée Expression
An expression that transforms frames. All start with `For` and can `Reduce` or `Select`. These are similar to SQL `SELECT` statements or C#'s LINQ expressions.
## Function-like Template
A template that has an attribute named `value` and expects `args` to be overridden. Flabbergast's templates can do everything functions can do, and more. There is convenience syntax for using them as functions more efficiently, but the templates must be structured in an expected way.
## -ifier Pattern
A function-like template that _amends_ another template. In an object-oriented language, this would be an _adapter_ pattern and, in functional programming, this would be a higher-order function.
## Infinite Instantiation
In Flabbergast, you never get infinite loops (since there are no loop) or infinite recursion (since there are no functions). If your program is running forever, then you have certainly made infinite instantiation, where one or more templates are being instantiated forever.
## Instantiate
Convert a template to a frame by executing all the code associated with a template's attributes and storing the results in matching attributes in the frame.
## Lookupwang
A lookup that captures a totally unexpected value. This is a reference to the nonsensical game show [Numberwang](https://www.youtube.com/results?search_query=numberwang) by comedians Mitchell and Webb. Lookupwang usually happens when using _modifiable inputs_ pattern incorrectly, causing a lookup that retrieves the template instead of the instantiated frame.
## Override
When amending or instantiating, replacing an attribute's value with a new one. Some overrides completely replace, while others capture the original value in some way.
## Re-parenting
Changing the ancestor of a frame using a well-placed definition to _capture_ a lookup.
## Template
An unevaluated proto-frame. Instead of attributes with values, attributes in templates are code to compute values. When a template is instantiated, its attributes will be evaluated. This is similar to JavaScript's prototypes and more distantly related to classes in other object-oriented languages.
