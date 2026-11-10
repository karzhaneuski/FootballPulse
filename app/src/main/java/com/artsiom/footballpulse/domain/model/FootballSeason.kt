package com.artsiom.footballpulse.domain.model

import java.time.Clock
import java.time.LocalDate
import java.time.Month

object FootballSeason {
    fun current(clock: Clock = Clock.systemDefaultZone()): Int {
        val date = LocalDate.now(clock)
        return if (date.monthValue >= Month.AUGUST.value) date.year else date.year - 1
    }
}
