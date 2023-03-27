package cz.vlasec.tables

import cz.vlasec.tables.Columns._

/** This object covers column types with erased `Target` that can be used for convenience. */
object Columns {
  /** Type-unsafe column definition to be used where type information is erased. */
  type ColumnDefinition[Source] = TypedColumnDefinition[Source, _]
  /** Type-unsafe column to be used where type information is erased. */
  type Column[Source] = TypedColumn[Source, _]
  /** Type-unsafe identifier to be used where type information is erased. */
  type Identifier[Source] = TypedIdentifier[Source, _]
  /** Entries as defined by macro expansion. */
  type ColumnEntry[Source] = (Identifier[Source], Column[Source])
}

/**
 * This type is a supertype of all column-related types. It defines columns in a table-based structure.
 *
 * The columns are type-safe during compilation, and this trait is sealed for purpose of exhaustive pattern matching.
 * There are only two direct subtypes that behave differently in resulting table:
 *
 *   1. [[TypedMacroColumn]] expands to multiple columns and it needs composite identifiers.
 *   1. [[TypedColumn]] is a singular column that is self-identifying in the resulting table.
 *
 * This type is mostly used as an umbrella for its two subtypes where they are expected to coexist, for example
 * in configuration of visibility, or in column definitions of a macro to allow multi-levels macros.
 */
sealed trait TypedColumnDefinition[Source, Target] {
  /** Column's header in the resulting output format. */
  def header: String

  /** An optional extraction that reads target type if it's present in source. */
  def optRead: Source => Option[Target]

  /** Macro expansion. It can expand a definition into multiple columns. */
  private[tables] def expand(): Seq[ColumnEntry[Source]]
}

/**
 * This type represents a single column in a table-based structure. It is type-safe during compile-time.
 *
 * Basically, it is a named extractor of a single (optional) attribute from the `Source`.
 *
 * @tparam Source Type of source the column gets populated from
 * @tparam Target Type of target that this column extracts for the table
 */
trait TypedColumn[Source, Target] extends TypedColumnDefinition[Source, Target] with TypedIdentifier[Source, Target] {
  /** Terminator to macro expansion: `TypedColumn` only expands to itself. */
  private[tables] final def expand(): Seq[ColumnEntry[Source]] = Seq(this -> this)

  final def contains(definition: ColumnDefinition[_]): Boolean = this == definition
}

/** A mixin for [[TypedColumnDefinition]] types that uses non-optional `read` for convenience. */
trait Required[Source, Target] {
  /** A non-optional extraction from source. */
  def read: Source => Target

  final def optRead: Source => Option[Target] = read.andThen(Some(_))
}

/** Embedded column is a result of a macro expansion, with composed headers and extractors. */
class EmbeddedColumn[Source](val header: String, val optRead: Source => Option[_]) extends TypedColumn[Source, Any]

/**
 * This column consists of multiple columns that the macro column embeds. Their targets may vary.
 *
 * @tparam Source Source data type for this macro column.
 * @tparam Flow Target data type for this macro. It is also source data type for embedded columns.
 */
trait TypedMacroColumn[Source, Flow] extends TypedColumnDefinition[Source, Flow] {
  /** The column definitions used to generate the macro. Recommended to be defined externally. */
  def columnDefinitions: Seq[ColumnDefinition[Flow]]

  /**
   * Composes macro column's header with embedded column's header. By default the macro gets prepended,
   * using space as separator.
   */
  protected def composeHeaders: (String, String) => String = _ + " " + _

  /** Macro expansion. This method causes a cascading macro expansion that expands macros inside it as well. */
  private[tables] final def expand(): Seq[ColumnEntry[Source]] =
    columnDefinitions.flatMap(_.expand().map(embedColumn))

  /** Embeds provided column whose source is the macro column's target. */
  private def embedColumn(entry: ColumnEntry[Flow]): ColumnEntry[Source] =
    (this :: entry._1) -> new EmbeddedColumn(
      composeHeaders(header, entry._2.header),
      optRead.andThen(_.flatMap(entry._2.optRead))
    )
}

/**
 * Identifies a simple [[TypedColumn]], or a [[CompositeTypedIdentifier]] path to it.
 */
sealed trait TypedIdentifier[Source, Target] {
  /** Prepends the identifier with a macro that takes source data from a `Super` source. */
  def ::[Super](macroColumn: TypedMacroColumn[Super, Source]): TypedIdentifier[Super, Target] =
    CompositeTypedIdentifier(macroColumn, this)

  /** A test whether the identifier contains given column definition. Mostly useful for filters. */
  def contains(definition: ColumnDefinition[_]): Boolean
}

/**
 * Identifies a column that is a result of one or more macro expansions.
 *
 * It is a list-like structure with [[TypedColumn]] at the end of list.
 */
case class CompositeTypedIdentifier[Source, Flow, Target](current: TypedMacroColumn[Source, Flow], next: TypedIdentifier[Flow, Target])
  extends TypedIdentifier[Source, Target] {
  final def contains(definition: ColumnDefinition[_]): Boolean = current == definition || next.contains(definition)
}
