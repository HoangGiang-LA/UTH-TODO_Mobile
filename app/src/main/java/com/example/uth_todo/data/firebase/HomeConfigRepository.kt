package com.example.uth_todo.data.firebase

import com.example.uth_todo.model.HomeConfig

fun interface HomeConfigRepository {
    suspend fun load(): HomeConfig
}
