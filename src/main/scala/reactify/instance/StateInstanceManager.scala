package reactify.instance

import reactify.{Listener, Observable, State}

class StateInstanceManager[T](state: State[T], cache: Boolean, recursion: RecursionMode) {
  @volatile private var previousValue: T = _
  @volatile private var instance: StateInstance[T] = StateInstance.empty[T]
  @volatile private[reactify] var observables: Set[Observable[_]] = Set.empty
  private val threadLocal = new ThreadLocal[StateInstance[T]] {
    override def initialValue(): StateInstance[T] = StateInstance.uninitialized[T]
  }
  private val updateInstanceListener: Listener[Any] = (_: Any) => updateInstance()

  def get: T = useInstance(_.get)

  def useInstance[R](f: StateInstance[T] => R): R = {
    val startingInstance = threadLocal.get()
    val instance: StateInstance[T] = startingInstance match {
      case i if i.isUninitialized => this.instance
      case i if i.isEmpty => throw new RuntimeException("Reached top of StateInstance stack!")
      case i => i
    }
    threadLocal.set(instance.previous)
    try {
      f(instance)
    } finally {
      threadLocal.set(startingInstance)
    }
  }

  def updateInstance(): Unit = synchronized {
    // Reset cache
    instance.reset()

    var value: T = previousValue
    val references = StateInstanceManager.withReferences {
      value = get
    }
    val oldObservables = observables
    observables = references.observables - state      // Update new observables list and remove reference to this

    if (oldObservables != observables) {
      // Out with the old
      oldObservables.foreach { ob =>
        if (!observables.contains(ob)) {
          ob.asInstanceOf[Observable[Any]].detach(updateInstanceListener)
        }
      }

      // In with the new
      observables.foreach { ob =>
        if (!oldObservables.contains(ob)) {
          ob.asInstanceOf[Observable[Any]].observe(updateInstanceListener)
        }
      }
    }

    // Cleanup recursive
    val cleaned = instance.cleanup(references)
//    println(s"References for Observables: ${references.observables}, Instances: ${references.instances}")
//    if (cleaned ne instance) {
//      println(s"\tOriginal: $instance")
//      println(s"\tCleaned: $cleaned")
//    } else {
//      println("\tnothing to clean!")
//    }
    instance = cleaned

    val pv = previousValue
    previousValue = value
    state.changed(value, pv)
  }

  def replaceInstance(f: () => T): Unit = synchronized {
    val previous = recursion match {
      case RecursionMode.Static => StateInstance.empty[T]
      case RecursionMode.None => StateInstance.empty[T]
      case RecursionMode.RetainPreviousValue => if (instance.isEmpty) instance else StateInstance.cached(instance.get)
      case RecursionMode.Full => instance
    }
    instance = if (recursion == RecursionMode.Static) {
      StateInstance.cached(f())
    } else if (cache) {
      StateInstance.updatable(f, None, previous)
    } else {
      StateInstance.functional(f, previous)
    }
    updateInstance()
  }
}

object StateInstanceManager {
  private val localReferences = new ThreadLocal[Option[LocalReferences]] {
    override def initialValue(): Option[LocalReferences] = None
  }

  def withReferences(f: => Unit): LocalReferences = {
    val references = new LocalReferences
    localReferences.set(Some(references))
    try {
      f
      references
    } finally {
      localReferences.remove()
    }
  }

  def referenced[T](observable: Observable[T]): Unit = localReferences.get().foreach(_.observables += observable)
  def referenced[T](instance: StateInstance[T]): Unit = localReferences.get().foreach(_.instances += instance)
}

class LocalReferences {
  var observables: Set[Observable[_]] = Set.empty
  var instances: Set[StateInstance[_]] = Set.empty
}