package com.artsiom.footballpulse.ui.presentation

import java.util.Locale

enum class MatchStatusStyle { FINISHED, LIVE, SCHEDULED }
enum class FormResult(val label: String) { WIN("W"), DRAW("D"), LOSS("L") }

fun matchStatusStyle(status: String): MatchStatusStyle = when (status.uppercase(Locale.ROOT)) {
    "FINISHED" -> MatchStatusStyle.FINISHED
    "IN_PLAY", "LIVE", "PAUSED" -> MatchStatusStyle.LIVE
    else -> MatchStatusStyle.SCHEDULED
}
