package code.lib

import clojure.java.api.Clojure
import clojure.lang.{RT, IFn}
import net.liftweb.actor.LiftActor

/**
 * Created by dpp on 3/27/15.
 */
object ClojureInterop {
  val require: IFn = Clojure.`var`("clojure.core", "require")
  val eval: IFn = Clojure.`var`("clojure.core", "eval")


  def boot(): Unit = {
    require.invoke(Clojure.read("code.core"))
    require.invoke(Clojure.read("code.util"))

    val cvt = Clojure.`var`("code.util", "to-c")
    val str = Clojure.`var`("clojure.core", "str")
    println(s"List ${str.invoke(cvt.invoke(Vector(1, 2, 3)))}\n" +
      s"Map ${str.invoke(cvt.invoke(Map("foo" -> "bar", "baz" -> 33)))}\n" +
      s"Tuple ${str.invoke(cvt.invoke((2, "moo")))}")

    val f2 = cvt.invoke(((a: Int) => println(s"The number is $a")): Function1[Int, Unit]).asInstanceOf[IFn]

    f2.invoke(42)

    val actor = Clojure.`var`("code.util", "build-actor").invoke(((a: Object) => println(s"dude, got $a"))).asInstanceOf[LiftActor]

    actor ! 8899
  }
}

abstract class MyActor extends LiftActor