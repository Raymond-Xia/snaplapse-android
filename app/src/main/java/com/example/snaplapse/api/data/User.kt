package com.example.snaplapse.api.data

data class User(
    val id: Int?,
    val username: String,
    val created: String?,
    val photos: List<String>?,
    val likes: List<String>?
)
