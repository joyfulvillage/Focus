package focus

import cats.Functor
import cats.state.State

/**
 * Trait provides a [[Lens]] that abstract `get` and `set` function
 * {{{
 *   get: (A) => B
 *   set: (B, A) => A
 * }}}
 *
 * @tparam A The container type of Lens
 * @tparam B The target type of Lens
 */
trait Lens[A, B] {

  /** get the target value B from container A */
  def get(a: A): B

  /** set a new target value of B to container A and return a new A */
  def set(b: B, a: A): A

  /** get an Option B from A */
  def getOption(a: A): Option[B] = Option(get(a))

  /** set a new target value B to A if Option B is defined, otherwise, return the existing A */
  def setOption(optB: Option[B], a: A): A =
    optB match {
      case Some(b) => set(b, a)
      case None => a
    }

  /** modify target value B and return a new container A */
  def modify(f: B => B, a: A): A

  /** modify target value B using Functor function and return a new container A */
  def modifyF[F[_]: Functor](f: B => F[B], a: A): F[A]

  /** cast to State */
  def asState: State[A, B] = State(a => (a, get(a)))

  /**
   * modify new B and cast to State, as generator in for-comprehension
   * {{{
   *   val a = Article(1, "A Brief History of Time", "Stephen")
   *
   *   (for {
   *     art1 <- aLens.modState(_ + " 2nd Version")
   *     art2 <- aLens.modState(_ + " Hard cover")
   *     aut1 <- auLens.modState(_ + " Hawking")
   *   } yield (art2, aut1)).runS(a).run
   *   //Article(1, "A Brief History of Time 2nd Version Hard cover", "Stephen Hawking")
   * }}}
   *
   * @param f function to modify target value B
   * @return State[A, B] where A is updated with new B
   */
   def modState(f: B => B): State[A, B] =
    State(a => (set(f(get(a)), a), f(get(a))))

  /** alias to modState */
  def += (f: B => B): State[A, B] = modState(f)

  /** chain wth Lens[B, C] to form a Lens[A, C] */
  def andThen[C](that: Lens[B, C]): Lens[A, C] =
    Lens[A, C] (
      (a: A) => that.get(get(a)),
      (c: C, a: A) => modify(b => that.set(c, b), a)
    )

  /** compose a new Lens[C, B] from Lens[C, A] */
  def compose[C](that: Lens[C, A]): Lens[C, B] =
    Lens[C, B] (
      (c: C) => get(that.get(c)),
      (b: B, c: C) => that.modify(a => set(b,a), c)
    )

  /** alias to andThen */
  def >>[C](that: Lens[B, C]): Lens[A, C] = andThen(that)

  /** alias to compose */
  def ~>[C](that: Lens[C, A]): Lens[C, B] = compose(that)
}

/** Factory for [[Lens]] instance */
object Lens {

  /** an identity Lens of A */
  def id[A] = Iso.id[A].asLens

  /** The apply function for Lens to take get and set function to construct a Lens */
  def apply[A, B](_get: A => B, _set: (B, A) => A): Lens[A, B] =

    new Lens[A, B] { self =>

      def get(a: A): B = _get(a)

      def set(b: B, a: A) = _set(b, a)

      def modify(f: B => B, a: A) = _set(f(_get(a)), a)

      def modifyF[F[_]: Functor](f: B => F[B], a: A): F[A] =
        Functor[F].map(f(_get(a)))(_set(_, a))
    }

  /**
   * F-bounded typed trait to provide DSL syntactic sugar
   *
   * @tparam T type of the class extends from
   */
  trait LensCat[T <: LensCat[T]] { self: T =>

    /**
     * provide a tool to update target value of container
     * can be used as builder pattern
     * {{{
     *   student
     *     .deltaAt(classIdLens)(_ => "CS101")
     *     .deltaAt(gradeLens)(_ => A)
     * }}}
     *
     * @param l Lens[T, S] where T is the self type
     * @param f function to modify target value typed S
     * @tparam S type of target value
     * @return new container T
     */
    def deltaAt[S](l: Lens[T, S])(f: S => S): T =
      l.set(f(l.get(self)), self)

    /** alias to deltaAt */
    def += [S](l: Lens[T, S])(f: S => S): T = deltaAt(l)(f)
  }
}



