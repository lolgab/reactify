package com.outr.reactify

import scala.language.experimental.macros

/**
  * Val, as the name suggests, is like a Scala `val`. This represents an immutable value that is set in the first place
  * and then not modified. However, since the value set may be built from `Observables`, the generated value may change
  * over time as its dependencies are modified. This class is `Observable` and will only fire changes if the underlying
  * value is derived from one or more `Observables`.
  *
  * @tparam T the type of value retained by this `State`
  */
class Val[T](val observables: List[Observable[_]], internal: => T) extends State[T] {
  override protected val internalFunction: () => T = () => internal

  observables.distinct.foreach(_.attach(_ => fire(get)))

  /**
    * Convenience method to get the current value.
    */
  def value: T = get
}

object Val {
  /**
    * Creates a new instance of a `Val[T]`
    */
  def apply[T](value: => T): Val[T] = macro Macros.newVal[T]

  /**
    * Convenience method to pre-evaluate the contents as opposed to apply that applies the contents as an anonymous
    * function.
    */
  def static[T](value: T): Val[T] = new Val[T](Nil, value)
}