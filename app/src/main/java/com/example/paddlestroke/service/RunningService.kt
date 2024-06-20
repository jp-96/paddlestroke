package com.example.paddlestroke.service

import com.example.paddlestroke.repository.DataRepository

interface RunningService {
    fun getDataRepository(): DataRepository
}