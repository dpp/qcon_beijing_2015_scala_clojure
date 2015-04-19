package code.lib

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import clojure.java.api.Clojure
import clojure.lang.{RT, IFn}
import net.liftweb.actor.LiftActor
import net.liftweb.common.Box
import net.liftweb.http.{S, LiftSession}
import net.liftweb.util.{ThreadGlobal, Helpers}

/**
 * Created by dpp on 3/27/15.
 */
object ClojureInterop {
  lazy val require: IFn = Clojure.`var`("clojure.core", "require")
  lazy val eval: IFn = Clojure.`var`("clojure.core", "eval")

  lazy val cvt = {
    require.invoke(Clojure.read("code.util"))
    require.invoke(Clojure.read("code.core"))

    Clojure.`var`("code.util", "to-c")
  }

  lazy val ctos = {
    require.invoke(Clojure.read("code.util"))
    require.invoke(Clojure.read("code.core"))

    Clojure.`var`("code.util", "to-s")
  }


  lazy val str = {
    Clojure.`var`("clojure.core", "str")
  }

  lazy val theEval = Clojure.`var`("clojure.core", "eval")

  lazy val transitReader = {
    require.invoke(Clojure.read("cognitect.transit"))
    Clojure.`var`("cognitect.transit", "reader")
  }

  lazy val transitRead = {
    require.invoke(Clojure.read("cognitect.transit"))
    Clojure.`var`("cognitect.transit", "read")
  }

  lazy val transitWriter = {
    require.invoke(Clojure.read("cognitect.transit"))
    Clojure.`var`("cognitect.transit", "writer")
  }

  lazy val transitWrite = {
    require.invoke(Clojure.read("cognitect.transit"))
    Clojure.`var`("cognitect.transit", "write")
  }

  lazy val keyword = {
    Clojure.`var`("clojure.core", "keyword")
  }

  def boot(): Unit = {
    cvt
    str
  }

  /**
   * Converts the Scala class into something that's Clojure-friendly
   * @param in the Scala-ish class
   * @return a Clojure-friendly class
   */
  def scalaToClojure(in: Any): Any = cvt.invoke(in)

  /**
   * Converts the Clojure class into something that's Scala-friendly
   * @param in the Clojure-ish class
   * @return a Scala-friendly class
   */
  def clojureToScala(in: Any): Any = ctos.invoke(in)

  /**
   * Send the String through the Clojure reader and then eval it
   * @param str the String that contains an S-expression
   * @return the eval'ed result
   */
  def eval(str: String): Object =
  theEval.invoke(Clojure.read(str))

  /**
   * Requires a Clojure package
   * @param packageName
   */
  def requirePackage(packageName: String): Unit = {
    require.invoke(Clojure.read(packageName))
  }

  def toKeyword(in: String): Object = {
    keyword.invoke(in)
  }

  /**
   * A thin layer over Clojure.var
   * @param pkg the package
   * @param vr the var name
   * @return the var
   */
  def findVar(pkg: String, vr: String): IFn = {
    requirePackage(pkg)
    Clojure.`var`(pkg, vr)
  }

  /**
   * Reload a named Clojure package
   *
   * @param packageName the package to reload
   */
  def reload(packageName: String): Unit = {
    require.invoke(Clojure.read(packageName), toKeyword("reload"))
  }

  def callClojure[T](pkg: String, function: String, a: Any)(implicit m: Manifest[T]): Box[T] =
  Helpers.tryo{
    val ifn: IFn = Clojure.`var`(pkg, function)
  for {
    ret <- Box.asA[T](ifn.invoke(a))

  } yield ret}.flatMap(a => a)

  def transitWrite(in: Any): String = {
    val bos = new ByteArrayOutputStream(4096)
    val writer = transitWriter.invoke(bos, toKeyword("json"))
    transitWrite.invoke(writer, in)
    new String(bos.toByteArray, "UTF-8")
  }

  def transitRead(in: String): Object = {
    val bis = new ByteArrayInputStream(in.getBytes("UTF-8"))
    val reader = transitReader.invoke(bis, toKeyword("json"))
    transitRead.invoke(reader)
  }
}

/**
 * LiftActor is a trait in Scala-land, but in Clojure-land,
 * we want to subclass LiftActor, so we use the Scala compiler
 * to reify LiftActor into a class that can be accessed in
 * Clojure-land
 */
abstract class MyActor extends LiftActor

class MySingleFunc(ifn: IFn) extends Function1[Any, Object] {
  def apply(p: Any): Object = ifn.invoke(p)
}

trait InSession {
  protected def session = S.session openOrThrowException "Called in a snippet"

}