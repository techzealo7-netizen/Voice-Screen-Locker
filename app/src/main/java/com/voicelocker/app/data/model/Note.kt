package com.voicelocker.app.data.model

data class Note(
    val id: String,
    var title: String,
    var text: String,
    var summary: String? = null,
    val created: Long = System.currentTimeMillis()
)

