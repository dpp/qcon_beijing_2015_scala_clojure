package code.lib

import clojure.java.api.Clojure
import clojure.lang.{RT, IFn}
import net.liftweb.actor.LiftActor
import net.liftweb.common.Box
import net.liftweb.util.Helpers

/**
 * Created by dpp on 3/27/15.
 */
object ClojureInterop {
  lazy val require: IFn = Clojure.`var`("clojure.core", "require")
  lazy val eval: IFn = Clojure.`var`("clojure.core", "eval")

  lazy val cvt = {
    require.invoke(Clojure.read("code.core"))
    require.invoke(Clojure.read("code.util"))
    Clojure.`var`("code.util", "to-c")
  }
  lazy val str = {
    require.invoke(Clojure.read("code.core"))
    require.invoke(Clojure.read("code.util"))
    Clojure.`var`("clojure.core", "str")
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
   * Requires a Clojure package
   * @param packageName
   */
  def requirePackage(packageName: String): Unit = {
    RT.load("foo")
    require.invoke(Clojure.read(packageName))
  }

  def toKeyword(in: String): Object = {
    keyword.invoke(in)
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
}

abstract class MyActor extends LiftActor