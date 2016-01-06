package focus

import cats.Functor
import cats.std.function._

import focus.Lens._

import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

class LensTest extends WordSpec with ScalaFutures {

  case class Address(street: String, city: String, State: String, zip: Int)
  case class Contact(phone: String, email: String, address: Address)
  case class Student(id: Int, name: String, contact: Contact)

  val myStreet = "123 street"
  val myAddress = Address(myStreet, "NY", "NY", 10003)
  val myContact = Contact("12345678", "test@test.com", myAddress)
  val myStudent = Student(1, "Tester", myContact)

  val contactLens = Lens[Student, Contact](
      _.contact,
      (c, s) => s.copy(contact = c)
    )

  val addressLens = Lens[Contact, Address](
      _.address,
      (a, c) => c.copy(address = a)
    )

  val streetLens = Lens[Address, String](
      _.street,
      (s, a) => a.copy(street = s)
    )

  "Lens" should {

    "obey the laws" in {

      //0. law if I get twice, I get the same answer
      val first = addressLens.get(myContact)
      val second = addressLens.get(myContact)
      assert(first == second)

      //1. law if I get, then set it back, nothing changes
      assert(addressLens.set(addressLens.get(myContact), myContact) == myContact)

      //2. law if I set, then get, I get what I set
      val newAddress = Address("456 Street", "Queens", "NY", 11366)
      val newContact = addressLens.set(newAddress, myContact)
      assert(addressLens.get(newContact) == newAddress)

      //3. law if I set twice then get, I get the second thing I set
      val newAddress2 = Address("789 Street", "Brooklyn", "NY", 11211)
      val res = addressLens.set(newAddress2, addressLens.set(newAddress, myContact))
      assert(addressLens.get(res) == newAddress2)
    }

    "modify" in {
      val updtreetApt = (s: String) => s + "Apt A"
      assert(updtreetApt(streetLens.get(myAddress)) == streetLens.get(streetLens.modify(updtreetApt, myAddress)))
    }

    "andThen in nested accessor" in {
      val caLens = contactLens >> addressLens
      assert(myAddress == caLens.get(myStudent))

      val newAddress = Address("456 Street", "Queens", "NY", 11366)
      assert(newAddress == caLens.get(caLens.set(newAddress, myStudent)))
    }

    "compose in nested accessor" in {
      val acLens = addressLens ~> contactLens
      assert(myAddress == acLens.get(myStudent))

      val newAddress = Address("456 Street", "Queens", "NY", 11366)
      assert(newAddress == acLens.get(acLens.set(newAddress, myStudent)))
    }

    "lift on modifyF" in {

      implicit val optionFunctor: Functor[Option] = new Functor[Option] {
        def map[A,B](fa: Option[A])(f: A => B) = fa map f
      }

      val updO = (a: Address) => Option(a)
      assert(addressLens.modifyF[Option](updO, myContact) == Option(myContact))

      import scala.concurrent.Future
      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val futureFunctor: Functor[Future] = new Functor[Future] {
        def map[A, B](fa: Future[A])(f: A => B) = fa map f
      }

      val updF = (a: Address) => Future(a)
      assert(addressLens.modifyF[Future](updF, myContact).futureValue == Future(myContact).futureValue)

      implicit val seqFunctor: Functor[Seq] = new Functor[Seq] {
        def map[A,B](fa: Seq[A])(f: A => B) = fa map f
      }

      val updC = (a: Address) => Seq(a)
      assert(addressLens.modifyF(updC, myContact) == Seq(myContact))

    }

    "get and set Option" in {
      assert(addressLens.getOption(myContact) === Option(myAddress))

      val newAddress = Address("456 Street", "Queens", "NY", 11366)
      assert(addressLens.setOption(Some(newAddress), myContact) === Contact("12345678", "test@test.com", newAddress))

      assert(addressLens.setOption(None, myContact) === myContact)
    }

    "works as state in for-comprehension" in {
      case class Article(id: Long, title: String, author: String)

      val aLens = Lens[Article, String] (
        _.title,
        (t, a) => a.copy(title = t)
      )

      val auLens = Lens[Article, String] (
        _.author,
        (au, a) => a.copy(author = au)
      )

      val a = Article(1, "A Brief History of Time", "Stephen")
      val expected = Article(1, "A Brief History of Time 2nd Version Hard cover", "Stephen Hawking")

      val res = (for {
        art1 <- aLens.modState(_ + " 2nd Version")
        art2 <- aLens.modState(_ + " Hard cover")
        aut1 <- auLens.modState(_ + " Hawking")
      } yield (art2, aut1)).runS(a).run

      assert(res === expected)

      def addVersion(s: String) = s + " 2nd Version"
      val addDes = (s: String) => s + " Hard cover"

      val res2 = (for {
        art1 <- aLens += addVersion
        art2 <- aLens += addDes
        aut1 <- auLens += (s => s + " Hawking")
      } yield (art2, aut1)).runS(a).run

      assert(res2 === expected)
    }

    "have LensCat[T] deltaAt" in {

      case class Image(id: Long, height: Int, width: Int) extends LensCat[Image]

      val image = Image(10, 480, 360)

      val hLens = Lens[Image, Int](
        _.height,
        (h, i) => i.copy(height = h)
      )

      val wLens = Lens[Image, Int](
        _.width,
        (w, i) => i.copy(width = w)
      )

      assert(image.deltaAt(hLens)(_ + 20) === Image(10, 500, 360))

      assert(image.deltaAt(hLens)(_ + 20).deltaAt(wLens)(_ => 400) == Image(10, 500, 400))

      assert(image.+=(wLens)(_ + 20) === Image(10, 480, 380))
    }

  }
}
