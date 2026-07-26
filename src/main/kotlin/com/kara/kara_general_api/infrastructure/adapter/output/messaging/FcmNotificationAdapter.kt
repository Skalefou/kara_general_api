package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.kara.kara_general_api.domain.port.output.NotificationService
import org.springframework.stereotype.Component

@Component
class FcmNotificationAdapter(
    firebaseApp: FirebaseApp,
) : NotificationService {
    private val messaging = FirebaseMessaging.getInstance(firebaseApp)

    override fun sendPushNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        val message =
            Message
                .builder()
                .setToken(token)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                ).putAllData(data)
                .build()

        messaging.send(message)
    }
}
