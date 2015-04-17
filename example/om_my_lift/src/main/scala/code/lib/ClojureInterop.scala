package code.lib

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

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
  def findVar(pkg: String, vr: String): IFn = Clojure.`var`(pkg, vr)

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