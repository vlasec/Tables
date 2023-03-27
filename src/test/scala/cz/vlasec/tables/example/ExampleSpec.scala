package cz.vlasec.tables.example

import cz.vlasec.tables.example.data.Fixture.{athleteSue, eventCortina, eventGarmisch, refereeZoe}
import cz.vlasec.tables.example.tables.AthleteColumns.FullName
import cz.vlasec.tables.example.tables.EventResultsColumns.{GiantSlalom, Location, Slalom}
import cz.vlasec.tables.example.tables.EventResultsTable
import cz.vlasec.tables.example.tables.EventResultsViews._
import cz.vlasec.tables.example.tables.RaceResultColumns.{Referee, Winner}
import cz.vlasec.tables.example.tables.StandingsEntryColumns.{Athlete, Time}
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class ExampleSpec extends FlatSpec with Matchers {

  val table: EventResultsTable = EventResultsTable.read(Seq(eventGarmisch, eventCortina))

  "Table" should "output only downhill data" in {
    table.toCsvLines(DownhillResults).mkString("\n") shouldBe getCsv("csv/downhill.csv")
  }

  it should "output only winners" in {
    table.toCsvLines(Winners).mkString("\n") shouldBe getCsv("csv/winners.csv")
  }

  it should "output intersected views" in {
    table.toCsvLines(DownhillResults.intersect(Winners)).mkString("\n") shouldBe getCsv("csv/intersect.csv")
  }

  it should "output basic info" in {
    table.toCsvLines(BasicInfo).mkString("\n") shouldBe getCsv("csv/basic.csv")
  }

  "Row" should "correctly retrieve direct data" in {
    table.data.head.optGet(Location) shouldBe Some(eventGarmisch.location)
  }

  it should "correctly retrieve single level embedded data" in {
    table.data.head.optGet(Slalom :: Referee) shouldBe Some(refereeZoe.fullName)
  }

  it should "correctly retrieve multi level embedded data" in {
    table.data.head.optGet(Slalom :: Winner :: Athlete :: FullName) shouldBe Some(athleteSue.fullName)
  }

  it should "correctly retrieve empty result" in {
    table.data(1).optGet(GiantSlalom :: Winner :: Time) shouldBe None
  }

  def getCsv(name: String): String =
    Source.fromResource(name).getLines.mkString("\n")
}
