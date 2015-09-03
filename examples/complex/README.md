# A Complex Aurora Example

Let's say we work at Rooster Solutions and are responsible for running the Chicken Service. It's a fairly standard application, with a separated replicated front-end, middle-ware, database, and an accessory server that looks something like this:

![](https://rawgithub.com/flabbergast-config/flabbergast/master/examples/complex/chickenA.svg)

Now, writing this configuration in Aurora's internal configuration format would be straight forward and probably easier than the Flabbergast equivalent. Flabbergast gets more useful as the configuration gets more complicated, which is inevitably what happens.

Let's start with the basic configuration: [chicken1](chicken1.o_0)

This configuration is pretty fragile. Let's replace all the hard-coded server-sets and paths into something better. [chicken2](chicken2.o_0)

After being deployed for a while, our first discovery, after email from some eggasperated customers, is that the site is slow in some parts of the world. Geographically distributing the service should be easy since we can just deploy the same configuration to different data centres... Except, not quite. We soon discover that different parts of the world have different egg-eating preferences and we need to adjust the egg laying service based on load. Actually, we see different total loads, so let's make those adjustable too: [chicken3](chicken3.o_0)

Great. As we get more users, that database starts to fill and we can no longer hold all our data in a single machine. We decide to separate it out into multiple databases, like so:

![](https://rawgithub.com/flabbergast-config/flabbergast/master/examples/complex/chickenB.svg)

Now, all of the database servers are almost the same, but the resource requirements vary based on the dataset loaded. So, let's create a database configuration that knows how to deal with that. [chicken4](chicken4.o_0) Adding new datasets it pretty trivial, and the command line arguments of the server even auto-configure. Oh, and we have to deal with the fact that the egg database can't be loaded in the US for legal reasons.

This is all running smoothly until some code that was not properly tested make it into production and stuff crashes. The developer points out that the testing environment doesn't really match the production one and that's how the bug slipped in. Let's create a testing environment that really matches production, but is much less resource intensive: [chicken5](chicken5.o_0)

Great, now we've moving. The problem for developers is that they have to create binary image every time, then paste the ID of the built package into the configuration. It would be great to have a shell script that can build the Docker image and deploy the Aurora configuration. If this where elsewhere, we could just write a shell script... Fortunately, Flabbergast generates text, so we _can_ generate a shell script. [chicken6](chicken6)

## Where to Go Next
This should give you an idea of what is possible using Flabbergast. Certainly, some of this is possible is Aurora's configuration system, Pystachio, but not all of it is easy and Flabbergast is capable of vertical integration. For instance, if you need to run an SQL command in a shell script in Aurora, you can write that using Flabbergast's SQL library and have it type checked before it is deployed. Suppose you want to write an A-B testing setup, Flabbergast could be used to write not only the Aurora configuration to bring up the A-B setup, but make use of production configs to gather logs, generate build commands, and run the test.

Flabbergast lets you build a suite of templates to cut, paste, and remix as you need. Unlike pure text templating, your templates can have the smarts to sanity check their inputs. For an example, have a look at [lib:sql](https://github.com/flabbergast-config/flabbergast/master/lib/sql.o_0), which makes type-checked SQL statements. The mechanism is very simple: each “piece” of SQL extends `base_expr_tmpl` which provides an SQL command fragment (`sql_expr` and a type `sql_type`. From only this information, type-checked correct SQL statements can be generated. Despite the type-checking, the library is still only generating text, so you can recompose it any context: it can generate an SQL statement to be executed in Flabbergast, run in a shell script, or put in an generated source file of another language.
