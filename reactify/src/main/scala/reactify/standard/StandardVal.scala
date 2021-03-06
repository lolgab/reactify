package reactify.standard

import reactify.{State, Val}

class StandardVal[T](f: => T, override val name: Option[String]) extends Val[T] {
  override val state: State[T] = new State[T](this, 1, () => f)

  state.update(None)
}
