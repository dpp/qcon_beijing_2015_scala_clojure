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



}

