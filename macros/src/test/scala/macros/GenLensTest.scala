package macros

import org.scalatest.WordSpec

class GenLensTest extends  WordSpec {

  @GenLens case class Address(street: String, city: String, state: String, zip: Int)
  @GenLens("ID", "address") case class Student(ID: Long, name: String, address: Address)
  @GenLens("id") case class Record(ID: Long, score: Int)

  @GenLens case class Message(id: Long, content: String)
  object Message {
    def dummy = "Dummy!"
  }

  val address = Address("12-34 56th Street", "NY", "NY", 12345)
  val student = Student(1, "James", address)
  val record = Record(2, 50)
  val message = Message(3, "empty content")

  "GenLens" should {

    "generates lens for all fields" in {

      val newAddress = Address("78-90 100th Street", "LA", "CA", 67890)

      assert(Address.streetLens.get(address) == address.street)
      assert(Address.streetLens.get(Address.streetLens.set(newAddress.street, address)) == newAddress.street)

      assert(Address.cityLens.get(address) == address.city)
      assert(Address.cityLens.get(Address.cityLens.set(newAddress.city, address)) == newAddress.city)

      assert(Address.stateLens.get(address) == address.state)
      assert(Address.stateLens.get(Address.stateLens.set(newAddress.state, address)) == newAddress.state)

      assert(Address.zipLens.get(address) == address.zip)
      assert(Address.zipLens.get(Address.zipLens.set(newAddress.zip, address)) == newAddress.zip)
    }

    "generates lends for specific fields only" in {

      assert(Student.IDLens.get(student) == student.ID)
      assert(Student.addressLens.get(student) == student.address)

      val ru = scala.reflect.runtime.universe

      ru.typeOf[Student.type].decl(ru.TermName("IDLens")).asMethod
      ru.typeOf[Student.type].decl(ru.TermName("addressLens")).asMethod

      val thrown = intercept[scala.ScalaReflectionException] {
        ru.typeOf[Student.type].decl(ru.TermName("nameLens")).asMethod
      }
      assert(thrown.getMessage == "<none> is not a method")
    }

    "annotation parameters are not case sensitive" in {
      assert(Record.IDLens.get(record) == record.ID)
    }

    "still works if an existing object is defined" in {
      assert(Message.idLens.get(message) == message.id)
      assert(Message.dummy == "Dummy!")
    }

    "have LensCat[T] extended" in {
      assert(address.deltaAt(Address.streetLens)(_ + " Apt 2") === Address("12-34 56th Street Apt 2", "NY", "NY", 12345))
    }
  }
}
