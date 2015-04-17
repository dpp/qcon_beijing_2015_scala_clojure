package code
package snippet

import clojure.java.api.Clojure
import clojure.lang.{IFn, IPersistentVector}
import net.liftweb._
import http._
import net.liftweb.actor.LiftActor
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

    "Do a deep convert" in {
      val from = ('frog, new LiftActor {
        override protected def messageHandler: PartialFunction[Any, Unit] =  {
          case _ =>
        }
      })
      val converted = ClojureInterop.scalaToClojure(from).asInstanceOf[java.util.List[Any]]
      converted.isInstanceOf[IPersistentVector] must_== true
      (converted.size()) must_== from.productArity
      converted.get(0) must_== ClojureInterop.toKeyword("frog")
      converted.get(1).isInstanceOf[LiftActor] must_== true
    }

    "Convert Clojure to Scala" in {
      ClojureInterop.clojureToScala(ClojureInterop.eval("""["foo" 42 :dog {:dog 88, "cat" :moose}]""")) must_==
        Vector("foo", 42, 'dog, Map('dog -> 88, "cat" -> 'moose))
    }

    "Test soup to nuts" in {
      var got: Vector[Any] = Vector()
      val act = new LiftActor {
        override protected def messageHandler: PartialFunction[Any, Unit] = {
          case x => got :+= ClojureInterop.clojureToScala(x)
        }
      }

      Actorize.postMsg.invoke('add -> act)
      Actorize.postMsg.invoke("Wombat")
      Actorize.postMsg.invoke("Sloth")
      Actorize.postMsg.invoke('remove -> act) // remove
      Actorize.postMsg.invoke('add -> act) // we should get the history
      Actorize.postMsg.invoke("3") // one more message, shouldn't dup if

      Thread.sleep(200)
      got must_== Vector(Vector(), "Wombat", "Sloth", Vector("Wombat", "Sloth"), "3")
    }

  }


}
