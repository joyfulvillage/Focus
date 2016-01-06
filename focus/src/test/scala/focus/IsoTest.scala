package focus

import org.scalatest.WordSpec

class IsoTest extends WordSpec {

  "Iso" should {

  	def s2i = (x: String) => Integer.parseInt(x.takeWhile(_ != '.'))
    def i2s = (x: Int) => x.toString
    def d2i = (x: Double) => x.toInt
    def i2d = (x: Int) => x.toDouble

    val s2iIso = Iso[String, Int] (s2i, i2s)
    val i2sIso = Iso[Int, String] (i2s, s2i)
    val d2iIso = Iso[Double, Int] (d2i, i2d)

  	 "obey the laws" in {
       //1 law if I get and reserveGet, I get the same value
       assert(s2iIso.reverseGet(s2iIso.get("456")) == "456")

       //2 law Vince versa from law 1
       assert(s2iIso.get(s2iIso.reverseGet(456)) == 456)
     }
  
    "translate" in {
      assert(s2iIso.get("123") equals 123)
    }

    "reverse" in {
      val rev = s2iIso.reverse
      assert(rev.get(123) == i2sIso.get(123))
    }

    "compose" in {
      val d2s = d2iIso ~> i2sIso
      assert(d2s.get(3.141516) == "3")
      assert(3.0 == d2s.reverseGet("3.141516"))
    }

    "get Option" in {
      assert(s2iIso.getOpt(Some("3")) == Some(3))
      assert(s2iIso.getOpt(None) == None)
    }

    "reverseGet Option" in {
      assert(s2iIso.reverseGetOpt(Some(3)) == Some("3"))
      assert(s2iIso.reverseGetOpt(None) == None)
    }

    "as Lens" in {
      val s2iLens: Lens[String, Int] = s2iIso.asLens

      assert(s2iLens.get("3.141616") === 3)
      assert(s2iLens.set(4, "3.141516") === "4")
    }
  }
}
