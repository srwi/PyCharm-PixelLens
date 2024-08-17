package com.github.srwi.pixellens.listeners

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.icons.ImageViewerIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import java.awt.Desktop
import java.net.URI
import java.time.LocalDateTime

internal class ExpandedNotification(groupId: String, title: String, content: String, type: NotificationType)
    : Notification(groupId, title, content, type), NotificationFullContent

internal class PluginActivationListener : ApplicationActivationListener {
    companion object {
        const val REMINDER_AFTER_N_DAYS: Long = 30
    }

    override fun applicationActivated(ideFrame: IdeFrame) {
        trackFirstUsage()

        if (is30DaysAfterFirstUse() && !hasNotificationBeenShown()) {
            showRateNotification()
            setNotificationShown()
        }
    }

    private fun trackFirstUsage() {
        if (UserSettings.firstUseDate == null) {
            UserSettings.firstUseDate = LocalDateTime.now()
        }
    }

    private fun is30DaysAfterFirstUse(): Boolean {
        val firstUseDate = UserSettings.firstUseDate ?: return false
        return LocalDateTime.now() > firstUseDate.plusDays(REMINDER_AFTER_N_DAYS)
    }

    private fun hasNotificationBeenShown(): Boolean {
        return UserSettings.supportReminderShown
    }

    private fun setNotificationShown() {
        UserSettings.supportReminderShown = true
    }

    private fun showRateNotification() {
        val notification = ExpandedNotification(
            "notificationGroup.sticky",
            "Thank you for using PixelLens!",
            "<p style=\"padding-bottom: 10px\">It's been over a month since you began using PixelLens. If you're enjoying it, I'd be truly grateful for your support.</p>" +
                    "<p style=\"padding-bottom: 10px\">You can show your appreciation by leaving a rating on the marketplace, giving a star on GitHub, or, if you're feeling especially generous, making a donation.</p>" +
                    "<p style=\"padding-bottom: 10px\">Thank you!</p>" +
                    "<p>(This message will not be shown again)</p>",
            NotificationType.INFORMATION
        )
        notification.addAction(NotificationAction.createSimple("Rate") {
            val url = "https://plugins.jetbrains.com/plugin/25039-pixellens/reviews"
            Desktop.getDesktop().browse(URI(url))
        })
        notification.addAction(NotificationAction.createSimple("Star on GitHub") {
            val url = "https://github.com/srwi/PyCharm-PixelLens"
            Desktop.getDesktop().browse(URI(url))
        })
        notification.addAction(NotificationAction.createSimple("Donate") {
            val url = "https://www.paypal.com/paypalme/rumswinkel"
            Desktop.getDesktop().browse(URI(url))
        })
        notification.icon = ImageViewerIcons.Heart

        Notifications.Bus.notify(notification)
    }
}
