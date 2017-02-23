import scala.language.implicitConversions

package object reactify {
  /**
    * Converts a `State[T]` to `T` implicitly. This is useful for DSL type-based operations like `5 + stateVar`.
    */
  implicit def state2Value[T](p: State[T]): T = p()
}
