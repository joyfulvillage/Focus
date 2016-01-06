package focus

/**
 * Isomorphism to help converting between values that abstract `get` and `reverseGet`
 * {{{
 *   get:        (A) => B
 *   reverseGet: (B) => A
 * }}}
 *
 * @tparam A type to convert from
 * @tparam B type to convert to
 */
trait Iso[A, B] {

  /** get a B from a given A */
  def get(a: A): B

  /** get an A from a given B */
  def reverseGet(b: B): A

  /** modify A and get the corresponding B */
  def modify(f: A => A): B => B

  /** reverse the Iso access */
  def reverse: Iso[B, A]

  /** get from a Option type A */
  def getOpt(OptA: Option[A]): Option[B] =
    OptA map get

  /** reverse get from an Option type B */
  def reverseGetOpt(OptB: Option[B]): Option[A] =
    OptB map reverseGet

  /** compose Iso[A, C] from Iso[B, C] */
  def compose[C](that: Iso[B, C]): Iso[A, C] =
    Iso[A, C](
      (a: A) => that.get(get(a)),
      (c: C) => reverseGet(that.reverseGet(c))
    )

  /** cast to Lens[A, B] */
  def asLens: Lens[A, B] =
    Lens[A, B](
      get,
      (b, a) => reverseGet(b)
    )

  /** alias to compose */
  def ~>[C](that: Iso[B, C]): Iso[A, C] = compose(that)

  /** alias to reverse */
  def <-> : Iso[B,A] = reverse
} 

/** Factory for [[Iso]] instance */
object Iso {

  /** An identity instance of Iso */
  def id[A] =
    new Iso[A, A] { self =>
      def get(a: A) = a
      def reverseGet(a: A) = a
      def modify(f: A => A) = f
      def reverse = self
    }

  /** Apply function for Iso to take get and reverseGet to construct Iso */
  def apply[A, B](_get: A => B, _reverseGet: B => A): Iso[A, B] = {
    
    new Iso[A, B] { self =>

      def get(a: A) = _get(a)

      def reverseGet(b: B) = _reverseGet(b)

      def modify(f: A => A) = b => get(f(reverseGet(b)))

      def reverse = 
        Iso[B, A](
          (b: B) => reverseGet(b),
          (a: A) => get(a)
        )
    }
  }
}
