package com.backend.domain.analysis.service

interface ProgressNotifier {
    fun notify(userId: Long, event: String, message: String)
}
