package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
enum class ParticipantStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}