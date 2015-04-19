package code
package snippet

import net.liftweb.actor.LiftActor
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.json.JsonAST.JString

import net.liftweb.common._
import code.lib._
import ClojureInterop._

object Actorize extends InSession {
  lazy val postMsg = findVar("code.core", "post-msg")

  def render = {
    <tail>
      {
      val clientProxy =
        session.serverActorForClient("omish.core.receive",
          shutdownFunc = Full(actor => postMsg.invoke('remove -> actor)),
          dataFilter = transitWrite(_))

      postMsg.invoke('add -> clientProxy) // register with the chat server

      // Create a server-side Actor that will receive messages when
      // a function on the client is called
      val serverActor = new LiftActor {
        override protected def messageHandler =
        {
          case JString(str) => postMsg.invoke(ClojureInterop.transitRead(str))
        }
      }

      Script(JsRaw("var sendToServer = " + session.clientActorFor(serverActor).toJsCmd).cmd)
    }
    </tail>

  }

}

