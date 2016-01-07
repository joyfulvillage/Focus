package macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

/**
 * Macro Annotation to provide fully automated Lens
 *
 * {{{
 *   @GenLens case class Message(id: Long, content: String)
 * }}}
 *
 * will be expanded to:
 *
 * {{{
 *   case class Message(id: Long, content: String) extends LensCat[Message]
 *
 *   object Message {
 *     def idLens = Lens[Message, Long](
 *       _.id,
 *       (a, b) => b.copy(id = a)
 *     )
 *     def contentLens = Lens[Message, String](
 *       _.content,
 *       (a, b) => b.copy(content = a)
 *     )
 *   }
 * }}}
 *
 * @param includeOnly fields to generate Lens, not case-sensitive.  (Empty list implies all)
 */
class GenLens(includeOnly: String*) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LensMacro.impl
}

/** Provide functions of [[GenLens]] macro expansion */
class LensMacro(val c: whitebox.Context) {

  import c.universe._

  def getAnnotationParams: List[String] =
    c.prefix.tree match {
      case q"new $name( ..$params )" => transformToName(params)
      case _ => List.empty[String]
    }

  def transformToName(trees: List[Tree]): List[String] =
    trees.collect { case Literal(Constant(field: String)) => field}

  def lensDef(clsName: TypeName, fields: List[ValDef]): List[Tree] = {

    val sensitiveFields: List[String] = getAnnotationParams.map(_.toLowerCase)

    val filteredFields = if (sensitiveFields.nonEmpty)
      fields.filter(field => sensitiveFields.contains(field.name.toString.toLowerCase))
    else
      fields

    filteredFields.map { field =>
      val fieldName: TermName = field.name.decodedName.toTermName
      val lensName = TermName(fieldName.toString + "Lens")
      q"""def $lensName =
          focus.Lens[$clsName, ${field.tpt}](
            _.$fieldName,
            (a, b) => b.copy($fieldName = a)
          )
        """
    }
  }

  def modifyClassDef(classDef: Tree): Tree = {
    val q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" = classDef

    val parentz: List[Tree] = parents

    //this is depending on the path of LensCat: focus.Lens.LensCat
    val lensCatTrait: Tree =
      AppliedTypeTree(
        Select(
          Select(
            Ident(TermName("focus")),
            TermName("Lens")
          ),
          TypeName("LensCat")
        ),
        List(Ident(TypeName(s"$tpname")))
      )

    val newParents = parentz ++ List(lensCatTrait)

    q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$newParents { $self => ..$stats }"
  }

  def modifyCompObject(clsName: TypeName, fields: List[ValDef], compDef: ModuleDef): Tree = {
    val q"object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" = compDef
    q"""
        object $tname extends { ..$earlydefns } with ..$parents { $self =>
          ..$body
          ..${lensDef(clsName, fields)}
        }
      """
  }

  def createCompObject(clsName: TypeName, fields: List[ValDef]): Tree =
    q"""
        object ${clsName.toTermName} {
          ..${lensDef(clsName, fields)}
        }
      """

  def resultExpr(classDef: Tree, objectDef: Tree): Expr[Any] =
    c.Expr( q"""
        $classDef
        $objectDef
      """)

  // syntax from http://docs.scala-lang.org/overviews/quasiquotes/syntax-summary.html
  def impl(annottees: c.Expr[Any]*): c.Expr[Any] = {
    annottees.map(_.tree) match {
      //case class only
      case (classDef @ q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }")
        :: Nil if mods.hasFlag(Flag.CASE) =>

        val objectDef = createCompObject(tpname, paramss.head)
        val clazzDef = modifyClassDef(classDef)

        resultExpr(clazzDef, objectDef)

      //case class with companion object
      case (classDef @ q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }")
        :: (compDef: ModuleDef) :: Nil if mods.hasFlag(Flag.CASE) =>

        val objectDef = modifyCompObject(tpname, paramss.head, compDef)
        val clazzDef = modifyClassDef(classDef)

        resultExpr(clazzDef, objectDef)

      case _ => c.abort(c.enclosingPosition, "Lens annotation only applies to case class")
    }
  }
}
