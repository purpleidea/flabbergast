These are more theoretical questions. Practical questions are answered in [the manual](flabbergast-manual.md).
# How do I do reflection/evaluation?

This isn't supported and isn't likely to be.

The language is sufficiently flexible that most of the things that would need reflection can be done using features of the language. Since frames can be introspected, there's no need to have Java/C#-style reflection of classes. Reflecting on templates is rather unhelpful since they contain the attribute expressions can't be converted into callable functions.

Converting strings to executable code is a rather undesirable prospect (_i.e._, `eval`). Again, since the language is meant to be embedded in other systems, there may be security implications to loading code from untrusted sources.

The more restricted version of this is allowing strings to be used for lookup. In some situations, this is useful, but the desired behaviour can usually be accomplished with:

    For n : Name, v : x Where n == name Reduce acc ?? v With acc : Null

This, however, only does a single layer of direct lookup, instead of contextual lookup.

The predecessor of Flabbergast had an evaluation function and the use cases were either limited or insane–some users constructed entire frames from strings, substituting supplied values into attribute names and values, even though better tools existed in the language. In general, it was use to bind frames together in unrelated parts of the program. With a hypothetical `Eval`, it tended to look as follows:

    cpu_limits : {
       foo : 1.0
       bar : 1.5
       baz : 0.75
    }
    task_tmpl : Template {
      name : Required
      cpu_limit : Eval "cpu_limits.\(name)"
    }
    tasks : {
       foo : task_tmpl {
         name : "foo"
       }
    }

In practise, this created more problems than it solved. First, debugging the evaluated expressions was difficult since they were opaque, and this only became more difficult as the names were more arbitrary. In this particular case, the fricassée solution proposed above would work (and indeed it would in most situations). As the item is operating on the string instead of the syntax tree, devious things can be done, for example, `name : "foo + 0.5"`, which might make `name` unusable in other contexts, or have unforeseen consequences.

Moreover, there is precious little reason not to set the attribute in the correct frame. That is much clearer.

With sufficient demonstration of utility, a reasonable plan would be to include a reflected contextual lookup operator. Imagine something to the effect of `Lookup Using [$a, $b, $c] In x` as the equivalent of `Lookup a.b.c In x`. This will be a difficult sales pitch.
# Why are templates special? Why can't every frame be used as a template? 

TL;DR: Frames are collections of values and templates are collections of code/functions/computations. It was too hard to reason about anything else properly.

This was a design decision based on previous experience with a language that *did* have frames and templates be synonymous. The problem is pretty thorny.

Let's start with why there are templates at all:

    t : Template {
    	w : 5
    	x : y + 1
    	y : Required
    }

What should `t.w` be? You could make the argument that it should be 5. Given that, what is `t.x`? Well, that depends on `t.y`, which is not supplied, so `y` will be an error, and `t.x` will be an error too. For most fields, in most templates, the answer will always be an error, so looking inside an uninstantiated template is not generally helpful. So, Flabbergast makes a distinction between templates and frames partially to make sure lookup never traverses into a template.

That's not quite the question: why not be able to use a frame in a context where a template is expected?

Let's go through some examples:

    t : Template {
    	x : y + 1
    	y : Required
    }
    
    a : t {
    	y : 1
    }
    
    b : a {
    	y : 2
    }

So, what should `b.x` be? If you say 2, then life is simple: each value is fixed during instantiation and this really exactly the same as:

    b : For left : { y : 2 }, right : a, name : Name Select name : left ?? right

Presumably, you are interested in the case where `b.x` is 3. That means each attribute value isn't really a value; it's a value and a hidden function to compute that value. By amending a frame, we have to throw away all the values and call all the functions again to generate new values.

Okay, that makes the implementation a little more complicated, but who cares? Let's get to the practical problems.

Suppose we changed the above example to have:

    a : If something Then t { y : 1 } Else Null

Well, `b` might break when `something` is off. I might have a good reason to want to turn `a` on and off, but I'd never really have a reason to remove a template. If I'm not using a template, it just sits there; no harm done.

Okay, let's try this:

    t : Template {
    	x : y + 1
    	y : Required
    }
    
    a : t {
    	y : 1
    }
    
    b : a {
    	y : 2
    }
    
    f : For left : a, right : b, name : Name Select name : left + right
    
    c : f {
    	y : 0
    }

What's the correct value of `c.x`? You have two options: 5 or something else.

If you chose something else, that question of “who cares about the implementation” just became you. Somehow, `x` needs to track what happened inside the fricassée operation. The complexity of those little hidden functions just became really complex. And what's the right value for `f.x`? If fricassée is now operating on functions, not values, then it is:

    f : {
    	x : Let left : y + 1, right : y + 1 In left + right
    	y : Left left : 1, right : 2, In left + right
    }

That's almost certainly not what you wanted and that's really difficult to reason about as a programmer. Try to image that with a `Reduce`.

Okay, suppose it is 5. That means there was something about the fricassée operation that dropped the hidden function. What other syntax should do that? Let? Append? If? Only frame-manipulation expressions? How do I decide? How far back does that history go? Does that dropping happen when the fricassée read the values from its input, or when it wrote its values? What happened to the environment in the fricassée expression? Do these hidden functions have environment or are they pure?

And there are some frames that behave specially: frames generated by `[]`, `Let`, or arguments to functions are frames, but they are evaluated in the surrounding context rather than in the frame's context. If you write a function-like template:

    fn : Template {
    	value : This
    	x : y + 1
    	y : Required
    }
    
    x : 5
    f : fn(y : x)
    g : f { }

What should `g.x` be? Circular evaluation? 6? I don't know.

The most pragmatic thing to do is to remove the hidden functions: frames are collections of values and templates are collections of functions. Instantiation evaluates those functions; amending rewrites functions; and every other part of the language operates on values.

I definitely see the convenience in it, but it's hard to come up with behaviour across the whole language that is consistent and intuitive. Ultimately, every time you need that functionality, you can always rewrite that code as:

    a_tmpl : Template t { y : 1 }
    a : a_tmpl {}
    b : b_tmpl { y : 2 }

# Why are there no functions/lambdas?
TL;DR: Everything you can do with a lambda, you can do with a template. If the syntax is awkward, we can fix it.

This argument is very similar to a “problem” in Java: anonymous inner classes (AIC). Java never provided a function pointer or closure. If you wanted such a thing, you simply defined an interface with one method and implemented or invoked that method as appropriate. That means there is one less group of types in the language; there doesn't need too be function types since these things are just regular interfaces (meaning Java only has classes, interfaces, arrays, and primitive types). As Microsoft developed J++, and later C#, they begged to differ. They added delegates, which were closures, and dropped AICs. In most cases, delegates were much simpler to use and you could pass methods, instance or static, as a delegates. Writing a lambda is also much simpler as a delegate. Inside the VM, delegates are...strange. They involve pointers that get wrapped up in special delegate objects that have some different semantics (_e.g._, delegates can't have inheritance relationships), meaning there is one more group of types. At the end of the day, AICs can support multiple methods and delegates can't and the implementation issues are not of concern to most programmers.

The question is which is better: AICs or delegates? I think the answer is this: ultimately AICs because they are both more powerful and a simpler abstraction. However, given the choice between Java and C#, I would choose C# because it has delegates. As a user of a programming language, I mostly care about what I have to type to get something done. The 95% of cases where delegates result in less boilerplate, is worth the 5% of the time when I need the power of AICs and I really never care about the implementation.

Clearly, templates are to functions as AICs are to delegates. As a design decision, Flabbergast is set on templates and not having functions. That being said, using templates for function-like purposes should be easy. If we need new syntax in the language to make that easier, we can provide better sugar. After nearly a decade, the Java developers have figured this out and provided a lambda expressions and method references in Java 8. Let me be clear: these are pure syntactic sugar. They are just methods for making AICs easier to write. If we need more of that in Flabbergast, I'm happy to oblige.

# Why is declaration done with colon instead of the equal sign?
It is a question of morality. Assignments are inflicted on people and don't necessarily agree with what they want or how they seem themselves. To tie that up with the idea of equality is very unsettling. The colon symbol doesn't have any emotional baggage; it can be a declaration without dragging along societal pressure.
