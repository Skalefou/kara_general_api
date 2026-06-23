package com.kara.kara_general_api.domain.port.output

interface NotificationService {
    fun sendPushNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    )
}
