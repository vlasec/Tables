package cz.vlasec.tables.example.tables

import cz.vlasec.tables.Columns.ColumnDefinition
import cz.vlasec.tables.{Required, TypedColumn, TypedMacroColumn}
import cz.vlasec.tables.example.data.{Athlete, EventResults, RaceResults, StandingsEntry}

import java.time.format.DateTimeFormatter
import scala.concurrent.duration.SECONDS

object AthleteColumns {
  // One could use some library to simplify enum creation, but I don't want to depend on a bag of cats with Enumeratum.
  val definitions: Seq[ColumnDefinition[Athlete]] =
    Seq(FullName, Born, Country, Category, HasDopingHistory, LatestDopingScandal)

  sealed trait AthleteColumn[T] extends TypedColumn[Athlete, T]

  sealed abstract class Opt[T](val header: String, val optRead: Athlete => Option[T]) extends AthleteColumn[T]

  sealed abstract class Req[T](header: String, val read: Athlete => T) extends Opt[T](header, read.andThen(Some(_)))

  // Column definitions follow

  object FullName extends Req[String]("Full name", _.fullName)

  object Born extends Req[Int]("Born", _.yearOfBirth)

  object Country extends Req[String]("Country", _.country)

  // Various data conversions can be a part of the column structure.
  object Category extends Req[String]("Category", a => if (a.sex == 'F') "Women" else "Men")

  // Or, simplifications of complex objects to a boolean that shows existence
  object HasDopingHistory extends Req[Boolean]("Has doping history", _.dopingHistory.nonEmpty)

  // Or, just showing the headOption of the collection. We are creating a table out of complex data structure.
  object LatestDopingScandal extends Opt[Int]("Latest doping scandal (year)", _.dopingHistory.headOption.map(_.year))
}

object StandingsEntryColumns {
  val definitions: Seq[ColumnDefinition[StandingsEntry]] =
    Seq(Time, Athlete, Status)

  sealed abstract class Opt[T](val header: String, val optRead: StandingsEntry => Option[T])
    extends TypedColumn[StandingsEntry, T]

  sealed abstract class AthleteCol(val header: String, val read: StandingsEntry => Athlete)
    extends TypedMacroColumn[StandingsEntry, Athlete] with Required[StandingsEntry, Athlete] {
    // Sometimes you might want to modify how the headers compose.
    override def composeHeaders: (String, String) => String = (_, c) => c

    override def columnDefinitions: Seq[ColumnDefinition[Athlete]] = AthleteColumns.definitions
  }

  // Column definitions follow

  case object Time extends Opt[BigDecimal]("Time (s)", _.time.map(BigDecimal(_).setScale(3)))

  case object Athlete extends AthleteCol("Athlete", _.athlete)

  case object Status extends Opt[String]("Status", _.status)

  // In some cases, having implicit classes or helper methods can also help keep the column definitions clear
  implicit class RichStandingsEntry(entry: StandingsEntry) {
    def status: Option[String] = (entry.finished, entry.qualified) match {
      case (true, true) => None
      case (false, true) => Some("DNF")
      case (_, false) => Some("DNQ")
    }
  }
}

// This definition even
object RaceResultColumns {
  val definitions: Seq[ColumnDefinition[RaceResults] with RaceResultsColumn[_]] =
    Seq(Track, Referee, Winner, Second, Third)

  // You might also want to have a parent trait for all columns.
  // It can only extend any of the column types if it doesn't combine regular columns with macros.
  sealed trait RaceResultsColumn[T]

  sealed abstract class Req[T](val header: String, val read: RaceResults => T)
    extends TypedColumn[RaceResults, T] with RaceResultsColumn[T] with Required[RaceResults, T]

  sealed abstract class Result(val header: String, val optRead: RaceResults => Option[StandingsEntry])
    extends TypedMacroColumn[RaceResults, StandingsEntry] with RaceResultsColumn[StandingsEntry] {

    override def columnDefinitions: Seq[ColumnDefinition[StandingsEntry]] = StandingsEntryColumns.definitions
  }

  // Column definitions follow

  case object Track extends Req[String]("Track name", _.trackName)

  case object Referee extends Req[String]("Referee name", _.mainReferee.fullName)

  case object Winner extends Result("1st", _.athleteResults.headOption)

  case object Second extends Result("2nd", _.athleteResults.tail.headOption)

  case object Third extends Result("3rd", _.athleteResults.drop(2).headOption)
}

// The top level columns. Note that there is no distinction between top level and embeds in this.
// Only the Table class really makes any distinction.
object EventResultsColumns {
  sealed abstract class Str(val header: String, val read: EventResults => String)
    extends TypedColumn[EventResults, String] with Required[EventResults, String]

  sealed abstract class Race(val header: String, val optRead: EventResults => Option[RaceResults])
    extends TypedMacroColumn[EventResults, RaceResults] {
    def columnDefinitions: Seq[ColumnDefinition[RaceResults]] = RaceResultColumns.definitions
  }

  val columns: Seq[ColumnDefinition[EventResults]] = Seq(EventName, DateStarted, Location, Downhill, Slalom, GiantSlalom)

  // Column definitions follow

  case object EventName extends Str("Event name", _.eventName)

  case object DateStarted extends Str("Date started", _.dateStarted.format(DateTimeFormatter.ISO_DATE))

  case object Location extends Str("Location", _.location)

  case object Downhill extends Race("Downhill", _.downhill)

  case object Slalom extends Race("Slalom", _.slalom)

  case object GiantSlalom extends Race("Giant slalom", _.giantSlalom)
}
