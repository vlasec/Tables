package cz.vlasec.tables.example.data

import java.time.LocalDate
import scala.language.postfixOps

//noinspection SpellCheckingInspection
object Fixture {
  val athleteTom: Athlete = Athlete("Thomas Brown", 1997, "USA", 'M', List(DopingCase(2018, 6, "THC")))
  val athleteJoe: Athlete = Athlete("Joseph White", 1999, "GBR", 'M', Nil)
  val athleteSue: Athlete = Athlete("Suzanne Noir", 2000, "FRA", 'F', Nil)
  val athleteLis: Athlete = Athlete("Lisbeth Gelb", 1996, "DEU", 'F', List(DopingCase(2018, 12, "steroids")))

  val refereeJon: Referee = Referee("Jonathan Bat", 1982, "CAN:24625")
  val refereeZoe: Referee = Referee("Zoe Feathers", 1985, "NED:A193H")

  val eventGarmisch: EventResults = EventResults(
    eventName = "Kleiner lokaler Wettbewerb",
    dateStarted = LocalDate.of(2023, 1, 17),
    location = "Garmisch‑Partenkirchen",
    downhill = Some(RaceResults(
      "Kreuzjoch", refereeJon, List(
        StandingsEntry(athleteTom, Some(92.13), finished = true, qualified = true),
        StandingsEntry(athleteLis, Some(93.84), finished = true, qualified = true),
        StandingsEntry(athleteSue, None, finished = false, qualified = true),
        StandingsEntry(athleteJoe, None, finished = false, qualified = false),
      )
    )),
    slalom = Some(RaceResults(
      "Gudiberg", refereeZoe, List(
        StandingsEntry(athleteSue, Some(125.11), finished = true, qualified = true),
        StandingsEntry(athleteTom, None, finished = false, qualified = true),
      )
    )),
    giantSlalom = Some(RaceResults(
      "Gudiberg", refereeZoe, List(
        StandingsEntry(athleteLis, Some(114.21), finished = true, qualified = true),
        StandingsEntry(athleteJoe, Some(117.89), finished = true, qualified = true),
        StandingsEntry(athleteTom, Some(118.01), finished = true, qualified = true),
        StandingsEntry(athleteSue, None, finished = false, qualified = false),
      )
    ))
  )

  val eventCortina: EventResults = EventResults(
    eventName = "Una piccola competizione locale",
    dateStarted = LocalDate.of(2023, 2, 4),
    location = "Cortina d'Ampezzo",
    downhill = Some(RaceResults(
      "Mount Tofana", refereeZoe, List(
        StandingsEntry(athleteJoe, Some(81.73), finished = true, qualified = true),
        StandingsEntry(athleteSue, Some(82.01), finished = true, qualified = true),
        StandingsEntry(athleteLis, Some(82.02), finished = true, qualified = true),
        StandingsEntry(athleteTom, Some(82.63), finished = true, qualified = true),
      )
    )),
    slalom = Some(RaceResults(
      "Mount Tofana", refereeJon, List(
        StandingsEntry(athleteJoe, Some(103.24), finished = false, qualified = false),
        StandingsEntry(athleteLis, Some(112.68), finished = true, qualified = true),
        StandingsEntry(athleteSue, None, finished = false, qualified = false),
        StandingsEntry(athleteTom, None, finished = false, qualified = false),
      )
    )),
    giantSlalom = None
  )
}
