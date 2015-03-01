# The Flabbergast Programming Language
![](https://rawgithub.com/apmasell/flabbergast/master/flabbergast.svg)

Flabbergast is a object-oriented macro system that uses contextual lookup (dynamic scope) and inheritance to making writing complex configurations easy.

In most languages, afterthoughts are not appreciated. However, most configurations are nothing but afterthoughts and exceptions. “I want the test version of the webserver to be the same as the production except for the database connection.” “I want the videos SMB share to be the same as the documents SMB share with a few extra users.” Flabbergast is built to service “except”, “and”, and “but”.

View the [User Community](https://plus.google.com/communities/103010827049942376743).

There is nothing so unutterably stupid that people won't waste their time on it. – Mark Rendle, [The Worst Programming Language Ever](http://skillsmatter.com/skillscasts/6088-the-worst-programming-language-ever). This was not explicitly about Flabbergast.

## Documentation

There are two important pieces of documentation: the rather dry [language spec man page](flabbergast_language.7) and the friendly [manual](flabbergast-manual.md).

The language spec describes the syntax and behaviour with formal semantics (or, at least, a poorly-written attempt at formal semantics). This is provided as a manual page such that it is included with the installed packages.

The manual describes the syntax is broader strokes and a more prosaic explanation of how it works with examples. It also describes philosophy, design patterns, and libraries.

The language can be compiled to a virtual machine simpler to implement than the full language spec and a self-hosting compiler is provided. The VM is documented in the [KWS VM](kws-vm.md) document, which does not include formal semantics because they are largely implied by the language spec.

## Implementation
There are not two implementations of Flabbergast. Presently, there is a bootstrap interpreter, written in Vala, which implements most of Flabbergast. It lacks some of the less crucial features and the parser does not support comments There is also work being done on the self-hosting version, which is not yet complete.

The self-hosting compiler is rather strange, as it is not really self-hosting. The self-hosting compiler is actually a Flabbergast program that, with added syntax templates, generates a compiler for a Flabbergast compiler in a target programming language. This generated compiler is capable of producing KWS VM bytecodes that can be run on one of the VMs targeted for various platforms.

Each platform also contains an implementation of the runtime library and non-portable pieces of each library.

## Patches
Patches are welcome and patches are preferred to whining. For details, see [Conduct unbecoming of a hacker](http://sealedabstract.com/rants/conduct-unbecoming-of-a-hacker/). In general, the rules are as follows:

- It is very important that the language stays in a small sandbox. Free roaming of the host system or the Internet is not permitted.
- Changes to the compiler or runtime that are invisible to user are welcome. Bonus points if you apply an improvement to multiple platforms.
- Changes to the libraries are welcome with two conditions: names are `lower_snake_case` and if the interface of a platform-dependent library is changed, the other implementations get updated, even if they are stubs that throw “not implemented” errors.
- Every change to the language itself is a tattoo. We must be conservative and sure that we aren't getting a face tattoo we will regret later. Expect slow and cautious. Many things deserve to be put in the `X` experimental keyword name space until a community process gives feedback.

## Miscellaneous
The Flabbergast language would not be possible without the help of [Kyle](https://github.com/edarc) and Jonathan.

The logo is the worst symbolic representation of contextual lookup, previously called inside-out lookup.

## Alternatives
You may be interested in [Jsonnet](http://google.github.io/jsonnet/doc/), which is inspired by the same forces, but takes design decisions in very different ways.
