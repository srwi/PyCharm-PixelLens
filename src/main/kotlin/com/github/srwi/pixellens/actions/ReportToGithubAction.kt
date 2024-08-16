package com.github.srwi.pixellens.actions

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ReportToGithubAction(text: String, private val exceptionText: String) : NotificationAction(text) {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        val baseUrl = "https://github.com/srwi/PyCharm-PixelLens/issues/new"

        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.github.srwi.pixellens"))
        val pluginVersion = plugin?.version ?: "Unknown"
        val pycharmVersion = ApplicationInfo.getInstance().fullVersion
        val pycharmEdition = ApplicationNamesInfo.getInstance().productName
        val os = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")

        val issueBody = """
        An exception occurred in the plugin.
        
        ### Environment:
        - Plugin Version: $pluginVersion
        - PyCharm Version: $pycharmVersion ($pycharmEdition)
        - Operating System: $os $osVersion
        
        ### Exception details:
        ```js
        
        """.trimIndent() + exceptionText + """
        
        ```
        
        ### Steps to reproduce:
        1. [Please fill in the steps to reproduce the issue]
        
        ### Code snippet:
        ```python
        # Python code to reproduce the issue
        ```
        """.trimIndent()

        val encodedBody = URLEncoder.encode(issueBody, StandardCharsets.UTF_8.toString())
        val issueUrl = "$baseUrl?body=$encodedBody"
        BrowserUtil.browse(issueUrl)

        notification.expire()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}