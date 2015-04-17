# [Scala](http://scala-lang.org/) & [Clojure](http://clojure.org/), [Lift](http://liftweb.net) & [Om](https://github.com/omcljs/om)

## All playing nice together

To run this example, you must install [sbt](http://www.scala-sbt.org/)
and [lein](http://leiningen.org/).

Open two command prompts.

In the first, `cd omish` and then `lein cljsbuild auto dev`. This will
compile the the Om-based client-side.

Next, in the current directory, type `sbt` and then `container:start`.

Finally, point one or more browsers to http://localhost:8080

And boom... an Om/Lift/Clojure based chat app using [Transit](http://blog.cognitect.com/blog/2014/7/22/transit)
to serialize the data.

The `omish/core.cljs` file contains the client code. When the server pushes
new data to the client, the server calls `receive` via [Lift's Comet](http://seventhings.liftweb.net/comet)
and Lift's [cross address space actors](http://lift.la/blog/lift_30#acting-across-address-spaces).

`Actorize.scala` sets up the "push to the client" actor that listens to the chat
messages and the "get stuff from client" actor that receives messages from the client
and Transit-decodes the message and forwards it to the chat server.

The chat server is in `code/core.clj`. It's a simple [core.async](https://github.com/clojure/core.async)
thing that adds and removes "listeners" and distributes chat messages to the listeners
as they come in.

`ClojureInterop.scala` and `code/util.clj` provides a bunch of helper functions/methods to
bridge between Clojure and Scala.
