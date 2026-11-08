package com.artsiom.footballpulse

import com.artsiom.footballpulse.data.FootballRepositoryImpl
import com.artsiom.footballpulse.data.remote.RetrofitInstance
import com.artsiom.footballpulse.domain.repository.FootballRepository

class AppContainer {
    val repository: FootballRepository by lazy {
        FootballRepositoryImpl(RetrofitInstance().api)
    }
}
