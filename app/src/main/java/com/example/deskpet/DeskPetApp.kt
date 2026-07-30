package com.example.deskpet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.BuildVersion

class DeskPetApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "desk_pet_overlay"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (BuildVersion.SDK_INT >= BuildVersion.VERSION_Codes.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
               "挄首尔物",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "挄首谤在可收艺可投军"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager.classjava)
            manager.createNotificationChannel(channel)
        }
    }
}