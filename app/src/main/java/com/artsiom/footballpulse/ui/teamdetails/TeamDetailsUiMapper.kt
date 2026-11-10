package com.artsiom.footballpulse.ui.teamdetails

import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.TeamDetails
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

data class InfoRowUiModel(val label: Int, val value: String)
data class SquadPlayerUiModel(val name: String, val number: String, val nationality: String, val age: String?)
data class SquadGroupUiModel(val label: Int, val players: List<SquadPlayerUiModel>)
data class TeamDetailsUiModel(
    val team: TeamDetails,
    val shortLabel: String,
    val infoRows: List<InfoRowUiModel>,
    val showSquad: Boolean,
    val squadGroups: List<SquadGroupUiModel>
)

fun mapTeamDetails(team: TeamDetails, clock: Clock = Clock.systemDefaultZone()): TeamDetailsUiModel {
    val groups = team.squad.groupBy { positionGroup(it.position) }
    val order = listOf(R.string.goalkeepers, R.string.defenders, R.string.midfielders, R.string.forwards)
    return TeamDetailsUiModel(
        team = team,
        shortLabel = listOfNotNull(team.shortName, team.tla?.let { "($it)" }).joinToString(" "),
        infoRows = buildList {
            team.founded?.let { add(InfoRowUiModel(R.string.founded, it.toString())) }
            team.venue?.let { add(InfoRowUiModel(R.string.venue, it)) }
            team.clubColors?.let { add(InfoRowUiModel(R.string.club_colors, it)) }
        },
        showSquad = team.squad.isNotEmpty(),
        squadGroups = order.mapNotNull { label ->
            groups[label]?.let { players ->
                SquadGroupUiModel(label, players.sortedBy { it.shirtNumber ?: Int.MAX_VALUE }.map { player ->
                    SquadPlayerUiModel(
                        name = player.name,
                        number = player.shirtNumber?.toString() ?: "–",
                        nationality = player.nationality?.let { "${countryFlag(it)} $it" }.orEmpty(),
                        age = calculateAge(player.dateOfBirth, clock)?.toString()
                    )
                })
            }
        }
    )
}

private fun positionGroup(position: String?): Int? = when (position) {
    "Goalkeeper" -> R.string.goalkeepers
    "Centre-Back", "Left-Back", "Right-Back" -> R.string.defenders
    "Defensive Midfield", "Central Midfield", "Attacking Midfield",
    "Left Midfield", "Right Midfield" -> R.string.midfielders
    "Centre-Forward", "Left Winger", "Right Winger", "Second Striker" -> R.string.forwards
    else -> null
}

private fun calculateAge(dateOfBirth: String?, clock: Clock): Int? {
    val birthDate = try {
        dateOfBirth?.let { LocalDate.parse(it) }
    } catch (_: DateTimeParseException) {
        null
    }
    return birthDate?.let { Period.between(it, LocalDate.now(clock)).years }
}

private fun countryFlag(nationality: String?): String = COUNTRY_FLAGS[nationality] ?: "🏳️"

private val COUNTRY_FLAGS = mapOf(
    "Spain" to "🇪🇸",
    "Poland" to "🇵🇱",
    "France" to "🇫🇷",
    "Germany" to "🇩🇪",
    "Brazil" to "🇧🇷",
    "Argentina" to "🇦🇷",
    "Portugal" to "🇵🇹",
    "England" to "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
    "Netherlands" to "🇳🇱",
    "Belgium" to "🇧🇪",
    "Italy" to "🇮🇹",
    "Croatia" to "🇭🇷",
    "Uruguay" to "🇺🇾",
    "Denmark" to "🇩🇰",
    "Sweden" to "🇸🇪",
    "Norway" to "🇳🇴",
    "Switzerland" to "🇨🇭",
    "Austria" to "🇦🇹",
    "Czech Republic" to "🇨🇿",
    "Slovakia" to "🇸🇰",
    "Serbia" to "🇷🇸",
    "Ukraine" to "🇺🇦",
    "Turkey" to "🇹🇷",
    "Morocco" to "🇲🇦",
    "Senegal" to "🇸🇳",
    "Ivory Coast" to "🇨🇮",
    "Ghana" to "🇬🇭",
    "Nigeria" to "🇳🇬",
    "Cameroon" to "🇨🇲",
    "USA" to "🇺🇸",
    "Canada" to "🇨🇦",
    "Mexico" to "🇲🇽",
    "Colombia" to "🇨🇴",
    "Chile" to "🇨🇱",
    "Ecuador" to "🇪🇨",
    "Japan" to "🇯🇵",
    "South Korea" to "🇰🇷",
    "Australia" to "🇦🇺",
    "Scotland" to "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
    "Wales" to "🏴󠁧󠁢󠁷󠁬󠁳󠁿",
    "Ireland" to "🇮🇪",
    "Albania" to "🇦🇱",
    "Slovenia" to "🇸🇮",
    "Hungary" to "🇭🇺",
    "Romania" to "🇷🇴",
    "Bulgaria" to "🇧🇬",
    "Greece" to "🇬🇷",
    "Finland" to "🇫🇮",
    "Russia" to "🇷🇺",
    "Belarus" to "🇧🇾"
)
