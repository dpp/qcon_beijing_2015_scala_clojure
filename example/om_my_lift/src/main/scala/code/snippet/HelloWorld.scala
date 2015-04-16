package code 
package snippet

import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.{JsExp, JsCmds, JsCmd}
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.http._
import net.liftweb.json
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST.JString

import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import java.util.Date
import code.lib._
import Helpers._

class HelloWorld {
  lazy val date: Box[Date] = DependencyFactory.inject[Date] // inject the date

  // replace the contents of the element with id "time" with the date
  def howdy = "#time *" #> date.map(_.toString)

  /*
   lazy val date: Date = DependencyFactory.time.vend // create the date via factory

   def howdy = "#time *" #> date.toString
   */
}

class Actorize {


  def render = {
    for (
      sess <- S.session
    ) yield {
      // get a server-side actor that when we send
      // a JSON serializable object, it will send it to the client
      // and call the named function with the parameter
      val clientProxy = sess.serverActorForClient("changeNode",
        shutdownFunc = Full(() => 33),
      datafilter = 44
      )

      // Create a server-side Actor that will receive messaes when
      // a function on the client is called
      val serverActor = new ScopedLiftActor {
        override def lowPriority = {
          case JString(str) => clientProxy ! ("You said: " + str)
        }
      }

      Script(JsRaw("var sendToServer = " + sess.clientActorFor(serverActor).toJsCmd).cmd )
    }
  }

}

class FancyTHingy extends CometActor {
/**
 * It's the main method to override, to define what is rendered by the CometActor
 *
 * There are implicit conversions for a bunch of stuff to
 * RenderOut (including NodeSeq).  Thus, if you don't declare the return
 * turn to be something other than RenderOut and return something that's
 * coercible into RenderOut, the compiler "does the right thing"(tm) for you.
 * <br/>
 * There are implicit conversions for NodeSeq, so you can return a pile of
 * XML right here.  There's an implicit conversion for NodeSeq => NodeSeq,
 * so you can return a function (e.g., a CssBindFunc) that will convert
 * the defaultHtml to the correct output.  There's an implicit conversion
 * from JsCmd, so you can return a pile of JavaScript that'll be shipped
 * to the browser.<br/>
 * Note that the render method will be called each time a new browser tab
 * is opened to the comet component or the comet component is otherwise
 * accessed during a full page load (this is true if a partialUpdate
 * has occurred.)  You may want to look at the fixedRender method which is
 * only called once and sets up a stable rendering state.
 */
def render: RenderOut = NodeSeq.Empty

override def localShutdown(): Unit = {
  super.localShutdown()
}



override def lifespan = Full(LiftRules.clientActorLifespan.vend.apply(this))

override def hasOuter = false

override def parentTag = <div style="display: none"/>

override def lowPriority: PartialFunction[Any, Unit] = {
case jsCmd: JsCmd => partialUpdate(JsCmds.JsSchedule(JsCmds.JsTry(jsCmd, false)))
case jsExp: JsExp => partialUpdate(JsCmds.JsSchedule(JsCmds.JsTry(jsExp.cmd, false)))
case jv: JsonAST.JValue => {
val s: String = json.pretty(json.render(jv))
partialUpdate(JsCmds.JsSchedule(JsCmds.JsTry(JsRaw("foo"+"("+s+")").cmd, false)))
}
case x: AnyRef => {
import json._
implicit val formats = Serialization.formats(NoTypeHints)

val ser: Box[String] = Helpers.tryo(Serialization.write(x))

ser.foreach(s => partialUpdate(JsCmds.JsSchedule(JsCmds.JsTry(JsRaw("foo"+"("+s+")").cmd, false))))

}

case _ => // this will never happen because the message is boxed

}
}
