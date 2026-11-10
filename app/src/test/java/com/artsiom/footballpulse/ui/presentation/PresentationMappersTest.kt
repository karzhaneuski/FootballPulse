package com.artsiom.footballpulse.ui.presentation

import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.FootballSeason
import com.artsiom.footballpulse.domain.model.LeagueZone
import com.artsiom.footballpulse.domain.model.Leagues
import com.artsiom.footballpulse.domain.model.SquadPlayer
import com.artsiom.footballpulse.testing.match
import com.artsiom.footballpulse.testing.matchDetail
import com.artsiom.footballpulse.testing.standing
import com.artsiom.footballpulse.testing.team
import com.artsiom.footballpulse.ui.matchdetails.mapMatchDetails
import com.artsiom.footballpulse.ui.matches.MatchListItem
import com.artsiom.footballpulse.ui.matches.mapMatches
import com.artsiom.footballpulse.ui.standings.mapStandings
import com.artsiom.footballpulse.ui.teamdetails.mapTeamDetails
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationMappersTest {
    private val utc = ZoneOffset.UTC
    private val clock = Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), utc)

    @Test
    fun `matches are grouped by local calendar day and headers sorted chronologically`() {
        val items = mapMatches(listOf(
            match.copy(id = 2, date = "2025-08-11T23:30:00Z"),
            match.copy(id = 1, date = "2025-08-10T23:30:00Z"),
            match.copy(id = 3, date = "2025-08-11T12:00:00Z")
        ), ZoneId.of("Europe/Warsaw"))
        assertEquals(listOf("11 August 2025", "12 August 2025"),
            items.filterIsInstance<MatchListItem.DateHeader>().map { it.dateLabel })
        assertEquals(listOf(1, 3, 2), items.filterIsInstance<MatchListItem.MatchCard>().map { it.card.match.id })
    }

    @Test
    fun `match cards format local time score and live status`() {
        val card = (mapMatches(listOf(match.copy(status = "PAUSED")), ZoneId.of("Europe/Warsaw"))[1]
            as MatchListItem.MatchCard).card
        assertEquals("14:00", card.timeLabel)
        assertEquals("2 - 1", card.scoreLabel)
        assertEquals(MatchStatusStyle.LIVE, card.statusStyle)
    }

    @Test
    fun `matches preserve input order within a date group`() {
        val items = mapMatches(listOf(match.copy(id = 2), match.copy(id = 1)), utc)
        assertEquals(listOf(2, 1), items.filterIsInstance<MatchListItem.MatchCard>().map { it.card.match.id })
    }

    @Test
    fun `invalid match dates get unknown header and retain raw time label`() {
        val items = mapMatches(listOf(match.copy(date = "invalid", homeScore = null)), utc)
        assertNull((items[0] as MatchListItem.DateHeader).dateLabel)
        val card = (items[1] as MatchListItem.MatchCard).card
        assertEquals("invalid", card.timeLabel)
        assertEquals("- : -", card.scoreLabel)
    }

    @Test
    fun `empty matches produce no headers`() {
        assertTrue(mapMatches(emptyList(), utc).isEmpty())
    }

    @Test
    fun `ISO timestamp offsets and daylight saving are respected`() {
        val dates = UiDates(ZoneId.of("Europe/Warsaw"))
        assertEquals("01:30", dates.timeLabel("2025-03-30T00:30:00Z"))
        assertEquals("03:30", dates.timeLabel("2025-03-30T01:30:00Z"))
        assertEquals("10 August 2025", dates.dateLabel("2025-08-11T00:30:00+03:00"))
    }

    @Test
    fun `match details preserve completed score date and both score rows`() {
        val model = mapMatchDetails(matchDetail, utc)
        assertEquals("2 - 1", model.scoreOrTime)
        assertEquals("10 August 2025", model.dateLabel)
        assertEquals(MatchStatusStyle.FINISHED, model.statusStyle)
        assertEquals(listOf(R.string.first_half, R.string.full_time), model.scores.map { it.label })
        assertEquals(listOf("1", "2"), model.scores.map { it.home })
        assertEquals(listOf("0", "1"), model.scores.map { it.away })
        assertFalse(model.notStarted)
    }

    @Test
    fun `scheduled details display kickoff and no score breakdown`() {
        val model = mapMatchDetails(matchDetail.copy(status = "TIMED", homeScore = null, awayScore = null), utc)
        assertEquals("12:00", model.scoreOrTime)
        assertTrue(model.notStarted)
        assertTrue(model.scores.isEmpty())
    }

    @Test
    fun `missing half time is omitted and missing full time is shown as dashes`() {
        val model = mapMatchDetails(matchDetail.copy(halfTimeHome = null, fullTimeAway = null), utc)
        assertEquals(1, model.scores.size)
        assertEquals(R.string.full_time, model.scores.single().label)
        assertEquals("-", model.scores.single().away)
    }

    @Test
    fun `malformed detail date retains raw label and score placeholder`() {
        val model = mapMatchDetails(matchDetail.copy(date = "bad", homeScore = null), utc)
        assertEquals("bad", model.dateLabel)
        assertEquals("- : -", model.scoreOrTime)
    }

    @Test
    fun `live and scheduled status classification preserves existing badge rules`() {
        assertEquals(MatchStatusStyle.LIVE, matchStatusStyle("in_play"))
        assertEquals(MatchStatusStyle.LIVE, matchStatusStyle("LIVE"))
        assertEquals(MatchStatusStyle.SCHEDULED, matchStatusStyle("POSTPONED"))
    }

    @Test
    fun `18 team leagues have two direct relegations and a separate playoff`() {
        for (code in listOf("BL1", "FL1")) {
            val league = Leagues.forCode(code)
            assertEquals(34, league.maxMatchdays)
            assertEquals(LeagueZone.NONE, league.zoneAt(15))
            assertEquals(LeagueZone.PLAYOFF, league.zoneAt(16))
            assertEquals(LeagueZone.RELEGATION, league.zoneAt(17))
            assertEquals(LeagueZone.RELEGATION, league.zoneAt(18))
        }
    }

    @Test
    fun `20 team leagues relegate positions 18 to 20 with no playoff`() {
        for (code in listOf("PL", "PD", "SA")) {
            val league = Leagues.forCode(code)
            assertEquals(38, league.maxMatchdays)
            assertEquals(LeagueZone.NONE, league.zoneAt(17))
            assertEquals(listOf(LeagueZone.RELEGATION, LeagueZone.RELEGATION, LeagueZone.RELEGATION),
                (18..20).map { league.zoneAt(it) })
        }
    }

    @Test
    fun `standard European allocations distinguish league places from English cup places`() {
        for (league in Leagues.ALL) {
            assertEquals(LeagueZone.UCL, league.zoneAt(1))
            assertEquals(LeagueZone.UCL, league.zoneAt(4))
            assertEquals(LeagueZone.UEL, league.zoneAt(5))
            assertEquals(if (league.code == "PL") LeagueZone.NONE else LeagueZone.UECL, league.zoneAt(6))
        }
    }

    @Test
    fun `season overrides overlay standard zones and can explicitly clear a place`() {
        val league = Leagues.forCode("PL").copy(seasonZoneOverrides = mapOf(
            2026 to mapOf(5 to LeagueZone.UCL, 6 to LeagueZone.UEL, 18 to LeagueZone.NONE)
        ))
        assertEquals(LeagueZone.UCL, league.zoneAt(5, 2026))
        assertEquals(LeagueZone.UEL, league.zoneAt(6, 2026))
        assertEquals(LeagueZone.NONE, league.zoneAt(18, 2026))
        assertEquals(LeagueZone.RELEGATION, league.zoneAt(20, 2026))
        assertEquals(LeagueZone.UEL, league.zoneAt(5, 2025))
        assertTrue(Leagues.ALL.all { it.seasonZoneOverrides.isEmpty() })
    }

    @Test
    fun `standings map configured positions independently of a partial response size`() {
        val rows = mapStandings(listOf(standing.copy(position = 16), standing.copy(position = 17)),
            Leagues.forCode("BL1"), 2025)
        assertEquals(listOf(LeagueZone.PLAYOFF, LeagueZone.RELEGATION), rows.map { it.zone })
    }

    @Test
    fun `standings format signed goal difference and filter pad last five results`() {
        val row = mapStandings(listOf(standing.copy(goalDifference = 4, form = "L,W,D,X,L,W,W,W"))).single()
        assertEquals("+4", row.goalDifference)
        assertEquals("5:1", row.goals)
        assertEquals(listOf(FormResult.DRAW, FormResult.LOSS, FormResult.WIN, FormResult.WIN, FormResult.WIN), row.form)
        assertEquals("7", row.points)
    }

    @Test
    fun `missing form pads empty slots and nonpositive differences have no plus`() {
        val rows = mapStandings(listOf(standing.copy(goalDifference = 0, form = null),
            standing.copy(goalDifference = -2, form = "W")))
        assertEquals(List<FormResult?>(5) { null }, rows[0].form)
        assertEquals("0", rows[0].goalDifference)
        assertEquals("-2", rows[1].goalDifference)
        assertEquals(listOf(null, null, null, null, FormResult.WIN), rows[1].form)
    }

    @Test
    fun `team mapper orders normalized groups and sorts missing shirt numbers last`() {
        val players = listOf(player("Forward", "Centre-Forward", 9), player("Unknown", "Other", 1),
            player("Keeper", "Goalkeeper", null), player("Back 2", "Centre-Back", 5),
            player("Back 1", "Left-Back", 2), player("Mid", "Central Midfield", 8))
        val model = mapTeamDetails(team.copy(squad = players), clock)
        assertEquals(listOf(R.string.goalkeepers, R.string.defenders, R.string.midfielders, R.string.forwards),
            model.squadGroups.map { it.label })
        assertEquals(listOf("Back 1", "Back 2"), model.squadGroups[1].players.map { it.name })
        assertEquals("–", model.squadGroups[0].players.single().number)
    }

    @Test
    fun `player age is fixed by injected clock before and on birthday`() {
        val p = player("Player", "Goalkeeper", 1).copy(dateOfBirth = "2000-05-10")
        val before = mapTeamDetails(team.copy(squad = listOf(p)), clock)
        assertEquals("25", before.squadGroups.single().players.single().age)
        val birthday = Clock.fixed(Instant.parse("2026-05-10T00:00:00Z"), utc)
        val onBirthday = mapTeamDetails(team.copy(squad = listOf(p)), birthday)
        assertEquals("26", onBirthday.squadGroups.single().players.single().age)
    }

    @Test
    fun `invalid and absent birth dates hide age and nullable nationality is blank`() {
        val players = listOf(player("A", "Goalkeeper", 1).copy(dateOfBirth = "2000-02-30", nationality = null),
            player("B", "Goalkeeper", 2).copy(dateOfBirth = null))
        val mapped = mapTeamDetails(team.copy(squad = players), clock).squadGroups.single().players
        assertNull(mapped[0].age)
        assertNull(mapped[1].age)
        assertEquals("", mapped[0].nationality)
        assertEquals("🇪🇸 Spain", mapped[1].nationality)
    }

    @Test
    fun `unknown nationality retains its name with existing fallback flag`() {
        val p = player("A", "Goalkeeper", 1).copy(nationality = "Unknown")
        assertEquals("🏳️ Unknown", mapTeamDetails(team.copy(squad = listOf(p)), clock)
            .squadGroups.single().players.single().nationality)
    }

    @Test
    fun `unrecognized positions preserve squad card visibility without adding a new group`() {
        val model = mapTeamDetails(team.copy(squad = listOf(player("Other", "Other", 1))), clock)
        assertTrue(model.showSquad)
        assertTrue(model.squadGroups.isEmpty())
        assertFalse(mapTeamDetails(team, clock).showSquad)
    }

    @Test
    fun `team information and short label omit absent values`() {
        val model = mapTeamDetails(team.copy(shortName = null, tla = null, founded = null, venue = null), clock)
        assertEquals("", model.shortLabel)
        assertEquals(listOf(R.string.club_colors), model.infoRows.map { it.label })
        assertEquals("Blue", model.infoRows.single().value)
        assertTrue(model.squadGroups.isEmpty())
        assertEquals("H (HOM)", mapTeamDetails(team, clock).shortLabel)
    }

    @Test
    fun `football season changes in August using clock timezone`() {
        assertEquals(2025, FootballSeason.current(clock))
        assertEquals(2026, FootballSeason.current(Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), utc)))
    }

    private fun player(name: String, position: String, number: Int?) =
        SquadPlayer(1, name, position, "Spain", number, "2000-01-01")
}
