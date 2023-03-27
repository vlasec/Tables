package cz.vlasec.tables

import Columns._
import cz.vlasec.tables.Table.{ColumnEntryMap, ColumnFilter, IdFilter, ValuesMap, ViewFilter}

/**
 * This library allows you to define table-based data with compile-time type safety.
 */
object Table {
  /** Mapping of optional columns of varying types to their values. Empty columns are omitted. */
  type ValuesMap[Source] = Map[Column[Source], _]
  /** A map of identifiers to the columns created by macro expansion. */
  type ColumnEntryMap[Source] = Map[Identifier[Source], Column[Source]]
  /** Filter for entries, useful to  */
  type ViewFilter = ColumnEntry[_] => Boolean
  type ColumnFilter = Column[_] => Boolean
  type IdFilter = Identifier[_] => Boolean
}

/**
 * A definition of table. It holds metadata that is necessary to create the data rows.
 *
 * @param rawColumns Tuple list of entries that came as a result of macro expansion.
 * @tparam Source Type of source data. All columns extract data from this type.
 */
private[tables] class TableDefinition[Source](private[tables] val rawColumns: Seq[ColumnDefinition[Source]]) {
  private[tables] val entries: Seq[ColumnEntry[Source]] = rawColumns.flatMap(_.expand())
  /** Mapping of identifiers to columns. */
  private[tables] val idMap: ColumnEntryMap[Source] = entries.toMap
  /** Columns in the order they will appear in output. */
  private[tables] val columns: Seq[Column[Source]] = entries.map(_._2)
}

/**  */
class Table[Source](definition: TableDefinition[Source]) {
  def this(rawColumns: Seq[ColumnDefinition[Source]]) = this(new TableDefinition(rawColumns))

  /** A view with given filters. */
  def view(filter: ViewFilter = _ => true) = new View(definition, filter)

  /** Produces a new column of this table. Doesn't keep original data object in memory. */
  def read(data: Source): Row[Source] =
    new Row(definition, definition.columns.flatMap(column => column.optRead(data).map(column -> _)).toMap)
}

/** A view that filters the table headers and rows, omitting columns that are filtered out. */
class View[Source] private[tables](tableDefinition: TableDefinition[Source], filter: ViewFilter) {
  /** Sequence of all visible columns in this view. `headers` and `toSeq` are filtered, `read` is unaffected. */
  private val filteredColumns: Vector[Column[Source]] =
    tableDefinition.entries.filter(filter).map(_._2).toVector
  /** Headers of the table, filtered to view. */
  val headers: Seq[String] = filteredColumns.map(_.header)

  /** Outputs a row into a sequence of `Option[Any]`. */
  def rowToSeq(row: Row[Source]): Seq[Option[Any]] = row.toSeq(filteredColumns)

  /** Outputs a row into a sequence of String, with `""` used for empty columns. */
  def rowToSeqString(row: Row[Source]): Seq[String] = rowToSeq(row).map(_.map(_.toString).getOrElse(""))

  /** Narrows the view by provided filter. Only columns that pass through both filters can go to output. */
  def narrowView(subView: ViewFilter): View[Source] =
    new View(tableDefinition, entry => filter(entry) && subView(entry))

  /** Narrows the view by provided filter. Only columns that pass through both filters can go to output. */
  def intersect(otherView: View[Source]): View[Source] =
    otherView.narrowView(filter)
}

/** Only fairly simple untyped blacklisting is provided. For whitelisting, enumerations could be useful. */
trait FilterHelper {
  /** Filters a row based on identifiers. */
  def idFilter(filter: IdFilter): ViewFilter = (e: ColumnEntry[_]) => filter(e._1)

  /** Blacklists all columns that contain provided definitions in their identifiers. */
  def idPathBlacklist(blacklist: ColumnDefinition[_]*): ViewFilter =
    idFilter(id => !blacklist.exists(id.contains))

  /** Whitelists provided full identifiers. */
  def idWhitelist(whitelist: Identifier[_]*): ViewFilter =
    idFilter(whitelist.contains)

  /** Filters a row based on columns. */
  def columnFilter(filter: ColumnFilter): ViewFilter = (e: ColumnEntry[_]) => filter(e._2)
}

/** A table row doesn't publicly display its state. Instead, it gives accessors to its contents. */
class Row[Source] private[tables] (tableDef: TableDefinition[Source], values: ValuesMap[Source]) {
  /** This implementation gets visible */
  private[tables] def toSeq(view: Seq[Column[Source]]): Seq[Option[Any]] = view.map(values.get)

  /**
   * Optional typed getter, to retrieve data from the row. Works with all types of columns and identifiers.
   *
   * Generally it is a better idea to take your data from the original data object, as rows are mostly suited to
   * be rendered to output format by a [[View]]. However, if you need to retrieve the data from a row, you can.
   */
  def optGet[Target](identifier: TypedIdentifier[Source, Target]): Option[Target] = (identifier match {
    case column: TypedColumn[Source, Target] => values.get(column)
    case id: TypedIdentifier[Source, Target] => tableDef.idMap.get(id).flatMap(values.get)
  }).asInstanceOf[Option[Target]]
}