package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*


enum class SidebarType {
    BatchSidebar,
    ChannelSidebar
}


class Sidebar {
    private val panel = JPanel(CardLayout())
    private val batchPanel: JPanel
    private val channelPanel: JPanel
    private val batchList: JBList<Int>
    private val channelList: JBList<Any>
    private var onBatchIndexChanged: ((Int) -> Unit)? = null
    private var onChannelIndexChanged: ((Int?) -> Unit)? = null
    private var hasRgbChannels: Boolean = false
    private val allChannelsLabel: String = "All"

    init {
        batchList = createBatchList()
        channelList = createChannelList()

        batchPanel = createSubPanel("Layer", batchList)
        channelPanel = createSubPanel("Channel", channelList)

        panel.border = BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.LIGHT_GRAY)
        panel.add(batchPanel, SidebarType.BatchSidebar.name)
        panel.add(channelPanel, SidebarType.ChannelSidebar.name)
    }

    private fun createBatchList(): JBList<Int> {
        val listModel = DefaultListModel<Int>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener { notifySelectedBatchIndexChanged() }
        return list
    }

    private fun createChannelList(): JBList<Any> {
        val listModel = DefaultListModel<Any>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener { notifySelectedChannelIndexChanged() }
        return list
    }

    private fun createSubPanel(title: String, list: JList<*>): JPanel {
        val titleLabel = SimpleColoredComponent().apply {
            append(title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        }
        val scrollPane = JBScrollPane(list).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        return JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun getComponent(): JComponent = panel

    fun showPanel(type: SidebarType) {
        (panel.layout as CardLayout).show(panel, type.name)
    }

    fun updateChannelList(channels: Int) {
        val hasRgbChannels = channels == 3 || channels == 4
        channelList.model = DefaultListModel<Any>().apply {
            if (hasRgbChannels) addElement(allChannelsLabel)
            for (i in 0 until channels) addElement(i)
        }
        if (channelList.model.size > 0) {
            channelList.selectedIndex = 0
        }
    }

    fun updateBatchList(batches: Int) {
        batchList.model = DefaultListModel<Int>().apply {
            for (i in 0 until batches) addElement(i)
        }
        if (batchList.model.size > 0) {
            batchList.selectedIndex = 0
        }
    }

    fun setSelectedBatchIndex(index: Int) {
        batchList.selectedIndex = index
    }

    fun setSelectedChannelIndex(index: Int?) {
        if (index == null) {
            channelList.selectedIndex = 0
        } else {
            channelList.selectedIndex = index + if (hasRgbChannels) 1 else 0
        }
    }

    private fun notifySelectedChannelIndexChanged() {
        if (channelList.selectedValue != null) {
            val newValue = if (channelList.selectedValue == allChannelsLabel) null else channelList.selectedValue as Int
            onChannelIndexChanged?.invoke(newValue)
        }
    }

    private fun notifySelectedBatchIndexChanged() {
        if (batchList.selectedValue != null) {
            val newValue = batchList.selectedValue
            onBatchIndexChanged?.invoke(newValue)
        }
    }

    fun onBatchIndexChanged(listener: (Int) -> Unit) {
        onBatchIndexChanged = listener
    }

    fun onChannelIndexChanged(listener: (Int?) -> Unit) {
        onChannelIndexChanged = listener
    }
}