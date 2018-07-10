package reactify

import reactify.reaction.{Reaction, ReactionStatus}
import reactify.standard.StandardDep

trait Dep[T, R] extends Var[T] with Reaction[R] {
  def owner: Var[R]
  def t2R(t: T): R
  def r2T(r: R): T

  override val state: State[T] = new State[T](this, 1L, () => r2T(owner()))

  state.update(None)
  owner.reactions += this

  override def set(value: => T): Unit = owner := t2R(value)

  override def apply(value: R, previous: Option[R]): ReactionStatus = {
    state.update()
    ReactionStatus.Continue
  }
}

object Dep {
  def apply[T, R](owner: Var[R])
                 (implicit r2T: R => T, t2R: T => R): Dep[T, R] = new StandardDep[T, R](None, owner, r2T, t2R)
  def apply[T, R](owner: Var[R],
                  name: String)
                 (implicit r2T: R => T, t2R: T => R): Dep[T, R] = new StandardDep[T, R](Option(name), owner, r2T, t2R)
}