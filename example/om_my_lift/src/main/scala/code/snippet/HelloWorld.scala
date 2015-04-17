package code 
package snippet

import clojure.java.api.Clojure
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.http._
import net.liftweb.json.JsonAST.JString

import net.liftweb.common._
import code.lib._


object Actorize {
  lazy val postMsg = {
    ClojureInterop.require.invoke(Clojure.read("code.core"))
    ClojureInterop.findVar("code.core", "post-msg")
  }

  def render = {
    for (
      sess <- S.session
    ) yield {
      // get a server-side actor that when we a message to, the
      // message gets Transit encoded and sent to the
      // browser and the `messageFromServer` function is
      // called
      val clientProxy = sess.serverActorForClient("messageFromServer",
        dataFilter = ClojureInterop.transitWrite(_)
      )

      postMsg.invoke('add -> clientProxy) // register with the chat server

      // Create a server-side Actor that will receive messages when
      // a function on the client is called
      val serverActor = new ScopedLiftActor {
        override def lowPriority = {
          case JString(str) => postMsg.invoke(ClojureInterop.transitRead(str))
        }
      }

      Script(JsRaw("var sendToServer = " + sess.clientActorFor(serverActor).toJsCmd).cmd )
    }
  }

}

