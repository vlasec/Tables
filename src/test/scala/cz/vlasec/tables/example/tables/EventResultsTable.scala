package cz.vlasec.tables.example.tables

import cz.vlasec.tables.Columns.{Column, Identifier}
import cz.vlasec.tables._
import cz.vlasec.tables.example.data.EventResults
import cz.vlasec.tables.example.tables.AthleteColumns.{Category, HasDopingHistory, LatestDopingScandal}
import cz.vlasec.tables.example.tables.EventResultsColumns._
import cz.vlasec.tables.example.tables.RaceResultColumns.Referee

class EventResultsTable(val data: Seq[Row[EventResults]]) {
  // An oversimplified CSV export that doesn't escape anything
  def toCsvLines(view: View[EventResults]): Stream[String] =
    (view.headers +: data.toStream.map(view.rowToSeqString)).map(_.mkString(";"))
}

// The table metadata. It can be a very simple one-liner unless you want to define extra features.
object EventResultsTable extends Table[EventResults](columns) {
  // Custom method that reads a list of source objects into a list of row.
  def read(results: Seq[EventResults]): EventResultsTable = new EventResultsTable(results.map(read))
}

object EventResultsViews extends FilterHelper {
  // You can use blacklisting helper utility
  val SimplifiedAthlete: View[EventResults] =
    EventResultsTable.view(idPathBlacklist(Category, HasDopingHistory, LatestDopingScandal))

  // You can explicitly whitelist selected columns
  val BasicInfo: View[EventResults] =
    EventResultsTable.view(idWhitelist(EventName, DateStarted, Location, Downhill :: Referee, Slalom :: Referee))

  // You can define it yourself with a more complicated definition
  val DownhillResults: View[EventResults] =
    EventResultsTable.view(idFilter(id => !Set(Slalom, GiantSlalom).exists(id.contains) || id.contains(Referee)))

  // You can even define it by looking at the resulting headers
  val Winners: View[EventResults] =
    SimplifiedAthlete.narrowView(columnFilter(col => !(col.header.contains("2nd") || col.header.contains("3rd"))))
}
