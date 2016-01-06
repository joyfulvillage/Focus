Focus
=====

A lightweight Scala lens library to manipulate immutable objects without pain

## Lens

***Lens*** is a powerful functional abstraction to access and update complex data types.
It is a simple concept to offer getter and setter on immutable objects.

```scala
  case class Address(street: String, city: String, state: String, zip: Int)
  
  val address = Address("123-45 67th Street", "NY", "NY", 12345)
  
  val streetLens = Lens[Address, String](
    _.street,                     //getter
    (s, a) => a.copy(street = s)  //setter
  )
  
  streetLens.get(address)                   //"123-45 67th Street"
  streetLens.set("42 Time Square", address) //Address("42 Time Square", "NY", "NY", 12345)
```

## Why do we need this?

Scala does provide simple getter-setter-like syntax:

```scala
  address.street                          //"123-45 67th Street"
  address.copy(street = "42 Time Square") //Address("42 Time Square", "NY", "NY", 12345)
```

which look simpler than the code above, right?

However, case could be different when we come to a nested object

## Nested-Accessor
```scala
  case class Address(street: String, city: String, state: String, zip: Int)
  case class Contact(phone: String, email: String, address: Address)
  case class Student(id: Int, name: String, contact: Contact)
```

In this case, if I want to access and update the address of a student, the code would be:

```scala

  student.contact.address.street //"123-45 67th Street"

  student.copy(
    contact.copy(
      address.copy(street = "42 Time Square")
    )
  ) //Address("42 Time Square", "NY", "NY", 12345)
```

## It is composable

The solution to make the code more simple (or readable) is to chain up multiple lenses

```scala
  val saLens = contactLens andThen addressLens //Lens[Student, Address]
  
  saLens.set("42 Time Square", student)        //Address("42 Time Square", "NY", "NY", 12345)
```

## LensCat trait

By extends the case class with `LensCat[T]`, which gives a DSL function, `deltaAt`, to manipulate the field:

```scala
  case class Order(type: String, volume: Long) extends LensCat[Order]
  val order = Order("limit", 100)
  
  limitOrder.deltaAt(typeLens)(_ => "market")
```

or as builder pattern 
```scala
  order
    .deltaAt(typeLens)(_ => "market")
    .deltaAt(volumeLens)(_ => 200)
```

## Different ways to create Lens
 
There is *two* ways to create Lens through Focus

Through declaration by providing get and set function with `Lens.apply`
```scala
  val streetLens = Lens[Address, String](
    _.street,
    (s, a) => a.copy(street = s)
  )
```

Or through macro annotation

```scala
  @GenLens case class Address(street: String, city: String, state: String, zip: Int)
```

Fully automated `@GenLens` can only works with case class. 
It generates or modifies a companion object of the class and insert lens for each of the fields.

```scala
Address.streetLens  //Lens[Address, String]
Address.cityLens    //Lens[Address, String]
Address.stateLens   //Lens[Address, String]
Address.zipLens     //Lens[Address, Int]
```

and extends the case class with `LensCat[T]`


To avoid code pollution, you can specific only the fields you want `Lens` to be generated

```scala
@GenLens("street", "zip") case class Address(street: String, city: String, state: String, zip: Int)

address.cityLens //fail at compilation
```

## Getting Start

In `build.sbt`, add the following entries:

```scala
  libraryDependencies += "com.github.joyfulvillage" %% "focus" % "0.1.0"
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```



