package cz.vlasec.tables.example.data

import java.time.LocalDate
import scala.concurrent.duration.FiniteDuration

case class EventResults(eventName: String, dateStarted: LocalDate, location: String,
                        downhill: Option[RaceResults], slalom: Option[RaceResults], giantSlalom: Option[RaceResults])

case class RaceResults(trackName: String, mainReferee: Referee, athleteResults: List[StandingsEntry])

case class StandingsEntry(athlete: Athlete, time: Option[Double], finished: Boolean, qualified: Boolean)

case class Referee(fullName: String, yearOfBirth: Int, refereeLicenseId: String)

case class Athlete(fullName: String, yearOfBirth: Int, country: String, sex: Char, dopingHistory: List[DopingCase])

case class DopingCase(year: Int, monthsBanned: Int, drugUsed: String)
