package code
package snippet

import clojure.lang.IPersistentVector
import net.liftweb._
import http._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import lib._
import org.specs2.mutable.Specification
import org.specs2.specification.AroundExample
import org.specs2.execute.AsResult
import scala.collection.JavaConversions._

object HelloWorldTestSpecs extends Specification with AroundExample{
  val session = new LiftSession("", randomString(20), Empty)
  val stableTime = now

  /**
   * For additional ways of writing tests,
   * please see http://www.assembla.com/spaces/liftweb/wiki/Mocking_HTTP_Requests
   */
  def around[T : AsResult](body: =>T) = {
    S.initIfUninitted(session) {
      DependencyFactory.time.doWith(stableTime) {
        AsResult( body)  // execute t inside a http session
      }
    }
  }

  "Scala Clojure Interopt" should {
    "Convert a Seq to a Clojure lazy seq" in {
      val from = List(1,2,3)
      val converted = ClojureInterop.scalaToClojure(from)
      converted.isInstanceOf[java.util.List[_]] must_== true
      (converted.asInstanceOf[java.util.List[Any]]: Seq[Any]) must_== from
    }

    "Convert a Map to a Clojure map" in {
      val from = Map(1 -> "dog", "moose" -> "breath")
      val converted = ClojureInterop.scalaToClojure(from)
      converted.isInstanceOf[java.util.Map[_, _]] must_== true
      (converted.asInstanceOf[java.util.Map[Any, Any]]: scala.collection.mutable.Map[Any, Any]) must_== from
    }

    "Convert a Product to a Clojure Vector" in {
      val from = ("2", "4", 42)
      val converted = ClojureInterop.scalaToClojure(from).asInstanceOf[java.util.List[Any]]
      converted.isInstanceOf[IPersistentVector] must_== true
      (converted.size()) must_== from.productArity
      converted.get(0) must_== from._1
      converted.get(1) must_== from._2
      converted.get(2) must_== from._3
    }

  }

  "HelloWorld Snippet" should {
    "Put the time in the node" in {
      val hello = new HelloWorld
      Thread.sleep(1000) // make sure the time changes

      val str = hello.howdy(<span>Welcome to your Lift app at <span id="time">Time goes here</span></span>).toString

      str.indexOf(stableTime.toString) must be >= 0
      str must startWith("<span>Welcome to")
    }
  }
}

class MyHelloWorld extends CometActor {

}
